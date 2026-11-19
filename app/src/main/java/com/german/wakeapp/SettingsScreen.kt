package com.german.wakeapp

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var showSoundPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    // Volumen del stream de alarma (0..1 relativo al máximo del sistema)
    var alarmVolume by remember {
        mutableFloatStateOf(readAlarmVolumeFraction(audioManager))
    }

    var confirmDelete by remember { mutableStateOf(settingsManager.isConfirmDeleteEnabled()) }
    var soundType by remember { mutableStateOf(settingsManager.getDefaultSoundType()) }
    var activeTheme by remember { mutableStateOf(themeManager.getActive()) }
    var lightMode by remember { mutableStateOf(settingsManager.isLightMode()) }

    // Observador del volumen del sistema: si se cambia desde el panel del móvil,
    // el slider se actualiza en vivo.
    DisposableEffect(audioManager, context) {
        // CORRECCIÓN: Usar el nombre del stream directamente para evitar el error de inferencia de tipos
        val uri = Settings.System.getUriFor("alarm_stream")
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                alarmVolume = readAlarmVolumeFraction(audioManager)
            }
        }
        try {
            context.contentResolver.registerContentObserver(uri, false, observer)
        } catch (_: Exception) {}
        onDispose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {}
        }
    }

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

        // Volumen real del stream de alarma
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
                    Text(
                        "${(alarmVolume * 100).toInt()}% · Volumen del sistema",
                        fontSize = 14.sp,
                        color = CleanTextDim
                    )
                }
            }
            Slider(
                value = alarmVolume,
                onValueChange = { fraction ->
                    alarmVolume = fraction
                    writeAlarmVolumeFraction(audioManager, fraction)
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

        // Modo claro / oscuro
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CleanSurface)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (lightMode) "☀️" else "🌙", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Modo claro", fontWeight = FontWeight.Bold, color = CleanText, fontSize = 16.sp)
                Text(
                    if (lightMode) "Interfaz clara activada." else "Interfaz oscura activada.",
                    fontSize = 13.sp,
                    color = CleanTextDim
                )
            }
            Switch(
                checked = lightMode,
                onCheckedChange = {
                    lightMode = it
                    settingsManager.setLightMode(it)
                    AppPalette.isLight = it   // repinta toda la app al instante
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
                        val unlocked = themeManager.isUnlocked(theme.id)
                        val isActive = activeTheme.id == theme.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) CleanSurfaceVariant else Color.Transparent)
                                .clickable(enabled = unlocked) {
                                    themeManager.setActive(theme.id)
                                    activeTheme = theme
                                    AppPalette.applyTheme(theme)   // repinta toda la app al instante
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
                                    if (!unlocked) "🔒 Cómpralo en la Tienda (${theme.price} 🪙)" else theme.emoji,
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

// --- Helpers para el volumen del stream de alarma del sistema ---

private fun readAlarmVolumeFraction(audioManager: AudioManager): Float {
    return try {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        (current.toFloat() / max).coerceIn(0f, 1f)
    } catch (_: Exception) {
        0.5f
    }
}

private fun writeAlarmVolumeFraction(audioManager: AudioManager, fraction: Float) {
    try {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val absolute = (fraction.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, absolute, 0)
    } catch (_: Exception) {}
}