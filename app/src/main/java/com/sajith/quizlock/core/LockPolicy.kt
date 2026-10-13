package com.sajith.quizlock.core

/** Snapshot of what's locked and what's temporarily open. */
data class LockState(
    val locked: Set<String>,
    /** Package → epoch millis until which it stays open. */
    val unlockedUntil: Map<String, Long>,
    val ownPackage: String,
)

/**
 * Decides when to show the quiz.
 *
 * The quiz only appears when the user *enters* a locked app (the foreground app changes to it),
 * never in the middle of using one. So an unlock lasts for the session, and the grace window
 * means "coming back within N minutes is free".
 */
class LockPolicy {
    private var previous: String? = null

    fun shouldShowQuiz(foreground: String?, state: LockState, now: Long): Boolean {
        if (foreground == null) return false
        val entered = foreground != previous
        previous = foreground
        if (!entered) return false
        if (foreground == state.ownPackage) return false
        if (foreground !in state.locked) return false
        val until = state.unlockedUntil[foreground]
        return until == null || until <= now
    }
}
