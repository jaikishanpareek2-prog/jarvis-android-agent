package com.jarvis.android.ui

import android.content.Intent
import android.os.Build
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.jarvis.android.R
import com.jarvis.android.service.VoiceAssistantService

@Composable
fun AssistantScreen() {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
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
                        .background(Color(0xFF34D399), CircleShape)
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
                        text = "Good evening, Sir. Your personal Android AI assistant is ready."
                            + " I can help manage your schedule, messages, notes, and device tasks.",
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF12213A),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        val serviceIntent = Intent(context, VoiceAssistantService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(96.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp)
                    )
                }
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
                onClick = { /* Agent action hook */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(text = "Run AI Assistant")
            }
        }
    }
}
