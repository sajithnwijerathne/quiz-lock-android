package com.sajith.quizlock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpikeDeckTest {
    @Test
    fun choiceHasFourDistinctOptionsIncludingTheAnswer() {
        for (index in -5 until 30) {
            val q = SpikeDeck.choice(index)
            assertEquals(4, q.choices.size)
            assertEquals("duplicates at $index", 4, q.choices.toSet().size)
            assertEquals(q.word.en, q.choices[q.correctIndex])
        }
    }

    @Test
    fun correctPositionRotates() {
        assertEquals(setOf(0, 1, 2, 3), (0 until 4).map { SpikeDeck.choice(it).correctIndex }.toSet())
    }
}

class ForegroundTrackerTest {
    private fun resumed(pkg: String, t: Long) = UsageEventLite(pkg, ForegroundTracker.ACTIVITY_RESUMED, t)
    private fun paused(pkg: String, t: Long) = UsageEventLite(pkg, 2, t)

    @Test
    fun startsUnknownAndKeepsLastAppOnEmptyBatch() {
        val tracker = ForegroundTracker()
        assertNull(tracker.consume(emptyList()))
        tracker.consume(listOf(resumed("a", 10)))
        assertEquals("a", tracker.consume(emptyList()))
    }

    @Test
    fun latestResumedWinsAndOtherEventTypesAreIgnored() {
        val tracker = ForegroundTracker()
        val result = tracker.consume(listOf(paused("a", 30), resumed("b", 20), resumed("a", 10)))
        assertEquals("b", result)
    }

    @Test
    fun overlappingBatchesDoNotReplayOldEvents() {
        val tracker = ForegroundTracker()
        tracker.consume(listOf(resumed("a", 10), resumed("b", 20)))
        // Next query overlaps and re-delivers event 10.
        assertEquals("b", tracker.consume(listOf(resumed("a", 10))))
        assertEquals("c", tracker.consume(listOf(resumed("a", 10), resumed("c", 25))))
    }
}

class LockPolicyTest {
    private val own = "com.sajith.quizlock"
    private fun state(unlocked: Map<String, Long> = emptyMap()) =
        LockState(locked = setOf("insta"), unlockedUntil = unlocked, ownPackage = own)

    @Test
    fun showsQuizOnlyOnEntryToALockedApp() {
        val policy = LockPolicy()
        assertFalse(policy.shouldShowQuiz("launcher", state(), 0))
        assertTrue(policy.shouldShowQuiz("insta", state(), 1))
        assertFalse("still in the same app", policy.shouldShowQuiz("insta", state(), 2))
    }

    @Test
    fun ownQuizScreenAndUnlockedAppsDoNotTrigger() {
        val policy = LockPolicy()
        assertTrue(policy.shouldShowQuiz("insta", state(), 1))
        assertFalse(policy.shouldShowQuiz(own, state(), 2))
        // Back in the app after a right answer: unlocked until 100.
        assertFalse(policy.shouldShowQuiz("insta", state(mapOf("insta" to 100L)), 3))
    }

    @Test
    fun reEntryAfterGraceWindowShowsQuizAgain() {
        val policy = LockPolicy()
        val unlocked = state(mapOf("insta" to 100L))
        assertFalse(policy.shouldShowQuiz("insta", unlocked, 50))
        assertFalse(policy.shouldShowQuiz("launcher", unlocked, 60))
        assertFalse("within grace", policy.shouldShowQuiz("insta", unlocked, 90))
        assertFalse(policy.shouldShowQuiz("launcher", unlocked, 95))
        assertTrue("grace over", policy.shouldShowQuiz("insta", unlocked, 150))
    }

    @Test
    fun nullForegroundIsIgnored() {
        assertFalse(LockPolicy().shouldShowQuiz(null, state(), 0))
    }
}
