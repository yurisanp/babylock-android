package app.babylock.lock

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import app.babylock.settings.Prefs

/**
 * Motor principal. O overlay de acessibilidade fica acima das barras do sistema,
 * o filtro de teclas segura o volume e a lista de janelas mostra quando a barra de notificações abriu.
 * Da tela, o serviço só olha o TIPO e o foco das janelas; nunca lê textos nem o conteúdo dos apps.
 */
class LockAccessibilityService : AccessibilityService(), LockSurface {

    private var overlay: LockOverlayView? = null
    private var visible = false
    private val main = Handler(Looper.getMainLooper())

    /** Quantas tentativas seguidas de fechar a barra já foram feitas (0 = barra fechada). */
    private var attempts = 0
    /** "Voltar" já enviados nesta abertura da barra (no máximo um, para nunca fechar o vídeo). */
    private var backs = 0
    private var openSince = 0L
    private var checkScheduled = false
    private val check = Runnable {
        checkScheduled = false
        enforceShadeClosed()
    }

    // Rede de segurança: nem todo sistema avisa quando a barra abre (ex.: HyperOS).
    // Conferir a lista de janelas é barato e só age quando a barra está aberta.
    private val watchdog = object : Runnable {
        override fun run() {
            if (!visible || !Prefs.closeShade) return
            enforceShadeClosed()
            main.postDelayed(this, WATCHDOG_MS)
        }
    }

    // Botão de acessibilidade do Android (na barra de navegação ou flutuante): trava com um toque.
    private val buttonCallback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) = onButtonClicked()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        accessibilityButtonController.registerAccessibilityButtonCallback(buttonCallback, main)
        updateButton(locked = LockController.locked.value)
    }

    /**
     * O adulto tocou no botão de acessibilidade: trava na hora, por cima do que estiver aberto.
     * Travado, o toque é ignorado (a criança não consegue destravar nem desligar nada por ele).
     */
    internal fun onButtonClicked() {
        if (LockController.locked.value) return
        // No Android 11+ o botão só existe se o adulto o colocou nas Configurações: isso já é o "ligar".
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || Prefs.accessibilityButton) LockController.lockNow(this)
    }

    /** Liga ou desliga o botão conforme a configuração. Some enquanto travado, para não ficar sobre o vídeo. */
    fun refreshButton() = updateButton(locked = LockController.locked.value)

    private fun updateButton(locked: Boolean) {
        val info = serviceInfo ?: return
        // Até o Android 10 este pedido mostra ou esconde o botão. Do 11 em diante quem mostra o botão é o
        // adulto, nas Configurações de acessibilidade; o pedido fica sempre ligado para o toque chegar aqui
        // (sem ele, tocar no botão desligaria o serviço).
        val want = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) true else Prefs.accessibilityButton && !locked
        val flags = if (want) {
            info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON
        } else {
            info.flags and AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON.inv()
        }
        if (flags != info.flags) {
            info.flags = flags
            serviceInfo = info
        }
    }

    override fun showOverlay() {
        val wm = getSystemService(WindowManager::class.java)
        val view = overlay ?: LockOverlayView(this, Prefs.holdSeconds * 1000L, Prefs.showCornerHint) {
            LockController.requestUnlock(this)
        }.also { overlay = it }
        if (view.parent == null) {
            wm.addView(view, lockWindowParams(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY))
        }
        visible = true
        attempts = 0
        backs = 0
        updateButton(locked = true)
        view.flashHint()
        main.removeCallbacks(watchdog)
        if (Prefs.closeShade) {
            enforceShadeClosed()
            main.postDelayed(watchdog, WATCHDOG_MS)
        }
    }

    override fun hideOverlay() {
        visible = false
        main.removeCallbacks(check)
        main.removeCallbacks(watchdog)
        checkScheduled = false
        overlay?.let { if (it.parent != null) getSystemService(WindowManager::class.java).removeView(it) }
    }

    override fun release() {
        hideOverlay()
        overlay = null
        updateButton(locked = false)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!visible || !Prefs.closeShade || event == null) return
        if (event.packageName == SYSTEM_UI) systemUiEvents++
        // A barra de notificações e as Configurações rápidas são da System UI. Qualquer sinal dela
        // (ou de mudança nas janelas) dispara uma conferência; a conferência só age se a barra abriu.
        if (event.packageName == SYSTEM_UI || event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            enforceShadeClosed()
        }
    }

    /**
     * Se a barra (ou outra tela do sistema) estiver aberta por cima do vídeo, fecha.
     * 1ª tentativa: o pedido oficial de fechar a barra. Se ela continuar aberta, "voltar":
     * com a barra em foco, o "voltar" vai para ela e não chega ao vídeo.
     */
    private fun enforceShadeClosed() {
        if (!visible || !Prefs.closeShade) return
        when (shadeState()) {
            ShadeState.CLOSED -> {
                attempts = 0
                backs = 0
                return
            }
            ShadeState.UNKNOWN -> {
                // Sem a lista de janelas: pede para fechar às cegas (não faz nada se já estiver fechada).
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dismissShade()
                return
            }
            ShadeState.OPEN -> Unit
        }
        attempts++
        shadeOpenings++
        val now = SystemClock.uptimeMillis()
        if (attempts == 1) {
            openSince = now
            dismissShade()
        } else if (backs == 0 && now - openSince >= BACK_AFTER_MS) {
            // O pedido oficial teve tempo de agir e a barra continua aberta: agora sim, "voltar".
            // Só um "voltar" por abertura: a lista de janelas demora a atualizar depois que a barra fecha,
            // e um "voltar" a mais fecharia o vídeo. Se a barra continuar aberta, ela fica por baixo da
            // camada de bloqueio (a criança não mexe nela) e o adulto recebe o aviso na tela inicial.
            backs++
            backPresses++
            performGlobalAction(GLOBAL_ACTION_BACK)
            if (backs == 1) Prefs.shadeEscaped = true // avisa o adulto na tela inicial
        }
        if (!checkScheduled) {
            checkScheduled = true
            main.postDelayed(check, RECHECK_MS)
        }
    }

    private enum class ShadeState { OPEN, CLOSED, UNKNOWN }

    private fun shadeState(): ShadeState {
        val list = try {
            windows
        } catch (_: RuntimeException) {
            null
        }
        if (list.isNullOrEmpty()) return ShadeState.UNKNOWN
        // A barra aberta é uma janela do sistema com foco. A barra de status fechada nunca tem foco.
        val open = list.any { it.type == AccessibilityWindowInfo.TYPE_SYSTEM && (it.isFocused || it.isActive) }
        return if (open) ShadeState.OPEN else ShadeState.CLOSED
    }

    // Público para o teste no emulador poder chamar direto (no Android ele é protected).
    public override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!visible || !Prefs.lockVolume) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE -> true
            else -> false
        }
    }

    private fun dismissShade() {
        dismissCalls++
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            lastDismissResult = performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            // Alguns sistemas recusam a ação; tenta o pedido antigo, que o sistema pode negar.
            if (lastDismissResult == false) closeSystemDialogs()
        } else {
            closeSystemDialogs()
        }
    }

    // Abaixo do Android 12 qualquer app pode pedir isso. A partir dele o sistema pode negar
    // (SecurityException), e aí a barra fica aberta, mas por baixo da camada de bloqueio.
    @Suppress("DEPRECATION", "MissingPermission")
    private fun closeSystemDialogs() {
        try {
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        } catch (_: SecurityException) {
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        accessibilityButtonController.unregisterAccessibilityButtonCallback(buttonCallback)
        release()
        if (instance === this) instance = null
        LockController.onSurfaceLost(this, this)
        super.onDestroy()
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"
        private const val RECHECK_MS = 350L
        private const val WATCHDOG_MS = 500L
        private const val BACK_AFTER_MS = 700L

        @Volatile
        var instance: LockAccessibilityService? = null
            private set

        // Contadores para diagnóstico nos testes (baratos, sem dados do usuário).
        @Volatile internal var dismissCalls = 0
        @Volatile internal var systemUiEvents = 0
        @Volatile internal var lastDismissResult: Boolean? = null
        @Volatile internal var shadeOpenings = 0
        @Volatile internal var backPresses = 0
    }
}
