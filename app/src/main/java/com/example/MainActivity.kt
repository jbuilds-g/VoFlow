package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SettingsScreen
import com.example.ui.components.HistoryScreen
import com.example.ui.components.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                var selectedDestination by remember { mutableStateOf(AppDestination.HOME) }
                val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    viewModel.refreshPermissionStates()
                    if (!isGranted) Toast.makeText(this, "Microphone permission is required for dictation", Toast.LENGTH_SHORT).show()
                }
                LaunchedEffect(uiState.feedbackMessage) {
                    uiState.feedbackMessage?.let { msg -> snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short); viewModel.clearFeedback() }
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 76.dp)
                        ) {
                            when (selectedDestination) {
                                AppDestination.SETTINGS -> SettingsScreen(
                                    apiKey = uiState.apiKey,
                                    onApiKeyChange = { viewModel.updateApiKey(it) },
                                    isValidatingApiKey = uiState.isValidatingApiKey,
                                    apiKeyValidationResult = uiState.apiKeyValidationResult,
                                    isApiKeyValid = uiState.isApiKeyValid,
                                    onValidateApiKey = { viewModel.validateAndSaveApiKey() },
                                    selectedModel = uiState.selectedModel,
                                    availableModels = uiState.availableModels,
                                    onModelSelect = { viewModel.setSelectedModel(it) },
                                    transcriptionMode = uiState.transcriptionMode,
                                    onModeSelect = { viewModel.setTranscriptionMode(it) },
                                    useExperimentalOpus = uiState.useExperimentalOpus,
                                    isOpusSupported = uiState.isOpusSupported,
                                    isDictationBusy = uiState.isSandboxRecording || uiState.isSandboxProcessing,
                                    onExperimentalOpusChange = { viewModel.setExperimentalOpusEnabled(it) },
                                    hasAudioPermission = uiState.hasAudioPermission,
                                    hasOverlayPermission = uiState.hasOverlayPermission,
                                    hasAccessibilityPermission = uiState.hasAccessibilityPermission,
                                    onRequestAudioPermission = { audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) },
                                    onRequestOverlayPermission = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) },
                                    onRequestAccessibilityPermission = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                                    onBack = { selectedDestination = AppDestination.HOME },
                                    modifier = Modifier.fillMaxSize()
                                )
                                AppDestination.HISTORY -> HistoryScreen(modifier = Modifier.fillMaxSize())
                                AppDestination.HOME -> DashboardScreen(
                                    uiState = uiState,
                                    onToggleOverlay = { viewModel.toggleOverlayService(it) },
                                    onStartSandboxRecording = { if (!uiState.hasAudioPermission) audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) else viewModel.startSandboxRecording() },
                                    onStopSandboxRecording = { viewModel.stopSandboxRecording() },
                                    onSandboxTextChange = { viewModel.updateSandboxText(it) },
                                    onClearSandboxText = { viewModel.clearSandboxText() },
                                    onCopySandboxText = { viewModel.copySandboxText() },
                                    onDismissSandboxError = { viewModel.clearErrorMessage() },
                                    onOpenSettings = { selectedDestination = AppDestination.SETTINGS },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        AppBottomNavigation(
                            selectedDestination = selectedDestination,
                            onDestinationSelected = { selectedDestination = it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); viewModel.refreshPermissionStates(); if (viewModel.uiState.value.hasValidApiKey) viewModel.loadAvailableModels() }
}

private enum class AppDestination(val label: String) {
    HOME("Home"),
    HISTORY("History"),
    SETTINGS("Settings")
}

@Composable
private fun AppBottomNavigation(
    selectedDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .widthIn(max = 420.dp)
            .wrapContentWidth(),
        color = colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
        tonalElevation = 3.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppDestination.entries.forEach { destination ->
                ExpressivePillNavItem(
                    destination = destination,
                    selected = selectedDestination == destination,
                    onClick = { onDestinationSelected(destination) }
                )
            }
        }
    }
}

@Composable
private fun ExpressivePillNavItem(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "navContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "navContentColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "navScale"
    )

    Surface(
        modifier = Modifier
            .height(44.dp)
            .wrapContentWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (selected) 16.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when (destination) {
                    AppDestination.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                    AppDestination.HISTORY -> if (selected) Icons.Filled.History else Icons.Outlined.History
                    AppDestination.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
                },
                contentDescription = destination.label,
                modifier = Modifier.size(20.dp)
            )
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = shrinkHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
