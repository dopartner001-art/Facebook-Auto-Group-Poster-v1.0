package com.example.automation

enum class AutomationStep {
    IDLE,
    LAUNCHING_FACEBOOK,
    OPENING_SEARCH,
    TYPING_GROUP_NAME,
    SELECTING_GROUP_RESULT,
    OPENING_COMPOSER,
    TYPING_CAPTION,
    ATTACHING_MEDIA,
    CLICKING_POST,
    HUMAN_DELAY,
    NAVIGATING_BACK,
    COMPLETED,
    STOPPED,
    ERROR
}

data class PostQueueState(
    val isRunning: Boolean = false,
    val queue: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val captionText: String = "",
    val mediaUriString: String? = null,
    val mediaMimeType: String? = null,
    val step: AutomationStep = AutomationStep.IDLE,
    val statusMessage: String = "Ready to post",
    val countdownSeconds: Int = 0,
    val completedGroups: List<String> = emptyList(),
    val failedGroups: List<String> = emptyList(),
    val isAccessibilityConnected: Boolean = false
) {
    val totalGroups: Int get() = queue.size
    val displayGroupNumber: Int
        get() = if (queue.isEmpty()) 0 else (currentIndex + 1).coerceAtMost(queue.size)
    val currentGroupName: String
        get() = queue.getOrNull(currentIndex).orEmpty()
}
