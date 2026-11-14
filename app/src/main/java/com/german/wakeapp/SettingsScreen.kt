package com.german.wakeapp

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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
    val themeManager = remember { ThemeManager(context) }

    var showSoundPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    var vibrationEnabled by remember { mutableStateOf(settingsManager.isVibrationEnabled()) }
    var volume by remember { mutableFloatStateOf(settingsManager.getAlarmVolume()) }
    var confirmDelete by remember { mutableStateOf(settingsManager.isConfirmDeleteEnabled()) }
    var soundType by remember { mutableStateOf(settingsManager.getDefaultSoundType()) }
    var activeTheme by remember { mutableStateOf(themeManager.getActive()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Ajustes", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = CleanText)
        Spacer(modifier = Modifier.height(32.dp))

        // --- AJUSTES GENERALES ---
        Text("GENERAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CleanTextDim, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Vibración
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CleanSurface)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📳", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Vibración al despertar", fontWeight = FontWeight.Bold, color = CleanText, fontSize = 16.sp)
                Text("El móvil vibrará mientras suena la alarma.", fontSize = 13.sp, color = CleanTextDim)
            }
            Switch(
                checked = vibrationEnabled,
                onCheckedChange = {
                    vibrationEnabled = it
                    settingsManager.setVibrationEnabled(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CleanText,
                    checkedTrackColor = WakeBlue,
                    uncheckedThumbColor = CleanTextDim,
                    uncheckedTrackColor = CleanSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Volumen
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
        Spacer(modifier = Modifier.height(16.dp))

        // Confirmar borrado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CleanSurface)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🗑", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Confirmar borrado", fontWeight = FontWeight.Bold, color = CleanText, fontSize = 16.sp)
                Text("Preguntar antes de eliminar una alarma.", fontSize = 13.sp, color = CleanTextDim)
            }
            Switch(
                checked = confirmDelete,
                onCheckedChange = {
                    confirmDelete = it
                    settingsManager.setConfirmDeleteEnabled(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CleanText,
                    checkedTrackColor = WakeBlue,
                    uncheckedThumbColor = CleanTextDim,
                    uncheckedTrackColor = CleanSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- PERSONALIZACIÓN ---
        Text("PERSONALIZACIÓN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CleanTextDim, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Sonido
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CleanSurface)
                .clickable { showSoundPicker = true }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔔", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Sonido por defecto", fontWeight = FontWeight.Bold, color = CleanText, fontSize = 16.sp)
                Text(when (soundType) {
                    "alarm" -> "Tono de alarma"
                    "notification" -> "Tono de notificación"
                    "ringtone" -> "Tono de llamada"
                    else -> "Tono de alarma"
                }, fontSize = 14.sp, color = CleanTextDim)
            }
            Text("›", fontSize = 24.sp, color = CleanTextDim, fontWeight = FontWeight.Light)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Tema
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CleanSurface)
                .clickable { showThemePicker = true }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(activeTheme.emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Tema de la app", fontWeight = FontWeight.Bold, color = CleanText, fontSize = 16.sp)
                Text(activeTheme.name, fontSize = 14.sp, color = CleanTextDim)
            }
            Text("›", fontSize = 24.sp, color = CleanTextDim, fontWeight = FontWeight.Light)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // --- Diálogo de sonido ---
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

    // --- Diálogo de tema ---
    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            containerColor = CleanSurface,
            title = { Text("Tema de la app", color = CleanText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeCatalog.all.forEach { theme ->
                        val unlocked = themeManager.isUnlocked(theme.id) || theme.price == 0
                        val isActive = activeTheme.id == theme.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) CleanSurfaceVariant else Color.Transparent)
                                .clickable(enabled = unlocked) {
                                    themeManager.setActive(theme.id)
                                    activeTheme = theme
                                    showThemePicker = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(theme.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    theme.name,
                                    color = if (unlocked) CleanText else CleanTextDim,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (!unlocked) "🔒 Bloqueado" else theme.emoji,
                                    color = CleanTextDim,
                                    fontSize = 12.sp
                                )
                            }
                            if (isActive) {
                                Text("✓", color = WakeBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePicker = false }) {
                    Text("Cerrar", color = WakeBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}