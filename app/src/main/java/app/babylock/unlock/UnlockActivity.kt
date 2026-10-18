package app.babylock.unlock

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.babylock.lock.LockController
import app.babylock.settings.Prefs
import app.babylock.ui.theme.BabyLockTheme
import kotlin.random.Random

/**
 * Tela de confirmação do adulto. Abre por cima do vídeo só depois do gesto do canto.
 * Ordem: biometria/credencial do aparelho -> PIN do BabyLock -> conta de multiplicar (se não houver nada).
 * Se o adulto não confirmar em 20 s, cancelar ou sair, o bloqueio volta.
 */
class UnlockActivity : FragmentActivity() {

    private val main = Handler(Looper.getMainLooper())
    private val timeout = Runnable { giveUp() }
    private var done = false
    private var authInProgress = false
    private lateinit var prompt: BiometricPrompt

    private var pinError by mutableStateOf<String?>(null)
    private val challenge = Random.nextInt(6, 10) to Random.nextInt(6, 10)
    private val gate: UnlockGate by lazy {
        sharedGate?.takeIf { Prefs.hasPin } ?: UnlockGate(Prefs::checkPin, { Prefs.hasPin }, challenge)
            .also { if (Prefs.hasPin) sharedGate = it }
    }

    private val authenticators: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }

    private val canUseSystemAuth: Boolean
        get() = BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = giveUp()
        })
        setContent {
            BabyLockTheme {
                UnlockScreen(
                    canUseSystemAuth = canUseSystemAuth,
                    hasPin = Prefs.hasPin,
                    challenge = challenge,
                    error = pinError,
                    onSystemAuth = ::showPrompt,
                    onSubmit = ::checkAnswer,
                    onCancel = ::giveUp,
                )
            }
        }
        prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authInProgress = false
                    success()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Cancelou a biometria: continua nesta tela para usar o PIN, até o tempo acabar.
                    authInProgress = false
                }
            },
        )
        main.postDelayed(timeout, TIMEOUT_MS)
        if (canUseSystemAuth) showPrompt()
    }

    private fun showPrompt() {
        authInProgress = true
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear o BabyLock")
            .setSubtitle("Confirme que é um adulto")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    private fun checkAnswer(answer: String) {
        pinError = when (val r = gate.submit(answer)) {
            UnlockGate.Result.Ok -> { success(); null }
            UnlockGate.Result.Wrong -> "Não confere. Tente de novo."
            is UnlockGate.Result.LockedOut -> "Muitas tentativas. Aguarde ${r.secondsLeft} s."
        }
    }

    private fun success() {
        if (done) return
        done = true
        main.removeCallbacks(timeout)
        window.decorView.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
        )
        LockController.unlock(this)
        finish()
    }

    private fun giveUp() {
        if (done) return
        done = true
        main.removeCallbacks(timeout)
        LockController.relock()
        finish()
    }

    override fun onStop() {
        super.onStop()
        // Saiu desta tela sem confirmar (Home, troca de app, tela apagou): trava de novo.
        // Exceção: a tela de senha do próprio sistema (Android 9 e anteriores) também para esta Activity.
        if (!isChangingConfigurations && !authInProgress) giveUp()
    }

    override fun onDestroy() {
        main.removeCallbacks(timeout)
        super.onDestroy()
    }

    companion object {
        private const val TIMEOUT_MS = 20_000L

        // Fica no companion para o limite de tentativas valer mesmo se a tela for reaberta.
        private var sharedGate: UnlockGate? = null
    }
}
