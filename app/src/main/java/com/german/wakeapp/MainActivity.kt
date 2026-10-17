package com.german.wakeapp

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.german.wakeapp.ui.theme.*
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
        // Capa oscura semitransparente para que el texto sea legible sobre cualquier imagen
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
    sectionTitle: String,
    onInfoClick: () -> Unit,
    onAddPoints: () -> Unit
) {
    val animatedPoints by animateIntAsState(targetValue = points, label = "pts")
    val animatedStreak by animateIntAsState(targetValue = streak, label = "strk")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground.copy(alpha = 0.85f)) // Fondo semitransparente
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
                    sectionTitle,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

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
        }

        Spacer(modifier = Modifier.height(12.dp))

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

    var currentStreak by remember { mutableIntStateOf(streakManager.getStreak()) }
    var points by remember { mutableIntStateOf(streakManager.getPoints()) }
    var alarms by remember { mutableStateOf(alarmRepository.loadAll()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedChallengeType by remember { mutableStateOf("MATH") }
    var selectedDifficulty by remember { mutableStateOf("MEDIUM") }
    var pendingSoundUri by remember { mutableStateOf<String?>(null) }
    var pendingSoundName by remember { mutableStateOf<String?>(null) }
    var currentSection by remember { mutableStateOf(AppSection.ALARMS) }

    LaunchedEffect(Unit) {
        alarms.filter { it.isEnabled }.forEach { scheduleAlarm(context, it) }
    }

    LaunchedEffect(alarms) {
        alarmRepository.saveAll(alarms)
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
                val title = when (currentSection) {
                    AppSection.ALARMS -> "Tus alarmas"
                    AppSection.PROGRESS -> "Tu progreso"
                    AppSection.SHOP -> "Tienda neón"
                    AppSection.READING -> "Modo lectura"
                }
                MainHeader(
                    streak = currentStreak,
                    points = points,
                    sectionTitle = title,
                    onInfoClick = { showInfoDialog = true },
                    onAddPoints = {
                        streakManager.addPoints(10)
                        points = streakManager.getPoints()
                    }
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
                // Animación de transición entre secciones
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
                        AppSection.PROGRESS -> ProgressSection(context = context, streak = currentStreak, points = points)
                        AppSection.SHOP -> ShopSection(
                            points = points,
                            onBuy = { cost ->
                                if (streakManager.spendPoints(cost)) points = streakManager.getPoints()
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

/* ================================================================
 *  SECCIÓN: ALARMAS (CON ANIMACIÓN DE ENTRADA)
 * ================================================================ */

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
                // Animación de entrada para cada tarjeta
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    visible = true
                }
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

    // Glassmorphism: Fondo con transparencia y borde más luminoso
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (alarm.isEnabled) 16.dp else 0.dp, RoundedCornerShape(24.dp), ambientColor = accentColor, spotColor = accentColor)
            .border(
                width = 1.dp,
                color = if (alarm.isEnabled) accentColor.copy(alpha = 0.5f * glow) else CardBorder,
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.75f)), // Transparencia
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

/* ================================================================
 *  RESTO DE SECCIONES (PROGRESO, TIENDA, LECTURA, DIÁLOGOS)
 * ================================================================ */

@Composable
fun ProgressSection(context: Context, streak: Int, points: Int) {
    val achievementManager = remember { AchievementManager(context) }
    val unlockedCount = remember { achievementManager.unlockedCount() }

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
                ProfileStatRow("Racha Activa", "$streak Días 🔥")
                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))
                ProfileStatRow("Puntos de Energía", "$points ⚡")
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
            AchievementRow(ach, unlocked)
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
fun AchievementRow(achievement: Achievement, unlocked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (unlocked) CardBackground.copy(alpha = 0.8f) else SurfaceDark.copy(alpha = 0.5f))
            .border(1.dp, if (unlocked) RedAccent.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(achievement.emoji, fontSize = 24.sp, modifier = Modifier.width(40.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(achievement.title, color = if (unlocked) TextWhite else TextMuted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(achievement.description, color = TextMuted, fontSize = 11.sp)
        }
        Text(if (unlocked) "✓" else "🔒", fontSize = 16.sp)
    }
}

data class ShopItem(val id: String, val name: String, val description: String, val price: Int, val icon: String)

@Composable
fun ShopSection(points: Int, onBuy: (Int) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("WakeAppPrefs", Context.MODE_PRIVATE) }
    var hasShield by remember { mutableStateOf(prefs.getBoolean("item_shield", false)) }

    val shopItems = listOf(
        ShopItem("shield", "Escudo Protector", "Protege tu racha si fallas un despertar.", 100, "🛡️"),
        ShopItem("snooze", "Pausa Temporal", "Otorga 5 minutos extra para preparar la mente.", 150, "⏳"),
        ShopItem("focus", "Elixir de Enfoque", "Facilita la resolución de operaciones complejas.", 75, "🧪"),
        ShopItem("gold_theme", "Tema Aurum", "Desbloquea la interfaz dorada exclusiva.", 300, "👑")
    )

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

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            items(shopItems) { item ->
                val isBought = item.id == "shield" && hasShield
                val canAfford = points >= item.price
                val borderColor = when {
                    isBought -> SuccessGreen
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
                                Text(item.icon, fontSize = 26.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(item.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.description, fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            enabled = !isBought && canAfford,
                            onClick = {
                                if (item.id == "shield") { hasShield = true; prefs.edit().putBoolean("item_shield", true).apply() }
                                onBuy(item.price)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBought) SuccessGreen.copy(alpha = 0.2f) else RedPrimary,
                                disabledContainerColor = SurfaceDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(if (isBought) "EQUIPADO" else "${item.price} 🪙", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (isBought) SuccessGreen else TextWhite)
                        }
                    }
                }
            }
        }
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

// Lógica de alarmas intacta
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