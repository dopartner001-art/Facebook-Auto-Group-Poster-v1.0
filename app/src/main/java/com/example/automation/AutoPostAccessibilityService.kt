package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
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
        // Keeps window node tree updated while coroutine engine drives automation steps.
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
            delay(2800L)

            while (PostQueueManager.state.value.isRunning) {
                val snapshot = PostQueueManager.state.value
                val groupTarget = snapshot.currentGroupName
                if (groupTarget.isBlank()) {
                    PostQueueManager.stopPosting("No groups remaining in queue")
                    break
                }

                val isUrl = AccessibilityPermissionHelper.isGroupUrl(groupTarget)

                // Step A: Open group directly via URL deep link OR search by group name
                val enteredGroup = if (isUrl) {
                    openGroupDirectlyByUrl(groupTarget, isFirstInQueue = snapshot.currentIndex == 0)
                } else {
                    openSearchAndEnterGroup(groupTarget)
                }

                if (!PostQueueManager.state.value.isRunning) break
                if (!enteredGroup) {
                    val hasMore = PostQueueManager.markCurrentGroupFailedAndAdvance("Group not found")
                    if (!hasMore) break
                    if (!AccessibilityPermissionHelper.isGroupUrl(PostQueueManager.state.value.currentGroupName)) {
                        navigateBackToFeed()
                    }
                    continue
                }

                // Step B: Click "Write something..." / "কিছু লিখুন..." inside the group
                val openedComposer = openGroupComposer()
                if (!PostQueueManager.state.value.isRunning) break
                if (!openedComposer) {
                    val hasMore = PostQueueManager.markCurrentGroupFailedAndAdvance("Write box not found")
                    if (!hasMore) break
                    if (!AccessibilityPermissionHelper.isGroupUrl(PostQueueManager.state.value.currentGroupName)) {
                        navigateBackToFeed()
                    }
                    continue
                }

                // Step C: If photo/video is selected, attach it FIRST while Create Post menu is fully visible
                val mediaUri = PostQueueManager.state.value.mediaUriString
                if (!mediaUri.isNullOrBlank()) {
                    val attached = attachMediaFromGallery()
                    if (!PostQueueManager.state.value.isRunning) break
                    if (!attached) {
                        val hasMore = PostQueueManager.markCurrentGroupFailedAndAdvance("Could not attach media")
                        if (!hasMore) break
                        if (!AccessibilityPermissionHelper.isGroupUrl(PostQueueManager.state.value.currentGroupName)) {
                            navigateBackToFeed()
                        }
                        continue
                    }
                }

                // Step D: Populate post caption directly via Bundle (ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE)
                val captionText = PostQueueManager.state.value.captionText
                if (captionText.isNotBlank()) {
                    val typedCaption = insertCaptionWithoutClipboard(captionText)
                    if (!PostQueueManager.state.value.isRunning) break
                    if (!typedCaption) {
                        val hasMore = PostQueueManager.markCurrentGroupFailedAndAdvance("Caption box not found")
                        if (!hasMore) break
                        if (!AccessibilityPermissionHelper.isGroupUrl(PostQueueManager.state.value.currentGroupName)) {
                            navigateBackToFeed()
                        }
                        continue
                    }

                    // If Facebook opened the "Add text" / "টেক্সট যোগ করুন" sub-screen, tap "Done" / "সম্পন্ন"
                    clickDoneOnTextSubScreenIfPresent()
                    if (!PostQueueManager.state.value.isRunning) break
                }

                val clickedPost = clickPostButton()
                if (!PostQueueManager.state.value.isRunning) break
                if (!clickedPost) {
                    val hasMore = PostQueueManager.markCurrentGroupFailedAndAdvance("Post button not found")
                    if (!hasMore) break
                    if (!AccessibilityPermissionHelper.isGroupUrl(PostQueueManager.state.value.currentGroupName)) {
                        navigateBackToFeed()
                    }
                    continue
                }

                // Record successful post timestamp locally in Room
                groupRepository.markGroupPosted(groupTarget)

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
                    statusMessage = "Moving to next group..."
                )

                val hasMore = PostQueueManager.markCurrentGroupCompletedAndAdvance()
                if (!hasMore) break

                val nextGroup = PostQueueManager.state.value.currentGroupName
                if (!AccessibilityPermissionHelper.isGroupUrl(nextGroup)) {
                    navigateBackToFeed()
                }
                delay(1200L)
            }
        }
    }

    private suspend fun openGroupDirectlyByUrl(groupUrl: String, isFirstInQueue: Boolean): Boolean {
        PostQueueManager.updateStep(
            step = AutomationStep.SELECTING_GROUP_RESULT,
            statusMessage = "Opening group link directly..."
        )

        if (!isFirstInQueue) {
            val opened = AccessibilityPermissionHelper.openFacebookGroupByUrl(applicationContext, groupUrl)
            if (!opened) return false
            delay(2800L)
        }

        return retryUntil(maxAttempts = 8, delayMs = 700L) {
            val root = rootInActiveWindow ?: return@retryUntil false
            if (root.packageName?.toString() != AccessibilityPermissionHelper.FACEBOOK_PACKAGE) {
                return@retryUntil false
            }

            val visitBtn = findNode(root) { node ->
                val text = node.text?.toString()?.trim().orEmpty()
                val desc = node.contentDescription?.toString()?.trim().orEmpty()
                (text.equals("Visit group", ignoreCase = true) ||
                    desc.equals("Visit group", ignoreCase = true) ||
                    text.equals("View group", ignoreCase = true) ||
                    desc.equals("View group", ignoreCase = true) ||
                    text.equals("গ্রুপ দেখুন", ignoreCase = true) ||
                    text.equals("গ্রুপ ভিজিট করুন", ignoreCase = true)) &&
                    hasClickableParentOrSelf(node)
            }
            if (visitBtn != null) {
                clickNodeOrParent(visitBtn)
                return@retryUntil false
            }

            findComposerTriggerNode(root) != null || findScrollableNode(root) != null
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
                    desc.contains("অনুসন্ধান", ignoreCase = true) ||
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

        retryFindAndClick(maxAttempts = 2, delayMs = 500L) { root ->
            findNode(root) { node ->
                val text = node.text?.toString().orEmpty()
                (text.contains("See results", ignoreCase = true) ||
                    text.contains("ফলাফল দেখুন", ignoreCase = true)) &&
                    hasClickableParentOrSelf(node)
            }
        }
        delay(1200L)

        retryFindAndClick(maxAttempts = 2, delayMs = 450L) { root ->
            findNode(root) { node ->
                val text = node.text?.toString()?.trim().orEmpty()
                val desc = node.contentDescription?.toString()?.trim().orEmpty()
                (text.equals("Groups", ignoreCase = true) ||
                    desc.equals("Groups", ignoreCase = true) ||
                    text.equals("গ্রুপ", ignoreCase = true) ||
                    text.equals("গ্রুপসমূহ", ignoreCase = true)) &&
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
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (rect.top < 240) return@findNode false

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
            statusMessage = "Looking for \"Write something...\" box..."
        )
        delay(1200L)

        var scrolledOnce = false
        return retryUntil(maxAttempts = 10, delayMs = 700L) {
            val root = rootInActiveWindow ?: return@retryUntil false
            val target = findComposerTriggerNode(root)
            if (target != null) {
                return@retryUntil clickNodeOrParent(target)
            }
            if (!scrolledOnce) {
                scrolledOnce = true
                findScrollableNode(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }
            false
        }
    }

    private fun findComposerTriggerNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNode(root) { node ->
            val text = node.text?.toString().orEmpty()
            val desc = node.contentDescription?.toString().orEmpty()
            val combined = "$text $desc"
            (combined.contains("Write something", ignoreCase = true) ||
                combined.contains("Create a public post", ignoreCase = true) ||
                combined.contains("Write a public post", ignoreCase = true) ||
                combined.contains("Start a discussion", ignoreCase = true) ||
                combined.contains("Share something", ignoreCase = true) ||
                combined.contains("What's on your mind", ignoreCase = true) ||
                combined.contains("Create post", ignoreCase = true) ||
                combined.contains("কিছু লিখুন", ignoreCase = true) ||
                combined.contains("পাবলিক পোস্ট", ignoreCase = true) ||
                combined.contains("পোস্ট তৈরি", ignoreCase = true) ||
                combined.contains("আপনার মনে কী", ignoreCase = true)) &&
                hasClickableParentOrSelf(node)
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

    private suspend fun clickDoneOnTextSubScreenIfPresent() {
        delay(600L)
        val root = rootInActiveWindow ?: return

        // Only press Done ("সম্পন্ন" / "Done") if we're on Facebook's "Add text" / "টেক্সট যোগ করুন" sub-screen
        // or if the Post button isn't visible yet while Done/সম্পন্ন is visible in the top bar.
        val hasPostButtonAlready = findPostButtonNode(root) != null
        if (hasPostButtonAlready) return

        val clickedDone = retryFindAndClick(maxAttempts = 3, delayMs = 450L) { currentRoot ->
            findNode(currentRoot) { node ->
                if (node.isEditable || node.className?.toString() == "android.widget.EditText") {
                    return@findNode false
                }
                val text = node.text?.toString()?.trim().orEmpty()
                val desc = node.contentDescription?.toString()?.trim().orEmpty()
                val isDoneLabel = text.equals("Done", ignoreCase = true) ||
                    desc.equals("Done", ignoreCase = true) ||
                    text.equals("সম্পন্ন", ignoreCase = true) ||
                    desc.equals("সম্পন্ন", ignoreCase = true) ||
                    text.equals("ঠিক আছে", ignoreCase = true) ||
                    text.equals("Save", ignoreCase = true) ||
                    text.equals("সংরক্ষণ করুন", ignoreCase = true)
                isDoneLabel && node.isEnabled && hasClickableParentOrSelf(node)
            }
        }
        if (clickedDone) {
            delay(1000L)
        }
    }

    private suspend fun attachMediaFromGallery(): Boolean {
        PostQueueManager.updateStep(
            step = AutomationStep.ATTACHING_MEDIA,
            statusMessage = "Looking for Photo / Video option..."
        )
        delay(900L)

        // First attempt: look directly for Photo/Video button or icon
        var clickedPhotoButton = retryFindAndClick(maxAttempts = 3, delayMs = 500L) { root ->
            findPhotoVideoTriggerNode(root)
        }

        // If not visible yet, Facebook's "Add to your post" / "আপনার পোস্টে যোগ করুন" drawer may be collapsed
        if (!clickedPhotoButton) {
            val expandedDrawer = retryFindAndClick(maxAttempts = 2, delayMs = 450L) { root ->
                findNode(root) { node ->
                    val combined = "${node.text?.toString().orEmpty()} ${node.contentDescription?.toString().orEmpty()}"
                    (combined.contains("Add to your post", ignoreCase = true) ||
                        combined.contains("আপনার পোস্টে যোগ করুন", ignoreCase = true) ||
                        combined.contains("পোস্টে যোগ করুন", ignoreCase = true)) &&
                        hasClickableParentOrSelf(node)
                }
            }
            if (expandedDrawer) {
                delay(900L)
                clickedPhotoButton = retryFindAndClick(maxAttempts = 4, delayMs = 550L) { root ->
                    findPhotoVideoTriggerNode(root)
                }
            }
        }

        if (!clickedPhotoButton) {
            PostQueueManager.updateStep(
                step = AutomationStep.ATTACHING_MEDIA,
                statusMessage = "Photo/Video button not found"
            )
            return false
        }

        PostQueueManager.updateStep(
            step = AutomationStep.ATTACHING_MEDIA,
            statusMessage = "Selecting photo/video from gallery..."
        )
        delay(1700L)

        // Handle optional runtime permission prompt inside Facebook ("Allow" / "অনুমতি দিন")
        retryFindAndClick(maxAttempts = 1, delayMs = 300L) { root ->
            findNode(root) { node ->
                val text = node.text?.toString()?.trim().orEmpty()
                (text.equals("Allow all", ignoreCase = true) ||
                    text.equals("Allow", ignoreCase = true) ||
                    text.equals("অনুমতি দিন", ignoreCase = true) ||
                    text.equals("Continue", ignoreCase = true) ||
                    text.equals("চালিয়ে যান", ignoreCase = true)) &&
                    hasClickableParentOrSelf(node)
            }
        }

        val screenHeight = resources.displayMetrics.heightPixels
        val headerCutoffY = (screenHeight * 0.18f).toInt()

        val pickedMedia = retryFindAndClick(maxAttempts = 6, delayMs = 650L) { root ->
            findFirstGalleryMediaNode(root, headerCutoffY)
        }

        if (!pickedMedia) {
            PostQueueManager.updateStep(
                step = AutomationStep.ATTACHING_MEDIA,
                statusMessage = "Could not select gallery item"
            )
            return false
        }

        delay(1200L)

        // If Facebook didn't auto-return to Create Post, tap Next / Done / পরবর্তী / সম্পন্ন
        val rootAfterPick = rootInActiveWindow
        if (rootAfterPick == null || findPostButtonNode(rootAfterPick) == null) {
            retryFindAndClick(maxAttempts = 4, delayMs = 500L) { root ->
                findNode(root) { node ->
                    val text = node.text?.toString()?.trim().orEmpty()
                    val desc = node.contentDescription?.toString()?.trim().orEmpty()
                    (text.equals("Next", ignoreCase = true) ||
                        desc.equals("Next", ignoreCase = true) ||
                        text.equals("Done", ignoreCase = true) ||
                        desc.equals("Done", ignoreCase = true) ||
                        text.equals("Add", ignoreCase = true) ||
                        text.equals("পরবর্তী", ignoreCase = true) ||
                        desc.equals("পরবর্তী", ignoreCase = true) ||
                        text.equals("সম্পন্ন", ignoreCase = true) ||
                        desc.equals("সম্পন্ন", ignoreCase = true) ||
                        text.equals("যোগ করুন", ignoreCase = true)) &&
                        node.isEnabled &&
                        hasClickableParentOrSelf(node)
                }
            }
            delay(1300L)
        }
        return true
    }

    private fun findPhotoVideoTriggerNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNode(root) { node ->
            if (node.isEditable || node.className?.toString() == "android.widget.EditText") {
                return@findNode false
            }
            val text = node.text?.toString().orEmpty()
            val desc = node.contentDescription?.toString().orEmpty()
            val viewId = node.viewIdResourceName.orEmpty()
            val combined = "$text $desc"

            val matchesTextOrDesc = combined.contains("Photo/video", ignoreCase = true) ||
                combined.contains("Photo / video", ignoreCase = true) ||
                combined.contains("Photos/videos", ignoreCase = true) ||
                combined.equals("Photo", ignoreCase = true) ||
                combined.equals("Photos", ignoreCase = true) ||
                combined.contains("Photo or video", ignoreCase = true) ||
                combined.contains("Gallery", ignoreCase = true) ||
                combined.contains("ছবি/ভিডিও", ignoreCase = true) ||
                combined.contains("ফটো/ভিডিও", ignoreCase = true) ||
                combined.contains("ফটো / ভিডিও", ignoreCase = true) ||
                combined.contains("ছবি বা ভিডিও", ignoreCase = true) ||
                combined.contains("ফটো বা ভিডিও", ignoreCase = true) ||
                combined.contains("ছবি যোগ", ignoreCase = true) ||
                combined.contains("ফটো যোগ", ignoreCase = true) ||
                combined.equals("ছবি", ignoreCase = true) ||
                combined.equals("ফটো", ignoreCase = true) ||
                combined.contains("গ্যালারি", ignoreCase = true)

            val matchesViewId = viewId.contains("media_picker", ignoreCase = true) ||
                viewId.contains("photo_button", ignoreCase = true) ||
                viewId.contains("gallery_button", ignoreCase = true)

            (matchesTextOrDesc || matchesViewId) && hasClickableParentOrSelf(node)
        }
    }

    private fun findFirstGalleryMediaNode(
        root: AccessibilityNodeInfo,
        headerCutoffY: Int
    ): AccessibilityNodeInfo? {
        // 1. Prefer finding a RecyclerView / GridView media item below the header
        val gridNode = findNode(root) { node ->
            val cls = node.className?.toString().orEmpty()
            (cls.contains("RecyclerView") || cls.contains("GridView")) && node.childCount > 0
        }
        if (gridNode != null) {
            for (i in 0 until gridNode.childCount) {
                val child = gridNode.getChild(i) ?: continue
                val rect = Rect()
                child.getBoundsInScreen(rect)
                if (rect.top < headerCutoffY || rect.width() < 100 || rect.height() < 100) continue
                val text = child.text?.toString().orEmpty()
                val desc = child.contentDescription?.toString().orEmpty()
                val combined = "$text $desc"
                if (combined.contains("Camera", ignoreCase = true) ||
                    combined.contains("ক্যামেরা", ignoreCase = true) ||
                    combined.contains("Select multiple", ignoreCase = true) ||
                    combined.contains("একাধিক", ignoreCase = true)
                ) {
                    continue
                }
                if (hasClickableParentOrSelf(child)) {
                    return child
                }
            }
        }

        // 2. Fallback: scan entire tree for clickable media tile below headerCutoffY
        return findNode(root) { node ->
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.top < headerCutoffY || rect.width() < 120 || rect.height() < 120) {
                return@findNode false
            }
            val text = node.text?.toString().orEmpty()
            val desc = node.contentDescription?.toString().orEmpty()
            val cls = node.className?.toString().orEmpty()
            val combined = "$text $desc"

            if (combined.contains("Camera", ignoreCase = true) ||
                combined.contains("ক্যামেরা", ignoreCase = true) ||
                combined.contains("Select multiple", ignoreCase = true) ||
                combined.contains("একাধিক", ignoreCase = true)
            ) {
                return@findNode false
            }

            (desc.contains("Photo", ignoreCase = true) ||
                desc.contains("Video", ignoreCase = true) ||
                desc.contains("Image", ignoreCase = true) ||
                desc.contains("ছবি", ignoreCase = true) ||
                desc.contains("ফটো", ignoreCase = true) ||
                desc.contains("ভিডিও", ignoreCase = true) ||
                (cls.contains("ImageView") && node.isClickable)) &&
                hasClickableParentOrSelf(node)
        }
    }

    private suspend fun clickPostButton(): Boolean {
        PostQueueManager.updateStep(
            step = AutomationStep.CLICKING_POST,
            statusMessage = "Publishing post..."
        )
        delay(800L)

        return retryFindAndClick(maxAttempts = 8, delayMs = 600L) { root ->
            findPostButtonNode(root)
        }
    }

    private fun findPostButtonNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNode(root) { node ->
            if (node.isEditable || node.className?.toString() == "android.widget.EditText") {
                return@findNode false
            }
            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()
            val isPostLabel = text.equals("Post", ignoreCase = true) ||
                text.equals("POST", ignoreCase = true) ||
                desc.equals("Post", ignoreCase = true) ||
                desc.equals("POST", ignoreCase = true) ||
                text.equals("Publish", ignoreCase = true) ||
                text.equals("পোস্ট", ignoreCase = true) ||
                desc.equals("পোস্ট", ignoreCase = true) ||
                text.equals("পোস্ট করুন", ignoreCase = true) ||
                desc.equals("পোস্ট করুন", ignoreCase = true) ||
                text.equals("প্রকাশ করুন", ignoreCase = true)
            isPostLabel && node.isEnabled && hasClickableParentOrSelf(node)
        }
    }

    private suspend fun navigateBackToFeed() {
        repeat(3) {
            if (!PostQueueManager.state.value.isRunning) return
            val root = rootInActiveWindow
            val hasSearch = root != null && findNode(root) { node ->
                val desc = node.contentDescription?.toString().orEmpty()
                desc.contains("Search Facebook", ignoreCase = true) ||
                    desc.equals("Search", ignoreCase = true) ||
                    desc.contains("খুঁজুন", ignoreCase = true)
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

    private fun findScrollableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val match = findScrollableNode(root.getChild(i))
            if (match != null) return match
        }
        return null
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
