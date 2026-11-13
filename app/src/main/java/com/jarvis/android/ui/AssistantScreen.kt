package com.jarvis.android.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.jarvis.android.ai.JarvisAiFacade
import com.jarvis.android.device.DeviceActionController
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
    var response by remember { mutableStateOf("Your conversations stay on this screen and are not saved by this starter build.") }
    var permissionMessage by remember { mutableStateOf("") }
    var showOverlaySettings by remember { mutableStateOf(false) }
    var currentState by remember { mutableStateOf(OrbVisualState.IDLE) }
    var settings by remember { mutableStateOf(FloatingOrbSettingsStore.load(context)) }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionMessage = if (granted) "Microphone permission granted. Tap Listen to speak." else "Microphone permission is required for voice input."
    }

    val engine = remember(context) {
        VoiceAssistantEngine(
            context = context,
            onResult = { recognized -> prompt = recognized },
            onStateChanged = { state -> currentState = state },
            onError = { error -> permissionMessage = error }
        )
    }
    DisposableEffect(engine) { onDispose { engine.release() } }

    val actionController = remember(context) { DeviceActionController(context, JarvisAiFacade(context)) }
    val suggestions = listOf("What is my battery status?", "Open Wi-Fi settings", "Tell me about JARVIS")

    fun submit() {
        val userPrompt = prompt.trim()
        if (userPrompt.isEmpty()) return
        val result = runCatching { actionController.executeCommand(userPrompt) }
            .getOrElse { "I couldn't complete that request: ${it.message ?: "Android reported an error."}" }
        response = result
        engine.speak(result)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF07131F), Color(0xFF101A2E), Color(0xFF0B1220)))
        ).padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.assistant_title), style = MaterialTheme.typography.headlineMedium,
                        color = Color.White, fontWeight = FontWeight.Bold)
                    Text("PERSONAL ANDROID ASSISTANT", style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF82DFFF))
                }
                Box(Modifier.size(12.dp).background(
                    when (currentState) {
                        OrbVisualState.LISTENING -> Color(0xFF34D399)
                        OrbVisualState.ERROR -> Color(0xFFFF7A59)
                        else -> Color(0xFF6EA8FE)
                    }, CircleShape
                ))
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111E33)),
                shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SmartToy, contentDescription = null, tint = Color(0xFF6EA8FE), modifier = Modifier.size(26.dp))
                        Text("JARVIS · ${currentState.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            color = Color(0xFFB8D2FF), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(response, color = Color.White, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { suggestion ->
                    Button(onClick = { prompt = suggestion }, shape = RoundedCornerShape(50.dp)) {
                        Text(suggestion, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(stringResource(R.string.prompt_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = ::submit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text("Ask JARVIS")
                }
                Button(onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        engine.startListening()
                        permissionMessage = "Listening. Android's microphone indicator will remain visible while active."
                    } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text("Listen")
                }
            }

            Button(onClick = {
                if (Settings.canDrawOverlays(context)) {
                    // Started from the visible Activity: a normal service is sufficient and avoids
                    // the startForegroundService timeout (the overlay service is not an FGS).
                    context.startService(Intent(context, FloatingCompanionService::class.java))
                    permissionMessage = "Floating companion enabled. Android overlay permission is active."
                } else {
                    permissionMessage = "Grant Android's Display over other apps permission to show the floating orb."
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")))
                }
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(if (Settings.canDrawOverlays(context)) "Enable Floating JARVIS" else "Grant overlay access")
            }

            Button(onClick = { showOverlaySettings = !showOverlaySettings }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)) { Text("Floating companion settings") }

            if (showOverlaySettings) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF101D31)),
                    shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleRow("Floating orb enabled", settings.enabled) {
                            settings = settings.copy(enabled = it); FloatingOrbSettingsStore.save(context, settings)
                        }
                        ToggleRow("Background listening (not implemented)", settings.backgroundListening) {
                            settings = settings.copy(backgroundListening = it); FloatingOrbSettingsStore.save(context, settings)
                        }
                        ToggleRow("Wake word (not implemented)", settings.wakeWordEnabled) {
                            settings = settings.copy(wakeWordEnabled = it); FloatingOrbSettingsStore.save(context, settings)
                        }
                        ToggleRow("Voice responses", settings.voiceResponses) {
                            settings = settings.copy(voiceResponses = it); FloatingOrbSettingsStore.save(context, settings)
                        }
                        Text("Orb size ${settings.sizeDp} dp · opacity ${(settings.opacity * 100).toInt()}%",
                            color = Color(0xFFB8D2FF))
                    }
                }
            }

            if (permissionMessage.isNotEmpty()) {
                Text(permissionMessage, color = Color(0xFFB8D2FF), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
