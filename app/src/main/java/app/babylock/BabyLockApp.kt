package app.babylock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import app.babylock.billing.Pro
import app.babylock.settings.Prefs

class BabyLockApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Pro.init()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_LOCK,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_LOCK = "lock"
    }
}
