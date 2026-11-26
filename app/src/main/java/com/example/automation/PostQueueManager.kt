package com.example.automation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object PostQueueManager {
    private val _state = MutableStateFlow(PostQueueState())
    val state: StateFlow<PostQueueState> = _state.asStateFlow()

    fun startPosting(
        groupNames: List<String>,
        captionText: String,
        mediaUriString: String?,
        mediaMimeType: String?
    ) {
        if (groupNames.isEmpty()) return
        _state.update { current ->
            current.copy(
                isRunning = true,
                queue = groupNames,
                currentIndex = 0,
                captionText = captionText,
                mediaUriString = mediaUriString,
                mediaMimeType = mediaMimeType,
                step = AutomationStep.LAUNCHING_FACEBOOK,
                statusMessage = "Opening Facebook for ${groupNames.first()}...",
                countdownSeconds = 0,
                completedGroups = emptyList(),
                failedGroups = emptyList()
            )
        }
    }

    fun updateStep(
        step: AutomationStep,
        statusMessage: String,
        countdownSeconds: Int = 0
    ) {
        _state.update { current ->
            if (!current.isRunning && step != AutomationStep.COMPLETED && step != AutomationStep.STOPPED && step != AutomationStep.ERROR) {
                current
            } else {
                current.copy(
                    step = step,
                    statusMessage = statusMessage,
                    countdownSeconds = countdownSeconds
                )
            }
        }
    }

    fun markCurrentGroupCompletedAndAdvance(): Boolean {
        var hasMore = false
        _state.update { current ->
            if (!current.isRunning) return@update current
            val finishedGroup = current.currentGroupName
            val updatedCompleted = if (finishedGroup.isNotBlank()) {
                current.completedGroups + finishedGroup
            } else {
                current.completedGroups
            }
            val nextIndex = current.currentIndex + 1
            if (nextIndex < current.queue.size) {
                hasMore = true
                current.copy(
                    currentIndex = nextIndex,
                    completedGroups = updatedCompleted,
                    step = AutomationStep.OPENING_SEARCH,
                    statusMessage = "Preparing next group: ${current.queue[nextIndex]}",
                    countdownSeconds = 0
                )
            } else {
                hasMore = false
                current.copy(
                    isRunning = false,
                    completedGroups = updatedCompleted,
                    step = AutomationStep.COMPLETED,
                    statusMessage = "Finished posting to ${updatedCompleted.size} of ${current.queue.size} groups",
                    countdownSeconds = 0
                )
            }
        }
        return hasMore
    }

    fun markCurrentGroupFailedAndAdvance(reason: String): Boolean {
        var hasMore = false
        _state.update { current ->
            if (!current.isRunning) return@update current
            val failedGroup = current.currentGroupName
            val updatedFailed = if (failedGroup.isNotBlank()) {
                current.failedGroups + failedGroup
            } else {
                current.failedGroups
            }
            val nextIndex = current.currentIndex + 1
            if (nextIndex < current.queue.size) {
                hasMore = true
                current.copy(
                    currentIndex = nextIndex,
                    failedGroups = updatedFailed,
                    step = AutomationStep.OPENING_SEARCH,
                    statusMessage = "Skipped $failedGroup ($reason). Moving to next...",
                    countdownSeconds = 0
                )
            } else {
                hasMore = false
                current.copy(
                    isRunning = false,
                    failedGroups = updatedFailed,
                    step = AutomationStep.COMPLETED,
                    statusMessage = "Completed (${current.completedGroups.size} posted, ${updatedFailed.size} skipped)",
                    countdownSeconds = 0
                )
            }
        }
        return hasMore
    }

    fun stopPosting(reason: String = "Stopped by user") {
        _state.update { current ->
            current.copy(
                isRunning = false,
                step = AutomationStep.STOPPED,
                statusMessage = reason,
                countdownSeconds = 0
            )
        }
    }

    fun markAccessibilityConnected(connected: Boolean) {
        _state.update { it.copy(isAccessibilityConnected = connected) }
    }

    fun resetToIdle() {
        _state.update { current ->
            current.copy(
                isRunning = false,
                step = AutomationStep.IDLE,
                statusMessage = "Ready to post",
                countdownSeconds = 0
            )
        }
    }
}
