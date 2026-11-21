package com.german.wakeapp

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val FastLoadingPhases = listOf(
    "Compilando archivos base..." to 0.25f,
    "Preparando desafíos y niveles..." to 0.55f,
    "Ajustando tu configuración inicial..." to 0.82f,
    "Finalizando calibración..." to 1.0f
)

@Composable
fun OnboardingScreen(
    settingsManager: SettingsManager,
    onComplete: () -> Unit
) {
    if (settingsManager.isOnboardingCompleted()) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    val context = LocalContext.current
    val profileManager = remember { ProfileManager(context) }
    val totalConfigSteps = 4

    // 0 = Portada
    // 1 = Idioma
    // 2 = Nombre
    // 3 = Dificultad
    // 4 = Hora habitual
    // 5 = Carga rápida centrada + botón Finalizar al completar
    var step by remember { mutableIntStateOf(0) }

    var selectedLanguage by remember { mutableStateOf("es") }
    var userName by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("MEDIUM") }
    var selectedHour by remember { mutableIntStateOf(7) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    var targetProgress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("Iniciando carga...") }
    var loadingFinished by remember { mutableStateOf(false) }

    val smoothBarProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "fast_onboarding_progress"
    )

    LaunchedEffect(step) {
        if (step == 5) {
            loadingFinished = false
            targetProgress = 0f

            val cleanName = userName.trim()
            AppLang.language = selectedLanguage.ifBlank { "es" }
            settingsManager.setLanguage(AppLang.language)
            settingsManager.setUserName(cleanName)
            settingsManager.setDefaultDifficulty(selectedDifficulty.ifBlank { "MEDIUM" })
            settingsManager.setHabitualWakeTime(selectedHour, selectedMinute)

            if (cleanName.isNotBlank()) {
                profileManager.setName(cleanName)
            }

            context.getSharedPreferences("wakeapp_onboarding_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("user_name", cleanName)
                .putString("default_difficulty", selectedDifficulty)
                .putInt("habitual_hour", selectedHour)
                .putInt("habitual_minute", selectedMinute)
                .putBoolean("onboarding_finished", true)
                .apply()

            context.getSharedPreferences("wakeapp_profile_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("user_name", cleanName)
                .apply()

            for ((phrase, value) in FastLoadingPhases) {
                statusText = phrase
                targetProgress = value
                delay(850L)
            }
            loadingFinished = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CleanBg, WakeBlue.copy(alpha = 0.08f), Color(0xFF101012))
                )
            )
    ) {
        when (step) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 36.dp)
                        .systemBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏰", fontSize = 54.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Esto es WakeApp",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CleanText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Configura lo esencial en un momento y entra a despertar con cabeza.",
                            fontSize = 15.5.sp,
                            color = CleanTextDim,
                            textAlign = TextAlign.Center,
                            lineHeight = 23.sp
                        )
                    }

                    Button(
                        onClick = { step = 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = WakeBlue, contentColor = CleanText),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Comenzar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            5 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .systemBarsPadding(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Preparando tu experiencia",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanText,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = statusText,
                            fontSize = 14.sp,
                            color = CleanTextDim,
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(CleanSurfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(smoothBarProgress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(WakeBlue)
                            )
                        }

                        Text(
                            text = "${(smoothBarProgress * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanText,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            val cleanName = userName.trim()
                            if (cleanName.isNotBlank()) {
                                profileManager.setName(cleanName)
                            }
                            settingsManager.setOnboardingCompleted(true)
                            onComplete()
                        },
                        enabled = loadingFinished,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WakeBlue,
                            contentColor = CleanText,
                            disabledContainerColor = CleanSurfaceVariant,
                            disabledContentColor = CleanTextDim
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(
                            text = if (loadingFinished) "Finalizar" else "Cargando...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .systemBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 1..totalConfigSteps) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (i == step) WakeBlue
                                        else if (i < step) WakeBlue.copy(alpha = 0.45f)
                                        else CleanSurfaceVariant
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Paso $step de $totalConfigSteps", fontSize = 12.sp, color = CleanTextDim)

                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
                        label = "onboarding_clean_steps",
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { currentStep ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (currentStep) {
                                1 -> {
                                    Text("Selecciona tu idioma", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CleanText)
                                    Spacer(modifier = Modifier.height(28.dp))
                                    LanguageOption("🇪🇸", "Español", selectedLanguage == "es") {
                                        selectedLanguage = "es"; AppLang.language = "es"
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LanguageOption("🇬🇧", "English", selectedLanguage == "en") {
                                        selectedLanguage = "en"; AppLang.language = "en"
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LanguageOption("🇫🇷", "Français", selectedLanguage == "fr") {
                                        selectedLanguage = "fr"; AppLang.language = "fr"
                                    }
                                }

                                2 -> {
                                    Text("¿Cómo te llamas?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CleanText)
                                    Spacer(modifier = Modifier.height(28.dp))
                                    OutlinedTextField(
                                        value = userName,
                                        onValueChange = { if (it.length <= 15) userName = it },
                                        label = { Text("Tu nombre") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WakeBlue,
                                            unfocusedBorderColor = CleanSurfaceVariant,
                                            focusedTextColor = CleanText,
                                            unfocusedTextColor = CleanText,
                                            cursorColor = WakeBlue
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(0.88f)
                                    )
                                }

                                3 -> {
                                    Text("Dificultad por defecto", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CleanText)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    DifficultyOption("🟢", "Fácil", "Cálculos sencillos, frases cortas", "EASY", selectedDifficulty) { selectedDifficulty = it }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DifficultyOption("🟡", "Media", "Cálculos medios, frases normales", "MEDIUM", selectedDifficulty) { selectedDifficulty = it }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DifficultyOption("🟠", "Difícil", "Multiplicaciones, frases largas", "HARD", selectedDifficulty) { selectedDifficulty = it }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DifficultyOption("🔴", "Extrema", "Operaciones complejas, textos exactos", "EXTREME", selectedDifficulty) { selectedDifficulty = it }
                                }

                                4 -> {
                                    Text("¿A qué hora sueles despertar?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CleanText)
                                    Spacer(modifier = Modifier.height(28.dp))
                                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                        NumberPicker(value = selectedHour, range = 0..23, onValueChange = { selectedHour = it })
                                        Text(":", fontSize = 40.sp, fontWeight = FontWeight.Light, color = CleanText, modifier = Modifier.padding(horizontal = 8.dp))
                                        NumberPicker(value = selectedMinute, range = 0..59, onValueChange = { selectedMinute = it })
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 1) {
                            TextButton(onClick = { step-- }) {
                                Text("Atrás", color = CleanTextDim, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = { step++ },
                            colors = ButtonDefaults.buttonColors(containerColor = WakeBlue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(54.dp).width(145.dp)
                        ) {
                            Text(
                                text = if (step == totalConfigSteps) "Cargar app" else "Siguiente",
                                color = CleanText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageOption(flag: String, name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) WakeBlue.copy(alpha = 0.16f) else CleanSurface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) WakeBlue else CleanSurfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(flag, fontSize = 32.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = CleanText, modifier = Modifier.weight(1f))
        if (isSelected) Text("✓", color = WakeBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
fun DifficultyOption(
    emoji: String,
    title: String,
    subtitle: String,
    difficultyId: String,
    currentSelection: String,
    onSelect: (String) -> Unit
) {
    val isSelected = currentSelection == difficultyId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) WakeBlue.copy(alpha = 0.16f) else CleanSurface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) WakeBlue else CleanSurfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelect(difficultyId) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CleanText)
            Text(subtitle, fontSize = 12.sp, color = CleanTextDim)
        }
        if (isSelected) Text("✓", color = WakeBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}