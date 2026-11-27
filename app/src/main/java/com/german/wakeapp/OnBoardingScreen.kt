package com.german.wakeapp

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

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

    // 1 = Idioma, 2 = Nombre, 3 = Hora, 4 = Carga final
    var step by remember { mutableIntStateOf(1) }
    val totalConfigSteps = 3

    var selectedLanguage by remember { mutableStateOf("es") }
    var userName by remember { mutableStateOf("") }
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
        if (step == 4) {
            loadingFinished = false
            targetProgress = 0f

            val cleanName = userName.trim()
            AppLang.language = selectedLanguage.ifBlank { "es" }
            settingsManager.setLanguage(AppLang.language)
            settingsManager.setUserName(cleanName)
            settingsManager.setDefaultDifficulty("MEDIUM")
            settingsManager.setHabitualWakeTime(selectedHour, selectedMinute)

            if (cleanName.isNotBlank()) profileManager.setName(cleanName)

            context.getSharedPreferences("wakeapp_onboarding_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("user_name", cleanName)
                .putString("default_difficulty", "MEDIUM")
                .putInt("habitual_hour", selectedHour)
                .putInt("habitual_minute", selectedMinute)
                .putBoolean("onboarding_finished", true)
                .apply()

            context.getSharedPreferences("wakeapp_profile_prefs", Context.MODE_PRIVATE)
                .edit().putString("user_name", cleanName).apply()

            for ((phrase, value) in FastLoadingPhases) {
                statusText = phrase
                targetProgress = value
                delay(900L)
            }
            loadingFinished = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CleanBg, WakeBlue.copy(alpha = 0.10f), Color(0xFF08080A))))
    ) {
        if (step == 1) BreathingHalo()

        when (step) {
            4 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp).systemBarsPadding(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Preparando tu experiencia", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = CleanText, textAlign = TextAlign.Center)
                        Text(statusText, fontSize = 14.sp, color = CleanTextDim, textAlign = TextAlign.Center)
                        Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(999.dp)).background(CleanSurfaceVariant)) {
                            Box(modifier = Modifier.fillMaxWidth(smoothBarProgress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(WakeBlue))
                        }
                        Text("${(smoothBarProgress * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CleanText)
                    }
                    Button(
                        onClick = {
                            if (userName.isNotBlank()) profileManager.setName(userName.trim())
                            settingsManager.setOnboardingCompleted(true)
                            onComplete()
                        },
                        enabled = loadingFinished,
                        colors = ButtonDefaults.buttonColors(containerColor = WakeBlue, contentColor = CleanText, disabledContainerColor = CleanSurfaceVariant, disabledContentColor = CleanTextDim),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(if (loadingFinished) "Entrar a WakeApp" else "Cargando...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).systemBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 1..totalConfigSteps) {
                            Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(999.dp)).background(
                                if (i == step) WakeBlue else if (i < step) WakeBlue.copy(alpha = 0.45f) else CleanSurfaceVariant
                            ))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Paso $step de $totalConfigSteps", fontSize = 12.sp, color = CleanTextDim)

                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { (fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 6 }) togetherWith (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 6 }) },
                        label = "onboarding_steps",
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { currentStep ->
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (currentStep) {
                                1 -> {
                                    Text("Elige tu idioma", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CleanText)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Podrás cambiarlo luego en Ajustes.", fontSize = 13.sp, color = CleanTextDim)
                                    Spacer(modifier = Modifier.height(28.dp))
                                    LanguageOption("🇪🇸", "Español", selectedLanguage == "es") { selectedLanguage = "es"; AppLang.language = "es" }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LanguageOption("🇬🇧", "English", selectedLanguage == "en") { selectedLanguage = "en"; AppLang.language = "en" }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LanguageOption("🇫🇷", "Français", selectedLanguage == "fr") { selectedLanguage = "fr"; AppLang.language = "fr" }
                                }
                                2 -> {
                                    Text("¿Cómo te llamas?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CleanText)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Solo para saludarte cuando te despiertes.", fontSize = 13.sp, color = CleanTextDim)
                                    Spacer(modifier = Modifier.height(28.dp))
                                    OutlinedTextField(
                                        value = userName, onValueChange = { if (it.length <= 15) userName = it },
                                        label = { Text("Tu nombre") }, singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WakeBlue, unfocusedBorderColor = CleanSurfaceVariant, focusedTextColor = CleanText, unfocusedTextColor = CleanText, cursorColor = WakeBlue),
                                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(0.88f)
                                    )
                                }
                                3 -> {
                                    Text("¿A qué hora sueles despertar?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CleanText, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("La usaremos como sugerencia al crear tu primera alarma.", fontSize = 13.sp, color = CleanTextDim, textAlign = TextAlign.Center)
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
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 1) TextButton(onClick = { step-- }) { Text("Atrás", color = CleanTextDim, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        else Spacer(modifier = Modifier.width(1.dp))

                        Button(
                            onClick = { step++ }, colors = ButtonDefaults.buttonColors(containerColor = WakeBlue), shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(54.dp).width(165.dp)
                        ) {
                            Text(if (step == totalConfigSteps) "Cargar app" else "Siguiente", color = CleanText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreathingHalo() {
    val infinite = rememberInfiniteTransition(label = "halo")
    val scale by infinite.animateFloat(initialValue = 0.85f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
    val alpha by infinite.animateFloat(initialValue = 0.18f, targetValue = 0.32f, animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alpha")
    Box(modifier = Modifier.fillMaxSize().blur(80.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(320.dp).scale(scale).alpha(alpha).clip(CircleShape).background(Brush.radialGradient(colors = listOf(WakeBlue, Color.Transparent), center = Offset.Unspecified)))
    }
}

@Composable
private fun AnimatedClockLogo(
    modifier: Modifier = Modifier,
    color: Color = CleanText,
    accent: Color = WakeBlue
) {
    val infinite = rememberInfiniteTransition(label = "clock")
    val hourRotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "hour_rot"
    )
    val minuteRotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "minute_rot"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.20f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_alpha"
    )

    Box(modifier = modifier.size(140.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().scale(pulse)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeWidth = 2.4.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = glowAlpha), Color.Transparent),
                    center = center, radius = radius * 1.4f
                ),
                radius = radius * 1.4f, center = center
            )

            drawCircle(
                color = color.copy(alpha = 0.85f),
                radius = radius, center = center,
                style = Stroke(width = strokeWidth)
            )

            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30).toDouble())
                val isQuarter = i % 3 == 0
                val inner = radius * if (isQuarter) 0.82f else 0.88f
                val outer = radius * 0.95f
                val start = Offset(
                    center.x + (cos(angle) * inner).toFloat(),
                    center.y + (sin(angle) * inner).toFloat()
                )
                val end = Offset(
                    center.x + (cos(angle) * outer).toFloat(),
                    center.y + (sin(angle) * outer).toFloat()
                )
                drawLine(
                    color = color.copy(alpha = if (isQuarter) 0.9f else 0.45f),
                    start = start, end = end,
                    strokeWidth = if (isQuarter) strokeWidth * 1.4f else strokeWidth * 0.8f,
                    cap = StrokeCap.Round
                )
            }

            val hourAngle = Math.toRadians((hourRotation - 90).toDouble())
            val hourEnd = Offset(
                center.x + (cos(hourAngle) * radius * 0.42f).toFloat(),
                center.y + (sin(hourAngle) * radius * 0.42f).toFloat()
            )
            drawLine(color = color, start = center, end = hourEnd, strokeWidth = strokeWidth * 2.2f, cap = StrokeCap.Round)

            val minAngle = Math.toRadians((minuteRotation - 90).toDouble())
            val minEnd = Offset(
                center.x + (cos(minAngle) * radius * 0.68f).toFloat(),
                center.y + (sin(minAngle) * radius * 0.68f).toFloat()
            )
            drawLine(color = accent, start = center, end = minEnd, strokeWidth = strokeWidth * 1.6f, cap = StrokeCap.Round)

            drawCircle(color = accent, radius = strokeWidth * 1.6f, center = center)
        }
    }
}

@Composable
private fun CinematicIntro(onStart: () -> Unit) {
    var logoVisible by remember { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(if (logoVisible) 1f else 0.55f, tween(1400, easing = FastOutSlowInEasing), label = "logo_scale")
    val logoAlpha by animateFloatAsState(if (logoVisible) 1f else 0f, tween(1200, easing = LinearEasing), label = "logo_alpha")

    LaunchedEffect(Unit) {
        delay(250); logoVisible = true
        delay(750); titleVisible = true
        delay(500); subtitleVisible = true
        delay(400); buttonVisible = true
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 36.dp).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedClockLogo(
                modifier = Modifier.scale(logoScale).alpha(logoAlpha),
                color = CleanText,
                accent = WakeBlue
            )
            Spacer(modifier = Modifier.height(28.dp))
            AnimatedVisibility(visible = titleVisible, enter = fadeIn(tween(900)) + slideInVertically(tween(900)) { it / 6 }) {
                Text("Esto es WakeApp", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = CleanText, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(14.dp))
            AnimatedVisibility(visible = subtitleVisible, enter = fadeIn(tween(900))) {
                Text("Configura lo esencial en un momento y entra a despertar con cabeza.", fontSize = 15.5.sp, color = CleanTextDim, textAlign = TextAlign.Center, lineHeight = 23.sp)
            }
        }
        AnimatedVisibility(visible = buttonVisible, enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 4 }) {
            Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = WakeBlue, contentColor = CleanText), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Comenzar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun LanguageOption(flag: String, name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(0.88f).clip(RoundedCornerShape(20.dp)).background(if (isSelected) WakeBlue.copy(alpha = 0.16f) else CleanSurface)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) WakeBlue else CleanSurfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable { onClick() }.padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(flag, fontSize = 32.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = CleanText, modifier = Modifier.weight(1f))
        if (isSelected) Text("✓", color = WakeBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}