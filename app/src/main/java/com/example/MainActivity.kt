package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automation.AccessibilityPermissionHelper
import com.example.data.local.AppDatabase
import com.example.data.repository.GroupRepository
import com.example.ui.AutoGroupPosterScreen
import com.example.ui.MainViewModel
import com.example.ui.MediaGetContentContract
import com.example.ui.theme.AutoGroupPosterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = AppDatabase.getInstance(applicationContext).groupDao()
        val repository = GroupRepository(dao)
        val viewModelFactory = MainViewModel.Factory(application, repository)

        setContent {
            AutoGroupPosterTheme {
                val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory)
                val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
                val groups by mainViewModel.groups.collectAsStateWithLifecycle()
                val queueState by mainViewModel.queueState.collectAsStateWithLifecycle()

                val mediaPickerLauncher = rememberLauncherForActivityResult(
                    contract = MediaGetContentContract(),
                    onResult = { uri ->
                        mainViewModel.onMediaPicked(uri)
                    }
                )

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            mainViewModel.refreshSystemStates()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                AutoGroupPosterScreen(
                    uiState = uiState,
                    groups = groups,
                    queueState = queueState,
                    onNewGroupNameChanged = mainViewModel::onNewGroupNameChanged,
                    onAddGroup = mainViewModel::addGroup,
                    onToggleGroup = mainViewModel::toggleGroupSelection,
                    onToggleSelectAll = mainViewModel::toggleSelectAll,
                    onDeleteGroup = mainViewModel::deleteGroup,
                    onCaptionChanged = mainViewModel::onCaptionChanged,
                    onSelectMediaClick = { mediaPickerLauncher.launch("*/*") },
                    onClearMediaClick = mainViewModel::clearMedia,
                    onEnableAccessibilityClick = {
                        AccessibilityPermissionHelper.openAccessibilitySettings(this)
                    },
                    onEnableOverlayClick = {
                        AccessibilityPermissionHelper.openOverlayPermissionSettings(this)
                    },
                    onPostToGroupsClick = mainViewModel::startAutoPosting,
                    onStopPostingClick = mainViewModel::stopAutoPosting,
                    onDismissAccessibilityDialog = mainViewModel::dismissAccessibilityDialog,
                    onDismissFacebookMissingDialog = mainViewModel::dismissFacebookMissingDialog,
                    onSnackbarShown = mainViewModel::clearSnackbarMessage
                )
            }
        }
    }
}
