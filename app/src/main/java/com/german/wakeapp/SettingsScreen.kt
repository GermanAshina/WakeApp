package com.german.wakeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsSection(
    streakManager: StreakManager,
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    var showSoundPicker by remember { mutableStateOf(false) }

    var soundType by remember { mutableStateOf(settingsManager.getDefaultSoundType()) }
    var volume by remember { mutableFloatStateOf(settingsManager.getAlarmVolume()) }

    val level = streakManager.getLevel()
    val xp = streakManager.getXp()
    val points = streakManager.getPoints()
    val streak = streakManager.getStreak()
    val freezeActive = streakManager.isStreakFrozen()

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) { "1.0" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Ajustes", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = CleanText)
        Spacer(modifier = Modifier.height(32.dp))

        // ============ SONIDO ============
        SectionHeader("SONIDO")

        SettingsClickRow(
            emoji = "🔔",
            title = "Sonido por defecto",
            subtitle = when (soundType) {
                "alarm" -> "Tono de alarma"
                "notification" -> "Tono de notificación"
                "ringtone" -> "Tono de llamada"
                else -> "Tono de alarma"
            },
            onClick = { showSoundPicker = true }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CleanSurface)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔊", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Volumen de alarma", fontWeight = FontWeight.Bold, color = CleanText, fontSize = 16.sp)
                    Text("${(volume * 100).toInt()}%", fontSize = 14.sp, color = CleanTextDim)
                }
            }
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    settingsManager.setAlarmVolume(it)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = CleanText,
                    activeTrackColor = WakeBlue,
                    inactiveTrackColor = CleanSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ============ TUS DATOS ============
        SectionHeader("TUS DATOS")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                SettingsInfoRow("Nivel actual", "$level")
                HorizontalDivider(color = CleanSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                SettingsInfoRow("XP total", "$xp")
                HorizontalDivider(color = CleanSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                SettingsInfoRow("Puntos", "$points")
                HorizontalDivider(color = CleanSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                SettingsInfoRow("Racha actual", "$streak días")
                HorizontalDivider(color = CleanSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                SettingsInfoRow("Estado", if (freezeActive) "Congelada" else "Activa")
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ============ INFORMACIÓN ============
        SectionHeader("INFORMACIÓN")

        SettingsInfoCard(
            emoji = "ℹ️",
            title = "Acerca de WakeApp",
            subtitle = "Versión $versionName · Despertador inteligente"
        )
        Spacer(modifier = Modifier.height(16.dp))

        SettingsInfoCard(
            emoji = "🔐",
            title = "Privacidad",
            subtitle = "Todos los datos se guardan localmente en tu dispositivo"
        )

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showSoundPicker) {
        AlertDialog(
            onDismissRequest = { showSoundPicker = false },
            containerColor = CleanSurface,
            title = { Text("Sonido por defecto", color = CleanText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(
                        "alarm" to "Tono de alarma",
                        "notification" to "Notificación",
                        "ringtone" to "Tono de llamada"
                    ).forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (soundType == key) CleanSurfaceVariant else Color.Transparent)
                                .clickable {
                                    soundType = key
                                    settingsManager.setDefaultSoundType(key)
                                    showSoundPicker = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = CleanText, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            if (soundType == key) {
                                Text("✓", color = WakeBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSoundPicker = false }) {
                    Text("Cerrar", color = WakeBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CleanTextDim,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        HorizontalDivider(color = CleanSurfaceVariant, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingsClickRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CleanSurface)
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = CleanText, fontSize = 16.sp)
            Text(subtitle, fontSize = 14.sp, color = CleanTextDim)
        }
        Text("›", fontSize = 24.sp, color = CleanTextDim, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun SettingsInfoCard(
    emoji: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CleanSurface)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = CleanText, fontSize = 16.sp)
            Text(subtitle, fontSize = 14.sp, color = CleanTextDim, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CleanTextDim, fontSize = 15.sp)
        Text(value, color = CleanText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}