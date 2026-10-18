package app.babylock.lock

import android.accessibilityservice.AccessibilityService
import android.os.Build
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
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.packageName == SYSTEM_UI) {
            dismissShade()
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
            // Antes do Android 12 não há ação própria; "voltar" fecha a barra aberta.
            performGlobalAction(GLOBAL_ACTION_BACK)
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

        @Volatile
        var instance: LockAccessibilityService? = null
            private set
    }
}
