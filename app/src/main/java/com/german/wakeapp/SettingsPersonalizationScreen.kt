package com.german.wakeapp

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

// Función para comprobar el permiso de uso (Anti-Desinstalación)
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

@Composable
fun SettingsPersonalizationScreen(
    settingsManager: SettingsManager,
    profileManager: ProfileManager,
    streakManager: StreakManager,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }

    // Estados locales para que los cambios sean instantáneos
    var hasUsagePermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    val isLightMode = AppPalette.isLight
    var currentLanguage by remember { mutableStateOf(settingsManager.getLanguage()) }
    var currentTheme by remember { mutableStateOf(themeManager.getActive()) }
    var soundType by remember { mutableStateOf(settingsManager.getDefaultSoundType()) }
    var confirmDelete by remember { mutableStateOf(settingsManager.isConfirmDeleteEnabled()) }

    // Diálogos
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Observador del ciclo de vida para el permiso
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasUsagePermission = hasUsageStatsPermission(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Forzamos el fondo sólido según el modo
    Column(modifier = Modifier.fillMaxSize().background(CleanBg)) {
        // CABECERA
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(CleanSurface).border(1.dp, CleanSurfaceVariant, CircleShape).clickable { onClose() },
                contentAlignment = Alignment.Center
            ) { Text("←", fontSize = 18.sp, color = CleanText) }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Ajustes y Personalización", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
        }
        HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.6f))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {

            // --- RECUADRO SUPERIOR: CUENTA, PRO, ANTI-DESINSTALACIÓN ---
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 1. Tu Cuenta
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(WakeBlue.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text(profileManager.getName().take(1).uppercase().ifBlank { "U" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WakeBlue)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Tu cuenta", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                            Text(profileManager.getName().ifBlank { "Usuario" }, fontSize = 13.sp, color = CleanTextDim)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. WakeApp Pro (Sutil)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CleanSurfaceVariant.copy(alpha = 0.5f)).clickable {
                            android.widget.Toast.makeText(context, "WakeApp Pro activado (Simulado)", android.widget.Toast.LENGTH_SHORT).show()
                        }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("WakeApp Pro", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CleanText)
                            Text("Desbloquea todo el potencial", fontSize = 12.sp, color = CleanTextDim)
                        }
                        Text("Mejorar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WakeBlue)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Protección Anti-Desinstalación
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bloquear cierre forzado", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                            Text("Evita que cierres la app para apagar la alarma.", fontSize = 11.5.sp, color = CleanTextDim)
                        }
                        Switch(
                            checked = hasUsagePermission,
                            onCheckedChange = { checked ->
                                if (checked && !hasUsagePermission) {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") })
                                } else if (!checked && hasUsagePermission) {
                                    android.widget.Toast.makeText(context, "Ve a Ajustes de Android > Uso para desactivarlo", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = CleanBg, checkedTrackColor = CleanText, uncheckedThumbColor = CleanTextDim, uncheckedTrackColor = CleanSurfaceVariant)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- APARIENCIA ---
            Text("APARIENCIA", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanTextDim, letterSpacing = 1.6.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Card(colors = CardDefaults.cardColors(containerColor = CleanSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(16.dp))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Modo Claro
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Modo claro", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                            Text(if (isLightMode) "Interfaz clara activada." else "Interfaz oscura activada.", fontSize = 12.5.sp, color = CleanTextDim)
                        }
                        Switch(
                            checked = isLightMode,
                            onCheckedChange = { checked ->
                                settingsManager.setLightMode(checked)
                                AppPalette.isLight = checked
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = CleanBg, checkedTrackColor = CleanText, uncheckedThumbColor = CleanTextDim, uncheckedTrackColor = CleanSurfaceVariant)
                        )
                    }

                    HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))

                    // Idioma
                    Row(modifier = Modifier.fillMaxWidth().clickable { showLanguageDialog = true }, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Idioma", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                            Text(when (currentLanguage) { "en" -> "English"; "fr" -> "Français"; else -> "Español" }, fontSize = 12.5.sp, color = CleanTextDim)
                        }
                        Text("›", fontSize = 20.sp, color = CleanTextDim)
                    }

                    HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))

                    // Tema de la app
                    Row(modifier = Modifier.fillMaxWidth().clickable { showThemeDialog = true }, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tema de la app", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                            Text(currentTheme.name, fontSize = 12.5.sp, color = CleanTextDim)
                        }
                        Text("›", fontSize = 20.sp, color = CleanTextDim)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- AJUSTES GENERALES ---
            Text("GENERAL", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanTextDim, letterSpacing = 1.6.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Card(colors = CardDefaults.cardColors(containerColor = CleanSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(16.dp))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Sonido por defecto
                    Row(modifier = Modifier.fillMaxWidth().clickable { showSoundDialog = true }, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sonido por defecto", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                            Text(when (soundType) { "alarm" -> "Tono de alarma"; "notification" -> "Notificación"; else -> "Tono de llamada" }, fontSize = 12.5.sp, color = CleanTextDim)
                        }
                        Text("›", fontSize = 20.sp, color = CleanTextDim)
                    }

                    HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))

                    // Confirmar borrado
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Confirmar borrado", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                            Text("Preguntar antes de eliminar una alarma.", fontSize = 12.5.sp, color = CleanTextDim)
                        }
                        Switch(
                            checked = confirmDelete, onCheckedChange = { settingsManager.setConfirmDeleteEnabled(it); confirmDelete = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CleanBg, checkedTrackColor = CleanText, uncheckedThumbColor = CleanTextDim, uncheckedTrackColor = CleanSurfaceVariant)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    // --- DIÁLOGOS ---
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false }, containerColor = CleanSurface,
            title = { Text("Idioma", color = CleanText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(Triple("es", "Español", "🇪🇸"), Triple("en", "English", "🇬🇧"), Triple("fr", "Français", "🇫🇷")).forEach { (code, name, flag) ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (currentLanguage == code) CleanSurfaceVariant else Color.Transparent).clickable {
                            currentLanguage = code
                            settingsManager.setLanguage(code)
                            AppLang.language = code
                            showLanguageDialog = false
                        }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(flag, fontSize = 22.sp); Spacer(modifier = Modifier.width(12.dp))
                            Text(name, color = CleanText, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            if (currentLanguage == code) Text("✓", color = WakeBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("Cerrar", color = WakeBlue, fontWeight = FontWeight.Bold) } }
        )
    }

    if (showSoundDialog) {
        AlertDialog(
            onDismissRequest = { showSoundDialog = false }, containerColor = CleanSurface,
            title = { Text("Sonido por defecto", color = CleanText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("alarm" to "Tono de alarma", "notification" to "Notificación", "ringtone" to "Tono de llamada").forEach { (key, label) ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (soundType == key) CleanSurfaceVariant else Color.Transparent).clickable {
                            soundType = key; settingsManager.setDefaultSoundType(key); showSoundDialog = false
                        }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, color = CleanText, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            if (soundType == key) Text("✓", color = WakeBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSoundDialog = false }) { Text("Cerrar", color = WakeBlue, fontWeight = FontWeight.Bold) } }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false }, containerColor = CleanSurface,
            title = { Text("Tema de la app", color = CleanText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeCatalog.all.forEach { theme ->
                        val unlocked = themeManager.isUnlocked(theme.id)
                        val isActive = currentTheme.id == theme.id
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isActive) CleanSurfaceVariant else Color.Transparent).clickable(enabled = unlocked) {
                            themeManager.setActive(theme.id); currentTheme = theme; AppPalette.applyTheme(theme); showThemeDialog = false
                        }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(theme.primary))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(theme.name, color = if (unlocked) CleanText else CleanTextDim, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text(if (!unlocked) "🔒 ${tr("Cómpralo en la Tienda")} (${theme.price} 🪙)" else theme.emoji, color = CleanTextDim, fontSize = 12.sp)
                            }
                            if (isActive) Text("✓", color = WakeBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Cerrar", color = WakeBlue, fontWeight = FontWeight.Bold) } }
        )
    }
}