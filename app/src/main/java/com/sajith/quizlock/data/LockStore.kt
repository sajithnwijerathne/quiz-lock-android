package com.sajith.quizlock.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sajith.quizlock.core.LockState

/**
 * Settings and lock state. SharedPreferences keeps reads cheap and synchronous for the
 * monitor service, which checks it every poll. Swap for DataStore when the app grows.
 */
class LockStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("quizlock", Context.MODE_PRIVATE)
    private val ownPackage = context.packageName

    var lockedPackages: Set<String>
        get() = prefs.getStringSet(KEY_LOCKED, emptySet())!!.toSet()
        set(value) = prefs.edit { putStringSet(KEY_LOCKED, value.toSet()) }

    /** The user wants locking on (the service may still be stopped if a permission is missing). */
    var lockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    var graceMinutes: Int
        get() = prefs.getInt(KEY_GRACE, 5)
        set(value) = prefs.edit { putInt(KEY_GRACE, value.coerceIn(1, 60)) }

    var questionIndex: Int
        get() = prefs.getInt(KEY_QUESTION, 0)
        set(value) = prefs.edit { putInt(KEY_QUESTION, value) }

    /** Package → epoch millis until which it stays open. Stored as "pkg|millis" strings. */
    fun unlockedUntil(now: Long = System.currentTimeMillis()): Map<String, Long> =
        prefs.getStringSet(KEY_UNLOCKS, emptySet())!!
            .mapNotNull { entry ->
                val pkg = entry.substringBeforeLast('|')
                val until = entry.substringAfterLast('|').toLongOrNull() ?: return@mapNotNull null
                if (until > now) pkg to until else null
            }
            .toMap()

    fun unlock(packageName: String, minutes: Int = graceMinutes, now: Long = System.currentTimeMillis()) {
        val updated = unlockedUntil(now) + (packageName to now + minutes * 60_000L)
        prefs.edit { putStringSet(KEY_UNLOCKS, updated.map { (pkg, until) -> "$pkg|$until" }.toSet()) }
    }

    fun clearUnlocks() = prefs.edit { remove(KEY_UNLOCKS) }

    fun snapshot(now: Long = System.currentTimeMillis()) =
        LockState(locked = lockedPackages, unlockedUntil = unlockedUntil(now), ownPackage = ownPackage)

    private companion object {
        const val KEY_LOCKED = "lockedPackages"
        const val KEY_ENABLED = "lockingEnabled"
        const val KEY_GRACE = "graceMinutes"
        const val KEY_QUESTION = "questionIndex"
        const val KEY_UNLOCKS = "unlocks"
    }
}
