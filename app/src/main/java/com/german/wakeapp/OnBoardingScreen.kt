package com.german.wakeapp

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun OnboardingScreen(
    settingsManager: SettingsManager,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }

    // Estados
    var selectedLanguage by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var selectedPurpose by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("") }
    var selectedHour by remember { mutableIntStateOf(7) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CleanBg, Color(0xFF141414))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Indicador de progreso (bolitas)
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..5) {
                    Box(
                        modifier = Modifier
                            .size(if (i == step) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (i == step) WakeBlue else if (i < step) WakeBlue.copy(alpha = 0.4f) else CleanSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // --- CONTENIDO ANIMADO ---
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "onboarding_step",
                modifier = Modifier.weight(8f)
            ) { currentStep ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (currentStep) {
                        1 -> {
                            // PASO 1: IDIOMA
                            Text("Selecciona tu idioma", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CleanText, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Select your language", fontSize = 16.sp, color = CleanTextDim, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(48.dp))

                            LanguageOption(
                                flag = "🇪🇸", name = "Español",
                                isSelected = selectedLanguage == "es",
                                onClick = { selectedLanguage = "es" }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LanguageOption(
                                flag = "🇬🇧", name = "English",
                                isSelected = selectedLanguage == "en",
                                onClick = { selectedLanguage = "en" }
                            )
                        }

                        2 -> {
                            // PASO 2: NOMBRE
                            Text("¿Cómo te llamas?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CleanText, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Usaremos tu nombre para saludarte", fontSize = 16.sp, color = CleanTextDim, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(48.dp))

                            OutlinedTextField(
                                value = userName,
                                onValueChange = { if (it.length <= 15) userName = it },
                                label = { Text("Tu nombre (opcional)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WakeBlue,
                                    unfocusedBorderColor = CleanSurfaceVariant,
                                    focusedTextColor = CleanText,
                                    unfocusedTextColor = CleanText,
                                    cursorColor = WakeBlue
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            )
                        }

                        3 -> {
                            // PASO 3: PROPÓSITO
                            Text("¿Para qué usarás WakeApp?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CleanText, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Elige tu objetivo principal", fontSize = 16.sp, color = CleanTextDim, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(32.dp))

                            PurposeOption(
                                emoji = "🚀", title = "Productividad",
                                subtitle = "Levantarme con energía para rendir más",
                                isSelected = selectedPurpose == "productividad",
                                onClick = { selectedPurpose = "productividad" }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            PurposeOption(
                                emoji = "🌅", title = "Madrugar",
                                subtitle = "Quiero levantarme más temprano",
                                isSelected = selectedPurpose == "madrugar",
                                onClick = { selectedPurpose = "madrugar" }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            PurposeOption(
                                emoji = "🔥", title = "Disciplina",
                                subtitle = "Obligarme a despertarme sí o sí",
                                isSelected = selectedPurpose == "obligatoria",
                                onClick = { selectedPurpose = "obligatoria" }
                            )
                        }

                        4 -> {
                            // PASO 4: DIFICULTAD POR DEFECTO
                            Text("¿Qué dificultad prefieres?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CleanText, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Será la que uses por defecto al crear alarmas", fontSize = 16.sp, color = CleanTextDim, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(32.dp))

                            DifficultyOption("🟢", "Fácil", "Cálculos sencillos, frases cortas", "EASY", selectedDifficulty) { selectedDifficulty = it }
                            Spacer(modifier = Modifier.height(10.dp))
                            DifficultyOption("🟡", "Media", "Cálculos medios, frases normales", "MEDIUM", selectedDifficulty) { selectedDifficulty = it }
                            Spacer(modifier = Modifier.height(10.dp))
                            DifficultyOption("🟠", "Difícil", "Multiplicaciones, frases largas", "HARD", selectedDifficulty) { selectedDifficulty = it }
                            Spacer(modifier = Modifier.height(10.dp))
                            DifficultyOption("🔴", "Extrema", "Operaciones complejas, textos exactos", "EXTREME", selectedDifficulty) { selectedDifficulty = it }
                        }

                        5 -> {
                            // PASO 5: HORA HABITUAL
                            Text("¿A qué hora sueles despertarte?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CleanText, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("La usaremos como punto de partida al crear tus alarmas", fontSize = 15.sp, color = CleanTextDim, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(32.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NumberPicker(
                                    value = selectedHour,
                                    range = 0..23,
                                    onValueChange = { selectedHour = it }
                                )
                                Text(
                                    ":",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Light,
                                    color = CleanText,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                NumberPicker(
                                    value = selectedMinute,
                                    range = 0..59,
                                    onValueChange = { selectedMinute = it }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(2f))

            // --- BOTONES ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
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

                val canProceed = when (step) {
                    1 -> selectedLanguage.isNotBlank()
                    2 -> true // Nombre opcional
                    3 -> selectedPurpose.isNotBlank()
                    4 -> selectedDifficulty.isNotBlank()
                    5 -> true // La hora siempre es válida
                    else -> false
                }

                Button(
                    onClick = {
                        if (step < 5) {
                            step++
                        } else {
                            // Guardar todo
                            settingsManager.setLanguage(selectedLanguage)
                            settingsManager.setUserName(userName.trim())
                            settingsManager.setPurpose(selectedPurpose)
                            settingsManager.setDefaultDifficulty(selectedDifficulty)
                            settingsManager.setHabitualWakeTime(selectedHour, selectedMinute)
                            settingsManager.setOnboardingCompleted(true)
                            onComplete()
                        }
                    },
                    enabled = canProceed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WakeBlue,
                        disabledContainerColor = CleanSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .width(if (step == 5) 160.dp else 120.dp)
                ) {
                    Text(
                        text = if (step == 5) "Empezar" else "Siguiente",
                        color = if (canProceed) CleanText else CleanTextDim,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageOption(flag: String, name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) WakeBlue.copy(alpha = 0.15f) else CleanSurface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) WakeBlue else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(flag, fontSize = 32.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
    }
}

@Composable
fun PurposeOption(emoji: String, title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) WakeBlue.copy(alpha = 0.15f) else CleanSurface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) WakeBlue else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isSelected) WakeBlue.copy(alpha = 0.3f) else CleanSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CleanText)
            Text(subtitle, fontSize = 13.sp, color = CleanTextDim, lineHeight = 18.sp)
        }
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
            .background(if (isSelected) WakeBlue.copy(alpha = 0.15f) else CleanSurface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) WakeBlue else Color.Transparent,
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
        if (isSelected) {
            Text("✓", color = WakeBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}