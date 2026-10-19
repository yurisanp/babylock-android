package app.babylock.lock

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.babylock.BabyLockApp
import app.babylock.R
import app.babylock.settings.Prefs
import app.babylock.ui.MainActivity

/**
 * Motor de reserva: overlay comum ("sobrepor a outros apps") num serviço em primeiro plano.
 * Bloqueia a área do app, mas não cobre a barra de status nem trava volume ou gestos do sistema.
 */
class OverlayLockService : Service(), LockSurface {

    private var overlay: LockOverlayView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        LockController.activeSurface = this
        showOverlay()
        return START_NOT_STICKY
    }

    override fun showOverlay() {
        val wm = getSystemService(WindowManager::class.java)
        val view = overlay ?: LockOverlayView(this, Prefs.holdSeconds * 1000L, Prefs.showCornerHint) {
            LockController.requestUnlock(this)
        }.also { overlay = it }
        if (view.parent == null) {
            wm.addView(view, lockWindowParams(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY))
        }
        view.flashHint()
    }

    override fun hideOverlay() {
        overlay?.let { if (it.parent != null) getSystemService(WindowManager::class.java).removeView(it) }
    }

    override fun release() {
        hideOverlay()
        overlay = null
        stopSelf()
    }

    override fun onDestroy() {
        hideOverlay()
        overlay = null
        LockController.onSurfaceLost(this, this)
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, BabyLockApp.CHANNEL_LOCK)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text, Prefs.holdSeconds))
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        fun lockIntent(context: Context) = Intent(context, OverlayLockService::class.java)
    }
}
