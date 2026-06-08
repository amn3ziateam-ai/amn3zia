package com.amn3zia.app.core.privacy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.amn3zia.app.AmnApplication

/**
 * Foreground service that performs an immediate "app close" auto-clean pass.
 * Started briefly from MainActivity.onDestroy / task-removed callbacks so the
 * cleanup can finish even as the UI process is going away.
 */
class AutoCleanService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        val app = AmnApplication.from(applicationContext)
        app.autoClean.onAppClosed()
        // Service stops itself shortly after kicking off cleanup; the cleanup
        // coroutines run on the application-scoped dispatcher and complete independently.
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId = "amn3zia_autoclean"
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Privacy cleanup", NotificationManager.IMPORTANCE_MIN)
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AMN3ZIA")
            .setContentText("Cleaning up locally…")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 4201
    }
}
