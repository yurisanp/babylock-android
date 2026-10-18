package app.babylock.lock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.SystemClock
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
    private var lastDismiss = 0L

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
    }

    override fun hideOverlay() {
        visible = false
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
            val now = SystemClock.uptimeMillis()
            if (now - lastDismiss >= DISMISS_INTERVAL_MS) {
                lastDismiss = now
                dismissShade()
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
            @Suppress("DEPRECATION")
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
        private const val DISMISS_INTERVAL_MS = 150L

        @Volatile
        var instance: LockAccessibilityService? = null
            private set
    }
}
