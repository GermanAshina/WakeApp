// MainActivity.kt
package com.german.wakeapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.german.wakeapp.ui.theme.*
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

const val ADMOB_REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917"
const val ADMOB_REWARDED_MONEDAS_TIENDA_REAL_ID = "ca-app-pub-1219684092719163/5455336103"

object AppPalette {
    var isLight by mutableStateOf(false)
    var accent by mutableStateOf(ThemeCatalog.DEFAULT_ACCENT)
    var isDefaultTheme by mutableStateOf(true)
    fun applyTheme(theme: AppTheme) {
        accent = theme.primary
        isDefaultTheme = theme.id == ThemeCatalog.DEFAULT_ID
    }
}

private fun themed(base: Color, fraction: Float): Color =
    if (AppPalette.isLight || AppPalette.isDefaultTheme) base else lerp(base, AppPalette.accent, fraction)

private val DarkBaseBg            = Color(0xFF0B1220)
private val DarkBaseSurface       = Color(0xFF141C2E)
private val DarkBaseSurfaceVariant = Color(0xFF1E2A42)

val CleanBg: Color get() = if (AppPalette.isLight) Color(0xFFFFFFFF) else themed(DarkBaseBg, 0.06f)
val CleanSurface: Color get() = if (AppPalette.isLight) Color(0xFFF2F2F2) else themed(DarkBaseSurface, 0.10f)
val CleanSurfaceVariant: Color get() = if (AppPalette.isLight) Color(0xFFE2E2E2) else themed(DarkBaseSurfaceVariant, 0.14f)
val CleanText: Color get() = if (AppPalette.isLight) Color(0xFF111111) else Color(0xFFFFFFFF)
val CleanTextDim: Color get() = if (AppPalette.isLight) Color(0xFF6B6B6B) else Color(0xFF9AA6BC)
val WakeBlue: Color get() = AppPalette.accent
val WakeRed = Color(0xFFFF5252)
val OnBgText = Color(0xFFFFFFFF)

fun wakeAppGradient(): Brush = Brush.verticalGradient(
    listOf(CleanBg, WakeBlue.copy(alpha = 0.10f), Color(0xFF08080A))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.parse("package:$packageName") }
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
            val settingsManager = remember { SettingsManager(this@MainActivity) }
            val profileManager = remember { ProfileManager(this@MainActivity) }
            remember {
                AppPalette.isLight = settingsManager.isLightMode()
                AppPalette.applyTheme(ThemeManager(this@MainActivity).getActive())
                AppLang.language = settingsManager.getLanguage()
                if (profileManager.getName().isBlank()) {
                    val fallbackName = settingsManager.getUserName().ifBlank {
                        getSharedPreferences("wakeapp_profile_prefs", Context.MODE_PRIVATE).getString("user_name", "") ?: ""
                    }
                    if (fallbackName.isNotBlank()) profileManager.setName(fallbackName.trim())
                }
                true
            }

            WakeAppTheme(darkTheme = !AppPalette.isLight, accent = if (AppPalette.isDefaultTheme) null else AppPalette.accent) {
                Surface(modifier = Modifier.fillMaxSize(), color = CleanBg) {
                    var isOnboarded by remember { mutableStateOf(settingsManager.isOnboardingCompleted()) }

                    if (!isOnboarded) {
                        OnboardingScreen(
                            settingsManager = settingsManager,
                            onComplete = {
                                val savedName = settingsManager.getUserName().ifBlank {
                                    getSharedPreferences("wakeapp_profile_prefs", Context.MODE_PRIVATE).getString("user_name", "") ?: ""
                                }.trim()
                                if (savedName.isNotBlank()) profileManager.setName(savedName)
                                getSharedPreferences("wakeapp_profile_prefs", Context.MODE_PRIVATE).edit().putString("user_name", profileManager.getName()).apply()
                                isOnboarded = true
                            }
                        )
                    } else {
                        MainScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingDarkBlueBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(wakeAppGradient()),
        content = content
    )
}

@Composable
fun BannerPruebaAdMob(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp, vertical = 2.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

fun loadAndShowRewardedAd(
    activity: android.app.Activity,
    adUnitId: String = ADMOB_REWARDED_TEST_ID,
    fixedRewardAmount: Int = 50,
    onLoadingStateChange: ((Boolean) -> Unit)? = null,
    onRewardEarned: (Int) -> Unit
) {
    onLoadingStateChange?.invoke(true)
    val adRequest = AdRequest.Builder().build()

    RewardedAd.load(activity, adUnitId, adRequest, object : RewardedAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
            onLoadingStateChange?.invoke(false)
            Toast.makeText(activity, "No se pudo cargar el anuncio ahora mismo. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
        }
        override fun onAdLoaded(rewardedAd: RewardedAd) {
            onLoadingStateChange?.invoke(false)
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Toast.makeText(activity, "Error al mostrar el anuncio.", Toast.LENGTH_SHORT).show()
                }
            }
            rewardedAd.show(activity) { _ ->
                onRewardEarned(fixedRewardAmount)
                Toast.makeText(activity, "+$fixedRewardAmount 🪙 conseguidos por ver el anuncio", Toast.LENGTH_SHORT).show()
            }
        }
    })
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
    val xpGained: Int, val levelUp: Boolean, val oldLevel: Int, val newLevel: Int,
    val achievements: List<RewardAchievement> = emptyList(), val bonusLevels: Int = 0, val bonusPoints: Int = 0
)

fun challengeAccent(type: String): Color = CleanText
fun challengeEmoji(type: String): String = ChallengeCatalog.byId(type).emoji
fun challengeTitle(type: String): String = tr(ChallengeCatalog.label(type))

data class AlarmColor(val id: String, val name: String, val color: Color)
object AlarmColorCatalog {
    const val FREE_COUNT = 2
    val all = listOf(
        AlarmColor("DEFAULT", "Azul", WakeBlue), AlarmColor("RED", "Rojo", WakeRed),
        AlarmColor("ORANGE", "Naranja", Color(0xFFFF9800)), AlarmColor("YELLOW", "Amarillo", Color(0xFFFFD600)),
        AlarmColor("GREEN", "Verde", Color(0xFF4CAF50)), AlarmColor("TEAL", "Turquesa", Color(0xFF00BFA5)),
        AlarmColor("CYAN", "Cian", Color(0xFF00E5FF)), AlarmColor("PURPLE", "Morado", Color(0xFFAB47BC)),
        AlarmColor("PINK", "Rosa", Color(0xFFFF4081)), AlarmColor("INDIGO", "Índigo", Color(0xFF5C6BC0))
    )
    fun byId(id: String): Color = all.firstOrNull { it.id == id }?.color ?: WakeBlue
}

fun getAlarmColor(colorHex: String, defaultAccent: Color): Color = AlarmColorCatalog.byId(colorHex)

@Composable
fun ColorWheelLockedCircle(size: Int, onClick: () -> Unit) {
    val rainbowColors = remember {
        listOf(Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFF00E676), Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFFD500F9), Color(0xFFFF1744))
    }
    Box(modifier = Modifier.size(size.dp).clip(CircleShape).border(1.5.dp, CleanText.copy(alpha = 0.55f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(brush = Brush.sweepGradient(colors = rainbowColors))
            drawCircle(color = Color.Black.copy(alpha = 0.42f))
        }
        Text("🔒", fontSize = 13.sp)
    }
}

@Composable
fun ColorPickerRow(selectedId: String, allUnlocked: Boolean, circleSize: Int, onSelect: (String) -> Unit, onLockedClick: (() -> Unit)? = null) {
    val visibleColors = remember(allUnlocked) { if (allUnlocked) AlarmColorCatalog.all else AlarmColorCatalog.all.take(AlarmColorCatalog.FREE_COUNT) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        items(visibleColors, key = { it.id }) { c ->
            val isSelected = selectedId == c.id
            Box(modifier = Modifier.size(circleSize.dp).clip(CircleShape).background(c.color).border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) CleanText else CleanText.copy(alpha = 0.15f), CircleShape).clickable { onSelect(c.id) })
        }
        if (!allUnlocked) {
            item(key = "more_colors_wheel_lock") { ColorWheelLockedCircle(size = circleSize, onClick = { onLockedClick?.invoke() }) }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    var name = tr("Sonido personalizado")
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx) ?: name
        }
    } catch (_: Exception) {}
    return name
}

enum class AppSection(val label: String, val emoji: String) {
    ALARMS("Alarmas", "⏰"), PROGRESS("Progreso", "📊"), SHOP("Tienda", "🛒"), CUSTOMIZATION("Ajustes", "⚙️")
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
    }
}

@Composable
fun AnonymousAvatar(size: Int = 40, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    val avatarPrefs = remember { context.getSharedPreferences("wakeapp_jpg_avatars", Context.MODE_PRIVATE) }
    val equippedId = avatarPrefs.getString("equipped_avatar", "avatar_0") ?: "avatar_0"
    val resName = when (equippedId) { "avatar_0" -> "avatar0"; else -> equippedId.removePrefix("avatar_").let { "avatar$it" } }
    val resId = remember(resName) { context.resources.getIdentifier(resName, "drawable", context.packageName) }

    Box(modifier = modifier.size(size.dp).clip(CircleShape).background(CleanSurface).border(1.dp, CleanSurfaceVariant, CircleShape).clickable(enabled = onClick != null) { onClick?.invoke() }, contentAlignment = Alignment.Center) {
        if (resId != 0) Image(painter = painterResource(id = resId), contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text("👤", fontSize = (size * 0.45f).sp)
    }
}

@Composable
fun MainHeader(level: Int, xp: Int, onProfileClick: () -> Unit, onProClick: () -> Unit) {
    val xpNeeded = StreakManager.xpForLevel(level)
    val xpProgress = (xp.toFloat() / xpNeeded).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth().background(CleanBg.copy(alpha = 0.90f)).statusBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            AnonymousAvatar(size = 38, onClick = onProfileClick)
            Text("WakeApp", fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.SansSerif, color = CleanText, letterSpacing = 0.5.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, CleanSurfaceVariant, RoundedCornerShape(50)).clickable { onProClick() }.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text("PRO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CleanTextDim, letterSpacing = 1.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Nivel $level", fontSize = 11.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(12.dp))
            LinearProgressIndicator(progress = { xpProgress }, modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)), color = CleanText, trackColor = CleanSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Text("$xp/$xpNeeded", fontSize = 10.sp, color = CleanTextDim, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun MainIntroClock(
    modifier: Modifier = Modifier,
    color: Color = CleanText,
    accent: Color = WakeBlue
) {
    val infinite = rememberInfiniteTransition(label = "main_intro_clock")
    val hourRotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "h_rot"
    )
    val minuteRotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "m_rot"
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

// CORRECCIÓN: Eliminado el saludo de buenos días/tardes/noches para evitar "cringe"
@Composable
fun WakeIntroAnimation(onFinished: () -> Unit) {
    var logoVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val logoScale by animateFloatAsState(if (logoVisible) 1f else 0.4f, tween(1200, easing = FastOutSlowInEasing), label = "intro_logo_scale")
    val logoAlpha by animateFloatAsState(if (logoVisible) 1f else 0f, tween(1000, easing = LinearEasing), label = "intro_logo_alpha")

    val subtitle = "Preparando tu WakeApp..."

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "intro_progress"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200); logoVisible = true
        kotlinx.coroutines.delay(700); subtitleVisible = true
        kotlinx.coroutines.delay(600); progress = 1f
        kotlinx.coroutines.delay(900); onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(wakeAppGradient()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            MainIntroClock(
                modifier = Modifier.scale(logoScale),
                color = CleanText,
                accent = WakeBlue
            )
            Spacer(modifier = Modifier.height(36.dp))
            AnimatedVisibility(visible = subtitleVisible, enter = fadeIn(tween(600))) {
                Text(subtitle, fontSize = 14.sp, color = CleanTextDim, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(28.dp))
            Box(modifier = Modifier.width(180.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(CleanSurfaceVariant)) {
                Box(modifier = Modifier.fillMaxWidth(animatedProgress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(WakeBlue))
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
    val settingsManager = remember { SettingsManager(context) }
    val profileManager = remember { ProfileManager(context) }

    val onboardingPrefs = remember { context.getSharedPreferences("wakeapp_onboarding_prefs", Context.MODE_PRIVATE) }
    var currentStreak by remember { mutableIntStateOf(streakManager.getStreak()) }
    var points by remember { mutableIntStateOf(streakManager.getPoints()) }
    var currentLevel by remember { mutableIntStateOf(streakManager.getLevel()) }
    var currentXp by remember { mutableIntStateOf(streakManager.getXp()) }
    var alarms by remember { mutableStateOf(alarmRepository.loadAll()) }

    var showTimePicker by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showRewardsDialog by remember { mutableStateOf(false) }
    var showAlarmLimitDialog by remember { mutableStateOf(false) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showSettingsPersonalization by remember { mutableStateOf(false) }

    var alarmPendingDelete by remember { mutableStateOf<AlarmItem?>(null) }
    var selectedChallengeType by remember { mutableStateOf("MATH") }
    var selectedDifficulty by remember { mutableStateOf("MEDIUM") }
    var selectedRounds by remember { mutableIntStateOf(1) }
    var selectedColor by remember { mutableStateOf("DEFAULT") }
    var pendingSoundUri by remember { mutableStateOf<String?>(null) }
    var pendingSoundName by remember { mutableStateOf<String?>(null) }

    var currentSection by remember { mutableStateOf(AppSection.ALARMS) }
    var rewardDialog by remember { mutableStateOf<RewardInfo?>(null) }
    var isLoadingRewardedAd by remember { mutableStateOf(false) }

    var showIntroAnimation by remember {
        mutableStateOf(!onboardingPrefs.getBoolean("intro_shown_in_main", false))
    }

    val activity = context as? android.app.Activity
    LaunchedEffect(AppPalette.isLight, activity) {
        activity?.window?.let { w ->
            val controller = WindowCompat.getInsetsController(w, w.decorView)
            controller.isAppearanceLightStatusBars = AppPalette.isLight
            controller.isAppearanceLightNavigationBars = AppPalette.isLight
        }
    }

    LaunchedEffect(Unit) { alarms.filter { it.isEnabled }.forEach { scheduleAlarm(context, it) } }
    LaunchedEffect(alarms) { alarmRepository.saveAll(alarms) }
    LaunchedEffect(Unit) { profileManager.refreshAutoUnlocks() }

    LaunchedEffect(Unit) {
        AlarmStateHolder.restoreIfNeeded(context)
        if (AlarmStateHolder.isRinging) {
            val intent = Intent(context, ChallengeActivity::class.java).apply {
                putStringArrayListExtra("CHALLENGE_TYPES", ArrayList(AlarmStateHolder.lastChallengeTypes))
                putExtra("CHALLENGE_TYPE", AlarmStateHolder.lastChallengeTypes.firstOrNull() ?: "MATH")
                putExtra("DIFFICULTY", AlarmStateHolder.lastDifficulty); putExtra("ROUNDS", AlarmStateHolder.lastRounds)
                putExtra("XP_BOOST", AlarmStateHolder.lastXpBoost); putExtra("FROM_SERVICE", true)
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
                        putExtra("DIFFICULTY", AlarmStateHolder.lastDifficulty); putExtra("ROUNDS", AlarmStateHolder.lastRounds)
                        putExtra("XP_BOOST", AlarmStateHolder.lastXpBoost); putExtra("FROM_SERVICE", true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    context.startActivity(intent)
                }
                currentStreak = streakManager.getStreak(); points = streakManager.getPoints()
                currentLevel = streakManager.getLevel(); currentXp = streakManager.getXp()

                profileManager.updateBestStreak(); profileManager.refreshAutoUnlocks()

                achievementManager.checkPointsAchievement(points).let { newAch ->
                    if (newAch.isNotEmpty()) {
                        rewardDialog = RewardInfo(0, false, currentLevel, currentLevel, newAch.map { RewardAchievement(it.id, it.title, it.emoji, it.rewardPoints) }, 0, newAch.sumOf { it.rewardPoints })
                    }
                }
                streakManager.consumePendingReward()?.let { data ->
                    rewardDialog = RewardInfo(data.xp, data.levelUp, data.oldLevel, data.newLevel, data.achievements, data.bonusLevels, data.bonusPoints)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val soundPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
            pendingSoundUri = uri.toString(); pendingSoundName = queryDisplayName(context, uri)
        }
    }

    fun performDelete(alarm: AlarmItem) { cancelAlarm(context, alarm); alarms = alarms.filter { it.id != alarm.id } }
    fun requestDelete(alarm: AlarmItem) { if (settingsManager.isConfirmDeleteEnabled()) alarmPendingDelete = alarm else performDelete(alarm) }

    if (showSettingsPersonalization) {
        SettingsPersonalizationScreen(settingsManager, profileManager, streakManager, onClose = { showSettingsPersonalization = false })
        return
    }
    if (showProfileScreen) {
        ProfileScreen(profileManager, streakManager, achievementManager, points, currentLevel, currentXp, currentStreak, { showProfileScreen = false }, { cost ->
            if (streakManager.spendPoints(cost)) { points = streakManager.getPoints(); achievementManager.checkShopBuyer() }
        })
        return
    }

    if (showIntroAnimation) {
        WakeIntroAnimation(onFinished = {
            onboardingPrefs.edit().putBoolean("intro_shown_in_main", true).apply()
            showIntroAnimation = false
        })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MainHeader(currentLevel, currentXp, onProfileClick = { showProfileScreen = true }, onProClick = { showSettingsPersonalization = true })
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth().background(CleanBg).navigationBarsPadding()) {
                    WakeBottomBar(current = currentSection, onSelect = { if (it == AppSection.CUSTOMIZATION) showSettingsPersonalization = true else currentSection = it })
                }
            },
            floatingActionButton = {
                if (currentSection == AppSection.ALARMS) {
                    FloatingActionButton(
                        onClick = {
                            if (alarms.size >= 4 && !streakManager.hasInfiniteAlarms()) showAlarmLimitDialog = true
                            else {
                                val defaultDiff = onboardingPrefs.getString("default_difficulty", null) ?: settingsManager.getDefaultDifficulty()
                                pendingSoundUri = null; pendingSoundName = null; selectedRounds = 1; selectedChallengeType = "MATH"; selectedColor = "DEFAULT"; selectedDifficulty = defaultDiff; showTimePicker = true
                            }
                        },
                        containerColor = CleanText, contentColor = CleanBg, shape = CircleShape,
                        modifier = Modifier.padding(bottom = 8.dp).size(54.dp)
                    ) { Text("+", fontSize = 26.sp, fontWeight = FontWeight.Normal) }
                }
            },
            floatingActionButtonPosition = FabPosition.Start
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                AnimatedContent(targetState = currentSection, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "section_transition") { section ->
                    when (section) {
                        AppSection.ALARMS -> AlarmsSection(alarms, onToggle = { alarm, enabled -> alarms = alarms.map { if (it.id == alarm.id) it.copy(isEnabled = enabled) else it }; if (enabled) scheduleAlarm(context, alarm.copy(isEnabled = true)) else cancelAlarm(context, alarm) }, onDelete = { requestDelete(it) }, onEdit = { editingAlarm = it })
                        AppSection.PROGRESS -> ProgressSection(context, currentStreak, points, currentLevel, currentXp)
                        AppSection.SHOP -> ShopSection(points, streakManager, profileManager, isLoadingRewardedAd, onWatchRewardedAd = { val act = context as? android.app.Activity; if (act != null && !isLoadingRewardedAd) loadAndShowRewardedAd(act, ADMOB_REWARDED_TEST_ID, 10000, { isLoadingRewardedAd = it }) { earnedCoins -> streakManager.addPoints(earnedCoins); points = streakManager.getPoints() } }, onBuy = { cost -> if (streakManager.spendPoints(cost)) points = streakManager.getPoints() })
                        AppSection.CUSTOMIZATION -> { showSettingsPersonalization = true }
                    }
                }

                val initialHourForPicker = remember(showTimePicker) { if (onboardingPrefs.contains("habitual_hour")) onboardingPrefs.getInt("habitual_hour", 7) else 7 }
                val initialMinuteForPicker = remember(showTimePicker) { if (onboardingPrefs.contains("habitual_minute")) onboardingPrefs.getInt("habitual_minute", 0) else 0 }

                if (showTimePicker) {
                    CustomTimePickerDialog(
                        initialHourForPicker, initialMinuteForPicker, onDismiss = { showTimePicker = false },
                        onConfirm = { hour, minute, label ->
                            val finalTypes = List(selectedRounds) { selectedChallengeType }
                            val newAlarm = AlarmItem(hour = hour, minute = minute, isEnabled = true, challengeTypes = finalTypes, difficulty = selectedDifficulty, rounds = finalTypes.size, soundUri = pendingSoundUri, soundName = pendingSoundName, colorHex = selectedColor, label = label)
                            alarms = (alarms + newAlarm).sortedBy { it.hour * 60 + it.minute }
                            scheduleAlarm(context, newAlarm); profileManager.incrementAlarmsCreated(); showTimePicker = false; pendingSoundUri = null; pendingSoundName = null
                        },
                        selectedChallengeType, { selectedChallengeType = it }, selectedDifficulty, { selectedDifficulty = it }, selectedRounds, { selectedRounds = it },
                        pendingSoundName, { soundPickerLauncher.launch(arrayOf("audio/*")) }, { pendingSoundUri = null; pendingSoundName = null }, selectedColor, { selectedColor = it }, streakManager, onGoToShop = { showTimePicker = false; currentSection = AppSection.SHOP }
                    )
                }
                editingAlarm?.let { alarmToEdit ->
                    EditAlarmDialog(alarmToEdit, onDismiss = { editingAlarm = null }, onSave = { updatedAlarm ->
                        alarms = alarms.map { if (it.id == updatedAlarm.id) updatedAlarm else it }.sortedBy { it.hour * 60 + it.minute }
                        cancelAlarm(context, alarmToEdit); if (updatedAlarm.isEnabled) scheduleAlarm(context, updatedAlarm); editingAlarm = null
                    }, onDelete = { val alarmToRemove = alarmToEdit; editingAlarm = null; requestDelete(alarmToRemove) }, streakManager, onGoToShop = { editingAlarm = null; currentSection = AppSection.SHOP })
                }
                if (showInfoDialog) InfoDialog { showInfoDialog = false }
                if (showRewardsDialog) LevelRewardsDialog { showRewardsDialog = false }
                if (showAlarmLimitDialog) {
                    AlertDialog(
                        onDismissRequest = { showAlarmLimitDialog = false }, containerColor = CleanSurface,
                        title = { Text("Límite alcanzado", color = CleanText, fontWeight = FontWeight.SemiBold) },
                        text = { Text("Has alcanzado el límite de 4 alarmas. Ve a la Tienda y compra 'Alarmas Infinitas'.", color = CleanTextDim) },
                        confirmButton = { Button(onClick = { showAlarmLimitDialog = false; currentSection = AppSection.SHOP }, colors = ButtonDefaults.buttonColors(containerColor = CleanText, contentColor = CleanBg)) { Text("Ir a la Tienda", fontWeight = FontWeight.SemiBold) } },
                        dismissButton = { TextButton(onClick = { showAlarmLimitDialog = false }) { Text("Cancelar", color = CleanTextDim) } }
                    )
                }
                alarmPendingDelete?.let { alarm ->
                    val timeFormatted = String.format("%02d:%02d", alarm.hour, alarm.minute)
                    AlertDialog(
                        onDismissRequest = { alarmPendingDelete = null }, containerColor = CleanSurface,
                        title = { Text("Eliminar alarma", color = CleanText, fontWeight = FontWeight.SemiBold) },
                        text = { Text("¿Seguro que quieres eliminar la alarma de las $timeFormatted" + (if (!alarm.label.isNullOrBlank()) " (${alarm.label})" else "") + "?", color = CleanTextDim) },
                        confirmButton = { Button(onClick = { performDelete(alarm); alarmPendingDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = WakeRed)) { Text("Eliminar", color = Color.White, fontWeight = FontWeight.SemiBold) } },
                        dismissButton = { TextButton(onClick = { alarmPendingDelete = null }) { Text("Cancelar", color = CleanTextDim) } }
                    )
                }
                rewardDialog?.let { info ->
                    if (info.achievements.isNotEmpty()) AchievementUnlockedOverlay(info, onDismiss = { currentLevel = streakManager.getLevel(); currentXp = streakManager.getXp(); points = streakManager.getPoints(); currentStreak = streakManager.getStreak(); rewardDialog = null })
                    else XpRewardDialog(info, onDismiss = { rewardDialog = null })
                }
            }
        }
    }
}

@Composable
fun EditAlarmDialog(alarm: AlarmItem, onDismiss: () -> Unit, onSave: (AlarmItem) -> Unit, onDelete: () -> Unit, streakManager: StreakManager, onGoToShop: (() -> Unit)? = null) {
    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var color by remember { mutableStateOf(alarm.colorHex) }
    var rounds by remember { mutableIntStateOf(alarm.rounds) }
    var label by remember { mutableStateOf(alarm.label ?: "") }
    var selectedType by remember { mutableStateOf(alarm.challengeTypes.firstOrNull() ?: "MATH") }
    var selectedDifficulty by remember { mutableStateOf(alarm.difficulty) }
    val hasExtraRounds = streakManager.hasExtraRounds()
    val maxRounds = if (hasExtraRounds) 10 else 4

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = CleanSurface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(22.dp))) {
            Column(modifier = Modifier.padding(22.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Editar Alarma", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(CleanSurfaceVariant).clickable { onDismiss() }, contentAlignment = Alignment.Center) { Text("✕", color = CleanTextDim, fontSize = 14.sp) }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    NumberPicker(hour, 0..23) { hour = it }
                    Text(":", fontSize = 30.sp, fontWeight = FontWeight.Light, color = CleanTextDim, modifier = Modifier.padding(horizontal = 8.dp))
                    NumberPicker(minute, 0..59) { minute = it }
                }
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("¿Para qué es esta alarma?") }, placeholder = { Text("Trabajo, Instituto, Gym...", color = CleanTextDim) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CleanText, unfocusedBorderColor = CleanSurfaceVariant, focusedTextColor = CleanText, unfocusedTextColor = CleanText, focusedLabelColor = CleanText, unfocusedLabelColor = CleanTextDim, cursorColor = WakeBlue), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(20.dp))
                Text("Minijuego", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ChallengeCatalog.all) { type ->
                        FilterChip(selected = selectedType == type.id, onClick = { selectedType = type.id }, label = { Text("${type.emoji} ${tr(type.label)}", fontSize = 13.sp, fontWeight = FontWeight.Medium) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText), border = null)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Rondas", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items((1..maxRounds).toList()) { r -> FilterChip(selected = rounds == r, onClick = { rounds = r }, label = { Text("$r", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText), border = null) }
                    if (!hasExtraRounds) { item(key = "more_rounds_lock") { FilterChip(selected = false, onClick = { onGoToShop?.invoke() }, label = { Text("+4 🔒", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }, colors = FilterChipDefaults.filterChipColors(containerColor = CleanSurfaceVariant.copy(alpha = 0.7f), labelColor = CleanTextDim), border = null) } }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Dificultad", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(listOf("EASY", "MEDIUM", "HARD", "EXTREME")) { diff -> FilterChip(selected = selectedDifficulty == diff, onClick = { selectedDifficulty = diff }, label = { Text(diff, fontSize = 13.sp, fontWeight = FontWeight.Medium) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText), border = null) }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Color", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(10.dp))
                ColorPickerRow(selectedId = color, allUnlocked = streakManager.hasExtraColors(), circleSize = 36, onSelect = { color = it }, onLockedClick = { onGoToShop?.invoke() })
                Spacer(modifier = Modifier.height(26.dp))
                Button(onClick = { val finalTypes = List(rounds) { selectedType }; onSave(alarm.copy(hour = hour, minute = minute, colorHex = color, challengeTypes = finalTypes, difficulty = selectedDifficulty, rounds = finalTypes.size, label = label.trim().ifBlank { null })) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CleanText, contentColor = CleanBg), shape = RoundedCornerShape(14.dp)) { Text("Guardar Cambios", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onDelete() }, modifier = Modifier.fillMaxWidth().height(46.dp)) { Text("Eliminar Alarma", color = WakeRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
fun LevelRewardsDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = CleanSurface, title = { Text("Recompensas de Nivel", color = CleanText, fontWeight = FontWeight.SemiBold) }, text = {
        Column {
            Text("Sube de nivel completando desafíos para ganar recompensas directas. Las monedas (🪙) solo se obtienen al desbloquear Logros:", fontSize = 13.5.sp, color = CleanTextDim, lineHeight = 19.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CleanSurfaceVariant).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⚡", fontSize = 20.sp); Spacer(modifier = Modifier.width(12.dp))
                Column { Text("Puntos de Energía", color = CleanText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text("Gana +50 🪙 por cada nivel alcanzado.", color = CleanTextDim, fontSize = 12.sp) }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido", color = CleanText, fontWeight = FontWeight.SemiBold) } })
}

@Composable
fun XpRewardDialog(info: RewardInfo, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { }) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CleanSurface).border(1.dp, CleanSurfaceVariant, RoundedCornerShape(22.dp)).padding(28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (info.levelUp) "🎉" else "⭐", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(if (info.levelUp) "NIVEL ALCANZADO" else "DESAFÍO COMPLETADO", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CleanTextDim, letterSpacing = 1.6.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Text("+${info.xpGained} XP", fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                Spacer(modifier = Modifier.height(26.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CleanText, contentColor = CleanBg), shape = RoundedCornerShape(14.dp)) { Text("Continuar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
            }
        }
    }
}

@Composable
fun WakeBottomBar(current: AppSection, onSelect: (AppSection) -> Unit) {
    Surface(color = Color.Transparent, tonalElevation = 0.dp, shadowElevation = 0.dp) {
        Row(modifier = Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            AppSection.entries.forEach { section ->
                val selected = section == current
                val color = if (selected) CleanText else CleanTextDim
                val stroke = if (selected) 2.2.dp else 1.7.dp
                Column(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onSelect(section) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Canvas(modifier = Modifier.size(26.dp)) {
                        val w = size.width; val h = size.height; val s = stroke.toPx()
                        when (section) {
                            AppSection.ALARMS -> {
                                drawCircle(color = color, radius = w / 2.4f, style = Stroke(width = s))
                                drawLine(color = color, start = Offset(w/2, h/2 - w/5), end = Offset(w/2, h/2), strokeWidth = s, cap = StrokeCap.Round)
                                drawLine(color = color, start = Offset(w/2, h/2), end = Offset(w/2 + w/5, h/2), strokeWidth = s, cap = StrokeCap.Round)
                                drawLine(color = color, start = Offset(w*0.2f, h*0.15f), end = Offset(w*0.35f, h*0.25f), strokeWidth = s, cap = StrokeCap.Round)
                                drawLine(color = color, start = Offset(w*0.8f, h*0.15f), end = Offset(w*0.65f, h*0.25f), strokeWidth = s, cap = StrokeCap.Round)
                            }
                            AppSection.PROGRESS -> {
                                drawLine(color = color, start = Offset(w*0.2f, h*0.88f), end = Offset(w*0.2f, h*0.48f), strokeWidth = s * 1.15f, cap = StrokeCap.Round)
                                drawLine(color = color, start = Offset(w*0.5f, h*0.88f), end = Offset(w*0.5f, h*0.18f), strokeWidth = s * 1.15f, cap = StrokeCap.Round)
                                drawLine(color = color, start = Offset(w*0.8f, h*0.88f), end = Offset(w*0.8f, h*0.58f), strokeWidth = s * 1.15f, cap = StrokeCap.Round)
                            }
                            AppSection.SHOP -> {
                                drawRoundRect(color = color, topLeft = Offset(w*0.15f, h*0.36f), size = Size(w*0.7f, h*0.5f), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(width = s))
                                drawArc(color = color, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w*0.3f, h*0.12f), size = Size(w*0.4f, h*0.38f), style = Stroke(width = s, cap = StrokeCap.Round))
                            }
                            AppSection.CUSTOMIZATION -> {
                                drawCircle(color = color, radius = w * 0.35f, style = Stroke(width = s))
                                drawCircle(color = color, radius = w * 0.12f, style = Stroke(width = s))
                                for (i in 0 until 8) {
                                    val angle = Math.PI * i / 4
                                    val startX = w/2 + (w * 0.35f) * kotlin.math.cos(angle).toFloat()
                                    val startY = h/2 + (w * 0.35f) * kotlin.math.sin(angle).toFloat()
                                    val endX = w/2 + (w * 0.5f) * kotlin.math.cos(angle).toFloat()
                                    val endY = h/2 + (w * 0.5f) * kotlin.math.sin(angle).toFloat()
                                    drawLine(color = color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = s, cap = StrokeCap.Round)
                                }
                            }
                        }
                    }
                    if (selected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(section.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmsSection(alarms: List<AlarmItem>, onToggle: (AlarmItem, Boolean) -> Unit, onDelete: (AlarmItem) -> Unit, onEdit: (AlarmItem) -> Unit) {
    if (alarms.isEmpty()) EmptyState()
    else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 110.dp)) {
            items(alarms, key = { it.id }) { alarm -> AlarmCard(alarm = alarm, onToggle = { enabled -> onToggle(alarm, enabled) }, onDelete = { onDelete(alarm) }, onClick = { onEdit(alarm) }) }
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(56.dp)) {
                drawCircle(color = CleanTextDim.copy(alpha = 0.4f), radius = size.width / 2.5f, style = Stroke(width = 2.dp.toPx()))
                drawLine(
                    color = CleanTextDim.copy(alpha = 0.4f),
                    start = Offset(size.width / 2f, size.height / 2f),
                    end = Offset(size.width / 2f, size.height / 3.2f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = CleanTextDim.copy(alpha = 0.4f),
                    start = Offset(size.width / 2f, size.height / 2f),
                    end = Offset(size.width / 1.6f, size.height / 2f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No hay alarmas",
                color = CleanText,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pulsa el botón + para empezar",
                color = CleanTextDim,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AlarmCard(alarm: AlarmItem, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val accentColor = getAlarmColor(alarm.colorHex, WakeBlue)
    val timeFormatted = String.format("%02d:%02d", alarm.hour, alarm.minute)
    val typeLabel = remember(alarm.challengeTypes, AppLang.language) { challengeTitle(alarm.challengeTypes.firstOrNull() ?: "MATH") }
    Card(modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(18.dp)).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = CleanSurface.copy(alpha = 0.94f)), shape = RoundedCornerShape(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(if (alarm.isEnabled) accentColor else CleanSurfaceVariant))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = timeFormatted, fontSize = 38.sp, fontWeight = FontWeight.Light, color = if (alarm.isEnabled) CleanText else CleanTextDim, letterSpacing = (-1).sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$typeLabel · ${alarm.difficulty}", fontSize = 12.5.sp, color = CleanTextDim, fontWeight = FontWeight.Medium)
                        if (alarm.rounds > 1) Text(" · ${alarm.rounds} ${tr("rondas")}", fontSize = 12.5.sp, color = CleanTextDim)
                    }
                    if (!alarm.label.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🏷 ${alarm.label}", fontSize = 12.5.sp, color = CleanText, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (!alarm.soundName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("🎵 ${alarm.soundName}", fontSize = 12.sp, color = CleanTextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Switch(checked = alarm.isEnabled, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = CleanBg, checkedTrackColor = CleanText, uncheckedThumbColor = CleanTextDim, uncheckedTrackColor = CleanSurfaceVariant))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onDelete() }, contentAlignment = Alignment.Center) { Text("🗑", fontSize = 16.sp) }
                }
            }
        }
    }
}

private enum class AchRarity(val label: String, val emoji: String, val color: Color, val glow: Color) {
    COMMON("Común", "⚪", Color(0xFF9EA7B0), Color(0xFFB8C0C8)), RARE("Raro", "🔵", Color(0xFF6B8EAE), Color(0xFF8AA8C4)),
    EPIC("Épico", "🟣", Color(0xFF8B739E), Color(0xFFA790BA)), LEGENDARY("Legendario", "🟡", Color(0xFFB5945B), Color(0xFFCCB07C))
}

private fun rarityOf(required: Int): AchRarity = when { required >= 100 -> AchRarity.LEGENDARY; required >= 30 -> AchRarity.EPIC; required >= 10 -> AchRarity.RARE; else -> AchRarity.COMMON }

@Composable
fun AchievementIngot(achievement: Achievement, unlocked: Boolean, progress: Int) {
    val isActuallyUnlocked = unlocked || (progress >= achievement.required)
    val rarity = rarityOf(achievement.required)
    val accent = rarity.color
    val progressFraction = (progress.toFloat() / achievement.required.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = if (isActuallyUnlocked) 1f else progressFraction, animationSpec = tween(650, easing = FastOutSlowInEasing), label = "progress_${achievement.id}")

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (isActuallyUnlocked) CleanSurface else CleanSurface.copy(alpha = 0.72f)).border(1.dp, if (isActuallyUnlocked) accent.copy(alpha = 0.45f) else CleanSurfaceVariant, RoundedCornerShape(16.dp))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (isActuallyUnlocked) accent.copy(alpha = 0.14f) else CleanSurfaceVariant).border(1.dp, if (isActuallyUnlocked) accent.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(if (isActuallyUnlocked) achievement.emoji else "🔒", fontSize = if (isActuallyUnlocked) 22.sp else 17.sp) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(achievement.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (isActuallyUnlocked) CleanText else CleanTextDim, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isActuallyUnlocked) accent.copy(alpha = 0.14f) else CleanSurfaceVariant).padding(horizontal = 7.dp, vertical = 2.dp)) { Text("${rarity.emoji} ${rarity.label}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (isActuallyUnlocked) accent else CleanTextDim) }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(achievement.description, fontSize = 12.5.sp, color = CleanTextDim, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(10.dp))
                if (isActuallyUnlocked) { Row(verticalAlignment = Alignment.CenterVertically) { Text("✓", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(5.dp)); Text("Completado", color = accent, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) } }
                else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(CleanSurfaceVariant)) { Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(CleanText.copy(alpha = 0.65f))) }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("$progress/${achievement.required}", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = CleanTextDim)
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressSection(context: Context, streak: Int, points: Int, level: Int, xp: Int) {
    val achievementManager = remember { AchievementManager(context) }
    val xpNeeded = StreakManager.xpForLevel(level)
    val totalAch = AchievementCatalog.all.size
    val unlockedCount = remember(streak, points) { AchievementCatalog.all.count { achievementManager.isUnlocked(it.id) } }
    val unlockedFraction = if (totalAch > 0) unlockedCount.toFloat() / totalAch else 0f

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 56.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Tu Progreso", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = OnBgText, letterSpacing = (-0.5).sp) }
        item {
            Card(modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(18.dp)), colors = CardDefaults.cardColors(containerColor = CleanSurface), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    ProfileStatRow("Nivel", "$level"); HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))
                    ProfileStatRow("XP", "$xp / $xpNeeded"); HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))
                    ProfileStatRow("Días Distintos", "$streak ${tr("días")}"); HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))
                    ProfileStatRow("Puntos de Energía", "$points")
                }
            }
        }
        item {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CleanSurface).border(1.dp, CleanSurfaceVariant, RoundedCornerShape(18.dp)).padding(18.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(CleanSurfaceVariant), contentAlignment = Alignment.Center) { Text("🏆", fontSize = 18.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) { Text("LOGROS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanTextDim, letterSpacing = 1.6.sp); Spacer(modifier = Modifier.height(2.dp)); Text("$unlockedCount de $totalAch desbloqueados", fontSize = 13.sp, color = CleanText) }
                        Text("${(unlockedFraction * 100).toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(CleanSurfaceVariant)) { Box(modifier = Modifier.fillMaxWidth(unlockedFraction).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(CleanText)) }
                }
            }
        }
        items(AchievementCatalog.all, key = { it.id }) { ach ->
            AchievementIngot(ach, achievementManager.isUnlocked(ach.id), achievementManager.getProgress(ach, streak, points))
        }
    }
}

@Composable
fun ProfileStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = CleanTextDim, fontSize = 14.5.sp)
        Text(value, color = CleanText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = CleanSurface, title = { Text("¿Qué es WakeApp?", color = CleanText, fontWeight = FontWeight.SemiBold) }, text = { Text("WakeApp es tu despertador inteligente. En lugar de un simple botón, cada alarma te lanza desafíos mentales o físicos que debes superar para desactivarla.", fontSize = 14.sp, color = CleanTextDim, lineHeight = 20.sp) }, confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido", color = CleanText, fontWeight = FontWeight.SemiBold) } })
}

@Composable
fun CustomTimePickerDialog(
    initialHour: Int, initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int, String?) -> Unit,
    selectedChallengeType: String, onChallengeTypeSelected: (String) -> Unit, selectedDifficulty: String, onDifficultySelected: (String) -> Unit,
    selectedRounds: Int, onRoundsChange: (Int) -> Unit, soundName: String?, onPickSound: () -> Unit, onClearSound: () -> Unit,
    selectedColor: String, onColorSelected: (String) -> Unit, streakManager: StreakManager, onGoToShop: (() -> Unit)? = null
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    var alarmLabel by remember { mutableStateOf("") }
    val hasExtraRounds = streakManager.hasExtraRounds()
    val maxRounds = if (hasExtraRounds) 10 else 4

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CleanSurface,
        title = { Text("Nueva Alarma", color = CleanText, fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    NumberPicker(selectedHour, 0..23) { selectedHour = it }
                    Text(":", fontSize = 30.sp, fontWeight = FontWeight.Light, color = CleanTextDim, modifier = Modifier.padding(horizontal = 8.dp))
                    NumberPicker(selectedMinute, 0..59) { selectedMinute = it }
                }
                Spacer(modifier = Modifier.height(18.dp))
                OutlinedTextField(value = alarmLabel, onValueChange = { alarmLabel = it }, label = { Text("¿Para qué es esta alarma?") }, placeholder = { Text("Trabajo, Instituto, Gym...", color = CleanTextDim) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CleanText, unfocusedBorderColor = CleanSurfaceVariant, focusedTextColor = CleanText, unfocusedTextColor = CleanText, focusedLabelColor = CleanText, unfocusedLabelColor = CleanTextDim, cursorColor = WakeBlue), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(20.dp))
                Text("Minijuego", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ChallengeCatalog.all) { type -> FilterChip(selected = selectedChallengeType == type.id, onClick = { onChallengeTypeSelected(type.id) }, label = { Text("${type.emoji} ${tr(type.label)}", fontSize = 13.sp, fontWeight = FontWeight.Medium) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText), border = null) }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Rondas", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items((1..maxRounds).toList()) { r -> FilterChip(selected = selectedRounds == r, onClick = { onRoundsChange(r) }, label = { Text("$r", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText), border = null) }
                    if (!hasExtraRounds) { item(key = "more_rounds_lock") { FilterChip(selected = false, onClick = { onGoToShop?.invoke() }, label = { Text("+4 🔒", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }, colors = FilterChipDefaults.filterChipColors(containerColor = CleanSurfaceVariant.copy(alpha = 0.7f), labelColor = CleanTextDim), border = null) } }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Dificultad", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(listOf("EASY", "MEDIUM", "HARD", "EXTREME")) { diff -> FilterChip(selected = selectedDifficulty == diff, onClick = { onDifficultySelected(diff) }, label = { Text(diff, fontSize = 13.sp, fontWeight = FontWeight.Medium) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText), border = null) }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Color de la Alarma", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(10.dp))
                ColorPickerRow(selectedId = selectedColor, allUnlocked = streakManager.hasExtraColors(), circleSize = 38, onSelect = onColorSelected, onLockedClick = { onGoToShop?.invoke() })
                Spacer(modifier = Modifier.height(20.dp))
                Text("Sonido", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CleanSurfaceVariant).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (soundName != null) "🎵" else "🔔", fontSize = 18.sp); Spacer(modifier = Modifier.width(12.dp))
                    Text(soundName ?: "Predeterminado", fontSize = 14.sp, color = CleanText, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (soundName != null) { Box(modifier = Modifier.size(24.dp).clickable { onClearSound() }, contentAlignment = Alignment.Center) { Text("✕", color = CleanTextDim, fontSize = 13.sp) } }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onPickSound, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = CleanSurfaceVariant), shape = RoundedCornerShape(12.dp)) { Text("Elegir archivo de audio", color = CleanText, fontSize = 13.5.sp) }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selectedHour, selectedMinute, alarmLabel.trim().ifBlank { null }) }, colors = ButtonDefaults.buttonColors(containerColor = CleanText, contentColor = CleanBg), shape = RoundedCornerShape(12.dp)) { Text("Guardar Alarma", fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = CleanTextDim) } }
    )
}

@Composable
fun NumberPicker(value: Int, range: IntRange, step: Int = 1, onValueChange: (Int) -> Unit) {
    val items = remember(range, step) { range.filter { it % step == 0 } }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = items.indexOf(value).coerceAtLeast(0))
    val itemHeight = 48.dp; val visibleItems = 3; val boxHeight = itemHeight * visibleItems; val verticalPadding = (boxHeight - itemHeight) / 2
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { info -> kotlin.math.abs((info.offset + info.size / 2) - viewportCenter) }?.index
        }.collect { centeredIndex ->
            if (centeredIndex != null && centeredIndex in items.indices) { val newValue = items[centeredIndex]; if (newValue != value) onValueChange(newValue) }
        }
    }
    Box(modifier = Modifier.height(boxHeight).width(80.dp), contentAlignment = Alignment.Center) {
        LazyColumn(state = listState, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = verticalPadding), flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)) {
            items(items) { item ->
                val isSelected = item == value
                Box(modifier = Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(String.format("%02d", item), fontSize = if (isSelected) 32.sp else 20.sp, fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Light, color = if (isSelected) CleanText else CleanTextDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
fun AchievementUnlockedOverlay(info: RewardInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { try { val resId = context.resources.getIdentifier("achievement_se", "raw", context.packageName); if (resId != 0) { val mediaPlayer = MediaPlayer.create(context, resId); mediaPlayer.start(); mediaPlayer.setOnCompletionListener { it.release() } } } catch (_: Exception) {} }
    var queue by remember { mutableStateOf(info.achievements.toList()) }
    val current = queue.firstOrNull()
    if (current == null) { LaunchedEffect(Unit) { onDismiss() }; return }
    Dialog(onDismissRequest = { }) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CleanSurface).border(1.dp, CleanSurfaceVariant, RoundedCornerShape(22.dp)).padding(28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(current.emoji, fontSize = 52.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Text("LOGRO DESBLOQUEADO", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanTextDim, letterSpacing = 1.6.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(current.title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = CleanText, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
                Text("+1 NIVEL", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                Spacer(modifier = Modifier.height(6.dp))
                Text("+${current.rewardPoints} ${tr("PUNTOS")}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = CleanTextDim)
                Spacer(modifier = Modifier.height(26.dp))
                Button(onClick = { if (queue.size > 1) { queue = queue.drop(1) } else { onDismiss() } }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CleanText, contentColor = CleanBg), shape = RoundedCornerShape(14.dp)) { Text(if (queue.size > 1) "Siguiente" else "Continuar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
            }
        }
    }
}

fun scheduleAlarm(context: Context, alarm: AlarmItem) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val types = if (alarm.challengeTypes.isNotEmpty()) alarm.challengeTypes else listOf("MATH")
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putStringArrayListExtra("CHALLENGE_TYPES", ArrayList(types))
        putExtra("CHALLENGE_TYPE", types.firstOrNull() ?: "MATH"); putExtra("DIFFICULTY", alarm.difficulty)
        putExtra("ROUNDS", alarm.rounds); putExtra("XP_BOOST", alarm.xpBoost)
        putExtra("SOUND_URI", alarm.soundUri); putExtra("SOUND_NAME", alarm.soundName); putExtra("ALARM_LABEL", alarm.label)
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
    try { alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent), pendingIntent) } catch (e: SecurityException) { e.printStackTrace(); try { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent) } catch (_: Exception) {} }
}

fun cancelAlarm(context: Context, alarm: AlarmItem) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, alarm.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    alarmManager.cancel(pendingIntent)
}

private data class JpgAvatarShopItem(val id: String, val resName: String, val title: String, val price: Int, val accent: Color)
private val JpgAvatarCatalog = listOf(
    JpgAvatarShopItem("avatar_0", "avatar0", "Por defecto", 0, Color(0xFF90A4AE)),
    JpgAvatarShopItem("avatar_1", "avatar1", "Caballero", 250, Color(0xFF42A5F5)),
    JpgAvatarShopItem("avatar_2", "avatar2", "Eclipse", 300, Color(0xFF26C6DA)),
    JpgAvatarShopItem("avatar_3", "avatar3", "Británico gordo", 350, Color(0xFF66BB6A)),
    JpgAvatarShopItem("avatar_4", "avatar4", "Julius", 400, Color(0xFFFFCA28))
)

private data class ShopItem(val id: String, val title: String, val description: String, val price: Int, val emoji: String)

@Composable
fun ShopSection(points: Int, streakManager: StreakManager, profileManager: ProfileManager, isLoadingAd: Boolean, onWatchRewardedAd: () -> Unit, onBuy: (Int) -> Unit) {
    val shopItems = listOf(
        ShopItem("infinite_alarms", "Alarmas Infinitas", "Crea todas las alarmas que quieras.", 500, "⏰"),
        ShopItem("extra_rounds", "Rondas Extra", "Aumenta el límite de rondas por alarma a 10.", 400, "🔁"),
        ShopItem("extra_colors", "Colores Desbloqueados", "Desbloquea todos los colores de alarma.", 300, "🎨")
    )
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Tienda", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = OnBgText)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Tus Puntos: ", fontSize = 15.sp, color = CleanTextDim); Text("$points 🪙", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CleanText) }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CleanSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(18.dp))) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎁 Anuncio Diario", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CleanText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Mira un video y gana 50 monedas.", fontSize = 13.sp, color = CleanTextDim)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onWatchRewardedAd, enabled = !isLoadingAd, colors = ButtonDefaults.buttonColors(containerColor = WakeBlue, contentColor = CleanText), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(46.dp)) { Text(if (isLoadingAd) "Cargando..." else "Ver Anuncio (+50 🪙)", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        items(shopItems) { item ->
            var isOwned by remember { mutableStateOf(false) }
            if (item.id == "infinite_alarms") isOwned = streakManager.hasInfiniteAlarms()
            if (item.id == "extra_rounds") isOwned = streakManager.hasExtraRounds()
            if (item.id == "extra_colors") isOwned = streakManager.hasExtraColors()

            Card(colors = CardDefaults.cardColors(containerColor = CleanSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(16.dp))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.emoji, fontSize = 32.sp); Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) { Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CleanText); Text(item.description, fontSize = 12.5.sp, color = CleanTextDim) }
                    Spacer(modifier = Modifier.width(12.dp))
                    if (isOwned) { Text("Comprado", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CleanTextDim) }
                    else { Button(onClick = { onBuy(item.price) }, enabled = points >= item.price, colors = ButtonDefaults.buttonColors(containerColor = CleanText, contentColor = CleanBg, disabledContainerColor = CleanSurfaceVariant, disabledContentColor = CleanTextDim), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("${item.price} 🪙", fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
                }
            }
        }
    }
}