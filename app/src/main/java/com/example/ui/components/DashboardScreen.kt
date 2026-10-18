package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.MainUiState
import com.example.data.TranscriptionHistory
import com.example.data.TranscriptionHistoryEntry
import java.util.Locale
import java.text.DateFormat
import java.util.Date

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    onToggleOverlay: (Boolean) -> Unit,
    onStartSandboxRecording: () -> Unit,
    onStopSandboxRecording: () -> Unit,
    onSandboxTextChange: (String) -> Unit,
    onClearSandboxText: () -> Unit,
    onCopySandboxText: () -> Unit,
    onDismissSandboxError: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var history by remember { mutableStateOf(emptyList<TranscriptionHistoryEntry>()) }
    var showQuickTest by remember { mutableStateOf(false) }

    fun refreshHistory() {
        history = TranscriptionHistory.getAll(context)
    }

    LaunchedEffect(Unit) { refreshHistory() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshHistory()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allPermissionsGranted = uiState.hasAudioPermission &&
        uiState.hasOverlayPermission && uiState.hasAccessibilityPermission
    val totalWords = history.sumOf { entry -> entry.text.trim().split(Regex("\\s+")).count { it.isNotBlank() } }
    val totalDurationMs = history.sumOf { it.durationMs }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Your voice, captured.",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "System-wide AI dictation, ready when you are.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // The in-app dictation test is the primary dashboard entry point.
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize().clickable { showQuickTest = !showQuickTest },
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Try voice dictation",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (showQuickTest) "Your dictation workspace is below" else "Speak naturally and see your words appear",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    if (showQuickTest) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (showQuickTest) "Collapse dictation test" else "Open dictation test",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        AnimatedVisibility(
            visible = showQuickTest,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            VoiceSandboxSection(
                isRecording = uiState.isSandboxRecording,
                isProcessing = uiState.isSandboxProcessing,
                recordingTimeSeconds = uiState.sandboxRecordingSeconds,
                transcribedText = uiState.sandboxTranscribedText,
                onTranscribedTextChange = onSandboxTextChange,
                onStartRecording = onStartSandboxRecording,
                onStopRecording = onStopSandboxRecording,
                onClearText = onClearSandboxText,
                onCopyText = onCopySandboxText,
                lastErrorMessage = uiState.lastErrorMessage,
                lastAudioInfo = uiState.lastAudioInfo,
                onDismissError = onDismissSandboxError
            )
        }

        HeroStatusBar(
            isOverlayRunning = uiState.isOverlayServiceRunning,
            allPermissionsGranted = allPermissionsGranted && uiState.hasValidApiKey,
            onToggleOverlay = onToggleOverlay,
            onOpenAbout = onOpenSettings
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Activity", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                Text("Last 30 days", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        value = history.size.toString(),
                        label = "Transcriptions"
                    )
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                        value = formatTotalDuration(totalDurationMs),
                        label = "Dictation time"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
                        value = String.format(Locale.getDefault(), "%,d", totalWords),
                        label = "Words"
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
            if (history.isEmpty()) {
                Text(
                    "Your stats will appear here after your first successful dictation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent transcriptions", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    TextButton(onClick = onOpenHistory) { Text("View all") }
                }
            }
            if (history.isEmpty()) {
                Text(
                    "Your latest dictations will show up here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                history.take(3).forEach { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenHistory),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                entry.text,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.timestampMs)) +
                                    " · " + formatTotalDuration(entry.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Readiness", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ReadinessRow("Microphone permission", uiState.hasAudioPermission, onRequestAudioPermission)
                    ReadinessRow("Display over other apps", uiState.hasOverlayPermission, onRequestOverlayPermission)
                    ReadinessRow("Accessibility service", uiState.hasAccessibilityPermission, onRequestAccessibilityPermission)
                    ReadinessRow("Gemini API key", uiState.hasValidApiKey, onOpenSettings)
                    if (!allPermissionsGranted || !uiState.hasValidApiKey) {
                        Text(
                            "Select a requirement to grant access or configure your API key.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Box(Modifier.padding(7.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSecondaryContainer
                        ) { icon() }
                    }
                }
            }
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
private fun ReadinessRow(label: String, isReady: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isReady) Icons.Rounded.CheckCircle else Icons.Rounded.Settings,
            contentDescription = null,
            tint = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            if (isReady) "Ready" else "Needs setup",
            style = MaterialTheme.typography.labelMedium,
            color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

private fun formatTotalDuration(durationMs: Long): String {
    val totalMinutes = (durationMs / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
}
