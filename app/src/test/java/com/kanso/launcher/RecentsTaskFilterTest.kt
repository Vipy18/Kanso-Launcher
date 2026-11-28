package com.kanso.launcher

import com.kanso.launcher.service.RecentsSnapshotCache
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentsTaskFilterTest {

    @Test
    fun shouldIncludeTask_excludesDismissedBeforeOrAtCutoff() {
        val cutoffs = mapOf("com.example.chat" to 1_700_000_000_000L)

        assertFalse(
            RecentsSnapshotCache.shouldIncludeTask(
                packageName = "com.example.chat",
                lastTimeUsedMs = 1_699_999_999_999L,
                dismissedCutoffs = cutoffs
            )
        )
        assertFalse(
            RecentsSnapshotCache.shouldIncludeTask(
                packageName = "com.example.chat",
                lastTimeUsedMs = 1_700_000_000_000L,
                dismissedCutoffs = cutoffs
            )
        )
    }

    @Test
    fun shouldIncludeTask_reIncludesTaskAfterGenuineRelaunch() {
        val cutoffs = mapOf("com.example.chat" to 1_700_000_000_000L)

        assertTrue(
            RecentsSnapshotCache.shouldIncludeTask(
                packageName = "com.example.chat",
                lastTimeUsedMs = 1_700_000_000_001L,
                dismissedCutoffs = cutoffs
            )
        )
    }

    @Test
    fun shouldIncludeTask_includesUnDismissedTaskWithPositiveTimestamp() {
        val cutoffs = mapOf("com.other.app" to 1_700_000_000_000L)

        assertTrue(
            RecentsSnapshotCache.shouldIncludeTask(
                packageName = "com.example.browser",
                lastTimeUsedMs = 1_699_000_000_000L,
                dismissedCutoffs = cutoffs
            )
        )
    }

    @Test
    fun shouldIncludeTask_excludesZeroOrNegativeTimestamps() {
        assertFalse(
            RecentsSnapshotCache.shouldIncludeTask(
                packageName = "com.example.browser",
                lastTimeUsedMs = 0L,
                dismissedCutoffs = emptyMap()
            )
        )
        assertFalse(
            RecentsSnapshotCache.shouldIncludeTask(
                packageName = "com.example.browser",
                lastTimeUsedMs = -100L,
                dismissedCutoffs = emptyMap()
            )
        )
    }
}
