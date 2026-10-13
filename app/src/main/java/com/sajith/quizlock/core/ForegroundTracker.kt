package com.sajith.quizlock.core

/** The bits of a `UsageEvents.Event` we need, so the logic can be unit-tested without Android. */
data class UsageEventLite(val packageName: String, val type: Int, val timeStamp: Long)

/**
 * Works out which app is in front from batches of usage events.
 *
 * Batches may overlap (we re-query a little of the previous window to catch events that were
 * written late), so events at or before the last one we processed are skipped.
 * An empty batch is normal and keeps the last known app.
 */
class ForegroundTracker(private val resumedType: Int = ACTIVITY_RESUMED) {
    var current: String? = null
        private set

    private var lastSeen = Long.MIN_VALUE

    fun consume(events: List<UsageEventLite>): String? {
        events
            .filter { it.timeStamp > lastSeen }
            .sortedBy { it.timeStamp }
            .forEach { event ->
                lastSeen = event.timeStamp
                if (event.type == resumedType) current = event.packageName
            }
        return current
    }

    companion object {
        /** `UsageEvents.Event.ACTIVITY_RESUMED` (API 29+), same value as the older MOVE_TO_FOREGROUND. */
        const val ACTIVITY_RESUMED = 1
    }
}
