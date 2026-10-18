package app.babylock.lock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import app.babylock.settings.Prefs

/**
 * Motor principal. O overlay de acessibilidade fica acima das barras do sistema,
 * o filtro de teclas segura o volume e os eventos de janela avisam quando a barra de notificações abre.
 * Não lê conteúdo da tela (canRetrieveWindowContent = false).
 */
class LockAccessibilityService : AccessibilityService(), LockSurface {

    private var overlay: LockOverlayView? = null
    private var visible = false
    private val main = Handler(Looper.getMainLooper())
    private var dismissScheduled = false
    private val dismissAgain = Runnable {
        dismissScheduled = false
        if (visible && Prefs.closeShade) dismissShade()
    }

    // Rede de segurança: nem toda versão avisa quando a barra abre. A partir do Android 12,
    // pedir para fechar a barra já fechada não tem efeito, então dá para conferir a cada segundo.
    private val watchdog = object : Runnable {
        override fun run() {
            if (!visible || !Prefs.closeShade) return
            dismissShade()
            main.postDelayed(this, WATCHDOG_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun showOverlay() {
        val wm = getSystemService(WindowManager::class.java)
        val view = overlay ?: LockOverlayView(this, Prefs.holdSeconds * 1000L) {
            LockController.requestUnlock(this)
        }.also { overlay = it }
        if (view.parent == null) {
            wm.addView(view, lockWindowParams(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY))
        }
        visible = true
        view.flashHint()
        if (Prefs.closeShade) dismissShade()
        main.removeCallbacks(watchdog)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) main.postDelayed(watchdog, WATCHDOG_MS)
    }

    override fun hideOverlay() {
        visible = false
        main.removeCallbacks(dismissAgain)
        main.removeCallbacks(watchdog)
        dismissScheduled = false
        overlay?.let { if (it.parent != null) getSystemService(WindowManager::class.java).removeView(it) }
    }

    override fun release() {
        hideOverlay()
        overlay = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!visible || !Prefs.closeShade || event == null) return
        // A barra de notificações e as Configurações rápidas pertencem ao pacote da System UI.
        // Conforme a versão, abrir a barra gera "estado da janela", "janelas" ou "conteúdo" mudou.
        // Fechar a barra quando ela já está fechada não faz nada, então qualquer um desses sinais basta.
        if (event.packageName == SYSTEM_UI || event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            dismissShade()
            // A barra ainda pode estar abrindo (animação) e ignorar o primeiro pedido: tenta de novo.
            if (!dismissScheduled) {
                dismissScheduled = true
                main.postDelayed(dismissAgain, RETRY_DISMISS_MS)
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!visible || !Prefs.lockVolume) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE -> true
            else -> false
        }
    }

    private fun dismissShade() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            // Antes do Android 12 não há ação própria. Não usar "voltar": com a barra fechada,
            // ele chegaria ao app de vídeo e poderia fechar o vídeo.
            // A permissão BROADCAST_CLOSE_SYSTEM_DIALOGS só passou a ser exigida no Android 12,
            // e este ramo roda apenas abaixo dele.
            @Suppress("DEPRECATION", "MissingPermission")
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        release()
        if (instance === this) instance = null
        LockController.onSurfaceLost(this, this)
        super.onDestroy()
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"
        private const val RETRY_DISMISS_MS = 400L
        private const val WATCHDOG_MS = 1_000L

        @Volatile
        var instance: LockAccessibilityService? = null
            private set
    }
}
