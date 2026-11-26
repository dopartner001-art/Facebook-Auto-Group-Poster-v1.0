package com.example

import com.example.automation.AutomationStep
import com.example.automation.PostQueueManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostQueueManagerTest {

    @After
    fun tearDown() {
        PostQueueManager.resetToIdle()
    }

    @Test
    fun `queue advances through selected groups and stops cleanly`() {
        PostQueueManager.startPosting(
            groupNames = listOf("Group Alpha", "Group Beta"),
            captionText = "Hello Groups!",
            mediaUriString = null,
            mediaMimeType = null
        )

        var state = PostQueueManager.state.value
        assertTrue(state.isRunning)
        assertEquals(2, state.totalGroups)
        assertEquals(1, state.displayGroupNumber)
        assertEquals("Group Alpha", state.currentGroupName)

        val hasMore = PostQueueManager.markCurrentGroupCompletedAndAdvance()
        assertTrue(hasMore)
        state = PostQueueManager.state.value
        assertEquals(2, state.displayGroupNumber)
        assertEquals("Group Beta", state.currentGroupName)

        val hasMoreAfterSecond = PostQueueManager.markCurrentGroupCompletedAndAdvance()
        assertFalse(hasMoreAfterSecond)
        state = PostQueueManager.state.value
        assertFalse(state.isRunning)
        assertEquals(AutomationStep.COMPLETED, state.step)
        assertEquals(2, state.completedGroups.size)
    }

    @Test
    fun `stopPosting immediately halts running queue`() {
        PostQueueManager.startPosting(
            groupNames = listOf("Group One", "Group Two"),
            captionText = "Test post",
            mediaUriString = null,
            mediaMimeType = null
        )
        PostQueueManager.stopPosting("Stopped by user")
        val state = PostQueueManager.state.value
        assertFalse(state.isRunning)
        assertEquals(AutomationStep.STOPPED, state.step)
    }
}
