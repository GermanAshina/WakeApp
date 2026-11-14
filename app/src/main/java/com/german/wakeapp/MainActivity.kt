package com.german.wakeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.runtime.snapshotFlow
import android.media.MediaPlayer
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
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

// --- PALETA CLÍNICA ---
val CleanBg = Color(0xFF0A0A0A)
val CleanSurface = Color(0xFF1A1A1A)
val CleanSurfaceVariant = Color(0xFF262626)
val CleanText = Color(0xFFFFFFFF)
val CleanTextDim = Color(0xFF888888)
val WakeBlue = Color(0xFF1D9BF0)
val WakeRed = Color(0xFFFF5252)

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
                Surface(modifier = Modifier.fillMaxSize(), color = CleanBg) {
                    // Lógica para ver si mostrar el onboarding o la app principal
                    val context = LocalContext.current
                    val settingsManager = remember { SettingsManager(context) }
                    var isOnboarded by remember { mutableStateOf(settingsManager.isOnboardingCompleted()) }

                    if (!isOnboarded) {
                        OnboardingScreen(
                            settingsManager = settingsManager,
                            onComplete = { isOnboarded = true }
                        )
                    } else {
                        MainScreen()
                    }
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
    val challengeTypes: List<String> = listOf("MATH"),
    val difficulty: String = "MEDIUM",
    val rounds: Int = 1,
    val xpBoost: Boolean = false,
    val soundUri: String? = null,
    val soundName: String? = null,
    val colorHex: String = "DEFAULT",
    val label: String? = null
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

fun challengeAccent(type: String): Color = CleanText
fun challengeEmoji(type: String): String = ChallengeCatalog.byId(type).emoji
fun challengeTitle(type: String): String = ChallengeCatalog.label(type)

fun getAlarmColor(colorHex: String, defaultAccent: Color): Color {
    return when (colorHex) {
        "RED" -> WakeRed
        else -> WakeBlue
    }
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
    SETTINGS("Ajustes", "⚙️")
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
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)))
    }
}

@Composable
fun MainHeader(
    level: Int,
    xp: Int,
    readingMode: Boolean,
    userName: String,
    onToggleReading: () -> Unit,
    onInfoClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onStreakClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val xpNeeded = StreakManager.xpForLevel(level)
    val xpProgress = (xp.toFloat() / xpNeeded).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CleanBg.copy(alpha = 0.85f))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // --- ZONA SUPERIOR ---
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // IZQUIERDA: Saludo con el nombre
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Hola,",
                    fontSize = 14.sp,
                    color = CleanTextDim,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = userName.ifBlank { "Madrugador" },
                    fontSize = 20.sp,
                    color = CleanText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // CENTRO: Logo y WakeApp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_icon_info),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "WakeApp",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = CleanText
                )
            }

            // DERECHA: Calendario y Luna (Apilados)
            Column(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Calendario (Arriba)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CleanSurface)
                        .clickable { onStreakClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        val w = size.width; val h = size.height; val s = 1.5.dp.toPx()
                        drawRoundRect(color = CleanText, topLeft = Offset(w*0.1f, h*0.2f), size = Size(w*0.8f, h*0.75f), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(width = s))
                        drawLine(color = CleanText, start = Offset(w*0.1f, h*0.4f), end = Offset(w*0.9f, h*0.4f), strokeWidth = s)
                        drawLine(color = CleanText, start = Offset(w*0.3f, h*0.1f), end = Offset(w*0.3f, h*0.3f), strokeWidth = s, cap = StrokeCap.Round)
                        drawLine(color = CleanText, start = Offset(w*0.7f, h*0.1f), end = Offset(w*0.7f, h*0.3f), strokeWidth = s, cap = StrokeCap.Round)
                    }
                }

                // Modo Lectura (Abajo)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (readingMode) WakeBlue else CleanSurface)
                        .clickable { onToggleReading() },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val w = size.width; val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.6f, h * 0.1f)
                            cubicTo(w * 0.2f, h * 0.2f, w * 0.2f, h * 0.8f, w * 0.6f, h * 0.9f)
                            cubicTo(w * 0.3f, h * 0.7f, w * 0.3f, h * 0.3f, w * 0.6f, h * 0.1f)
                        }
                        drawPath(path, color = CleanText, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- BARRA DE NIVEL Y RECOMPENSAS ---
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Nivel $level", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(12.dp))
            LinearProgressIndicator(
                progress = { xpProgress },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CleanText,
                trackColor = CleanSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.clickable { onRewardsClick() }) {
                Text("Recompensas", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val streakManager = remember { StreakManager(context) }
    val alarmRepository = remember { AlarmRepository(context) }
    val achievementManager = remember { AchievementManager(context) }
    val readingModeManager = remember { ReadingModeManager(context) }
    val settingsManager = remember { SettingsManager(context) }

    var currentStreak by remember { mutableIntStateOf(streakManager.getStreak()) }
    var points by remember { mutableIntStateOf(streakManager.getPoints()) }
    var currentLevel by remember { mutableIntStateOf(streakManager.getLevel()) }
    var currentXp by remember { mutableIntStateOf(streakManager.getXp()) }
    var alarms by remember { mutableStateOf(alarmRepository.loadAll()) }

    // Obtener el nombre de usuario
    val userName = remember { settingsManager.getUserName() }

    var showTimePicker by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showRewardsDialog by remember { mutableStateOf(false) }
    var showStreakCalendar by remember { mutableStateOf(false) }
    var showAlarmLimitDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    var selectedChallengeType by remember { mutableStateOf("MATH") }
    var selectedDifficulty by remember { mutableStateOf(settingsManager.getDefaultDifficulty()) } // NUEVO: Dificultad por defecto
    var selectedRounds by remember { mutableIntStateOf(1) }
    var selectedColor by remember { mutableStateOf("DEFAULT") }
    var pendingSoundUri by remember { mutableStateOf<String?>(null) }
    var pendingSoundName by remember { mutableStateOf<String?>(null) }

    var currentSection by remember { mutableStateOf(AppSection.ALARMS) }
    var rewardDialog by remember { mutableStateOf<RewardInfo?>(null) }
    var readingMode by remember { mutableStateOf(readingModeManager.isEnabled()) }

    val activity = context as? android.app.Activity
    LaunchedEffect(readingMode, activity) {
        activity?.window?.let { readingModeManager.applyToWindow(it, readingMode) }
    }

    LaunchedEffect(Unit) {
        alarms.filter { it.isEnabled }.forEach { scheduleAlarm(context, it) }
    }

    LaunchedEffect(alarms) {
        alarmRepository.saveAll(alarms)
    }

    LaunchedEffect(Unit) {
        AlarmStateHolder.restoreIfNeeded(context)
        if (AlarmStateHolder.isRinging) {
            val intent = Intent(context, ChallengeActivity::class.java).apply {
                putStringArrayListExtra("CHALLENGE_TYPES", ArrayList(AlarmStateHolder.lastChallengeTypes))
                putExtra("CHALLENGE_TYPE", AlarmStateHolder.lastChallengeTypes.firstOrNull() ?: "MATH")
                putExtra("DIFFICULTY", AlarmStateHolder.lastDifficulty)
                putExtra("ROUNDS", AlarmStateHolder.lastRounds)
                putExtra("XP_BOOST", AlarmStateHolder.lastXpBoost)
                putExtra("FROM_SERVICE", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                AlarmStateHolder.restoreIfNeeded(context)
                if (AlarmStateHolder.isRinging) {
                    val intent = Intent(context, ChallengeActivity::class.java).apply {
                        putStringArrayListExtra("CHALLENGE_TYPES", ArrayList(AlarmStateHolder.lastChallengeTypes))
                        putExtra("CHALLENGE_TYPE", AlarmStateHolder.lastChallengeTypes.firstOrNull() ?: "MATH")
                        putExtra("DIFFICULTY", AlarmStateHolder.lastDifficulty)
                        putExtra("ROUNDS", AlarmStateHolder.lastRounds)
                        putExtra("XP_BOOST", AlarmStateHolder.lastXpBoost)
                        putExtra("FROM_SERVICE", true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    context.startActivity(intent)
                }

                currentStreak = streakManager.getStreak()
                points = streakManager.getPoints()
                currentLevel = streakManager.getLevel()
                currentXp = streakManager.getXp()
                readingMode = readingModeManager.isEnabled()

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
                        xpGained = data.xp, levelUp = data.levelUp, oldLevel = data.oldLevel, newLevel = data.newLevel,
                        achievements = data.achievements, bonusLevels = data.bonusLevels, bonusPoints = data.bonusPoints
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

    Box(modifier = Modifier
        .fillMaxSize()
        .background(CleanBg)) {
        if (currentSection != AppSection.SETTINGS) {
            AnimatedBackground()
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MainHeader(
                    level = currentLevel,
                    xp = currentXp,
                    readingMode = readingMode,
                    userName = userName,
                    onToggleReading = {
                        readingMode = !readingMode
                        readingModeManager.setEnabled(readingMode)
                    },
                    onInfoClick = { showInfoDialog = true },
                    onRewardsClick = { showRewardsDialog = true },
                    onStreakClick = { showStreakCalendar = true },
                    onProfileClick = { showProfileDialog = true }
                )
            },
            bottomBar = { WakeBottomBar(current = currentSection, onSelect = { currentSection = it }) },
            floatingActionButton = {
                if (currentSection == AppSection.ALARMS) {
                    FloatingActionButton(
                        onClick = {
                            if (alarms.size >= 4 && !streakManager.hasInfiniteAlarms()) {
                                showAlarmLimitDialog = true
                            } else {
                                pendingSoundUri = null
                                pendingSoundName = null
                                selectedRounds = 1
                                selectedChallengeType = "MATH"
                                selectedColor = "DEFAULT"
                                // NUEVO: Aplicar la dificultad por defecto guardada en el onboarding
                                selectedDifficulty = settingsManager.getDefaultDifficulty()
                                showTimePicker = true
                            }
                        },
                        containerColor = WakeBlue,
                        contentColor = CleanText,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Text("+", fontSize = 32.sp, fontWeight = FontWeight.Light)
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Start
        ) { paddingValues ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
                AnimatedContent(
                    targetState = currentSection,
                    transitionSpec = { fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200)) },
                    label = "section_transition"
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
                            },
                            onEdit = { editingAlarm = it }
                        )
                        AppSection.PROGRESS -> ProgressSection(
                            context = context, streak = currentStreak, points = points,
                            level = currentLevel, xp = currentXp
                        )
                        AppSection.SHOP -> ShopSection(
                            points = points,
                            streakManager = streakManager,
                            onBuy = { cost -> if (streakManager.spendPoints(cost)) { points = streakManager.getPoints() } }
                        )
                        AppSection.SETTINGS -> SettingsSection(
                            streakManager = streakManager,
                            settingsManager = settingsManager
                        )
                    }
                }

                if (showTimePicker) {
                    CustomTimePickerDialog(
                        // NUEVO: Pasar la hora habitual guardada en el onboarding
                        initialHour = settingsManager.getHabitualWakeHour(),
                        initialMinute = settingsManager.getHabitualWakeMinute(),
                        onDismiss = { showTimePicker = false },
                        onConfirm = { hour, minute, label ->
                            val finalTypes = List(selectedRounds) { selectedChallengeType }
                            val newAlarm = AlarmItem(
                                hour = hour, minute = minute, isEnabled = true,
                                challengeTypes = finalTypes,
                                difficulty = selectedDifficulty,
                                rounds = finalTypes.size,
                                soundUri = pendingSoundUri, soundName = pendingSoundName,
                                colorHex = selectedColor,
                                label = label
                            )
                            alarms = (alarms + newAlarm).sortedBy { it.hour * 60 + it.minute }
                            scheduleAlarm(context, newAlarm)
                            showTimePicker = false; pendingSoundUri = null; pendingSoundName = null
                        },
                        selectedChallengeType = selectedChallengeType,
                        onChallengeTypeSelected = { selectedChallengeType = it },
                        selectedDifficulty = selectedDifficulty, onDifficultySelected = { selectedDifficulty = it },
                        selectedRounds = selectedRounds, onRoundsChange = { selectedRounds = it },
                        soundName = pendingSoundName,
                        onPickSound = { soundPickerLauncher.launch(arrayOf("audio/*")) },
                        onClearSound = { pendingSoundUri = null; pendingSoundName = null },
                        selectedColor = selectedColor, onColorSelected = { selectedColor = it },
                        streakManager = streakManager
                    )
                }

                editingAlarm?.let { alarmToEdit ->
                    EditAlarmDialog(
                        alarm = alarmToEdit,
                        onDismiss = { editingAlarm = null },
                        onSave = { updatedAlarm ->
                            alarms = alarms.map { if (it.id == updatedAlarm.id) updatedAlarm else it }.sortedBy { it.hour * 60 + it.minute }
                            cancelAlarm(context, alarmToEdit)
                            if (updatedAlarm.isEnabled) scheduleAlarm(context, updatedAlarm)
                            editingAlarm = null
                        },
                        onDelete = {
                            cancelAlarm(context, alarmToEdit)
                            alarms = alarms.filter { it.id != alarmToEdit.id }
                            editingAlarm = null
                        },
                        streakManager = streakManager
                    )
                }

                if (showInfoDialog) InfoDialog(onDismiss = { showInfoDialog = false })
                if (showRewardsDialog) LevelRewardsDialog(onDismiss = { showRewardsDialog = false })

                if (showStreakCalendar) {
                    StreakCalendarDialog(streak = currentStreak, onDismiss = { showStreakCalendar = false })
                }

                if (showAlarmLimitDialog) {
                    AlertDialog(
                        onDismissRequest = { showAlarmLimitDialog = false },
                        containerColor = CleanSurface,
                        title = { Text("Límite alcanzado", color = CleanText, fontWeight = FontWeight.Bold) },
                        text = { Text("Has alcanzado el límite de 4 alarmas. Ve a la Tienda y compra 'Alarmas Infinitas' para crear más.", color = CleanTextDim) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showAlarmLimitDialog = false
                                    currentSection = AppSection.SHOP
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WakeBlue)
                            ) {
                                Text("Ir a la Tienda", color = CleanText, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAlarmLimitDialog = false }) {
                                Text("Cancelar", color = CleanTextDim)
                            }
                        }
                    )
                }

                if (showProfileDialog) {
                    AlertDialog(
                        onDismissRequest = { showProfileDialog = false },
                        containerColor = CleanSurface,
                        title = { Text("Perfil", color = CleanText, fontWeight = FontWeight.Bold) },
                        text = { Text("Próximamente...", color = CleanTextDim, fontSize = 16.sp) },
                        confirmButton = {
                            TextButton(onClick = { showProfileDialog = false }) {
                                Text("Entendido", color = WakeBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                rewardDialog?.let { info ->
                    if (info.achievements.isNotEmpty()) {
                        AchievementUnlockedOverlay(
                            info = info,
                            onDismiss = {
                                currentLevel = streakManager.getLevel(); currentXp = streakManager.getXp()
                                points = streakManager.getPoints(); currentStreak = streakManager.getStreak()
                                rewardDialog = null
                            }
                        )
                    } else {
                        XpRewardDialog(info = info, onDismiss = { rewardDialog = null })
                    }
                }

                if (readingMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x33FF6600),
                                        Color(0x22FF8800)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun EditAlarmDialog(
    alarm: AlarmItem,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit,
    onDelete: () -> Unit,
    streakManager: StreakManager
) {
    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var color by remember { mutableStateOf(alarm.colorHex) }
    var rounds by remember { mutableIntStateOf(alarm.rounds) }
    var label by remember { mutableStateOf(alarm.label ?: "") }
    var selectedType by remember { mutableStateOf(alarm.challengeTypes.firstOrNull() ?: "MATH") }

    val maxRounds = if (streakManager.hasExtraRounds()) 10 else 4

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Editar Alarma", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CleanText)
                    Box(modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CleanSurfaceVariant)
                        .clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                        Text("✕", color = CleanTextDim, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    NumberPicker(value = hour, range = 0..23, onValueChange = { hour = it })
                    Text(":", fontSize = 32.sp, fontWeight = FontWeight.Light, color = CleanText, modifier = Modifier.padding(horizontal = 8.dp))
                    NumberPicker(value = minute, range = 0..59, onValueChange = { minute = it })
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("¿Para qué es esta alarma?") },
                    placeholder = { Text("Trabajo, Instituto, Gym...", color = CleanTextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WakeBlue,
                        unfocusedBorderColor = CleanSurfaceVariant,
                        focusedTextColor = CleanText,
                        unfocusedTextColor = CleanText,
                        focusedLabelColor = WakeBlue,
                        unfocusedLabelColor = CleanTextDim,
                        cursorColor = WakeBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Minijuego", fontSize = 14.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ChallengeCatalog.all) { type ->
                        val isSelected = selectedType == type.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = type.id },
                            label = { Text("${type.emoji} ${type.label}", fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WakeBlue, selectedLabelColor = CleanText, containerColor = CleanSurfaceVariant, labelColor = CleanText),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Rondas", fontSize = 14.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items((1..maxRounds).toList()) { r ->
                        FilterChip(
                            selected = rounds == r,
                            onClick = { rounds = r },
                            label = { Text("$r", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WakeBlue,
                                selectedLabelColor = CleanText,
                                containerColor = CleanSurfaceVariant,
                                labelColor = CleanText
                            ),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Color", fontSize = 14.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    val colors = listOf("DEFAULT" to WakeBlue, "RED" to WakeRed)
                    items(colors) { (hex, c) ->
                        val isSelected = color == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(if (isSelected) 3.dp else 0.dp, CleanText, CircleShape)
                                .clickable { color = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val finalTypes = List(rounds) { selectedType }
                        onSave(alarm.copy(
                            hour = hour, minute = minute, colorHex = color,
                            challengeTypes = finalTypes, rounds = finalTypes.size,
                            label = label.trim().ifBlank { null }
                        ))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WakeBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Guardar Cambios", color = CleanText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { onDelete() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Eliminar Alarma", color = WakeRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun LevelRewardsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CleanSurface,
        title = { Text("Recompensas de Nivel", color = CleanText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Sube de nivel completando desafíos para ganar recompensas directas. Las monedas (🪙) solo se obtienen al desbloquear Logros:", fontSize = 14.sp, color = CleanTextDim)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CleanSurfaceVariant)
                    .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Puntos de Energía", color = CleanText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Gana +50 🪙 por cada nivel alcanzado.", color = CleanTextDim, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido", color = WakeBlue, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun XpRewardDialog(info: RewardInfo, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { /* bloquea cierre */ }) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CleanSurface)
            .padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = if (info.levelUp) "🎉" else "⭐", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = if (info.levelUp) "¡NIVEL SUBIDO!" else "¡COMPLETADO!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CleanText, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text("+${info.xpGained} XP", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = WakeBlue)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onDismiss, modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = WakeBlue), shape = RoundedCornerShape(16.dp)) {
                    Text("Continuar", color = CleanText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun WakeBottomBar(current: AppSection, onSelect: (AppSection) -> Unit) {
    Surface(color = CleanBg.copy(alpha = 0.98f), tonalElevation = 0.dp, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppSection.values().forEach { section ->
                val selected = section == current
                val color = if (selected) CleanText else CleanTextDim
                val stroke = if (selected) 2.5.dp else 1.5.dp

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(section) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val w = size.width
                        val h = size.height
                        val s = stroke.toPx()

                        when (section) {
                            AppSection.ALARMS -> {
                                drawCircle(color = color, radius = w / 2.5f, style = Stroke(width = s))
                                drawLine(color = color, start = Offset(w/2, h/2 - w/5), end = Offset(w/2, h/2), strokeWidth = s, cap = StrokeCap.Round)
                                drawLine(color = color, start = Offset(w/2, h/2), end = Offset(w/2 + w/5, h/2), strokeWidth = s, cap = StrokeCap.Round)
                                drawLine(color = color, start = Offset(w*0.2f, h*0.15f), end = Offset(w*0.35f, h*0.25f), strokeWidth = s, cap = StrokeCap.Round)
                                drawLine(color = color, start = Offset(w*0.8f, h*0.15f), end = Offset(w*0.65f, h*0.25f), strokeWidth = s, cap = StrokeCap.Round)
                            }
                            AppSection.PROGRESS -> {
                                drawLine(color = color, start = Offset(w*0.2f, h*0.9f), end = Offset(w*0.2f, h*0.5f), strokeWidth = s, cap = StrokeCap.Round)
                                drawLine(color = color, start = Offset(w*0.5f, h*0.9f), end = Offset(w*0.5f, h*0.2f), strokeWidth = s, cap = StrokeCap.Round)
                                drawLine(color = color, start = Offset(w*0.8f, h*0.9f), end = Offset(w*0.8f, h*0.6f), strokeWidth = s, cap = StrokeCap.Round)
                            }
                            AppSection.SHOP -> {
                                drawRoundRect(color = color, topLeft = Offset(w*0.15f, h*0.35f), size = Size(w*0.7f, h*0.5f), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(width = s))
                                drawArc(color = color, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w*0.3f, h*0.1f), size = Size(w*0.4f, h*0.4f), style = Stroke(width = s, cap = StrokeCap.Round))
                            }
                            AppSection.SETTINGS -> {
                                val center = Offset(w/2, h/2)
                                val rOuter = w * 0.45f
                                val rInner = w * 0.25f
                                val teeth = 8
                                val path = Path()
                                for (i in 0 until teeth * 2) {
                                    val r = if (i % 2 == 0) rOuter else rOuter * 0.75f
                                    val angle = (i * Math.PI / teeth).toFloat() - (Math.PI / 2).toFloat()
                                    val x = center.x + r * kotlin.math.cos(angle)
                                    val y = center.y + r * kotlin.math.sin(angle)
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                path.close()
                                drawPath(path, color = color, style = Stroke(width = s, join = StrokeJoin.Round))
                                drawCircle(color = color, radius = rInner, center = center, style = Stroke(width = s))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (selected) {
                        Text(section.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmsSection(
    alarms: List<AlarmItem>,
    onToggle: (AlarmItem, Boolean) -> Unit,
    onDelete: (AlarmItem) -> Unit,
    onEdit: (AlarmItem) -> Unit
) {
    if (alarms.isEmpty()) {
        EmptyState()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onToggle = { enabled -> onToggle(alarm, enabled) },
                    onDelete = { onDelete(alarm) },
                    onClick = { onEdit(alarm) }
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⏰", fontSize = 80.sp, color = CleanSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Text("No hay alarmas próximas", color = CleanText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Pulsa el botón + para crear una", color = CleanTextDim, fontSize = 16.sp)
    }
}

@Composable
fun AlarmCard(alarm: AlarmItem, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val accentColor = getAlarmColor(alarm.colorHex, WakeBlue)
    val timeFormatted = String.format("%02d:%02d", alarm.hour, alarm.minute)

    val typeLabel = remember(alarm.challengeTypes) {
        challengeTitle(alarm.challengeTypes.firstOrNull() ?: "MATH")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CleanSurface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)) {
            Box(modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(accentColor))

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timeFormatted,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Light,
                        color = if (alarm.isEnabled) CleanText else CleanTextDim,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$typeLabel · ${alarm.difficulty}",
                            fontSize = 13.sp,
                            color = CleanTextDim,
                            fontWeight = FontWeight.Medium
                        )
                        if (alarm.rounds > 1) {
                            Text(" · ${alarm.rounds} rondas", fontSize = 13.sp, color = CleanTextDim)
                        }
                    }

                    if (!alarm.label.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🏷 ${alarm.label}", fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    if (!alarm.soundName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🎵 ${alarm.soundName}", fontSize = 12.sp, color = CleanTextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CleanText,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = CleanTextDim,
                            uncheckedTrackColor = CleanSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onDelete() }, contentAlignment = Alignment.Center) {
                        Text("🗑", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressSection(context: Context, streak: Int, points: Int, level: Int, xp: Int) {
    val achievementManager = remember { AchievementManager(context) }
    val unlockedCount = achievementManager.unlockedCount()
    val xpNeeded = StreakManager.xpForLevel(level)

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp)) {
        Text("Tu Progreso", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CleanText)
        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CleanSurface), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                ProfileStatRow("Nivel", "$level")
                HorizontalDivider(color = CleanSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                ProfileStatRow("XP", "$xp / $xpNeeded")
                HorizontalDivider(color = CleanSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                ProfileStatRow("Días Distintos", "$streak días")
                HorizontalDivider(color = CleanSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                ProfileStatRow("Puntos de Energía", "$points")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("LOGROS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CleanTextDim, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        AchievementCatalog.all.forEach { ach ->
            val unlocked = achievementManager.isUnlocked(ach.id)
            val progress = achievementManager.getProgress(ach, streak, points)
            AchievementRow(ach, unlocked, progress)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ProfileStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = CleanTextDim, fontSize = 16.sp)
        Text(value, color = CleanText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun AchievementRow(achievement: Achievement, unlocked: Boolean, progress: Int) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(CleanSurface)
        .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(achievement.emoji, fontSize = 24.sp, modifier = Modifier.width(40.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(achievement.title, color = if (unlocked) CleanText else CleanTextDim, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(achievement.description, color = CleanTextDim, fontSize = 13.sp)
        }
        Text(if (unlocked) "✓" else "$progress/${achievement.required}", color = if (unlocked) WakeBlue else CleanTextDim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CleanSurface,
        title = { Text("¿Qué es WakeApp?", color = CleanText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("WakeApp es tu despertador inteligente. En lugar de un simple botón, cada alarma te lanza desafíos mentales o físicos que debes superar para desactivarla.", fontSize = 15.sp, color = CleanTextDim, lineHeight = 22.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido", color = WakeBlue, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun CustomTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit, onConfirm: (Int, Int, String?) -> Unit,
    selectedChallengeType: String, onChallengeTypeSelected: (String) -> Unit,
    selectedDifficulty: String, onDifficultySelected: (String) -> Unit,
    selectedRounds: Int, onRoundsChange: (Int) -> Unit,
    soundName: String?, onPickSound: () -> Unit, onClearSound: () -> Unit,
    selectedColor: String, onColorSelected: (String) -> Unit,
    streakManager: StreakManager
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    var alarmLabel by remember { mutableStateOf("") }
    val maxRounds = if (streakManager.hasExtraRounds()) 10 else 4

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CleanSurface,
        title = { Text("Nueva Alarma", color = CleanText, fontWeight = FontWeight.Bold, fontSize = 22.sp) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.verticalScroll(rememberScrollState())) {

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    NumberPicker(value = selectedHour, range = 0..23, onValueChange = { selectedHour = it })
                    Text(":", fontSize = 32.sp, fontWeight = FontWeight.Light, color = CleanText, modifier = Modifier.padding(horizontal = 8.dp))
                    NumberPicker(value = selectedMinute, range = 0..59, step = 1, onValueChange = { selectedMinute = it })
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = alarmLabel,
                    onValueChange = { alarmLabel = it },
                    label = { Text("¿Para qué es esta alarma?") },
                    placeholder = { Text("Trabajo, Instituto, Gym...", color = CleanTextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WakeBlue,
                        unfocusedBorderColor = CleanSurfaceVariant,
                        focusedTextColor = CleanText,
                        unfocusedTextColor = CleanText,
                        focusedLabelColor = WakeBlue,
                        unfocusedLabelColor = CleanTextDim,
                        cursorColor = WakeBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Color de la Alarma", fontSize = 14.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val colors = listOf("DEFAULT" to WakeBlue, "RED" to WakeRed)
                    items(colors) { (hex, color) ->
                        val isSelected = selectedColor == hex
                        Box(modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                if (isSelected) 4.dp else 0.dp,
                                if (isSelected) CleanText else Color.Transparent,
                                CircleShape
                            )
                            .clickable { onColorSelected(hex) })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Minijuego", fontSize = 14.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ChallengeCatalog.all) { type ->
                        val isSelected = selectedChallengeType == type.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onChallengeTypeSelected(type.id) },
                            label = { Text("${type.emoji} ${type.label}", fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WakeBlue, selectedLabelColor = CleanText, containerColor = CleanSurfaceVariant, labelColor = CleanText),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Rondas", fontSize = 14.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..maxRounds).toList()) { r ->
                        FilterChip(
                            selected = selectedRounds == r,
                            onClick = { onRoundsChange(r) },
                            label = { Text("$r", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WakeBlue, selectedLabelColor = CleanText, containerColor = CleanSurfaceVariant, labelColor = CleanText),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Dificultad", fontSize = 14.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("EASY", "MEDIUM", "HARD", "EXTREME")) { diff ->
                        FilterChip(
                            selected = selectedDifficulty == diff, onClick = { onDifficultySelected(diff) },
                            label = { Text(diff, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WakeBlue, selectedLabelColor = CleanText, containerColor = CleanSurfaceVariant, labelColor = CleanText),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Sonido", fontSize = 14.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CleanSurfaceVariant)
                    .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (soundName != null) "🎵" else "🔔", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(soundName ?: "Predeterminado", fontSize = 15.sp, color = CleanText, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (soundName != null) {
                        Box(modifier = Modifier
                            .size(24.dp)
                            .clickable { onClearSound() }, contentAlignment = Alignment.Center) { Text("✕", color = CleanTextDim, fontSize = 14.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onPickSound, modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanSurfaceVariant), shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Elegir archivo de audio", color = CleanText, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedHour, selectedMinute, alarmLabel.trim().ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = WakeBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar Alarma", fontWeight = FontWeight.Bold, color = CleanText)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = CleanTextDim) } }
    )
}

@Composable
fun NumberPicker(value: Int, range: IntRange, step: Int = 1, onValueChange: (Int) -> Unit) {
    val items = remember(range, step) { range.filter { it % step == 0 } }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = items.indexOf(value).coerceAtLeast(0))
    val itemHeight = 48.dp
    val visibleItems = 3
    val boxHeight = itemHeight * visibleItems
    val verticalPadding = (boxHeight - itemHeight) / 2

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { info -> kotlin.math.abs((info.offset + info.size / 2) - viewportCenter) }?.index
        }.collect { centeredIndex ->
            if (centeredIndex != null && centeredIndex in items.indices) {
                val newValue = items[centeredIndex]
                if (newValue != value) onValueChange(newValue)
            }
        }
    }

    Box(modifier = Modifier
        .height(boxHeight)
        .width(80.dp), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = listState, horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = verticalPadding),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(items) { item ->
                val isSelected = item == value
                Box(modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = String.format("%02d", item),
                        fontSize = if (isSelected) 32.sp else 20.sp,
                        fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Light,
                        color = if (isSelected) CleanText else CleanTextDim,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementUnlockedOverlay(info: RewardInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
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
    var queue by remember { mutableStateOf(info.achievements.toList()) }
    val current = queue.firstOrNull()
    if (current == null) { LaunchedEffect(Unit) { onDismiss() }; return }
    Dialog(onDismissRequest = { /* bloquea cierre */ }) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CleanSurface)
            .padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = current.emoji, fontSize = 72.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "¡LOGRO DESBLOQUEADO!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CleanTextDim, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = current.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CleanText, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Text("+1 NIVEL", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WakeBlue) }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Text("+${current.rewardPoints} PUNTOS", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = CleanTextDim) }

                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { if (queue.size > 1) { queue = queue.drop(1) } else { onDismiss() } }, modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = WakeBlue), shape = RoundedCornerShape(16.dp)) {
                    Text(if (queue.size > 1) "Siguiente" else "Genial", color = CleanText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

fun scheduleAlarm(context: Context, alarm: AlarmItem) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val types = if (alarm.challengeTypes.isNotEmpty()) alarm.challengeTypes
    else listOf("MATH")
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putStringArrayListExtra("CHALLENGE_TYPES", ArrayList(types))
        putExtra("CHALLENGE_TYPE", types.firstOrNull() ?: "MATH")
        putExtra("DIFFICULTY", alarm.difficulty)
        putExtra("ROUNDS", alarm.rounds)
        putExtra("XP_BOOST", alarm.xpBoost)
        putExtra("SOUND_URI", alarm.soundUri)
        putExtra("SOUND_NAME", alarm.soundName)
        putExtra("ALARM_LABEL", alarm.label)
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
fun ShopSection(points: Int, streakManager: StreakManager, onBuy: (Int) -> Unit) {
    var hasInfinite by remember { mutableStateOf(streakManager.hasInfiniteAlarms()) }
    var hasColors by remember { mutableStateOf(streakManager.hasExtraColors()) }
    var hasRounds by remember { mutableStateOf(streakManager.hasExtraRounds()) }
    var hasNoAds by remember { mutableStateOf(streakManager.isNoAdsUnlocked()) }
    var hasAvatar by remember { mutableStateOf(streakManager.isAvatarUnlocked()) }
    var hasXpMultiplier by remember { mutableStateOf(streakManager.isXpMultiplierUnlocked()) }

    val hasComplete = hasInfinite && hasColors && hasRounds && hasNoAds && hasAvatar && hasXpMultiplier

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp)) {
        Text("Tienda", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CleanText)
        Text("Mejoras permanentes", fontSize = 14.sp, color = CleanTextDim)
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CleanSurface)
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🪙", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("$points Puntos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CleanText)
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (!hasComplete) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        2.dp,
                        Brush.linearGradient(listOf(WakeBlue, Color(0xFF00E5FF))),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier
                            .background(WakeBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("OFERTA", color = WakeBlue, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pack Completo", color = CleanText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Desbloquea todas las mejoras de la tienda de una vez. Ahorra tiempo y puntos.", color = CleanTextDim, fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (points >= 100) {
                                streakManager.unlockCompletePack()
                                hasInfinite = true; hasColors = true; hasRounds = true
                                hasNoAds = true; hasAvatar = true; hasXpMultiplier = true
                                onBuy(100)
                            }
                        },
                        enabled = points >= 100,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WakeBlue, disabledContainerColor = CleanSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Comprar Todo · 100 🪙", color = CleanText, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        Text("MEJORAS INDIVIDUALES", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CleanTextDim, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        ShopItemRow("Alarmas Infinitas", "Crea todas las alarmas que quieras sin límite.", 1500, hasInfinite, points >= 1500) {
            if (!hasInfinite) { streakManager.unlockInfiniteAlarms(); hasInfinite = true; onBuy(1500) }
        }

        ShopItemRow("Nuevos Marcadores", "Añade más colores para organizar tus alarmas.", 1200, hasColors, points >= 1200) {
            if (!hasColors) { streakManager.unlockExtraColors(); hasColors = true; onBuy(1200) }
        }

        ShopItemRow("Más Asaltos", "Aumenta el límite de rondas por alarma a 10.", 1500, hasRounds, points >= 1500) {
            if (!hasRounds) { streakManager.unlockExtraRounds(); hasRounds = true; onBuy(1500) }
        }

        ShopItemRow("Elimina todos los anuncios", "Una experiencia totalmente limpia y sin interrupciones.", 2000, hasNoAds, points >= 2000) {
            if (!hasNoAds) { streakManager.unlockNoAds(); hasNoAds = true; onBuy(2000) }
        }

        ShopItemRow("Desbloquea tu Avatar", "Muestra una foto de perfil personalizada en la app.", 2500, hasAvatar, points >= 2500) {
            if (!hasAvatar) { streakManager.unlockAvatar(); hasAvatar = true; onBuy(2500) }
        }

        ShopItemRow("Multiplicador de XP", "Gana el doble de experiencia al despertar.", 3000, hasXpMultiplier, points >= 3000) {
            if (!hasXpMultiplier) { streakManager.unlockXpMultiplier(); hasXpMultiplier = true; onBuy(3000) }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ShopItemRow(title: String, desc: String, price: Int, isOwned: Boolean, canAfford: Boolean, onBuy: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(CleanSurface)
        .padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = CleanText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(desc, color = CleanTextDim, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("$price 🪙", color = if (canAfford || isOwned) WakeBlue else CleanTextDim, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = onBuy, enabled = !isOwned && canAfford,
            colors = ButtonDefaults.buttonColors(containerColor = WakeBlue, disabledContainerColor = CleanSurfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isOwned) "Equipado" else "Comprar", color = if (isOwned) CleanTextDim else CleanText, fontWeight = FontWeight.Bold)
        }
    }
}