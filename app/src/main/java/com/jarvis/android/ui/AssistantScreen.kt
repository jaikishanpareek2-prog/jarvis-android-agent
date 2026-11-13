package com.jarvis.android.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.android.R
import com.jarvis.android.overlay.FloatingCompanionService
import com.jarvis.android.overlay.FloatingOrbSettings
import com.jarvis.android.overlay.FloatingOrbSettingsStore
import com.jarvis.android.overlay.OrbVisualState
import com.jarvis.android.viewmodel.AssistantViewModel
import com.jarvis.android.voice.VoiceAssistantEngine

@Composable
fun AssistantScreen(viewModel: AssistantViewModel = viewModel()) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var permissionMessage by remember { mutableStateOf("") }
    var showOverlaySettings by remember { mutableStateOf(false) }
    var currentState by remember { mutableStateOf(OrbVisualState.IDLE) }
    var settings by remember { mutableStateOf(FloatingOrbSettingsStore.load(context)) }
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionMessage = "Microphone permission granted. You can now use voice activation."
        } else {
            permissionMessage = "Microphone permission is required for voice input."
        }
    }

    val engine = remember(context) {
        VoiceAssistantEngine(
            context = context,
            onResult = { text ->
                prompt = text
                permissionMessage = "Heard: $text"
            },
            onStateChanged = { state -> currentState = state },
            onError = { error -> permissionMessage = error }
        )
    }

    DisposableEffect(engine) {
        onDispose { engine.release() }
    }

    val suggestions = listOf(
        "Summarize my day",
        "Find my nearest meeting",
        "Draft a response"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07131F),
                        Color(0xFF101A2E),
                        Color(0xFF0B1220)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.assistant_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            when (currentState) {
                                OrbVisualState.LISTENING -> Color(0xFF34D399)
                                OrbVisualState.ERROR -> Color(0xFFFF7A59)
                                else -> Color(0xFF6EA8FE)
                            },
                            CircleShape
                        )
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111E33)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFF6EA8FE),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "System online",
                            color = Color(0xFFB8D2FF),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Text(
                        text = "Good evening, Sir. JARVIS is ready for voice, hearing, and text interaction.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { suggestion ->
                            Button(
                                onClick = { prompt = suggestion },
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(text = suggestion)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@Button
                        }
                        if (Settings.canDrawOverlays(context)) {
                            val serviceIntent = Intent(context, FloatingCompanionService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                            permissionMessage = "Floating JARVIS is enabled. Tap the orb to open controls."
                        } else {
                            permissionMessage = "Android requires overlay permission before the floating orb can appear."
                            val overlayIntent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(overlayIntent)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(text = if (Settings.canDrawOverlays(context)) "Enable Floating JARVIS" else "Grant overlay access")
                }

                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@Button
                        }
                        if (engine.hasPermission()) {
                            engine.startListening()
                            permissionMessage = "Listening…"
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(text = "Listen")
                }
            }

            if (showOverlaySettings) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101D31)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToggleRow("Floating orb enabled", settings.enabled) {
                            settings = settings.copy(enabled = it)
                            FloatingOrbSettingsStore.save(context, settings)
                        }
                        ToggleRow("Background listening", settings.backgroundListening) {
                            settings = settings.copy(backgroundListening = it)
                            FloatingOrbSettingsStore.save(context, settings)
                        }
                        ToggleRow("Wake word", settings.wakeWordEnabled) {
                            settings = settings.copy(wakeWordEnabled = it)
                            FloatingOrbSettingsStore.save(context, settings)
                        }
                        ToggleRow("Voice responses", settings.voiceResponses) {
                            settings = settings.copy(voiceResponses = it)
                            FloatingOrbSettingsStore.save(context, settings)
                        }
                        Text(
                            text = "Orb size: ${settings.sizeDp} dp | Transparency: ${(settings.opacity * 100).toInt()}%",
                            color = Color(0xFFB8D2FF)
                        )
                    }
                }
            }

            Button(
                onClick = { showOverlaySettings = !showOverlaySettings },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(text = "Orb Settings")
            }

            if (permissionMessage.isNotEmpty()) {
                Text(
                    text = permissionMessage,
                    color = Color(0xFFB8D2FF),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(stringResource(id = R.string.prompt_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                shape = RoundedCornerShape(18.dp)
            )

            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        engine.speak(prompt)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(text = "Speak Response")
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
