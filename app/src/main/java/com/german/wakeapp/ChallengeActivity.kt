package com.german.wakeapp

import org.json.JSONArray
import org.json.JSONObject
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.german.wakeapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class ChallengeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val challengeType = intent.getStringExtra("CHALLENGE_TYPE") ?: "MATH"
        val difficulty = intent.getStringExtra("DIFFICULTY") ?: "MEDIUM"

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
                val ambientColor = when (challengeType) {
                    "TRACING" -> NeonGreen
                    "TEXT" -> NeonCyan
                    "STROOP" -> Color(0xFFD14545)
                    "SHAKE" -> Color(0xFFE08E3C)
                    else -> PurplePrimary
                }

                val infinite = rememberInfiniteTransition(label = "bg")
                val scale by infinite.animateFloat(
                    initialValue = 0.8f, targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(ambientColor.copy(alpha = 0.25f), Color.Transparent),
                                center = Offset(500f * scale, 500f * scale),
                                radius = 1800f
                            )
                        )
                ) {
                    when (challengeType) {
                        "TRACING" -> TracingChallengeScreen(difficulty, onSuccess = { stopAndFinish(difficulty) })
                        "STROOP" -> StroopChallengeScreen(difficulty, onSuccess = { stopAndFinish(difficulty) })
                        "SHAKE" -> ShakeChallengeScreen(difficulty, onSuccess = { stopAndFinish(difficulty) })
                        else -> StandardChallengeScreen(challengeType, difficulty, onSuccess = { stopAndFinish(difficulty) })
                    }
                }
            }
        }
    }

    private fun stopAndFinish(difficulty: String) {
        val streakManager = StreakManager(this)
        val achievementManager = AchievementManager(this)

        val levelBefore = streakManager.getLevel()

        // 1) Registrar despertar
        val newStreak = streakManager.registerSuccessfulWakeUp()

        // 2) XP según dificultad
        val xpGained = when (difficulty) {
            "EASY" -> 40
            "MEDIUM" -> 60
            "HARD" -> 90
            "EXTREME" -> 150
            else -> 60
        }
        val levelsUpByXp = streakManager.addXp(xpGained)

        // 3) Comprobar logros
        val newlyUnlocked = mutableListOf<Achievement>()
        newlyUnlocked.addAll(achievementManager.checkStreakAchievements(newStreak))

        // NUEVO: Comprobar si es antes de las 7:00
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        newlyUnlocked.addAll(achievementManager.checkEarlyBird(currentHour))

        // NUEVO: Contar uso de minijuegos (leyendo el tipo desde el Intent)
        val currentChallengeType = intent.getStringExtra("CHALLENGE_TYPE") ?: "MATH"
        newlyUnlocked.addAll(achievementManager.checkChallengeAchievements(currentChallengeType))

        // 4) Recompensas
        val bonusLevels = newlyUnlocked.size
        val bonusPoints = newlyUnlocked.sumOf { it.rewardPoints }
        if (bonusLevels > 0) streakManager.addLevels(bonusLevels)
        if (bonusPoints > 0) streakManager.addPoints(bonusPoints)

        val levelAfter = streakManager.getLevel()

        // 5) Serializar logros
        val achievementsJson = if (newlyUnlocked.isEmpty()) null else {
            val arr = org.json.JSONArray()
            newlyUnlocked.forEach { ach ->
                arr.put(
                    org.json.JSONObject().apply {
                        put("id", ach.id)
                        put("title", ach.title)
                        put("emoji", ach.emoji)
                        put("rewardPoints", ach.rewardPoints)
                    }
                )
            }
            arr.toString()
        }

        // 6) Guardar recompensa
        streakManager.setPendingReward(
            xp = xpGained,
            levelUp = (levelsUpByXp > 0 || bonusLevels > 0),
            oldLevel = levelBefore,
            newLevel = levelAfter,
            achievementsJson = achievementsJson,
            bonusLevels = bonusLevels,
            bonusPoints = bonusPoints
        )

        stopService(Intent(this, AlarmService::class.java))

        val result = Intent().apply {
            putExtra("XP_GAINED", xpGained)
            putExtra("LEVEL_UP", levelsUpByXp > 0 || bonusLevels > 0)
            putExtra("NEW_LEVEL", levelAfter)
            putExtra("OLD_LEVEL", levelBefore)
            putExtra("ACHIEVEMENTS_COUNT", newlyUnlocked.size)
        }
        setResult(RESULT_OK, result)
        finish()
    }
}

@Composable
fun ChallengeHeader(emoji: String, title: String, subtitle: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f))
                .border(2.dp, accent.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 36.sp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = accent,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = TextWhite.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun BigActionButton(text: String, enabled: Boolean, containerColor: Color, contentColor: Color, glowColor: Color, onClick: () -> Unit) {
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(if (enabled) 20.dp else 0.dp, RoundedCornerShape(20.dp), ambientColor = glowColor, spotColor = glowColor),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, disabledContainerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = if (enabled) contentColor else TextMuted,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun StandardChallengeScreen(challengeType: String, difficulty: String, onSuccess: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val phrases = remember(difficulty) {
        when (difficulty) {
            "EASY" -> listOf("despierta ya", "hora de levantarse", "buenos dias")
            "MEDIUM" -> listOf("despierta, niñato", "niñato, empieza a despertar", "o te despiertas o te zurro")
            "HARD" -> listOf("¡O te despiertas AHORA MISMO, niñato, o no hay café!", "WakeApp: Código 404, sueño no encontrado.", "¡Levántate, 2026 nos está esperando!")
            "EXTREME" -> listOf("ADVERTENCIA CRÍTICA: ¡Riesgo severo de procrastinación detectado en el núcleo!", "¡El modo Extremo exige vigilia total inmediata, sin excusas ni retrasos!", "Protocolo 0-Alpha: Escribe esta línea exacta para autorizar el apagado.")
            else -> listOf("despierta ya", "hora de levantarse")
        }
    }

    val mathProblem = remember(difficulty) {
        when (difficulty) {
            "EASY" -> { val a = Random.nextInt(10, 40); val b = Random.nextInt(10, 40); Pair("$a + $b = ?", "${a + b}") }
            "MEDIUM" -> { val a = Random.nextInt(12, 25); val b = Random.nextInt(3, 9); Pair("$a × $b = ?", "${a * b}") }
            "HARD" -> { val a = Random.nextInt(12, 30); val b = Random.nextInt(4, 9); val c = Random.nextInt(10, 40); Pair("($a × $b) - $c = ?", "${(a * b) - c}") }
            "EXTREME" -> { val a = Random.nextInt(25, 65); val b = Random.nextInt(7, 16); val c = Random.nextInt(115, 350); Pair("($a × $b) - $c = ?", "${(a * b) - c}") }
            else -> { val a = Random.nextInt(10, 20); val b = Random.nextInt(10, 20); Pair("$a + $b = ?", "${a + b}") }
        }
    }

    val targetText = remember { if (challengeType == "TEXT") phrases.random() else mathProblem.second }
    val displayText = remember { if (challengeType == "TEXT") "\"$targetText\"" else mathProblem.first }

    var userInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "title")
    val titleAlpha by infinite.animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "titleAlpha"
    )

    val accent = if (challengeType == "TEXT") NeonCyan else PurpleAccent
    val emoji = if (challengeType == "TEXT") "✎" else "∑"
    val title = if (challengeType == "TEXT") "FRASE EXACTA" else "CÁLCULO MENTAL"
    val subtitle = if (challengeType == "TEXT") "Escribe la secuencia exacta que ves abajo:" else "Resuelve el cálculo para silenciar la alarma:"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ChallengeHeader(emoji = emoji, title = title, subtitle = subtitle, accent = accent.copy(alpha = titleAlpha))
        Spacer(modifier = Modifier.height(36.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(20.dp, RoundedCornerShape(28.dp), ambientColor = accent, spotColor = accent)
                .border(2.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = displayText,
                    fontSize = if (challengeType == "TEXT" && difficulty == "EXTREME") 16.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it; isError = false },
            label = { Text("Respuesta") },
            isError = isError,
            singleLine = difficulty != "EXTREME",
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                errorBorderColor = Color.Red,
                focusedContainerColor = SurfaceDark.copy(alpha = 0.5f),
                unfocusedContainerColor = SurfaceDark.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (isError) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Respuesta incorrecta. Inténtalo de nuevo.", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        BigActionButton(
            text = "DESACTIVAR ALARMA",
            enabled = true,
            containerColor = PurplePrimary,
            contentColor = TextWhite,
            glowColor = PurpleAccent,
            onClick = {
                val success = if (challengeType == "TEXT") {
                    if (difficulty == "EXTREME") userInput.trim() == targetText.trim() else userInput.trim().equals(targetText.trim(), ignoreCase = true)
                } else {
                    userInput.trim() == targetText.trim()
                }

                if (success) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSuccess()
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isError = true
                    scope.launch {
                        shakeOffset.animateTo(24f, tween(50))
                        shakeOffset.animateTo(-24f, tween(50))
                        shakeOffset.animateTo(16f, tween(50))
                        shakeOffset.animateTo(-16f, tween(50))
                        shakeOffset.animateTo(0f, tween(60))
                    }
                }
            }
        )
    }
}

enum class ShapeType(val title: String) { CIRCLE("Círculo Neón"), TRIANGLE("Triángulo"), STAR("Estrella"), HEART("Corazón"), WAVE("Onda Senoidal") }

@Composable
fun TracingChallengeScreen(difficulty: String, onSuccess: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val currentShape = remember { ShapeType.values().random() }
    val userPathPoints = remember { mutableStateListOf<Offset>() }
    var coverageProgress by remember { mutableFloatStateOf(0f) }

    val requiredTolerance = remember(difficulty) { when (difficulty) { "EASY" -> 50f; "MEDIUM" -> 38f; "HARD" -> 28f; else -> 20f } }
    val ready = coverageProgress >= 0.85f

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(8.dp))
            ChallengeHeader(emoji = "⌖", title = "DELINEADO PRECISO", subtitle = "Traza la figura: ${currentShape.title}", accent = NeonGreen)
        }

        Box(
            modifier = Modifier.size(300.dp).background(CardBackground.copy(alpha = 0.8f), RoundedCornerShape(28.dp))
                .border(2.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> userPathPoints.clear(); userPathPoints.add(offset) },
                        onDrag = { change, _ ->
                            val newPoint = change.position
                            userPathPoints.add(newPoint)
                            val targetPoints = generateShapePoints(currentShape, size.width.toFloat(), size.height.toFloat())
                            var matched = 0
                            targetPoints.forEach { target ->
                                val isCovered = userPathPoints.any { userPoint ->
                                    val dx = userPoint.x - target.x; val dy = userPoint.y - target.y
                                    sqrt(dx * dx + dy * dy) <= requiredTolerance
                                }
                                if (isCovered) matched++
                            }
                            coverageProgress = (matched.toFloat() / targetPoints.size).coerceIn(0f, 1f)
                            if (coverageProgress >= 0.85f) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
            ) {
                val targetPoints = generateShapePoints(currentShape, size.width, size.height)
                val targetPath = Path().apply {
                    if (targetPoints.isNotEmpty()) {
                        moveTo(targetPoints.first().x, targetPoints.first().y)
                        for (i in 1 until targetPoints.size) lineTo(targetPoints[i].x, targetPoints[i].y)
                    }
                }
                drawPath(targetPath, NeonGreen.copy(alpha = 0.35f), style = Stroke(requiredTolerance * 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(targetPath, NeonCyan, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                if (userPathPoints.size > 1) {
                    val userPath = Path().apply {
                        moveTo(userPathPoints.first().x, userPathPoints.first().y)
                        for (i in 1 until userPathPoints.size) lineTo(userPathPoints[i].x, userPathPoints[i].y)
                    }
                    drawPath(userPath, Color.Yellow, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            Text("${(coverageProgress * 100).toInt()}%", fontSize = 32.sp, fontWeight = FontWeight.Black, color = if (ready) NeonGreen else TextWhite.copy(alpha = 0.5f))
        }

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(
                progress = { coverageProgress },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(8.dp)),
                color = NeonGreen, trackColor = SurfaceDark
            )
            Spacer(modifier = Modifier.height(24.dp))
            BigActionButton(
                text = "DESACTIVAR ALARMA", enabled = ready, containerColor = NeonGreen, contentColor = Color.Black, glowColor = NeonGreen,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSuccess() }
            )
        }
    }
}

private data class StroopRound(val word: String, val inkColor: Color, val correctName: String, val options: List<Pair<String, Color>>)

@Composable
fun StroopChallengeScreen(difficulty: String, onSuccess: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val palette = remember { listOf("ROJO" to Color(0xFFFF3B30), "AZUL" to Color(0xFF0A84FF), "VERDE" to Color(0xFF30D158), "AMARILLO" to Color(0xFFFFD60A), "NARANJA" to Color(0xFFFF9F0A), "MORADO" to Color(0xFFBF5AF2), "ROSA" to Color(0xFFFF375F), "CIAN" to Color(0xFF00E5FF)) }
    val config = remember(difficulty) { when (difficulty) { "EASY" -> Triple(3, 3, 4); "MEDIUM" -> Triple(4, 4, 5); "HARD" -> Triple(6, 4, 6); else -> Triple(6, 5, 8) } }
    val totalRounds = config.first; val optionCount = config.second; val poolSize = config.third

    var currentRound by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<Boolean?>(null) }
    val shakeOffset = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    val roundData: StroopRound = remember(currentRound) {
        if (currentRound >= totalRounds) StroopRound("", Color.Transparent, "", emptyList())
        else {
            val pool = palette.take(poolSize); val wordPick = pool.random(); val inkPick = pool.filter { it.first != wordPick.first }.random()
            val distractors = pool.filter { it.first != inkPick.first }.shuffled().take(optionCount - 1)
            StroopRound(wordPick.first, inkPick.second, inkPick.first, (distractors + inkPick).shuffled())
        }
    }

    LaunchedEffect(currentRound) { if (currentRound >= totalRounds) { delay(300); onSuccess() } }
    val progress = currentRound.toFloat() / totalRounds

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp).offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween
    ) {
        ChallengeHeader(emoji = "🎨", title = "TEST DE STROOP", subtitle = "Pulsa el COLOR de la tinta, ¡no la palabra!", accent = Color(0xFFD14545))
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)), color = Color(0xFFD14545), trackColor = SurfaceDark)
            Spacer(modifier = Modifier.width(10.dp))
            Text("${currentRound.coerceAtMost(totalRounds)}/$totalRounds", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().shadow(24.dp, RoundedCornerShape(32.dp), ambientColor = Color(0xFFD14545), spotColor = Color(0xFFD14545))
                .border(2.dp, when (feedback) { true -> NeonGreen; false -> Color.Red; else -> Color(0xFFD14545).copy(alpha = 0.6f) }, RoundedCornerShape(32.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(32.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text(roundData.word, fontSize = 58.sp, fontWeight = FontWeight.Black, color = roundData.inkColor, letterSpacing = 4.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            roundData.options.chunked(2).forEach { rowOptions ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowOptions.forEach { (name, color) ->
                        Button(
                            onClick = {
                                if (feedback != null) return@Button
                                if (name == roundData.correctName) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress); feedback = true
                                    scope.launch { flashAlpha.snapTo(0.35f); flashAlpha.animateTo(0f, tween(300)); feedback = null; currentRound++ }
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); feedback = false
                                    scope.launch {
                                        shakeOffset.animateTo(24f, tween(45)); shakeOffset.animateTo(-24f, tween(55))
                                        shakeOffset.animateTo(16f, tween(55)); shakeOffset.animateTo(-16f, tween(55))
                                        shakeOffset.animateTo(0f, tween(70)); feedback = null
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(76.dp).shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = color, spotColor = color),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, color.copy(alpha = 0.6f))
                        ) {
                            Text(name, fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextWhite, letterSpacing = 1.sp)
                        }
                    }
                    if (rowOptions.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (feedback) { true -> "✓ ¡Correcto!"; false -> "✕ ¡Ese no era el color!"; else -> "Toca el color de la TINTA arriba" },
            color = when (feedback) { true -> NeonGreen; false -> Color.Red; else -> TextMuted },
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold
        )

        if (flashAlpha.value > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(NeonGreen.copy(alpha = flashAlpha.value * 0.4f)))
        }
    }
}

@Composable
fun ShakeChallengeScreen(difficulty: String, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val targetShakes = remember(difficulty) { when (difficulty) { "EASY" -> 8; "MEDIUM" -> 15; "HARD" -> 25; else -> 40 } }
    val shakeThreshold = remember(difficulty) { when (difficulty) { "EASY" -> 11f; "MEDIUM" -> 13f; "HARD" -> 15f; else -> 17f } }

    val shakeCount = remember { mutableIntStateOf(0) }
    val lastShakeTime = remember { longArrayOf(0L) }
    val previousAccel = remember { floatArrayOf(0f, 0f, 0f) }
    val pulseAnim = remember { Animatable(1f) }
    val ringPulse = remember { Animatable(1f) }
    var sensorAvailable by remember { mutableStateOf(true) }
    var justShook by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) { sensorAvailable = false; return@DisposableEffect onDispose { } }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime[0] < 200) return
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                val delta = abs(x - previousAccel[0]) + abs(y - previousAccel[1]) + abs(z - previousAccel[2])
                if (delta > shakeThreshold && shakeCount.intValue < targetShakes) {
                    shakeCount.intValue++; lastShakeTime[0] = now; justShook = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { pulseAnim.snapTo(1.2f); pulseAnim.animateTo(1f, tween(250)) }
                    scope.launch { ringPulse.snapTo(1.5f); ringPulse.animateTo(1f, tween(350, easing = FastOutSlowInEasing)) }
                }
                previousAccel[0] = x; previousAccel[1] = y; previousAccel[2] = z
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(shakeCount.intValue) { if (shakeCount.intValue >= targetShakes) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); delay(600); onSuccess() } }
    LaunchedEffect(justShook) { if (justShook) { delay(400); justShook = false } }
    val progress = (shakeCount.intValue.toFloat() / targetShakes).coerceIn(0f, 1f)

    val infinite = rememberInfiniteTransition(label = "shakePulse")
    val ambientPulse by infinite.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ambient")

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween
    ) {
        ChallengeHeader(emoji = "📳", title = "SACUDIDA DE ENERGÍA", subtitle = if (sensorAvailable) "¡Sacude el móvil con fuerza $targetShakes veces!" else "Sensor no disponible", accent = Color(0xFFE08E3C))
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.size(290.dp).scale(ambientPulse * pulseAnim.value), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(240.dp).scale(ringPulse.value).clip(CircleShape).background(Color(0xFFE08E3C).copy(alpha = (1.5f - ringPulse.value).coerceIn(0f, 1f) * 0.4f)))
            Canvas(modifier = Modifier.size(260.dp)) {
                val strokeWidth = 16.dp.toPx(); val radius = size.minDimension / 2 - strokeWidth / 2
                drawCircle(SurfaceDark, radius, style = Stroke(strokeWidth))
                drawArc(Color(0xFFE08E3C), -90f, 360f * progress, false, style = Stroke(strokeWidth, cap = StrokeCap.Round), topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2), size = Size(radius * 2, radius * 2))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(text = if (justShook) "💥" else "📳", fontSize = 60.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("${shakeCount.intValue}", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFFE08E3C), letterSpacing = (-1).sp)
                Text("/ $targetShakes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (!sensorAvailable) {
                Text("Tu dispositivo no tiene acelerómetro 😢", color = Color.Red, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                BigActionButton(text = "DESACTIVAR IGUALMENTE", enabled = true, containerColor = PurplePrimary, contentColor = TextWhite, glowColor = PurpleAccent, onClick = onSuccess)
            } else {
                Text(
                    text = when { progress >= 1f -> "✓ ¡Alarma desactivada!"; progress > 0.66f -> "¡Ya casi! ¡Sigue sacudiendo!"; progress > 0.33f -> "¡Vamos, más fuerte!"; else -> "¡Empieza a sacudir!" },
                    color = when { progress >= 1f -> NeonGreen; progress > 0.33f -> Color(0xFFE08E3C); else -> TextMuted },
                    fontSize = 15.sp, fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), color = Color(0xFFE08E3C), trackColor = SurfaceDark)
                Spacer(modifier = Modifier.height(12.dp))
                Text("⚠️  Sujeta bien el móvil para no lanzarlo", fontSize = 11.sp, color = TextMuted.copy(alpha = 0.7f))
            }
        }
    }
}

fun generateShapePoints(shape: ShapeType, width: Float, height: Float): List<Offset> {
    val points = mutableListOf<Offset>(); val cx = width / 2f; val cy = height / 2f; val r = width.coerceAtMost(height) * 0.35f
    when (shape) {
        ShapeType.CIRCLE -> { val count = 40; for (i in 0 until count) { val angle = (2 * Math.PI * i / count); points.add(Offset((cx + r * cos(angle)).toFloat(), (cy + r * sin(angle)).toFloat())) } }
        ShapeType.TRIANGLE -> {
            val stepsPerSide = 15; val p1 = Offset(cx, cy - r); val p2 = Offset(cx + r * 0.866f, cy + r * 0.5f); val p3 = Offset(cx - r * 0.866f, cy + r * 0.5f)
            for (i in 0 until stepsPerSide) points.add(Offset(p1.x + (p2.x - p1.x) * i / stepsPerSide, p1.y + (p2.y - p1.y) * i / stepsPerSide))
            for (i in 0 until stepsPerSide) points.add(Offset(p2.x + (p3.x - p2.x) * i / stepsPerSide, p2.y + (p3.y - p2.y) * i / stepsPerSide))
            for (i in 0 until stepsPerSide) points.add(Offset(p3.x + (p1.x - p3.x) * i / stepsPerSide, p3.y + (p1.y - p3.y) * i / stepsPerSide))
        }
        ShapeType.STAR -> { for (i in 0 until 10) { val angle = i * Math.PI / 5 - Math.PI / 2; val currentR = if (i % 2 == 0) r else r * 0.4f; points.add(Offset((cx + currentR * cos(angle)).toFloat(), (cy + currentR * sin(angle)).toFloat())) } }
        ShapeType.HEART -> { val count = 50; for (i in 0 until count) { val t = (2 * Math.PI * i / count); val x = 16 * Math.sin(t) * Math.sin(t) * Math.sin(t); val y = -(13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)); points.add(Offset((cx + x * (r / 18f)).toFloat(), (cy + y * (r / 18f)).toFloat())) } }
        ShapeType.WAVE -> { val count = 40; for (i in 0 until count) { val x = cx - r + (2 * r * i / count); val y = cy + (r * 0.5f * sin(2 * Math.PI * i / 20)).toFloat(); points.add(Offset(x, y)) } }
    }
    return points
}