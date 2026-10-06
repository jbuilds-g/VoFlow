package com.example.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.TranscriptionHistory
import com.example.data.TranscriptionHistoryEntry
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(emptyList<TranscriptionHistoryEntry>()) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPlayback() {
        player?.runCatching { stop() }
        player?.release()
        player = null
        playingId = null
        isPlaying = false
    }

    fun playEntry(entry: TranscriptionHistoryEntry) {
        if (playingId == entry.id && player != null) {
            if (isPlaying) {
                player?.pause()
                isPlaying = false
            } else {
                player?.start()
                isPlaying = true
            }
            return
        }

        stopPlayback()
        val file = TranscriptionHistory.getAudioFile(context, entry) ?: return
        runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    release()
                    player = null
                    playingId = null
                    isPlaying = false
                }
                prepare()
                start()
            }.also {
                player = it
                playingId = entry.id
                isPlaying = true
            }
        }.onFailure {
            stopPlayback()
        }
    }

    fun deleteEntry(id: String) {
        if (playingId == id) stopPlayback()
        TranscriptionHistory.delete(context, id)
        entries = TranscriptionHistory.getAll(context)
    }

    LaunchedEffect(Unit) {
        entries = TranscriptionHistory.getAll(context)
    }

    DisposableEffect(Unit) {
        onDispose { stopPlayback() }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "History",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    "Your transcriptions and recordings from the last ${TranscriptionHistory.RETENTION_DAYS} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entries.isNotEmpty()) {
                OutlinedButton(onClick = { showClearDialog = true }, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (entries.isEmpty()) {
            HistoryEmptyState(modifier = Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    HistoryEntryCard(
                        entry = entry,
                        isPlaying = playingId == entry.id && isPlaying,
                        isLoaded = playingId == entry.id,
                        onPlayPause = { playEntry(entry) },
                        onStop = { if (playingId == entry.id) stopPlayback() },
                        onDelete = { deleteId = entry.id }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Delete transcription?") },
            text = { Text("This deletes the transcription and its saved audio recording.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteEntry(id)
                    deleteId = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear history?") },
            text = { Text("This permanently deletes all saved transcriptions and their audio recordings.") },
            confirmButton = {
                TextButton(onClick = {
                    stopPlayback()
                    TranscriptionHistory.clear(context)
                    entries = emptyList()
                    showClearDialog = false
                }) { Text("Clear all") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HistoryEntryCard(
    entry: TranscriptionHistoryEntry,
    isPlaying: Boolean,
    isLoaded: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(entry.text, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = if (isPlaying) colors.primaryContainer else colors.surfaceContainerHighest
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(onClick = onPlayPause) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause recording" else "Play recording",
                                tint = if (isPlaying) colors.onPrimaryContainer else colors.onSurfaceVariant
                            )
                        }
                    }
                }
                if (isLoaded) {
                    IconButton(onClick = onStop) {
                        Icon(Icons.Rounded.Stop, contentDescription = "Stop recording")
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.timestampMs)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${formatDuration(entry.durationMs)} • ${formatBytes(entry.fileSizeBytes)} • ${entry.mode.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        entry.model.removePrefix("models/"),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete transcription", tint = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HistoryEmptyState(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(52.dp), tint = colors.primary)
            Text("No transcriptions yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Successful dictations will appear here with their original audio recording.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
            Text(
                "History is stored locally and automatically retained for 30 days.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "${bytes} B"
    val kb = bytes / 1024L
    if (kb < 1024L) return "${kb} KB"
    return "%.1f MB".format(bytes / 1024f / 1024f)
}
