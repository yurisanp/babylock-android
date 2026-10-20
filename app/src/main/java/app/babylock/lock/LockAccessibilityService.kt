package app.babylock.lock

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
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
    private var bubble: LockBubbleView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
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

    // Atalho de acessibilidade do Android (botão flutuante, canto, teclas de volume): trava com um toque.
    // Só chega aqui porque o serviço pede o botão em xml-v30/accessibility_service_config.xml.
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
     * O adulto tocou no botão de travar (o flutuante do BabyLock ou o atalho de acessibilidade do sistema):
     * trava na hora, por cima do que estiver aberto. Travado, o toque é ignorado.
     */
    internal fun onButtonClicked() {
        if (!LockController.locked.value) LockController.lockNow(this)
    }

    /** Mostra ou esconde o botão flutuante conforme a configuração. Some enquanto travado. */
    fun refreshButton() = updateButton(locked = LockController.locked.value)

    internal val bubbleShown: Boolean get() = bubble?.parent != null

    /** Centro do botão flutuante na tela (para o teste no emulador tocar nele). */
    internal fun bubbleCenter(): Pair<Int, Int>? {
        val b = bubble?.takeIf { it.parent != null && it.width > 0 } ?: return null
        val loc = IntArray(2).also { b.getLocationOnScreen(it) }
        return loc[0] + b.width / 2 to loc[1] + b.height / 2
    }

    private fun updateButton(locked: Boolean) {
        val want = Prefs.accessibilityButton && !locked
        val wm = getSystemService(WindowManager::class.java)
        val view = bubble
        if (want && (view == null || view.parent == null)) {
            val b = view ?: LockBubbleView(
                this,
                onTap = ::onButtonClicked,
                onDrag = { dx, dy -> moveBubble(dx, dy) },
                onDrop = ::dropBubble,
            ).also { bubble = it }
            val params = bubbleParams ?: newBubbleParams().also { bubbleParams = it }
            clampBubble(params)
            try {
                wm.addView(b, params)
            } catch (_: RuntimeException) {
                // Sem janela agora (serviço sendo desligado): tenta de novo na próxima vez.
            }
        } else if (!want && view?.parent != null) {
            wm.removeView(view)
        }
    }

    private fun bubbleSize() = (56 * resources.displayMetrics.density).toInt()

    private fun newBubbleParams() = WindowManager.LayoutParams(
        bubbleSize(),
        bubbleSize(),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        title = "BabyLock botão"
        gravity = Gravity.TOP or Gravity.START
        val dm = resources.displayMetrics
        x = Prefs.bubbleX.takeIf { it >= 0 } ?: (dm.widthPixels - bubbleSize())
        y = Prefs.bubbleY.takeIf { it >= 0 } ?: (dm.heightPixels * 2 / 5)
    }

    private fun clampBubble(p: WindowManager.LayoutParams) {
        val dm = resources.displayMetrics
        p.x = p.x.coerceIn(0, (dm.widthPixels - bubbleSize()).coerceAtLeast(0))
        p.y = p.y.coerceIn(0, (dm.heightPixels - bubbleSize()).coerceAtLeast(0))
    }

    private fun moveBubble(dx: Int, dy: Int) {
        val b = bubble ?: return
        val p = bubbleParams ?: return
        p.x += dx
        p.y += dy
        clampBubble(p)
        if (b.parent != null) getSystemService(WindowManager::class.java).updateViewLayout(b, p)
    }

    /** Ao soltar, o botão gruda na borda mais perto (fica fora do meio do vídeo) e o lugar é lembrado. */
    private fun dropBubble() {
        val b = bubble ?: return
        val p = bubbleParams ?: return
        val dm = resources.displayMetrics
        p.x = if (p.x + bubbleSize() / 2 < dm.widthPixels / 2) 0 else dm.widthPixels - bubbleSize()
        clampBubble(p)
        Prefs.bubbleX = p.x
        Prefs.bubbleY = p.y
        if (b.parent != null) getSystemService(WindowManager::class.java).updateViewLayout(b, p)
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
        hideOverlay()
        overlay = null
        bubble?.let { if (it.parent != null) getSystemService(WindowManager::class.java).removeView(it) }
        bubble = null
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
