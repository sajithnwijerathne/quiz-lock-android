package com.sajith.quizlock.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sajith.quizlock.data.DebugLog
import com.sajith.quizlock.data.LockStore

/** Restarts monitoring after a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val relevant = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (!relevant) return
        if (LockStore(context).lockingEnabled && Permissions.canLock(context)) {
            DebugLog.add(context, "boot", "${intent.action} → starting monitor")
            MonitorService.start(context)
        }
    }
}
