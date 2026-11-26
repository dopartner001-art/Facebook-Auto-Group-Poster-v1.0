package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberSoft

@Composable
fun PermissionStatusBanner(
    isAccessibilityEnabled: Boolean,
    isOverlayGranted: Boolean,
    onEnableAccessibilityClick: () -> Unit,
    onEnableOverlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isAccessibilityEnabled && isOverlayGranted) {
            MaterialTheme.colorScheme.surface
        } else if (!isAccessibilityEnabled) {
            WarningAmberSoft.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        },
        label = "permissionBannerColor"
    )

    val borderColor = when {
        isAccessibilityEnabled && isOverlayGranted -> MaterialTheme.colorScheme.outlineVariant
        !isAccessibilityEnabled -> WarningAmber.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAccessibilityEnabled) 1.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        color = if (isAccessibilityEnabled) SuccessGreenSoft else WarningAmberSoft
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityEnabled) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.AccessibilityNew
                            },
                            contentDescription = "Accessibility Service status",
                            tint = if (isAccessibilityEnabled) SuccessGreen else WarningAmber,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isAccessibilityEnabled) {
                                "Accessibility Service Active"
                            } else {
                                "Accessibility Permission Needed"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAccessibilityEnabled) {
                                "Ready to automate group posting in Facebook"
                            } else {
                                "Enable Auto Group Poster in Settings to auto-fill & publish"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isAccessibilityEnabled) {
                    Button(
                        onClick = onEnableAccessibilityClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("enable_accessibility_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessibilityNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Enable Service",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onEnableAccessibilityClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("enable_accessibility_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Service Settings",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (!isOverlayGranted) {
                    OutlinedButton(
                        onClick = onEnableOverlayClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("enable_overlay_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Allow Overlay",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SuccessGreenSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = "Floating widget permitted",
                                tint = SuccessGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Floating Widget On",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (!isOverlayGranted && isAccessibilityEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Allow Display Over Other Apps to see the floating Stop & Progress widget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
