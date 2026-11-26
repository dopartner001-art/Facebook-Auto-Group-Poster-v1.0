package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.automation.PostQueueState
import com.example.data.local.GroupEntity
import com.example.ui.components.AutoPostFooterBar
import com.example.ui.components.GroupChecklistSection
import com.example.ui.components.LiveAutomationStatusCard
import com.example.ui.components.PermissionStatusBanner
import com.example.ui.components.PostComposerSection
import com.example.ui.theme.FbBlue
import com.example.ui.theme.FbBlueDark

@Composable
fun AutoGroupPosterScreen(
    uiState: MainUiState,
    groups: List<GroupEntity>,
    queueState: PostQueueState,
    onNewGroupNameChanged: (String) -> Unit,
    onAddGroup: () -> Unit,
    onToggleGroup: (GroupEntity) -> Unit,
    onToggleSelectAll: () -> Unit,
    onDeleteGroup: (GroupEntity) -> Unit,
    onCaptionChanged: (String) -> Unit,
    onSelectMediaClick: () -> Unit,
    onClearMediaClick: () -> Unit,
    onEnableAccessibilityClick: () -> Unit,
    onEnableOverlayClick: () -> Unit,
    onPostToGroupsClick: () -> Unit,
    onStopPostingClick: () -> Unit,
    onDismissAccessibilityDialog: () -> Unit,
    onDismissFacebookMissingDialog: () -> Unit,
    onSnackbarShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            onSnackbarShown()
        }
    }

    val selectedCount = groups.count { it.isSelected }
    val hasPostContent = uiState.captionText.isNotBlank() || uiState.selectedMedia != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AutoGroupPosterTopHeader(
                selectedCount = selectedCount,
                totalCount = groups.size,
                isServiceReady = uiState.isAccessibilityEnabled
            )
        },
        bottomBar = {
            AutoPostFooterBar(
                selectedGroupCount = selectedCount,
                hasPostContent = hasPostContent,
                isRunning = queueState.isRunning,
                onPostClick = onPostToGroupsClick,
                onStopClick = onStopPostingClick
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpanded = maxWidth >= 840.dp

            if (isExpanded) {
                // Tablet / Foldable Two-Pane Canonical Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1120.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PermissionStatusBanner(
                            isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                            isOverlayGranted = uiState.isOverlayGranted,
                            onEnableAccessibilityClick = onEnableAccessibilityClick,
                            onEnableOverlayClick = onEnableOverlayClick
                        )

                        GroupChecklistSection(
                            groups = groups,
                            newGroupName = uiState.newGroupName,
                            onNewGroupNameChanged = onNewGroupNameChanged,
                            onAddGroup = onAddGroup,
                            onToggleGroup = onToggleGroup,
                            onToggleSelectAll = onToggleSelectAll,
                            onDeleteGroup = onDeleteGroup
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LiveAutomationStatusCard(
                            queueState = queueState,
                            onStopClick = onStopPostingClick
                        )

                        PostComposerSection(
                            captionText = uiState.captionText,
                            selectedMedia = uiState.selectedMedia,
                            onCaptionChanged = onCaptionChanged,
                            onSelectMediaClick = onSelectMediaClick,
                            onClearMediaClick = onClearMediaClick
                        )
                    }
                }
            } else {
                // Handheld Vertical Layout
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .align(Alignment.TopCenter),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "permissions_banner") {
                        PermissionStatusBanner(
                            isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                            isOverlayGranted = uiState.isOverlayGranted,
                            onEnableAccessibilityClick = onEnableAccessibilityClick,
                            onEnableOverlayClick = onEnableOverlayClick
                        )
                    }

                    item(key = "live_status_card") {
                        LiveAutomationStatusCard(
                            queueState = queueState,
                            onStopClick = onStopPostingClick
                        )
                    }

                    item(key = "group_checklist_section") {
                        GroupChecklistSection(
                            groups = groups,
                            newGroupName = uiState.newGroupName,
                            onNewGroupNameChanged = onNewGroupNameChanged,
                            onAddGroup = onAddGroup,
                            onToggleGroup = onToggleGroup,
                            onToggleSelectAll = onToggleSelectAll,
                            onDeleteGroup = onDeleteGroup
                        )
                    }

                    item(key = "post_composer_section") {
                        PostComposerSection(
                            captionText = uiState.captionText,
                            selectedMedia = uiState.selectedMedia,
                            onCaptionChanged = onCaptionChanged,
                            onSelectMediaClick = onSelectMediaClick,
                            onClearMediaClick = onClearMediaClick
                        )
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (uiState.showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = onDismissAccessibilityDialog,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.AccessibilityNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(text = "Enable Accessibility Service")
            },
            text = {
                Text(
                    text = "Auto Group Poster needs Android Accessibility Service enabled to search your checked groups, insert your caption directly without the clipboard, attach media, and click Post.\n\nIn Settings, tap \"Installed apps\" -> \"Auto Group Poster Engine\" -> Turn On."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissAccessibilityDialog()
                        onEnableAccessibilityClick()
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissAccessibilityDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showFacebookMissingDialog) {
        AlertDialog(
            onDismissRequest = onDismissFacebookMissingDialog,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(text = "Official Facebook App Required")
            },
            text = {
                Text(
                    text = "The official Facebook app (com.facebook.katana) was not found on this device. Install and sign in to Facebook before starting auto posting."
                )
            },
            confirmButton = {
                Button(onClick = onDismissFacebookMissingDialog) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun AutoGroupPosterTopHeader(
    selectedCount: Int,
    totalCount: Int,
    isServiceReady: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(FbBlueDark, FbBlue)
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Campaign,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isServiceReady) {
                            "Accessibility Engine Ready"
                        } else {
                            "Facebook Group Automation"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "$selectedCount/$totalCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
