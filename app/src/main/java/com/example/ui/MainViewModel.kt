package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.automation.AccessibilityPermissionHelper
import com.example.automation.PostQueueManager
import com.example.automation.PostQueueState
import com.example.data.local.GroupEntity
import com.example.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val newGroupName: String = "",
    val captionText: String = "",
    val selectedMedia: SelectedMediaInfo? = null,
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isFacebookInstalled: Boolean = false,
    val snackbarMessage: String? = null,
    val showAccessibilityDialog: Boolean = false,
    val showFacebookMissingDialog: Boolean = false
)

class MainViewModel(
    application: Application,
    private val groupRepository: GroupRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val groups: StateFlow<List<GroupEntity>> = groupRepository.allGroups
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val queueState: StateFlow<PostQueueState> = PostQueueManager.state

    init {
        refreshSystemStates()
    }

    fun refreshSystemStates() {
        val context = getApplication<Application>()
        _uiState.update {
            it.copy(
                isAccessibilityEnabled = AccessibilityPermissionHelper.isAccessibilityServiceEnabled(context),
                isOverlayGranted = AccessibilityPermissionHelper.canDrawOverlays(context),
                isFacebookInstalled = AccessibilityPermissionHelper.isFacebookInstalled(context)
            )
        }
    }

    fun onNewGroupNameChanged(name: String) {
        _uiState.update { it.copy(newGroupName = name) }
    }

    fun addGroup() {
        val candidate = _uiState.value.newGroupName.trim()
        if (candidate.isEmpty()) {
            showMessage("Please enter a Facebook group name")
            return
        }
        viewModelScope.launch {
            val inserted = groupRepository.addGroup(candidate)
            if (inserted) {
                _uiState.update { it.copy(newGroupName = "") }
                showMessage("Added \"$candidate\"")
            } else {
                showMessage("\"$candidate\" is already in your checklist")
            }
        }
    }

    fun toggleGroupSelection(group: GroupEntity) {
        viewModelScope.launch {
            groupRepository.setGroupSelected(group.id, !group.isSelected)
        }
    }

    fun toggleSelectAll() {
        val currentGroups = groups.value
        if (currentGroups.isEmpty()) return
        val allSelected = currentGroups.all { it.isSelected }
        viewModelScope.launch {
            groupRepository.setAllSelected(!allSelected)
        }
    }

    fun deleteGroup(group: GroupEntity) {
        viewModelScope.launch {
            groupRepository.deleteGroup(group.id)
            showMessage("Removed \"${group.name}\"")
        }
    }

    fun onCaptionChanged(text: String) {
        _uiState.update { it.copy(captionText = text) }
    }

    fun onMediaPicked(uri: Uri?) {
        if (uri == null) return
        val context = getApplication<Application>()
        val info = MediaPreviewHelper.resolveMediaInfo(context, uri)
        _uiState.update { it.copy(selectedMedia = info) }
    }

    fun clearMedia() {
        _uiState.update { it.copy(selectedMedia = null) }
    }

    fun dismissAccessibilityDialog() {
        _uiState.update { it.copy(showAccessibilityDialog = false) }
    }

    fun dismissFacebookMissingDialog() {
        _uiState.update { it.copy(showFacebookMissingDialog = false) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun startAutoPosting() {
        refreshSystemStates()
        val state = _uiState.value
        val selectedGroups = groups.value.filter { it.isSelected }.map { it.name }

        if (selectedGroups.isEmpty()) {
            showMessage("Select at least one Facebook group from your checklist")
            return
        }
        if (state.captionText.isBlank() && state.selectedMedia == null) {
            showMessage("Write a post caption or select a photo/video first")
            return
        }
        if (!state.isAccessibilityEnabled) {
            _uiState.update { it.copy(showAccessibilityDialog = true) }
            return
        }
        if (!state.isFacebookInstalled) {
            _uiState.update { it.copy(showFacebookMissingDialog = true) }
            return
        }

        PostQueueManager.startPosting(
            groupNames = selectedGroups,
            captionText = state.captionText.trim(),
            mediaUriString = state.selectedMedia?.uri?.toString(),
            mediaMimeType = state.selectedMedia?.mimeType
        )

        val launched = AccessibilityPermissionHelper.launchFacebookApp(getApplication())
        if (!launched) {
            PostQueueManager.stopPosting("Could not open Facebook app")
            _uiState.update { it.copy(showFacebookMissingDialog = true) }
        }
    }

    fun stopAutoPosting() {
        PostQueueManager.stopPosting("Stopped by user")
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    class Factory(
        private val application: Application,
        private val groupRepository: GroupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(application, groupRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
