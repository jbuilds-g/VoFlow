package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

private fun modelLabel(model: String): String = when (model) {
    DEFAULT_MODEL -> "Gemini 3.5 Flash-Lite (Default)"
    "gemini-3.5-transcribe" -> "Gemini 3.5 Transcribe"
    else -> model
}

@Composable
fun ModelSelectionSection(
    selectedModel: String,
    models: List<String>,
    isLoading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    onModelSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme

    Card(modifier = modifier.fillMaxWidth(), shape = androidx.compose.material3.MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer), border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Gemini model", style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = !isLoading && models.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isLoading && models.isEmpty()) "Loading available models…" else modelLabel(selectedModel))
                }
                DropdownMenu(
                    expanded = expanded && models.isNotEmpty() && !isLoading,
                    onDismissRequest = { expanded = false }
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(modelLabel(model)) },
                            onClick = {
                                onModelSelect(model)
                                expanded = false
                            }
                        )
                    }
                }
            }
            when {
                isLoading -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                    Text("Loading models from Google AI Studio…", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                }
                loadError != null -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Could not load models: $loadError", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
                models.isEmpty() -> Text("No models available yet. Save and validate your API key to load models.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                else -> Text("Choose which Gemini model handles dictation. The default is Gemini 3.5 Flash-Lite.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
            }
        }
    }
}
