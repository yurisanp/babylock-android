package app.babylock

import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import app.babylock.ads.Ads
import app.babylock.lock.LockAccessibilityService
import app.babylock.lock.LockController
import app.babylock.settings.Prefs
import app.babylock.ui.MainActivity
import org.junit.After
import java.util.regex.Pattern

/**
 * Ajudantes comuns dos testes de ponta a ponta no emulador.
 * Os textos vêm dos recursos do app, então os testes passam em qualquer idioma do aparelho.
 */
abstract class EndToEndBase {

    protected val instrumentation = InstrumentationRegistry.getInstrumentation()
    protected val context: Context = instrumentation.targetContext

    // Sem esta flag, o UiAutomator desliga os outros serviços de acessibilidade
    // (inclusive o do BabyLock) assim que se conecta.
    protected val device: UiDevice = run {
        Configurator.getInstance().setUiAutomationFlags(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        UiDevice.getInstance(instrumentation)
    }

    protected val pin = "1357"

    init {
        // Sem anúncios nos testes: a tela fica igual a cada execução (e as capturas da Play saem limpas).
        Ads.suppressed = true
    }

    protected fun prepareDevice() {
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        Prefs.init(context)
        Prefs.setPin(pin)
        Prefs.holdSeconds = 2
        Prefs.lockVolume = true
        Prefs.closeShade = true
        Prefs.onboardingDone = true
    }

    @After
    fun cleanUpBase() {
        instrumentation.runOnMainSync {
            LockController.cancelScheduled()
            LockController.unlock(context)
        }
        Prefs.clearPin()
        TouchProbeActivity.current?.finish()
    }

    protected fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    protected fun enableAccessibilityService() {
        val component = "${context.packageName}/${LockAccessibilityService::class.java.name}"
        device.executeShellCommand("settings put secure enabled_accessibility_services $component")
        device.executeShellCommand("settings put secure accessibility_enabled 1")
        waitUntil("o serviço de acessibilidade ligar", timeoutMs = 15_000) { LockAccessibilityService.instance != null }
    }

    protected fun disableAccessibilityService() {
        device.executeShellCommand("settings delete secure enabled_accessibility_services")
        waitUntil("o serviço de acessibilidade desligar", timeoutMs = 15_000) { LockAccessibilityService.instance == null }
    }

    protected fun setOverlayPermission(allowed: Boolean) {
        val mode = if (allowed) "allow" else "default"
        device.executeShellCommand("appops set ${context.packageName} SYSTEM_ALERT_WINDOW $mode")
    }

    protected fun openApp() {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 5_000)
        device.waitForIdle()
    }

    protected fun openProbe() {
        TouchProbeActivity.touches.set(0)
        val intent = Intent(context, TouchProbeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        waitUntil("a tela de teste abrir") { TouchProbeActivity.current != null }
        device.waitForIdle()
        TouchProbeActivity.touches.set(0)
    }

    protected fun lockNow() {
        var ok = false
        instrumentation.runOnMainSync { ok = LockController.lockNow(context) }
        check(ok) { "Não travou: nenhuma permissão" }
        device.waitForIdle()
        SystemClock.sleep(700)
    }

    protected fun tapCenterTimes(n: Int) {
        repeat(n) { device.click(device.displayWidth / 2, device.displayHeight / 2) }
        SystemClock.sleep(500)
    }

    /** Ponto dentro do canto (96 dp da camada), abaixo da barra de status no modo básico. */
    protected fun corner(): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density
        return device.displayWidth - (24 * density).toInt() to (60 * density).toInt()
    }

    /** Segura o canto superior direito com um dedo por mais que [Prefs.holdSeconds]. */
    protected fun holdCorner() {
        val (x, y) = corner()
        // Cada passo do swipe leva ~5 ms: 600 passos ≈ 3 s.
        device.swipe(x, y, x, y, 600)
    }

    protected fun byTag(tag: String, timeoutMs: Long = 5_000): UiObject2 =
        device.wait(Until.findObject(By.res(tag)), timeoutMs)
            ?: throw AssertionError("Não achei o elemento \"$tag\" na tela. ${screenSummary()}")

    /** O que está na tela agora, para entender uma falha sem o logcat. */
    protected fun screenSummary(): String {
        val ids = device.findObjects(By.res(Pattern.compile(".+"))).mapNotNull { it.resourceName }.distinct().take(25)
        val texts = device.findObjects(By.text(Pattern.compile(".+"))).mapNotNull { it.text }.distinct().take(10)
        return "Pacote: ${device.currentPackageName}; ids: $ids; textos: $texts"
    }

    /** Faz o gesto e digita o PIN na tela de confirmação. */
    protected fun unlockWithPin(code: String = pin) {
        holdCorner()
        byTag("unlock_answer").text = code
        val confirm = device.wait(Until.findObject(By.res("unlock_confirm").enabled(true)), 3_000)
            ?: throw AssertionError("O botão Confirmar não ficou ativo")
        confirm.click()
    }

    protected fun waitUntil(what: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < end) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Tempo esgotado esperando $what")
    }
}
