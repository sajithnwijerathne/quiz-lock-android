package com.sajith.quizlock.lock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sajith.quizlock.R
import com.sajith.quizlock.core.ForegroundTracker
import com.sajith.quizlock.core.LockPolicy
import com.sajith.quizlock.core.UsageEventLite
import com.sajith.quizlock.data.DebugLog
import com.sajith.quizlock.data.LockStore
import com.sajith.quizlock.ui.MainActivity
import com.sajith.quizlock.ui.quiz.QuizActivity

/**
 * Foreground service (type specialUse) that polls UsageStatsManager for the app in front and
 * opens the quiz when the user enters a locked app. No AccessibilityService.
 */
class MonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val tracker = ForegroundTracker()
    private val policy = LockPolicy()
    private lateinit var store: LockStore
    private lateinit var usageStats: UsageStatsManager
    private var lastQueryEnd = 0L

    private val poll = object : Runnable {
        override fun run() {
            check()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = LockStore(this)
        usageStats = getSystemService(UsageStatsManager::class.java)
        lastQueryEnd = System.currentTimeMillis() - 10_000
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Permissions.canLock(this)) {
            DebugLog.add(this, "service", "missing usage access or overlay permission, stopping")
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        handler.removeCallbacks(poll)
        handler.post(poll)
        DebugLog.add(this, "service", "monitoring ${store.lockedPackages.size} apps")
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        DebugLog.add(this, "service", "stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun check() {
        val now = System.currentTimeMillis()
        val foreground = try {
            // Re-query 2 s of the last window to catch late events; the tracker skips duplicates.
            tracker.consume(readEvents(lastQueryEnd - 2_000, now)).also { lastQueryEnd = now }
        } catch (e: SecurityException) {
            DebugLog.add(this, "service", "usage access revoked, stopping")
            stopSelf()
            return
        }

        if (policy.shouldShowQuiz(foreground, store.snapshot(now), now)) {
            DebugLog.add(this, "service", "locked app entered: $foreground → quiz")
            startActivity(QuizActivity.intent(this, foreground!!))
        }
    }

    private fun readEvents(from: Long, to: Long): List<UsageEventLite> {
        val events = usageStats.queryEvents(from, to)
        val event = UsageEvents.Event()
        val result = mutableListOf<UsageEventLite>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            result += UsageEventLite(event.packageName, event.eventType, event.timeStamp)
        }
        return result
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_monitor), NotificationManager.IMPORTANCE_LOW),
        )
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, store.lockedPackages.size))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 700L
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "monitor"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}
