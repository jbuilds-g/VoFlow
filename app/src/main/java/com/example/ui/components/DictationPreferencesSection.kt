package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DictationPreferencesSection(
    transcriptionMode: String,
    onModeSelect: (String) -> Unit,
    useExperimentalOpus: Boolean,
    isOpusSupported: Boolean,
    isBusy: Boolean,
    onExperimentalOpusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dictation preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    "Choose how VoFlow processes speech and which audio format it records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transcription mode", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = transcriptionMode == "smart",
                        onClick = { onModeSelect("smart") },
                        label = { Text("Smart") }
                    )
                    FilterChip(
                        selected = transcriptionMode == "verbatim",
                        onClick = { onModeSelect("verbatim") },
                        label = { Text("Verbatim") }
                    )
                }
                Text(
                    if (transcriptionMode == "smart") "Removes filler words and applies formatting." else "Keeps the spoken wording as close to the original as possible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Recording format", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !useExperimentalOpus,
                        onClick = { onExperimentalOpusChange(false) },
                        enabled = !isBusy,
                        label = { Text("AAC (Recommended)") }
                    )
                    FilterChip(
                        selected = useExperimentalOpus,
                        onClick = { if (isOpusSupported) onExperimentalOpusChange(true) },
                        enabled = isOpusSupported && !isBusy,
                        label = { Text("Opus (Experimental)") }
                    )
                }
                Text(
                    when {
                        !isOpusSupported -> "Opus requires Android 10 or newer. AAC is active on this device."
                        useExperimentalOpus -> "Experimental: records Opus in an Ogg file. If recording cannot start Opus, VoFlow falls back to AAC."
                        else -> "AAC is the default format. Opus is an experimental option for Android 10 and newer."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
