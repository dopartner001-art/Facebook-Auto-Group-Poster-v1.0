package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.automation.AutomationStep
import com.example.automation.PostQueueState
import com.example.ui.theme.DangerRed

@Composable
fun LiveAutomationStatusCard(
    queueState: PostQueueState,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = queueState.isRunning ||
            queueState.step == AutomationStep.COMPLETED ||
            queueState.step == AutomationStep.STOPPED
    ) {
        val isRunning = queueState.isRunning
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (isRunning) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            border = BorderStroke(
                1.dp,
                if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRunning) {
                                "Posting to ${queueState.displayGroupNumber} of ${queueState.totalGroups} groups..."
                            } else {
                                queueState.statusMessage
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isRunning && queueState.currentGroupName.isNotBlank()) {
                            val stepLine = if (queueState.countdownSeconds > 0) {
                                "${queueState.currentGroupName} • ${queueState.statusMessage} (${queueState.countdownSeconds}s)"
                            } else {
                                "${queueState.currentGroupName} • ${queueState.statusMessage}"
                            }
                            Text(
                                text = stepLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isRunning) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onStopClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("in_app_stop_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerRed,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.StopCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.stop_posting))
                        }
                    }
                }

                if (isRunning && queueState.totalGroups > 0) {
                    LinearProgressIndicator(
                        progress = {
                            queueState.displayGroupNumber.toFloat() / queueState.totalGroups.toFloat()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@Composable
fun AutoPostFooterBar(
    selectedGroupCount: Int,
    hasPostContent: Boolean,
    isRunning: Boolean,
    onPostClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedGroupCount == 0) {
                        "No groups checked"
                    } else {
                        "Queue: $selectedGroupCount checked ${if (selectedGroupCount == 1) "group" else "groups"}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selectedGroupCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "8–15s human pause per post",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isRunning) {
                Button(
                    onClick = onStopClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("post_to_groups_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerRed,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.StopCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stop Auto Posting",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onPostClick,
                    enabled = selectedGroupCount > 0 && hasPostContent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("post_to_groups_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.post_to_selected_groups),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
