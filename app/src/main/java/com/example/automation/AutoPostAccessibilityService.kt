package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.local.AppDatabase
import com.example.data.repository.GroupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random

class AutoPostAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlayController: FloatingOverlayController? = null
    private var automationJob: Job? = null
    private lateinit var groupRepository: GroupRepository

    override fun onCreate() {
        super.onCreate()
        val dao = AppDatabase.getInstance(applicationContext).groupDao()
        groupRepository = GroupRepository(dao)
        overlayController = FloatingOverlayController(applicationContext) {
            stopAutomation("Stopped by user")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        PostQueueManager.markAccessibilityConnected(true)

        serviceScope.launch {
            PostQueueManager.state.collectLatest { state ->
                if (state.isRunning) {
                    overlayController?.show(state)
                    overlayController?.update(state)
                    if (automationJob == null || automationJob?.isActive != true) {
                        startAutomationLoop()
                    }
                } else {
                    if (state.step == AutomationStep.COMPLETED) {
                        overlayController?.show(state)
                        overlayController?.update(state)
                        delay(2500L)
                        overlayController?.hide()
                    } else {
                        overlayController?.hide()
                    }
                    automationJob?.cancel()
                    automationJob = null
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events keep rootInActiveWindow fresh while the coroutine automation engine inspects nodes.
    }

    override fun onInterrupt() {
        stopAutomation("Accessibility service interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        PostQueueManager.markAccessibilityConnected(false)
        stopAutomation("Accessibility service disconnected")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        PostQueueManager.markAccessibilityConnected(false)
        overlayController?.hide()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopAutomation(reason: String) {
        automationJob?.cancel()
        automationJob = null
        PostQueueManager.stopPosting(reason)
        overlayController?.hide()
    }

    private fun startAutomationLoop() {
        automationJob?.cancel()
        automationJob = serviceScope.launch {
            // Allow Facebook app to launch and settle
            delay(2200L)

            while (PostQueueManager.state.value.isRunning) {
                val snapshot = PostQueueManager.state.value
                val groupName = snapshot.currentGroupName
                if (groupName.isBlank()) {
                    PostQueueManager.stopPosting("No groups remaining in queue")
                    break
                }

                // Step A: Open Facebook Search, type group name via ACTION_SET_TEXT, and enter the group
                val enteredGroup = openSearchAndEnterGroup(groupName)
                if (!PostQueueManager.state.value.isRunning) break
                if (!enteredGroup) {
                    val hasMore = PostQueueManager.markCurrentGroupFailedAndAdvance("Group not found")
                    if (!hasMore) break
                    navigateBackToFeed()
                    continue
                }

                // Step B: Click "Write something..." inside the group
                val openedComposer = openGroupComposer()
                if (!PostQueueManager.state.value.isRunning) break
                if (!openedComposer) {
                    val hasMore = PostQueueManager.markCurrentGroupFailedAndAdvance("Composer unavailable")
                    if (!hasMore) break
                    navigateBackToFeed()
                    continue
                }

                // Step C: Populate post caption directly via Bundle (ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE)
                val captionText = PostQueueManager.state.value.captionText
                if (captionText.isNotBlank()) {
                    val typedCaption = insertCaptionWithoutClipboard(captionText)
                    if (!PostQueueManager.state.value.isRunning) break
                    if (!typedCaption) {
                        val hasMore = PostQueueManager.markCurrentGroupFailedAndAdvance("Caption box not found")
                        if (!hasMore) break
                        navigateBackToFeed()
                        continue
                    }
                }

                // Step D: Attach selected photo/video (if any) and click "Post"
                val mediaUri = PostQueueManager.state.value.mediaUriString
                if (!mediaUri.isNullOrBlank()) {
                    attachMediaFromGallery()
                    if (!PostQueueManager.state.value.isRunning) break
                }

                val clickedPost = clickPostButton()
                if (!PostQueueManager.state.value.isRunning) break
                if (!clickedPost) {
                    val hasMore = PostQueueManager.markCurrentGroupFailedAndAdvance("Post button not found")
                    if (!hasMore) break
                    navigateBackToFeed()
                    continue
                }

                // Record successful post timestamp locally in Room
                groupRepository.markGroupPosted(groupName)

                // Step E: Random human-like pause (8-15 seconds), navigate back, and advance queue
                val pauseSeconds = Random.nextInt(8, 16)
                for (remaining in pauseSeconds downTo 1) {
                    if (!PostQueueManager.state.value.isRunning) break
                    PostQueueManager.updateStep(
                        step = AutomationStep.HUMAN_DELAY,
                        statusMessage = "Human-like cooldown before next group",
                        countdownSeconds = remaining
                    )
                    delay(1000L)
                }
                if (!PostQueueManager.state.value.isRunning) break

                PostQueueManager.updateStep(
                    step = AutomationStep.NAVIGATING_BACK,
                    statusMessage = "Returning to feed..."
                )
                navigateBackToFeed()

                val hasMore = PostQueueManager.markCurrentGroupCompletedAndAdvance()
                if (!hasMore) break
                delay(1200L)
            }
        }
    }

    private suspend fun openSearchAndEnterGroup(groupName: String): Boolean {
        PostQueueManager.updateStep(
            step = AutomationStep.OPENING_SEARCH,
            statusMessage = "Opening Facebook search..."
        )

        val searchClicked = retryFindAndClick(maxAttempts = 8, delayMs = 700L) { root ->
            findNode(root) { node ->
                val text = node.text?.toString().orEmpty()
                val desc = node.contentDescription?.toString().orEmpty()
                val viewId = node.viewIdResourceName.orEmpty()
                (desc.contains("Search", ignoreCase = true) ||
                    desc.contains("খুঁজুন", ignoreCase = true) ||
                    text.equals("Search", ignoreCase = true) ||
                    viewId.contains("search_button", ignoreCase = true) ||
                    viewId.contains("search_bar", ignoreCase = true)) &&
                    hasClickableParentOrSelf(node)
            }
        }

        if (!searchClicked) return false
        delay(900L)

        PostQueueManager.updateStep(
            step = AutomationStep.TYPING_GROUP_NAME,
            statusMessage = "Searching for \"$groupName\"..."
        )

        val typed = retryUntil(maxAttempts = 8, delayMs = 600L) {
            val root = rootInActiveWindow ?: return@retryUntil false
            val searchField = findNode(root) { node ->
                node.isEditable || node.className?.toString() == "android.widget.EditText"
            } ?: return@retryUntil false

            searchField.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val bundle = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, groupName)
            }
            val setOk = searchField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            if (setOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                searchField.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
            }
            setOk
        }

        if (!typed) return false
        delay(1400L)

        // Tap "See results for ..." or keyboard search button if suggestion list is open
        retryFindAndClick(maxAttempts = 2, delayMs = 500L) { root ->
            findNode(root) { node ->
                val text = node.text?.toString().orEmpty()
                val desc = node.contentDescription?.toString().orEmpty()
                (text.contains("See results", ignoreCase = true) ||
                    desc.contains("Search", ignoreCase = true) && !node.isEditable) &&
                    hasClickableParentOrSelf(node)
            }
        }
        delay(1200L)

        // Prefer switching to "Groups" filter chip if visible
        retryFindAndClick(maxAttempts = 2, delayMs = 450L) { root ->
            findNode(root) { node ->
                val text = node.text?.toString()?.trim().orEmpty()
                val desc = node.contentDescription?.toString()?.trim().orEmpty()
                (text.equals("Groups", ignoreCase = true) ||
                    desc.equals("Groups", ignoreCase = true) ||
                    text.equals("গ্রুপ", ignoreCase = true)) &&
                    hasClickableParentOrSelf(node)
            }
        }
        delay(1000L)

        PostQueueManager.updateStep(
            step = AutomationStep.SELECTING_GROUP_RESULT,
            statusMessage = "Entering \"$groupName\"..."
        )

        return retryFindAndClick(maxAttempts = 8, delayMs = 700L) { root ->
            findNode(root) { node ->
                if (node.isEditable || node.className?.toString() == "android.widget.EditText") {
                    return@findNode false
                }
                val text = node.text?.toString()?.trim().orEmpty()
                val desc = node.contentDescription?.toString()?.trim().orEmpty()
                val matchesName = text.equals(groupName, ignoreCase = true) ||
                    desc.equals(groupName, ignoreCase = true) ||
                    text.contains(groupName, ignoreCase = true) ||
                    desc.contains(groupName, ignoreCase = true)
                matchesName && hasClickableParentOrSelf(node)
            }
        }
    }

    private suspend fun openGroupComposer(): Boolean {
        PostQueueManager.updateStep(
            step = AutomationStep.OPENING_COMPOSER,
            statusMessage = "Opening \"Write something...\" box..."
        )
        delay(1500L)

        return retryFindAndClick(maxAttempts = 10, delayMs = 700L) { root ->
            findNode(root) { node ->
                val text = node.text?.toString().orEmpty()
                val desc = node.contentDescription?.toString().orEmpty()
                val combined = "$text $desc"
                (combined.contains("Write something", ignoreCase = true) ||
                    combined.contains("Create a public post", ignoreCase = true) ||
                    combined.contains("Start a discussion", ignoreCase = true) ||
                    combined.contains("Share something", ignoreCase = true) ||
                    combined.contains("কিছু লিখুন", ignoreCase = true)) &&
                    hasClickableParentOrSelf(node)
            }
        }
    }

    private suspend fun insertCaptionWithoutClipboard(caption: String): Boolean {
        PostQueueManager.updateStep(
            step = AutomationStep.TYPING_CAPTION,
            statusMessage = "Inserting caption via Accessibility Bundle..."
        )
        delay(1200L)

        return retryUntil(maxAttempts = 8, delayMs = 600L) {
            val root = rootInActiveWindow ?: return@retryUntil false
            val inputNode = findNode(root) { node ->
                node.isEditable || node.className?.toString() == "android.widget.EditText"
            } ?: return@retryUntil false

            inputNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            inputNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val bundle = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, caption)
            }
            inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        }
    }

    private suspend fun attachMediaFromGallery() {
        PostQueueManager.updateStep(
            step = AutomationStep.ATTACHING_MEDIA,
            statusMessage = "Attaching selected photo/video..."
        )
        delay(900L)

        val clickedPhotoButton = retryFindAndClick(maxAttempts = 5, delayMs = 600L) { root ->
            findNode(root) { node ->
                val text = node.text?.toString().orEmpty()
                val desc = node.contentDescription?.toString().orEmpty()
                val combined = "$text $desc"
                (combined.contains("Photo/video", ignoreCase = true) ||
                    combined.contains("Photo/Video", ignoreCase = true) ||
                    combined.equals("Photo", ignoreCase = true) ||
                    combined.contains("Photos", ignoreCase = true) ||
                    combined.contains("ছবি/ভিডিও", ignoreCase = true)) &&
                    hasClickableParentOrSelf(node)
            }
        }

        if (!clickedPhotoButton) return
        delay(1500L)

        // Pick the target media thumbnail in Facebook's gallery picker
        val pickedMedia = retryFindAndClick(maxAttempts = 5, delayMs = 600L) { root ->
            findNode(root) { node ->
                val desc = node.contentDescription?.toString().orEmpty()
                val cls = node.className?.toString().orEmpty()
                (desc.contains("Photo", ignoreCase = true) ||
                    desc.contains("Video", ignoreCase = true) ||
                    desc.contains("Image", ignoreCase = true) ||
                    cls.contains("ImageView") && node.isClickable) &&
                    !desc.contains("Camera", ignoreCase = true) &&
                    hasClickableParentOrSelf(node)
            }
        }

        if (pickedMedia) {
            delay(1000L)
            // Click "Next" or "Done" if Facebook shows a multi-select confirmation bar
            retryFindAndClick(maxAttempts = 3, delayMs = 500L) { root ->
                findNode(root) { node ->
                    val text = node.text?.toString()?.trim().orEmpty()
                    val desc = node.contentDescription?.toString()?.trim().orEmpty()
                    (text.equals("Next", ignoreCase = true) ||
                        desc.equals("Next", ignoreCase = true) ||
                        text.equals("Done", ignoreCase = true) ||
                        desc.equals("Done", ignoreCase = true) ||
                        text.equals("পরবর্তী", ignoreCase = true)) &&
                        hasClickableParentOrSelf(node)
                }
            }
            delay(1000L)
        }
    }

    private suspend fun clickPostButton(): Boolean {
        PostQueueManager.updateStep(
            step = AutomationStep.CLICKING_POST,
            statusMessage = "Publishing post..."
        )
        delay(800L)

        return retryFindAndClick(maxAttempts = 8, delayMs = 600L) { root ->
            findNode(root) { node ->
                val text = node.text?.toString()?.trim().orEmpty()
                val desc = node.contentDescription?.toString()?.trim().orEmpty()
                val isPostLabel = text.equals("Post", ignoreCase = true) ||
                    text.equals("POST", ignoreCase = true) ||
                    desc.equals("Post", ignoreCase = true) ||
                    desc.equals("POST", ignoreCase = true) ||
                    text.equals("পোস্ট", ignoreCase = true) ||
                    desc.equals("পোস্ট", ignoreCase = true)
                isPostLabel && node.isEnabled && hasClickableParentOrSelf(node)
            }
        }
    }

    private suspend fun navigateBackToFeed() {
        repeat(3) {
            if (!PostQueueManager.state.value.isRunning) return
            val root = rootInActiveWindow
            val hasSearch = root != null && findNode(root) { node ->
                val desc = node.contentDescription?.toString().orEmpty()
                desc.contains("Search Facebook", ignoreCase = true) ||
                    desc.equals("Search", ignoreCase = true)
            } != null
            if (hasSearch) return
            performGlobalAction(GLOBAL_ACTION_BACK)
            delay(900L)
        }
    }

    private suspend fun retryUntil(
        maxAttempts: Int,
        delayMs: Long,
        block: () -> Boolean
    ): Boolean {
        repeat(maxAttempts) {
            if (!PostQueueManager.state.value.isRunning) return false
            if (block()) return true
            delay(delayMs)
        }
        return false
    }

    private suspend fun retryFindAndClick(
        maxAttempts: Int,
        delayMs: Long,
        finder: (AccessibilityNodeInfo) -> AccessibilityNodeInfo?
    ): Boolean {
        return retryUntil(maxAttempts, delayMs) {
            val root = rootInActiveWindow ?: return@retryUntil false
            val target = finder(root) ?: return@retryUntil false
            clickNodeOrParent(target)
        }
    }

    private fun findNode(
        root: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (root == null) return null
        if (predicate(root)) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val match = findNode(child, predicate)
            if (match != null) return match
        }
        return null
    }

    private fun hasClickableParentOrSelf(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isClickable) return true
            current = current.parent
            depth++
        }
        return false
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isClickable && current.isEnabled) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
            depth++
        }
        return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }
}
