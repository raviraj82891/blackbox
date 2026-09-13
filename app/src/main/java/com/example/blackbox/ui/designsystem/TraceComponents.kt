package com.example.blackbox.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.domain.fusion.SeverityLevel
import com.example.blackbox.service.ProtectionState
import com.example.blackbox.ui.theme.*

/**
 * TRACE Precision Design System Component Library.
 * Engineered, technical, sharp geometry (0dp corners), hairline borders,
 * high-contrast typography, and restrained semantic status indicators.
 */

@Composable
fun TraceTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    protectionState: ProtectionState? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        color = TraceCanvas
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TracePrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TracePrimary,
                                letterSpacing = 2.sp
                            )
                            if (protectionState != null) {
                                Spacer(modifier = Modifier.width(10.dp))
                                TraceStatusDot(state = protectionState)
                            }
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = TraceMuted
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    actions()
                }
            }
            TraceDivider()
        }
    }
}

@Composable
fun TraceStatusDot(
    state: ProtectionState,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        ProtectionState.ACTIVE -> TraceMintSuccess
        ProtectionState.PERMISSION_LIMITED -> TraceAmberWarning
        ProtectionState.PAUSED -> TraceMuted
        ProtectionState.ERROR -> TraceRedCritical
        ProtectionState.STARTING -> TraceBlueInfo
        ProtectionState.STOPPED -> TraceMuted
    }

    Box(
        modifier = modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}

@Composable
fun TraceDivider(
    modifier: Modifier = Modifier,
    color: Color = TraceHairline
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = color
    )
}

@Composable
fun TraceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = TracePrimary,
            letterSpacing = 2.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TraceMintSuccess,
                modifier = Modifier
                    .clickable { onActionClick() }
                    .padding(vertical = 4.dp),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun TracePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isCritical: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isCritical) TraceRedCritical else TracePrimary,
            contentColor = Color.Black,
            disabledContainerColor = TraceHairline,
            disabledContentColor = TraceMuted
        )
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TraceSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RectangleShape,
        border = BorderStroke(1.dp, TraceHairline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TracePrimary,
            disabledContentColor = TraceMuted
        )
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TraceCompactButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp),
        shape = RectangleShape,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        border = BorderStroke(1.dp, if (isPrimary) TraceMintSuccess else TraceHairline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isPrimary) TraceMintSuccess else TracePrimary,
            disabledContentColor = TraceMuted
        )
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.0.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TraceEmergencyAction(
    title: String = "SOS EMERGENCY ALERT",
    subtitle: String = "HOLD FOR 3 SECONDS OR TAP TO TRIGGER ALERT",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEmergencyActive: Boolean = false
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .border(1.dp, TraceRedCritical, RectangleShape)
            .clickable { onClick() }
            .semantics {
                contentDescription = "$title. $subtitle"
            },
        color = if (isEmergencyActive) TraceRedCritical else TraceSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (isEmergencyActive) Color.White else TraceRedCritical, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = (if (isEmergencyActive) "EMERGENCY ALERT ACTIVE" else title).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEmergencyActive) Color.White else TracePrimary,
                    letterSpacing = 1.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEmergencyActive) Color.White.copy(alpha = 0.9f) else TraceRedCritical,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TraceSpecRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isOk: Boolean = true,
    valueColor: Color? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = TraceMuted,
                letterSpacing = 1.sp,
                softWrap = true,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 12.dp)
            )
            Text(
                text = value.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor ?: if (isOk) TracePrimary else TraceAmberWarning,
                letterSpacing = 1.sp,
                textAlign = TextAlign.End,
                softWrap = true,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        TraceDivider()
    }
}

@Composable
fun TraceSettingRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingText: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 16.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TraceMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = TracePrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TraceMuted
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailingText != null) {
                    Text(
                        text = trailingText.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TraceMintSuccess,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TraceMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        TraceDivider()
    }
}

@Composable
fun TraceBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TraceMintSuccess
) {
    Surface(
        modifier = modifier.border(1.dp, color, RectangleShape),
        color = TraceSoftSurface
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            letterSpacing = 1.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun TraceEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TracePrimary,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TraceMuted,
            textAlign = TextAlign.Center
        )

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            TracePrimaryButton(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier.width(240.dp)
            )
        }
    }
}

@Composable
fun TraceSeverityBadge(
    severity: SeverityLevel,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (severity) {
        SeverityLevel.CRITICAL -> Pair("CRITICAL", TraceRedCritical)
        SeverityLevel.WARNING -> Pair("WARNING", TraceAmberWarning)
        SeverityLevel.INFO -> Pair("INFO", TraceBlueInfo)
    }

    TraceBadge(text = label, modifier = modifier, color = color)
}
