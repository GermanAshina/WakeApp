package com.german.wakeapp

import android.media.MediaPlayer
import android.content.res.Resources
import androidx.compose.ui.res.painterResource
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.german.wakeapp.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200)
            }
        }

        setContent {
            WakeAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
                    MainScreen()
                }
            }
        }
    }
}

data class AlarmItem(
    val id: Long = System.currentTimeMillis(),
    val hour: Int,
    val minute: Int,
    var isEnabled: Boolean = true,
    val challengeType: String = "MATH",
    val difficulty: String = "MEDIUM",
    val soundUri: String? = null,
    val soundName: String? = null
)

data class RewardInfo(
    val xpGained: Int,
    val levelUp: Boolean,
    val oldLevel: Int,
    val newLevel: Int,
    val achievements: List<RewardAchievement> = emptyList(),
    val bonusLevels: Int = 0,
    val bonusPoints: Int = 0
)

fun challengeAccent(type: String): Color = when (type) {
    "MATH" -> RedAccent
    "TEXT" -> AmberAccent
    "TRACING" -> SuccessGreen
    "STROOP" -> Color(0xFFD14545)
    "SHAKE" -> Color(0xFFE08E3C)
    else -> RedPrimary
}

fun challengeEmoji(type: String): String = when (type) {
    "MATH" -> "∑"
    "TEXT" -> "✎"
    "TRACING" -> "⌖"
    "STROOP" -> "🎨"
    "SHAKE" -> "📳"
    else -> "⏰"
}

fun challengeTitle(type: String): String = when (type) {
    "MATH" -> "Matemáticas"
    "TEXT" -> "Frase Exacta"
    "TRACING" -> "Delineado"
    "STROOP" -> "Colores"
    "SHAKE" -> "Sacudida"
    else -> "Desafío"
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    var name = "Sonido personalizado"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx) ?: name
            }
        }
    } catch (_: Exception) {}
    return name
}

enum class AppSection(val label: String, val emoji: String) {
    ALARMS("Alarmas", "⏰"),
    PROGRESS("Progreso", "📊"),
    SHOP("Tienda", "🛒"),
    READING("Lectura", "📖")
}

@Composable
fun AnimatedBackground() {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val backgroundRes = when {
        hour in 6..17 -> R.drawable.fondo_base_dia
        hour in 18..19 -> R.drawable.fondo_base_tarde
        else -> R.drawable.fondo_base_noche
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )
    }
}

@Composable
fun MainHeader(
    streak: Int,
    points: Int,
    level: Int,
    xp: Int,
    onInfoClick: () -> Unit,
    onAddPoints: () -> Unit,
    onRewardsClick: () -> Unit,
    onWatchAd: () -> Unit
) {
    val animatedPoints by animateIntAsState(targetValue = points, label = "pts")
    val animatedStreak by animateIntAsState(targetValue = streak, label = "strk")
    val animatedXp by animateIntAsState(targetValue = xp, label = "xp")

    val xpNeeded = StreakManager.xpForLevel(level)
    val xpProgress = (animatedXp.toFloat() / xpNeeded).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground.copy(alpha = 0.85f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.5.dp, RedAccent, CircleShape)
                    .clickable { onInfoClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_icon_info),
                    contentDescription = "Información",
                    modifier = Modifier.size(30.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "WakeApp",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = TextWhite,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "Nivel $level",
                    fontSize = 11.sp,
                    color = AmberAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onAddPoints,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        "+10⚡",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // BOTÓN DE ANUNCIO FALSO
                Button(
                    onClick = onWatchAd,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("📺", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AmberAccent.copy(alpha = 0.15f))
                    .border(1.5.dp, AmberAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$level", fontSize = 13.sp, fontWeight = FontWeight.Black, color = AmberAccent)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { xpProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AmberAccent,
                    trackColor = SurfaceDark
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("XP", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        // ICONO DE REGALO PARA VER RECOMPENSAS
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(AmberAccent.copy(alpha = 0.2f))
                                .clickable { onRewardsClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎁", fontSize = 10.sp)
                        }
                    }
                    Text("$animatedXp / $xpNeeded", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatPill(
                icon = "🔥",
                label = "$animatedStreak días",
                accent = Color(0xFFE08E3C),
                modifier = Modifier.weight(1f)
            )
            StatPill(
                icon = "⚡",
                label = "$animatedPoints pts",
                accent = AmberAccent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatPill(icon: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark.copy(alpha = 0.7f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            label,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            fontSize = 13.sp
        )
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val streakManager = remember { StreakManager(context) }
    val alarmRepository = remember { AlarmRepository(context) }
    val readingModeManager = remember { ReadingModeManager(context) }
    val achievementManager = remember { AchievementManager(context) }

    var currentStreak by remember { mutableIntStateOf(streakManager.getStreak()) }
    var points by remember { mutableIntStateOf(streakManager.getPoints()) }
    var currentLevel by remember { mutableIntStateOf(streakManager.getLevel()) }
    var currentXp by remember { mutableIntStateOf(streakManager.getXp()) }
    var alarms by remember { mutableStateOf(alarmRepository.loadAll()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showRewardsDialog by remember { mutableStateOf(false) }
    var selectedChallengeType by remember { mutableStateOf("MATH") }
    var selectedDifficulty by remember { mutableStateOf("MEDIUM") }
    var pendingSoundUri by remember { mutableStateOf<String?>(null) }
    var pendingSoundName by remember { mutableStateOf<String?>(null) }
    var currentSection by remember { mutableStateOf(AppSection.ALARMS) }
    var rewardDialog by remember { mutableStateOf<RewardInfo?>(null) }
    var showAdDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        alarms.filter { it.isEnabled }.forEach { scheduleAlarm(context, it) }
    }

    LaunchedEffect(alarms) {
        alarmRepository.saveAll(alarms)
    }

    // Refrescar al volver a la app (por si la recompensa se generó con la app cerrada)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Revisar si la racha ha caducado por pasar 24h
                streakManager.checkStreakExpiration()

                currentStreak = streakManager.getStreak()
                points = streakManager.getPoints()
                currentLevel = streakManager.getLevel()
                currentXp = streakManager.getXp()

                // Comprobar si hemos llegado a 10k puntos
                achievementManager.checkPointsAchievement(points).let { newAch ->
                    if (newAch.isNotEmpty()) {
                        rewardDialog = RewardInfo(
                            xpGained = 0, levelUp = false, oldLevel = currentLevel, newLevel = currentLevel,
                            achievements = newAch.map { RewardAchievement(it.id, it.title, it.emoji, it.rewardPoints) },
                            bonusLevels = 0, bonusPoints = newAch.sumOf { it.rewardPoints }
                        )
                    }
                }

                streakManager.consumePendingReward()?.let { data ->
                    rewardDialog = RewardInfo(
                        xpGained = data.xp,
                        levelUp = data.levelUp,
                        oldLevel = data.oldLevel,
                        newLevel = data.newLevel,
                        achievements = data.achievements,
                        bonusLevels = data.bonusLevels,
                        bonusPoints = data.bonusPoints
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val soundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {}
            pendingSoundUri = uri.toString()
            pendingSoundName = queryDisplayName(context, uri)
        }
    }

    val infinite = rememberInfiniteTransition(label = "fab")
    val fabScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fabScale"
    )
    val ringScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ringScale"
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ringAlpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        AnimatedBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MainHeader(
                    streak = currentStreak,
                    points = points,
                    level = currentLevel,
                    xp = currentXp,
                    onInfoClick = { showInfoDialog = true },
                    onRewardsClick = { showRewardsDialog = true },
                    onAddPoints = {
                        streakManager.addPoints(10)
                        points = streakManager.getPoints()
                    },
                    onWatchAd = { showAdDialog = true }
                )
            },
            bottomBar = {
                WakeBottomBar(current = currentSection, onSelect = { currentSection = it })
            },
            floatingActionButton = {
                if (currentSection == AppSection.ALARMS) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .scale(ringScale)
                                .clip(CircleShape)
                                .background(RedAccent.copy(alpha = ringAlpha))
                        )
                        FloatingActionButton(
                            onClick = {
                                pendingSoundUri = null
                                pendingSoundName = null
                                showTimePicker = true
                            },
                            containerColor = RedPrimary,
                            contentColor = TextWhite,
                            shape = CircleShape,
                            modifier = Modifier
                                .scale(fabScale)
                                .shadow(20.dp, CircleShape, ambientColor = RedAccent, spotColor = RedAccent)
                        ) {
                            Text("+", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                AnimatedContent(
                    targetState = currentSection,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    }, label = "section_transition"
                ) { section ->
                    when (section) {
                        AppSection.ALARMS -> AlarmsSection(
                            alarms = alarms,
                            onToggle = { alarm, enabled ->
                                alarms = alarms.map { if (it.id == alarm.id) it.copy(isEnabled = enabled) else it }
                                if (enabled) scheduleAlarm(context, alarm.copy(isEnabled = true)) else cancelAlarm(context, alarm)
                            },
                            onDelete = { alarm ->
                                cancelAlarm(context, alarm)
                                alarms = alarms.filter { it.id != alarm.id }
                            }
                        )
                        AppSection.PROGRESS -> ProgressSection(
                            context = context,
                            streak = currentStreak,
                            points = points,
                            level = currentLevel,
                            xp = currentXp
                        )
                        AppSection.SHOP -> ShopSection(
                            points = points,
                            streakManager = streakManager,
                            onBuy = { cost ->
                                if (streakManager.spendPoints(cost)) {
                                    points = streakManager.getPoints()
                                }
                            },
                            onAchievementUnlocked = {
                                achievementManager.checkShopBuyer().let { newAch ->
                                    if (newAch.isNotEmpty()) {
                                        // Si compra, le damos el logro y le sumamos los puntos
                                        streakManager.addPoints(newAch.sumOf { it.rewardPoints })
                                        points = streakManager.getPoints()
                                        rewardDialog = RewardInfo(
                                            xpGained = 0, levelUp = false, oldLevel = currentLevel, newLevel = currentLevel,
                                            achievements = newAch.map { RewardAchievement(it.id, it.title, it.emoji, it.rewardPoints) },
                                            bonusLevels = 0, bonusPoints = newAch.sumOf { it.rewardPoints }
                                        )
                                    }
                                }
                            }
                        )
                        AppSection.READING -> ReadingSection(manager = readingModeManager)
                    }
                }

                if (showTimePicker) {
                    CustomTimePickerDialog(
                        onDismiss = { showTimePicker = false },
                        onConfirm = { hour, minute ->
                            val newAlarm = AlarmItem(
                                hour = hour, minute = minute, isEnabled = true,
                                challengeType = selectedChallengeType, difficulty = selectedDifficulty,
                                soundUri = pendingSoundUri, soundName = pendingSoundName
                            )
                            alarms = alarms + newAlarm
                            scheduleAlarm(context, newAlarm)
                            showTimePicker = false
                            pendingSoundUri = null
                            pendingSoundName = null
                        },
                        selectedChallengeType = selectedChallengeType,
                        onChallengeTypeSelected = { selectedChallengeType = it },
                        selectedDifficulty = selectedDifficulty,
                        onDifficultySelected = { selectedDifficulty = it },
                        soundName = pendingSoundName,
                        onPickSound = { soundPickerLauncher.launch(arrayOf("audio/*")) },
                        onClearSound = { pendingSoundUri = null; pendingSoundName = null }
                    )
                }

                if (showInfoDialog) {
                    InfoDialog(onDismiss = { showInfoDialog = false })
                }

                if (showRewardsDialog) {
                    LevelRewardsDialog(onDismiss = { showRewardsDialog = false })
                }

                if (showAdDialog) {
                    FakeAdDialog(
                        onDismiss = { showAdDialog = false },
                        onReward = {
                            achievementManager.checkAdWatch().let { newAch ->
                                if (newAch.isNotEmpty()) {
                                    streakManager.addPoints(newAch.sumOf { it.rewardPoints })
                                    points = streakManager.getPoints()
                                    rewardDialog = RewardInfo(
                                        xpGained = 0, levelUp = false, oldLevel = currentLevel, newLevel = currentLevel,
                                        achievements = newAch.map { RewardAchievement(it.id, it.title, it.emoji, it.rewardPoints) },
                                        bonusLevels = 0, bonusPoints = newAch.sumOf { it.rewardPoints }
                                    )
                                }
                            }
                        }
                    )
                }

                rewardDialog?.let { info ->
                    if (info.achievements.isNotEmpty()) {
                        AchievementUnlockedOverlay(
                            info = info,
                            onDismiss = {
                                // refrescar contadores por si el nivel subió por logros
                                currentLevel = streakManager.getLevel()
                                currentXp = streakManager.getXp()
                                points = streakManager.getPoints()
                                currentStreak = streakManager.getStreak()
                                rewardDialog = null
                            }
                        )
                    } else {
                        XpRewardDialog(
                            info = info,
                            onDismiss = { rewardDialog = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FakeAdDialog(onDismiss: () -> Unit, onReward: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        onReward()
        onDismiss()
    }

    Dialog(onDismissRequest = { /* Bloqueado */ }) {
        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ANUNCIO", color = Color.Gray, fontSize = 12.sp, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("📺", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Espera $secondsLeft segundos...", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LevelRewardsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(AmberAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Recompensas de Nivel", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Cada vez que subas de nivel superando desafíos, obtendrás recompensas directas en tu cuenta:",
                    fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta de recompensa
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, AmberAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(AmberAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Puntos de Energía", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Gana +50 🪙 por cada nivel alcanzado.", color = TextMuted, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Recuerda: resolver desafíos también te otorga XP y puntos directamente. ¡Mantén tu racha activa!",
                    fontSize = 12.sp, color = TextMuted, lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("¡A por ello!", color = AmberAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun XpRewardDialog(info: RewardInfo, onDismiss: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val glow = remember { Animatable(0f) }
    var displayedXp by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        glow.animateTo(1f, tween(400))
        val steps = 30
        for (i in 0..steps) {
            displayedXp = (info.xpGained * i / steps)
            delay(18)
        }
    }

    Dialog(onDismissRequest = { /* bloquea el cierre por toque */ }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale.value)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(CardBackground, SurfaceDark)
                    )
                )
                .border(
                    width = 2.dp,
                    color = AmberAccent.copy(alpha = glow.value),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val infinite = rememberInfiniteTransition(label = "reward")
                val pulse by infinite.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        tween(900, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Text(
                    text = if (info.levelUp) "🎉" else "⭐",
                    fontSize = 64.sp,
                    modifier = Modifier.scale(pulse)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (info.levelUp) "¡NIVEL SUBIDO!" else "¡DESAFÍO COMPLETADO!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = if (info.levelUp) AmberAccent else SuccessGreen,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("+$displayedXp", fontSize = 42.sp, fontWeight = FontWeight.Black, color = AmberAccent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("XP", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AmberAccent.copy(alpha = 0.8f))
                }

                if (info.levelUp) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nivel ${info.oldLevel}", fontSize = 14.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("➜", fontSize = 16.sp, color = AmberAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nivel ${info.newLevel}", fontSize = 18.sp, color = TextWhite, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("¡SEGUIR!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun WakeBottomBar(current: AppSection, onSelect: (AppSection) -> Unit) {
    Surface(
        color = SurfaceDark.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(70.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppSection.values().forEach { section ->
                val selected = section == current
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { onSelect(section) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (selected) RedPrimary.copy(alpha = 0.2f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(section.emoji, fontSize = if (selected) 22.sp else 20.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        section.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) RedAccent else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmsSection(alarms: List<AlarmItem>, onToggle: (AlarmItem, Boolean) -> Unit, onDelete: (AlarmItem) -> Unit) {
    if (alarms.isEmpty()) {
        EmptyState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(alarms, key = { it.id }) { alarm ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = tween(400)
                    )
                ) {
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { enabled -> onToggle(alarm, enabled) },
                        onDelete = { onDelete(alarm) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    val infinite = rememberInfiniteTransition(label = "empty")
    val scale by infinite.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp).scale(scale).clip(CircleShape)
                .background(RedPrimary.copy(alpha = 0.12f))
                .border(2.dp, RedAccent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("⏰", fontSize = 60.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Sin alarmas programadas", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Pulsa  +  para crear la primera", color = TextMuted, fontSize = 13.sp)
    }
}

@Composable
fun AlarmCard(alarm: AlarmItem, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val accentColor = challengeAccent(alarm.challengeType)
    val timeFormatted = String.format("%02d:%02d", alarm.hour, alarm.minute)

    val glow by animateFloatAsState(targetValue = if (alarm.isEnabled) 1f else 0f, animationSpec = tween(500), label = "glow")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (alarm.isEnabled) 16.dp else 0.dp, RoundedCornerShape(24.dp), ambientColor = accentColor, spotColor = accentColor)
            .border(
                width = 1.dp,
                color = if (alarm.isEnabled) accentColor.copy(alpha = 0.5f * glow) else CardBorder,
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier.width(6.dp).fillMaxHeight().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (alarm.isEnabled) 1f else 0.25f),
                            accentColor.copy(alpha = if (alarm.isEnabled) 0.3f else 0.1f)
                        )
                    )
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(challengeEmoji(alarm.challengeType), fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timeFormatted,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (alarm.isEnabled) TextWhite else TextMuted,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(challengeTitle(alarm.challengeType), fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(TextMuted))
                        Text(text = alarm.difficulty, fontSize = 12.sp, color = AmberAccent, fontWeight = FontWeight.SemiBold)
                    }
                    if (!alarm.soundName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎵", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = alarm.soundName, fontSize = 11.sp, color = SuccessGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "🔔 Sonido del sistema", fontSize = 11.sp, color = TextMuted.copy(alpha = 0.7f))
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceDark
                        )
                    )
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🗑", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressSection(context: Context, streak: Int, points: Int, level: Int, xp: Int) {
    val achievementManager = remember { AchievementManager(context) }
    // Al quitarle el 'remember', forzamos a que se recalcule cada vez que se dibuja la pantalla
    val unlockedCount = achievementManager.unlockedCount()
    val xpNeeded = StreakManager.xpForLevel(level)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape)
                .background(SurfaceDark).border(2.dp, RedAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("👤", fontSize = 48.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Tu progreso", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Text(text = "Sigue así, cada día cuenta", fontSize = 13.sp, color = AmberAccent)
        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                ProfileStatRow("Nivel", "$level 🎖️")
                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))
                ProfileStatRow("XP", "$xp / $xpNeeded ⚡")
                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))
                ProfileStatRow("Racha Activa", "$streak Días 🔥")
                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))
                ProfileStatRow("Puntos de Energía", "$points 🪙")
                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))
                ProfileStatRow("Logros", "$unlockedCount / ${AchievementCatalog.all.size} 🏆")
                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))
                ProfileStatRow("Efectividad Alarma", "100%")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("LOGROS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.5.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(12.dp))

        AchievementCatalog.all.forEach { ach ->
            val unlocked = achievementManager.isUnlocked(ach.id)
            val progress = achievementManager.getProgress(ach, streak, points)
            AchievementRow(ach, unlocked, progress)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProfileStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, fontSize = 14.sp)
        Text(value, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun AchievementRow(achievement: Achievement, unlocked: Boolean, progress: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (unlocked) CardBackground.copy(alpha = 0.8f) else SurfaceDark.copy(alpha = 0.5f))
            .border(1.dp, if (unlocked) RedAccent.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(achievement.emoji, fontSize = 24.sp, modifier = Modifier.width(40.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                achievement.title,
                color = if (unlocked) TextWhite else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(achievement.description, color = TextMuted, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Progreso: 0/1, 0/3, 0/7 (o 1/1, 3/3, 7/7 si desbloqueado)
                Text(
                    text = "$progress/${achievement.required}",
                    color = if (unlocked) SuccessGreen else AmberAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Recompensa visible: +1 Nivel (iconito) y +N 🪙
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmberAccent.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⬆️", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "+1 Nivel",
                                color = AmberAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SuccessGreen.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "+${achievement.rewardPoints} 🪙",
                            color = SuccessGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Text(if (unlocked) "✓" else "🔒", fontSize = 16.sp)
    }
}

@Composable
fun ReadingSection(manager: ReadingModeManager) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(manager.isEnabled()) }
    var brightness by remember { mutableStateOf(manager.getCustomBrightness()) }

    LaunchedEffect(enabled) {
        manager.setEnabled(enabled)
        if (context is MainActivity) { manager.applyToWindow(context.window, enabled) }
    }
    LaunchedEffect(brightness) {
        manager.setCustomBrightness(brightness)
        if (enabled && context is MainActivity) {
            val params = context.window.attributes
            params.screenBrightness = brightness
            context.window.attributes = params
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MODO LECTURA", fontSize = 22.sp, fontWeight = FontWeight.Black, color = AmberAccent, letterSpacing = 2.sp)
                Text("Cuida tus ojos y tu sueño", fontSize = 12.sp, color = TextMuted)
            }
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = TextWhite, checkedTrackColor = AmberAccent, uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceDark)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.8f)), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("¿Qué es el Modo Lectura?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "El modo lectura reduce la cantidad de luz azul que emite la pantalla y baja el brillo general. La luz azul engaña a tu cerebro haciéndole creer que aún es de día, retrasando la melatonina.\n\nCon este modo activado, la pantalla se vuelve más cálida y tenue, perfecta para antes de acostarte.",
                    fontSize = 13.sp, color = TextMuted, lineHeight = 19.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.8f)), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Brillo personalizado", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔅", fontSize = 18.sp)
                    Slider(
                        value = brightness, onValueChange = { brightness = it }, valueRange = 0.05f..1f,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        colors = SliderDefaults.colors(thumbColor = AmberAccent, activeTrackColor = RedAccent, inactiveTrackColor = SurfaceDark)
                    )
                    Text("🔆", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("${(brightness * 100).toInt()}%", fontSize = 12.sp, color = AmberAccent, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(RedPrimary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Text("ℹ️", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("¿Qué es WakeApp?", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("WakeApp es tu despertador inteligente que no te deja volver a dormir.", fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("En lugar de un simple botón de \"apagar\", cada alarma te lanza un desafío mental o físico:\n\n• Cálculo mental\n• Frase exacta\n• Delineado\n• Test de Stroop\n• Sacudida", fontSize = 13.sp, color = TextMuted)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido", color = AmberAccent, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun CustomTimePickerDialog(
    onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit,
    selectedChallengeType: String, onChallengeTypeSelected: (String) -> Unit,
    selectedDifficulty: String, onDifficultySelected: (String) -> Unit,
    soundName: String?, onPickSound: () -> Unit, onClearSound: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(7) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(RedPrimary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Text("⏰", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Nueva Alarma", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    NumberPicker(value = selectedHour, range = 0..23, onValueChange = { selectedHour = it })
                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.padding(horizontal = 8.dp))
                    NumberPicker(value = selectedMinute, range = 0..59, step = 1, onValueChange = { selectedMinute = it })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Tipo de Desafío", fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("MATH" to "Cálculo", "TEXT" to "Frase", "TRACING" to "Delinear", "STROOP" to "Stroop", "SHAKE" to "Sacudir")) { (type, label) ->
                        FilterChip(
                            selected = selectedChallengeType == type, onClick = { onChallengeTypeSelected(type) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = challengeAccent(type), selectedLabelColor = Color.Black, containerColor = SurfaceDark, labelColor = TextMuted)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Dificultad", fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("EASY", "MEDIUM", "HARD", "EXTREME")) { diff ->
                        FilterChip(
                            selected = selectedDifficulty == diff, onClick = { onDifficultySelected(diff) },
                            label = { Text(diff, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AmberAccent, selectedLabelColor = Color.Black, containerColor = SurfaceDark, labelColor = TextMuted)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Sonido de Alarma", fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Text("MP3 · M4A · WAV · OGG", fontSize = 9.sp, color = TextMuted.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceDark)
                        .border(1.dp, if (soundName != null) SuccessGreen.copy(alpha = 0.7f) else CardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (soundName != null) "🎵" else "🔔", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(soundName ?: "Sonido predeterminado", fontSize = 13.sp, color = if (soundName != null) SuccessGreen else TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (soundName != null) "Seleccionado" else "Sin personalizar", fontSize = 10.sp, color = TextMuted.copy(alpha = 0.7f))
                    }
                    if (soundName != null) {
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onClearSound() }, contentAlignment = Alignment.Center) {
                            Text("✕", color = Color(0xFFFF5252), fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onPickSound,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.6f))
                ) {
                    Text("📁  Elegir archivo de audio", color = AmberAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selectedHour, selectedMinute) }, colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)) { Text("Guardar", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } }
    )
}

@Composable
fun NumberPicker(value: Int, range: IntRange, step: Int = 1, onValueChange: (Int) -> Unit) {
    val items = remember(range, step) { range.filter { it % step == 0 } }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = items.indexOf(value).coerceAtLeast(0))

    Box(modifier = Modifier.height(120.dp).width(64.dp), contentAlignment = Alignment.Center) {
        LazyColumn(state = listState, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
            items(items) { item ->
                val isSelected = item == value
                Text(
                    text = String.format("%02d", item),
                    fontSize = if (isSelected) 26.sp else 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) AmberAccent else TextMuted,
                    modifier = Modifier.padding(vertical = 6.dp).clickable { onValueChange(item) }
                )
            }
        }
    }
}

@Composable
fun AchievementUnlockedOverlay(
    info: RewardInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // REPRODUCIR SONIDO AL MOSTRAR EL LOGRO
    LaunchedEffect(Unit) {
        try {
            val resId = context.resources.getIdentifier("achievement_se", "raw", context.packageName)
            if (resId != 0) {
                val mediaPlayer = MediaPlayer.create(context, resId)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener { it.release() }
            }
        } catch (_: Exception) {}
    }

    // Cola de logros: mostramos uno detrás de otro
    var queue by remember { mutableStateOf(info.achievements.toList()) }
    val current = queue.firstOrNull()

    if (current == null) {
        // Cuando terminan todos, cerramos
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    Dialog(onDismissRequest = { /* bloquea cierre por toque */ }) {
        val scale = remember(current.id) { Animatable(0.5f) }
        val glow = remember(current.id) { Animatable(0f) }
        val pulse = rememberInfiniteTransition(label = "achPulse")
        val pulseScale by pulse.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                tween(700, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        LaunchedEffect(current.id) {
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
            glow.animateTo(1f, tween(500))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale.value)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(CardBackground, SurfaceDark)))
                .border(
                    width = 2.dp,
                    color = AmberAccent.copy(alpha = glow.value),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = current.emoji,
                    fontSize = 72.sp,
                    modifier = Modifier.scale(pulseScale)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "¡LOGRO DESBLOQUEADO!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = AmberAccent,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = current.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                // +1 Nivel
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⬆️", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "+1 NIVEL",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = SuccessGreen
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // +puntos
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪙", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "+${current.rewardPoints} PUNTOS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                }

                // Si hay varios, indicar cuántos quedan
                if (queue.size > 1) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "+${queue.size - 1} logro(s) más…",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (queue.size > 1) {
                            queue = queue.drop(1)
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (queue.size > 1) "SIGUIENTE" else "¡GENIAL!",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

fun scheduleAlarm(context: Context, alarm: AlarmItem) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("CHALLENGE_TYPE", alarm.challengeType)
        putExtra("DIFFICULTY", alarm.difficulty)
        putExtra("SOUND_URI", alarm.soundUri)
        putExtra("SOUND_NAME", alarm.soundName)
    }
    val pendingIntent = PendingIntent.getBroadcast(context, alarm.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarm.hour); set(Calendar.MINUTE, alarm.minute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        try { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.parse("package:${context.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
        try { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent) } catch (_: Exception) {}
        return
    }
    try {
        val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(info, pendingIntent)
    } catch (e: SecurityException) {
        e.printStackTrace()
        try { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent) } catch (_: Exception) {}
    }
}

fun cancelAlarm(context: Context, alarm: AlarmItem) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, alarm.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    alarmManager.cancel(pendingIntent)
}

@Composable
fun ShopSection(
    points: Int,
    streakManager: StreakManager,
    onBuy: (Int) -> Unit,
    onAchievementUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var hasTheme by remember { mutableStateOf(streakManager.isCyberpunkThemeUnlocked()) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("SUMINISTROS", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AmberAccent, letterSpacing = 2.sp)
                Text("Mejoras y ventajas", fontSize = 12.sp, color = TextMuted)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(CardBackground, SurfaceDark)))
                    .border(1.5.dp, RedAccent, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪙", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$points", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val canAfford = points >= 150
        val borderColor = when {
            hasTheme -> SuccessGreen
            canAfford -> RedAccent
            else -> CardBorder
        }

        Card(
            modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)).border(1.5.dp, borderColor, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(SurfaceDark), contentAlignment = Alignment.Center) {
                        Text("👑", fontSize = 26.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Tema Matrix", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Interfaz oscura verde/cian de bajo consumo.", fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    enabled = !hasTheme && canAfford,
                    onClick = {
                        if (!hasTheme) {
                            streakManager.unlockCyberpunkTheme()
                            hasTheme = true
                            onBuy(150)
                            onAchievementUnlocked()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasTheme) SuccessGreen.copy(alpha = 0.2f) else RedPrimary,
                        disabledContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(if (hasTheme) "EQUIPADO" else "150 🪙", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (hasTheme) SuccessGreen else TextWhite)
                }
            }
        }
    }
}