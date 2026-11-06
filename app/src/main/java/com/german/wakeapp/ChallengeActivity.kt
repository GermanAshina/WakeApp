package com.german.wakeapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import kotlinx.coroutines.launch
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
        val rounds = intent.getIntExtra("ROUNDS", 1).coerceIn(1, 10)

        // Bloquea el botón atrás: la alarma no se puede cerrar así
        onBackPressedDispatcher.addCallback(this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // No hacer nada. Obliga a resolver el desafío.
                }
            }
        )

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
                val ambientColor = when (challengeType) {
                    "TRACING" -> NeonGreen
                    "TEXT" -> NeonCyan
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
                    MultiRoundChallengeHost(
                        challengeType = challengeType,
                        difficulty = difficulty,
                        totalRounds = rounds,
                        onAllDone = { stopAndFinish(difficulty) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Si por alguna razón el servicio ya no está sonando (proceso matado, etc.)
        // pero seguimos aquí, lo relanzamos para no dejar un estado inconsistente.
        AlarmStateHolder.restoreIfNeeded(this)
        if (!AlarmStateHolder.isRinging) {
            // El servicio ya no corre: significa que el desafío ya se resolvió
            // (o el sistema mató el servicio). Cerramos sin más.
            // Esto es defensivo: no debería pasar normalmente.
        }
    }

    private fun stopAndFinish(difficulty: String) {
        val streakManager = StreakManager(this)
        val achievementManager = AchievementManager(this)

        val levelBefore = streakManager.getLevel()

        val newStreak = streakManager.registerSuccessfulWakeUp()

        val baseXp = when (difficulty) {
            "EASY" -> 40
            "MEDIUM" -> 60
            "HARD" -> 90
            "EXTREME" -> 150
            else -> 60
        }
        val boostConsumed = streakManager.consumeInventoryItem("xp_boost")
        val xpGained = if (boostConsumed) (baseXp * 1.5f).roundToInt() else baseXp
        val levelsUpByXp = streakManager.addXp(xpGained)

        val newlyUnlocked = mutableListOf<Achievement>()
        newlyUnlocked.addAll(achievementManager.checkStreakAchievements(newStreak))

        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        newlyUnlocked.addAll(achievementManager.checkEarlyBird(currentHour))

        val currentChallengeType = intent.getStringExtra("CHALLENGE_TYPE") ?: "MATH"
        newlyUnlocked.addAll(achievementManager.checkChallengeAchievements(currentChallengeType))
        newlyUnlocked.addAll(achievementManager.checkMultiRoundAchievements(intent.getIntExtra("ROUNDS", 1)))

        val bonusLevels = newlyUnlocked.size
        val bonusPoints = newlyUnlocked.sumOf { it.rewardPoints }
        if (bonusLevels > 0) streakManager.addLevels(bonusLevels)
        if (bonusPoints > 0) streakManager.addPoints(bonusPoints)

        val levelAfter = streakManager.getLevel()

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

        streakManager.setPendingReward(
            xp = xpGained,
            levelUp = (levelsUpByXp > 0 || bonusLevels > 0),
            oldLevel = levelBefore,
            newLevel = levelAfter,
            achievementsJson = achievementsJson,
            bonusLevels = bonusLevels,
            bonusPoints = bonusPoints
        )

        // Marcar como detenida ANTES de parar el servicio para que MainActivity
        // no piense que sigue sonando si se reabre rápido.
        AlarmStateHolder.markStopped(this)

        // Parar servicio explícitamente (también se llama en onDestroy, pero aquí
        // aseguramos que el audio se corte YA)
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
fun MultiRoundChallengeHost(
    challengeType: String,
    difficulty: String,
    totalRounds: Int,
    onAllDone: () -> Unit
) {
    val context = LocalContext.current
    val streakManager = remember { StreakManager(context) }
    var roundsDone by remember { mutableIntStateOf(0) }
    var autoDisableUsed by remember { mutableStateOf(false) }

    fun onRoundSuccess() {
        roundsDone += 1

        val hasAutoDisable = streakManager.getInventoryCount("auto_disable") > 0
        if (hasAutoDisable && totalRounds > 1 && roundsDone < totalRounds) {
            streakManager.consumeInventoryItem("auto_disable")
            autoDisableUsed = true
            onAllDone()
            return
        }

        if (roundsDone >= totalRounds) {
            onAllDone()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (totalRounds > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { roundsDone.toFloat() / totalRounds },
                    modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = AmberAccent,
                    trackColor = SurfaceDark
                )
                Text("$roundsDone / $totalRounds", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (autoDisableUsed) Text("🔓", fontSize = 16.sp)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            key(roundsDone) {
                when (challengeType) {
                    "TRACING" -> TracingChallengeScreen(
                        difficulty = difficulty,
                        onSuccess = { onRoundSuccess() }
                    )
                    else -> StandardChallengeScreen(
                        challengeType = challengeType,
                        difficulty = difficulty,
                        onSuccess = { onRoundSuccess() }
                    )
                }
            }
        }
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

enum class ShapeType(val title: String) {
    CIRCLE("Círculo Neón"),
    TRIANGLE("Triángulo"),
    STAR("Estrella"),
    HEART("Corazón"),
    WAVE("Onda Senoidal")
}

@Composable
fun TracingChallengeScreen(difficulty: String, onSuccess: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val currentShape = remember { ShapeType.values().random() }
    val userPathPoints = remember { mutableStateListOf<Offset>() }
    var coverageProgress by remember { mutableFloatStateOf(0f) }

    val requiredTolerance = remember(difficulty) {
        when (difficulty) {
            "EASY" -> 50f
            "MEDIUM" -> 38f
            "HARD" -> 28f
            else -> 20f
        }
    }
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
                text = "DESACTIVAR ALARMA",
                enabled = ready,
                containerColor = NeonGreen,
                contentColor = Color.Black,
                glowColor = NeonGreen,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSuccess()
                }
            )
        }
    }
}

fun generateShapePoints(shape: ShapeType, width: Float, height: Float): List<Offset> {
    val points = mutableListOf<Offset>()
    val cx = width / 2f
    val cy = height / 2f
    val r = width.coerceAtMost(height) * 0.35f

    when (shape) {
        ShapeType.CIRCLE -> {
            val count = 40
            for (i in 0 until count) {
                val angle = (2 * Math.PI * i / count)
                points.add(Offset((cx + r * cos(angle)).toFloat(), (cy + r * sin(angle)).toFloat()))
            }
        }
        ShapeType.TRIANGLE -> {
            val stepsPerSide = 15
            val p1 = Offset(cx, cy - r)
            val p2 = Offset(cx + r * 0.866f, cy + r * 0.5f)
            val p3 = Offset(cx - r * 0.866f, cy + r * 0.5f)
            for (i in 0 until stepsPerSide) points.add(Offset(p1.x + (p2.x - p1.x) * i / stepsPerSide, p1.y + (p2.y - p1.y) * i / stepsPerSide))
            for (i in 0 until stepsPerSide) points.add(Offset(p2.x + (p3.x - p2.x) * i / stepsPerSide, p2.y + (p3.y - p2.y) * i / stepsPerSide))
            for (i in 0 until stepsPerSide) points.add(Offset(p3.x + (p1.x - p3.x) * i / stepsPerSide, p3.y + (p1.y - p3.y) * i / stepsPerSide))
        }
        ShapeType.STAR -> {
            for (i in 0 until 10) {
                val angle = i * Math.PI / 5 - Math.PI / 2
                val currentR = if (i % 2 == 0) r else r * 0.4f
                points.add(Offset((cx + currentR * cos(angle)).toFloat(), (cy + currentR * sin(angle)).toFloat()))
            }
        }
        ShapeType.HEART -> {
            val count = 50
            for (i in 0 until count) {
                val t = (2 * Math.PI * i / count)
                val x = 16 * Math.sin(t) * Math.sin(t) * Math.sin(t)
                val y = -(13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t))
                points.add(Offset((cx + x * (r / 18f)).toFloat(), (cy + y * (r / 18f)).toFloat()))
            }
        }
        ShapeType.WAVE -> {
            val count = 40
            for (i in 0 until count) {
                val x = cx - r + (2 * r * i / count)
                val y = cy + (r * 0.5f * sin(2 * Math.PI * i / 20)).toFloat()
                points.add(Offset(x, y))
            }
        }
    }
    return points
}