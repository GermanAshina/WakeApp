// MainActivity.kt
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
import android.widget.Toast
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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

// --- IDs DE ANUNCIOS ADMOB ---
const val ADMOB_REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917"
const val ADMOB_REWARDED_MONEDAS_TIENDA_REAL_ID = "ca-app-pub-1219684092719163/5455336103"
private const val PRO_PRICE_LABEL = "3,99 €"

// --- PALETA CLÍNICA ---
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

val CleanBg: Color get() = if (AppPalette.isLight) Color(0xFFFFFFFF) else themed(Color(0xFF0A0A0A), 0.06f)
val CleanSurface: Color get() = if (AppPalette.isLight) Color(0xFFF2F2F2) else themed(Color(0xFF1A1A1A), 0.10f)
val CleanSurfaceVariant: Color get() = if (AppPalette.isLight) Color(0xFFE2E2E2) else themed(Color(0xFF262626), 0.14f)
val CleanText: Color get() = if (AppPalette.isLight) Color(0xFF111111) else Color(0xFFFFFFFF)
val CleanTextDim: Color get() = if (AppPalette.isLight) Color(0xFF6B6B6B) else Color(0xFF888888)
val WakeBlue: Color get() = AppPalette.accent
val WakeRed = Color(0xFFFF5252)

val OnBgText = Color(0xFFFFFFFF)
val OnBgTextDim = Color(0xFFBBBBBB)

class ProManager(context: Context) {
    private val prefs = context.getSharedPreferences("wakeapp_pro_prefs", Context.MODE_PRIVATE)
    var isPro by mutableStateOf(prefs.getBoolean("is_pro_unlocked", false))
        private set

    fun unlockPro() {
        prefs.edit().putBoolean("is_pro_unlocked", true).apply()
        isPro = true
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)

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
            val settingsManager = remember { SettingsManager(this@MainActivity) }
            val profileManager = remember { ProfileManager(this@MainActivity) }
            remember {
                AppPalette.isLight = settingsManager.isLightMode()
                AppPalette.applyTheme(ThemeManager(this@MainActivity).getActive())
                AppLang.language = settingsManager.getLanguage()
                if (profileManager.getName().isBlank()) {
                    val fallbackName = settingsManager.getUserName().ifBlank {
                        getSharedPreferences("wakeapp_profile_prefs", Context.MODE_PRIVATE)
                            .getString("user_name", "") ?: ""
                    }
                    if (fallbackName.isNotBlank()) {
                        profileManager.setName(fallbackName.trim())
                    }
                }
                true
            }

            WakeAppTheme(
                darkTheme = !AppPalette.isLight,
                accent = if (AppPalette.isDefaultTheme) null else AppPalette.accent
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = CleanBg) {
                    var isOnboarded by remember { mutableStateOf(settingsManager.isOnboardingCompleted()) }

                    if (!isOnboarded) {
                        OnboardingScreen(
                            settingsManager = settingsManager,
                            onComplete = {
                                val savedName = settingsManager.getUserName().ifBlank {
                                    getSharedPreferences("wakeapp_profile_prefs", Context.MODE_PRIVATE)
                                        .getString("user_name", "") ?: ""
                                }.trim()
                                if (savedName.isNotBlank()) {
                                    profileManager.setName(savedName)
                                }
                                getSharedPreferences("wakeapp_profile_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("user_name", profileManager.getName())
                                    .apply()

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
fun BannerPruebaAdMob(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp),
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

    RewardedAd.load(
        activity,
        adUnitId,
        adRequest,
        object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                onLoadingStateChange?.invoke(false)
                Toast.makeText(
                    activity,
                    "No se pudo cargar el anuncio ahora mismo. Inténtalo de nuevo.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                onLoadingStateChange?.invoke(false)
                rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Toast.makeText(
                            activity,
                            "Error al mostrar el anuncio.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                rewardedAd.show(activity) { _ ->
                    onRewardEarned(fixedRewardAmount)
                    Toast.makeText(
                        activity,
                        "+$fixedRewardAmount 🪙 conseguidos por ver el anuncio",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    )
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
fun challengeTitle(type: String): String = tr(ChallengeCatalog.label(type))

data class AlarmColor(val id: String, val name: String, val color: Color)
object AlarmColorCatalog {
    const val FREE_COUNT = 2
    val all = listOf(
        AlarmColor("DEFAULT", "Azul", WakeBlue),
        AlarmColor("RED", "Rojo", WakeRed),
        AlarmColor("ORANGE", "Naranja", Color(0xFFFF9800)),
        AlarmColor("YELLOW", "Amarillo", Color(0xFFFFD600)),
        AlarmColor("GREEN", "Verde", Color(0xFF4CAF50)),
        AlarmColor("TEAL", "Turquesa", Color(0xFF00BFA5)),
        AlarmColor("CYAN", "Cian", Color(0xFF00E5FF)),
        AlarmColor("PURPLE", "Morado", Color(0xFFAB47BC)),
        AlarmColor("PINK", "Rosa", Color(0xFFFF4081)),
        AlarmColor("INDIGO", "Índigo", Color(0xFF5C6BC0))
    )
    fun byId(id: String): Color = all.firstOrNull { it.id == id }?.color ?: WakeBlue
}

fun getAlarmColor(colorHex: String, defaultAccent: Color): Color = AlarmColorCatalog.byId(colorHex)

@Composable
fun ColorWheelLockedCircle(
    size: Int,
    onClick: () -> Unit
) {
    val rainbowColors = remember {
        listOf(
            Color(0xFFFF1744),
            Color(0xFFFF9100),
            Color(0xFFFFEA00),
            Color(0xFF00E676),
            Color(0xFF00E5FF),
            Color(0xFF2979FF),
            Color(0xFFD500F9),
            Color(0xFFFF1744)
        )
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .border(1.5.dp, CleanText.copy(alpha = 0.55f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.sweepGradient(colors = rainbowColors)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.42f)
            )
        }
        Text("🔒", fontSize = 13.sp)
    }
}

@Composable
fun ColorPickerRow(
    selectedId: String,
    allUnlocked: Boolean,
    circleSize: Int,
    onSelect: (String) -> Unit,
    onLockedClick: (() -> Unit)? = null
) {
    val visibleColors = remember(allUnlocked) {
        if (allUnlocked) AlarmColorCatalog.all
        else AlarmColorCatalog.all.take(AlarmColorCatalog.FREE_COUNT)
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        items(visibleColors, key = { it.id }) { c ->
            val isSelected = selectedId == c.id
            Box(
                modifier = Modifier
                    .size(circleSize.dp)
                    .clip(CircleShape)
                    .background(c.color)
                    .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) CleanText else CleanText.copy(alpha = 0.15f), CircleShape)
                    .clickable { onSelect(c.id) }
            )
        }
        if (!allUnlocked) {
            item(key = "more_colors_wheel_lock") {
                ColorWheelLockedCircle(
                    size = circleSize,
                    onClick = { onLockedClick?.invoke() }
                )
            }
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

// --- BARRA INFERIOR ---
enum class AppSection(val label: String, val emoji: String) {
    ALARMS("Alarmas", "⏰"),
    PROGRESS("Progreso", "📊"),
    SHOP("Tienda", "🛒"),
    CUSTOMIZATION("Personalización", "🎨")
}

private data class FaqItem(val question: String, val answer: String)

// --- AJUSTES ---
@Composable
fun SettingsScreen(settingsManager: SettingsManager, onClose: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(settingsManager.isConfirmDeleteEnabled()) }

    val faqs = remember {
        listOf(
            FaqItem(
                question = "¿Cómo se desactiva una alarma?",
                answer = "Al sonar la alarma tendrás que superar los desafíos configurados y las rondas indicadas para apagarla."
            ),
            FaqItem(
                question = "¿Qué pasa si reinicio el móvil o cambia la hora?",
                answer = "Las alarmas activas se reprograman y automáticamente con el horario de tu dispositivo."
            ),
            FaqItem(
                question = "¿Cómo gano XP, niveles y puntos?",
                answer = "Completando desafíos sumas XP para subir de nivel, y al desbloquear logros obtienes puntos (🪙) para gastar en la Tienda."
            ),
            FaqItem(
                question = "¿Puedo usar mis propios archivos de audio?",
                answer = "Sí, al configurar una alarma puedes elegir un archivo de audio desde tu dispositivo."
            )
        )
    }
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(CleanBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CleanSurface)
                    .border(1.dp, CleanSurfaceVariant, CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontSize = 18.sp, color = CleanText)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Ajustes", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
        }

        HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.6f))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("GENERAL", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanTextDim, letterSpacing = 1.6.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Confirmar borrado", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                            Text("Pedir confirmación antes de eliminar una alarma.", fontSize = 12.5.sp, color = CleanTextDim)
                        }
                        Switch(
                            checked = confirmDelete,
                            onCheckedChange = { checked ->
                                confirmDelete = checked
                                settingsManager.setConfirmDeleteEnabled(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CleanBg,
                                checkedTrackColor = CleanText,
                                uncheckedThumbColor = CleanTextDim,
                                uncheckedTrackColor = CleanSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text("INFORMACIÓN DE LA APP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanTextDim, letterSpacing = 1.6.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("¿Qué hace WakeApp?", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "WakeApp es un despertador con desafíos interactivos diseñado para ayudarte a despertar de verdad, registrar tu avance y personalizar la app a tu gusto.",
                        fontSize = 13.5.sp, color = CleanTextDim, lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text("PRIVACIDAD DEL USUARIO", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanTextDim, letterSpacing = 1.6.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Datos y preferencias", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Tus alarmas, preferencias, progreso y niveles se almacenan en tu dispositivo. Si seleccionas un archivo de audio propio, la app solo utiliza el archivo que elijas.",
                        fontSize = 13.5.sp, color = CleanTextDim, lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text("DUDAS FRECUENTES", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanTextDim, letterSpacing = 1.6.sp)
            Spacer(modifier = Modifier.height(10.dp))

            faqs.forEachIndexed { index, item ->
                val isExpanded = expandedIndex == index
                Card(
                    colors = CardDefaults.cardColors(containerColor = CleanSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(1.dp, CleanSurfaceVariant, RoundedCornerShape(16.dp))
                        .clickable { expandedIndex = if (isExpanded) null else index }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.question, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = CleanText, modifier = Modifier.weight(1f).padding(end = 8.dp))
                            Text(if (isExpanded) "−" else "+", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = CleanTextDim)
                        }
                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(item.answer, fontSize = 13.sp, color = CleanTextDim, lineHeight = 19.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
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
        Image(painter = painterResource(id = backgroundRes), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)))
    }
}

@Composable
fun AnonymousAvatar(
    size: Int = 40,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val avatarPrefs = remember { context.getSharedPreferences("wakeapp_jpg_avatars", Context.MODE_PRIVATE) }
    val equippedId = avatarPrefs.getString("equipped_avatar", "avatar_0") ?: "avatar_0"
    val resName = when (equippedId) {
        "avatar_0" -> "avatar0"
        else -> equippedId.removePrefix("avatar_").let { "avatar$it" }
    }
    val resId = remember(resName) {
        context.resources.getIdentifier(resName, "drawable", context.packageName)
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(CleanSurface)
            .border(1.dp, CleanSurfaceVariant, CircleShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("👤", fontSize = (size * 0.45f).sp)
        }
    }
}

@Composable
fun ProPurchaseDialog(
    onDismiss: () -> Unit,
    onConfirmPurchase: () -> Unit
) {
    var processingPayment by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!processingPayment) onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CleanSurfaceVariant, RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(WakeRed, Color(0xFFFF6D00), Color(0xFFFF1744))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "✦ WAKEAPP PRO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Desbloquea WakeApp Pro",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Activa la edición Pro con pago único y muestra WakeApp Pro en la esquina superior.",
                    fontSize = 13.5.sp,
                    color = CleanTextDim,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CleanSurfaceVariant.copy(alpha = 0.65f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("✓ Insignia WakeApp Pro en la esquina superior", fontSize = 12.5.sp, color = CleanText)
                    Text("✓ Pago único sin suscripciones", fontSize = 12.5.sp, color = CleanText)
                    Text("✓ Activación inmediata en tu dispositivo", fontSize = 12.5.sp, color = CleanText)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        processingPayment = true
                        onConfirmPurchase()
                    },
                    enabled = !processingPayment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WakeRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (processingPayment) "Procesando pago..." else "Pagar $PRO_PRICE_LABEL",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onDismiss,
                    enabled = !processingPayment
                ) {
                    Text("Ahora no", color = CleanTextDim, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun MainHeader(
    level: Int,
    xp: Int,
    readingMode: Boolean,
    isPro: Boolean = false,
    onProClick: () -> Unit = {},
    onToggleReading: () -> Unit,
    onInfoClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val xpNeeded = StreakManager.xpForLevel(level)
    val xpProgress = (xp.toFloat() / xpNeeded).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CleanBg.copy(alpha = 0.94f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AnonymousAvatar(size = 36, onClick = onProfileClick)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Text(
                        text = "WakeApp",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        color = CleanText,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            top = if (isPro) 6.dp else 0.dp,
                            end = if (isPro) 24.dp else 0.dp
                        )
                    )

                    if (isPro) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(WakeRed, Color(0xFFFF6D00), Color(0xFFFF1744))
                                    )
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PRO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (!isPro) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(WakeRed, Color(0xFFFF5722), Color(0xFFD500F9))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
                            .clickable { onProClick() }
                            .padding(horizontal = 8.5.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PRO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CleanSurface)
                        .border(1.dp, CleanSurfaceVariant, CircleShape)
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) { Text("⚙️", fontSize = 15.sp) }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (readingMode) CleanText else CleanSurface)
                        .border(1.dp, if (readingMode) CleanText else CleanSurfaceVariant, CircleShape)
                        .clickable { onToggleReading() },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(17.dp)) {
                        val w = size.width; val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.62f, h * 0.12f)
                            cubicTo(w * 0.18f, h * 0.22f, w * 0.18f, h * 0.78f, w * 0.62f, h * 0.88f)
                            cubicTo(w * 0.32f, h * 0.68f, w * 0.32f, h * 0.32f, w * 0.62f, h * 0.12f)
                        }
                        drawPath(
                            path,
                            color = if (readingMode) CleanBg else CleanText,
                            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${tr("Nivel")} $level", fontSize = 11.5.sp, color = CleanTextDim, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(10.dp))
            LinearProgressIndicator(
                progress = { xpProgress },
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CleanText,
                trackColor = CleanSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.clickable { onRewardsClick() }) {
                Text("Recompensas", fontSize = 11.5.sp, color = CleanTextDim, fontWeight = FontWeight.Medium)
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
    val profileManager = remember { ProfileManager(context) }
    val proManager = remember { ProManager(context) }

    val onboardingPrefs = remember {
        context.getSharedPreferences("wakeapp_onboarding_prefs", Context.MODE_PRIVATE)
    }

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
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }

    var alarmPendingDelete by remember { mutableStateOf<AlarmItem?>(null) }

    var selectedChallengeType by remember { mutableStateOf("MATH") }
    var selectedDifficulty by remember { mutableStateOf("MEDIUM") }
    var selectedRounds by remember { mutableIntStateOf(1) }
    var selectedColor by remember { mutableStateOf("DEFAULT") }
    var pendingSoundUri by remember { mutableStateOf<String?>(null) }
    var pendingSoundName by remember { mutableStateOf<String?>(null) }

    var currentSection by remember { mutableStateOf(AppSection.ALARMS) }
    var rewardDialog by remember { mutableStateOf<RewardInfo?>(null) }
    var readingMode by remember { mutableStateOf(readingModeManager.isEnabled()) }
    var isLoadingRewardedAd by remember { mutableStateOf(false) }

    val activity = context as? android.app.Activity
    LaunchedEffect(readingMode, activity) {
        activity?.window?.let { readingModeManager.applyToWindow(it, readingMode) }
    }

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

                profileManager.updateBestStreak()
                profileManager.refreshAutoUnlocks()

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

    val soundPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
            pendingSoundUri = uri.toString()
            pendingSoundName = queryDisplayName(context, uri)
        }
    }

    fun performDelete(alarm: AlarmItem) {
        cancelAlarm(context, alarm)
        alarms = alarms.filter { it.id != alarm.id }
    }

    fun requestDelete(alarm: AlarmItem) {
        if (settingsManager.isConfirmDeleteEnabled()) alarmPendingDelete = alarm else performDelete(alarm)
    }

    if (showSettingsScreen) {
        SettingsScreen(settingsManager = settingsManager, onClose = { showSettingsScreen = false })
        return
    }
    if (showProfileScreen) {
        ProfileScreen(
            profileManager = profileManager,
            streakManager = streakManager,
            achievementManager = achievementManager,
            points = points,
            level = currentLevel,
            xp = currentXp,
            streak = currentStreak,
            onClose = { showProfileScreen = false },
            onSpendPoints = { cost ->
                if (streakManager.spendPoints(cost)) {
                    points = streakManager.getPoints()
                    achievementManager.checkShopBuyer()
                }
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(CleanBg)) {
        if (currentSection != AppSection.CUSTOMIZATION) AnimatedBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MainHeader(
                    level = currentLevel,
                    xp = currentXp,
                    readingMode = readingMode,
                    isPro = proManager.isPro,
                    onProClick = { showProDialog = true },
                    onToggleReading = { readingMode = !readingMode; readingModeManager.setEnabled(readingMode) },
                    onInfoClick = { showInfoDialog = true },
                    onRewardsClick = { showRewardsDialog = true },
                    onProfileClick = { showProfileScreen = true },
                    onSettingsClick = { showSettingsScreen = true }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.fillMaxWidth().background(CleanBg).navigationBarsPadding()
                ) {
                    WakeBottomBar(current = currentSection, onSelect = { currentSection = it })
                }
            },
            floatingActionButton = {
                if (currentSection == AppSection.ALARMS) {
                    FloatingActionButton(
                        onClick = {
                            if (alarms.size >= 4 && !streakManager.hasInfiniteAlarms()) showAlarmLimitDialog = true
                            else {
                                val defaultDiffFromOnboarding = onboardingPrefs.getString("default_difficulty", null)
                                    ?: settingsManager.getDefaultDifficulty()
                                pendingSoundUri = null
                                pendingSoundName = null
                                selectedRounds = 1
                                selectedChallengeType = "MATH"
                                selectedColor = "DEFAULT"
                                selectedDifficulty = defaultDiffFromOnboarding
                                showTimePicker = true
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
                AnimatedContent(targetState = currentSection, transitionSpec = { fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200)) }, label = "section_transition") { section ->
                    when (section) {
                        AppSection.ALARMS -> AlarmsSection(
                            alarms = alarms,
                            onToggle = { alarm, enabled ->
                                alarms = alarms.map { if (it.id == alarm.id) it.copy(isEnabled = enabled) else it }
                                if (enabled) scheduleAlarm(context, alarm.copy(isEnabled = true)) else cancelAlarm(context, alarm)
                            },
                            onDelete = { alarm -> requestDelete(alarm) },
                            onEdit = { editingAlarm = it }
                        )
                        AppSection.PROGRESS -> ProgressSection(context = context, streak = currentStreak, points = points, level = currentLevel, xp = currentXp)
                        AppSection.SHOP -> ShopSection(
                            points = points,
                            streakManager = streakManager,
                            profileManager = profileManager,
                            isLoadingAd = isLoadingRewardedAd,
                            onWatchRewardedAd = {
                                val act = context as? android.app.Activity
                                if (act != null && !isLoadingRewardedAd) {
                                    loadAndShowRewardedAd(
                                        activity = act,
                                        adUnitId = ADMOB_REWARDED_TEST_ID,
                                        fixedRewardAmount = 50,
                                        onLoadingStateChange = { isLoadingRewardedAd = it },
                                        onRewardEarned = { earnedCoins ->
                                            streakManager.addPoints(earnedCoins)
                                            points = streakManager.getPoints()
                                            achievementManager.checkPointsAchievement(points).let { newAch ->
                                                if (newAch.isNotEmpty()) {
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
                            },
                            onBuy = { cost ->
                                if (streakManager.spendPoints(cost)) {
                                    points = streakManager.getPoints()
                                    achievementManager.checkShopBuyer()
                                }
                            }
                        )
                        AppSection.CUSTOMIZATION -> PersonalizationSection(settingsManager)
                    }
                }

                val initialHourForPicker = remember(showTimePicker) {
                    if (onboardingPrefs.contains("habitual_hour")) onboardingPrefs.getInt("habitual_hour", settingsManager.getHabitualWakeHour())
                    else settingsManager.getHabitualWakeHour()
                }
                val initialMinuteForPicker = remember(showTimePicker) {
                    if (onboardingPrefs.contains("habitual_minute")) onboardingPrefs.getInt("habitual_minute", settingsManager.getHabitualWakeMinute())
                    else settingsManager.getHabitualWakeMinute()
                }

                if (showTimePicker) {
                    CustomTimePickerDialog(
                        initialHour = initialHourForPicker,
                        initialMinute = initialMinuteForPicker,
                        onDismiss = { showTimePicker = false },
                        onConfirm = { hour, minute, label ->
                            val finalTypes = List(selectedRounds) { selectedChallengeType }
                            val newAlarm = AlarmItem(hour = hour, minute = minute, isEnabled = true, challengeTypes = finalTypes, difficulty = selectedDifficulty, rounds = finalTypes.size, soundUri = pendingSoundUri, soundName = pendingSoundName, colorHex = selectedColor, label = label)
                            alarms = (alarms + newAlarm).sortedBy { it.hour * 60 + it.minute }
                            scheduleAlarm(context, newAlarm)
                            profileManager.incrementAlarmsCreated()
                            showTimePicker = false; pendingSoundUri = null; pendingSoundName = null
                        },
                        selectedChallengeType = selectedChallengeType, onChallengeTypeSelected = { selectedChallengeType = it },
                        selectedDifficulty = selectedDifficulty, onDifficultySelected = { selectedDifficulty = it },
                        selectedRounds = selectedRounds, onRoundsChange = { selectedRounds = it },
                        soundName = pendingSoundName, onPickSound = { soundPickerLauncher.launch(arrayOf("audio/*")) }, onClearSound = { pendingSoundUri = null; pendingSoundName = null },
                        selectedColor = selectedColor, onColorSelected = { selectedColor = it },
                        streakManager = streakManager,
                        onGoToShop = {
                            showTimePicker = false
                            currentSection = AppSection.SHOP
                        }
                    )
                }
                editingAlarm?.let { alarmToEdit ->
                    EditAlarmDialog(
                        alarm = alarmToEdit,
                        onDismiss = { editingAlarm = null },
                        onSave = { updatedAlarm ->
                            alarms = alarms.map { if (it.id == updatedAlarm.id) updatedAlarm else it }.sortedBy { it.hour * 60 + it.minute }
                            cancelAlarm(context, alarmToEdit); if (updatedAlarm.isEnabled) scheduleAlarm(context, updatedAlarm); editingAlarm = null
                        },
                        onDelete = { val alarmToRemove = alarmToEdit; editingAlarm = null; requestDelete(alarmToRemove) },
                        streakManager = streakManager,
                        onGoToShop = {
                            editingAlarm = null
                            currentSection = AppSection.SHOP
                        }
                    )
                }
                if (showInfoDialog) InfoDialog(onDismiss = { showInfoDialog = false })
                if (showRewardsDialog) LevelRewardsDialog(onDismiss = { showRewardsDialog = false })
                if (showProDialog) {
                    ProPurchaseDialog(
                        onDismiss = { showProDialog = false },
                        onConfirmPurchase = {
                            proManager.unlockPro()
                            showProDialog = false
                            Toast.makeText(context, "¡WakeApp Pro activado!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                if (showAlarmLimitDialog) {
                    AlertDialog(
                        onDismissRequest = { showAlarmLimitDialog = false }, containerColor = CleanSurface,
                        title = { Text("Límite alcanzado", color = CleanText, fontWeight = FontWeight.SemiBold) },
                        text = { Text("Has alcanzado el límite de 4 alarmas. Ve a la Tienda y compra 'Alarmas Infinitas' para crear más.", color = CleanTextDim) },
                        confirmButton = { Button(onClick = { showAlarmLimitDialog = false; currentSection = AppSection.SHOP }, colors = ButtonDefaults.buttonColors(containerColor = CleanText, contentColor = CleanBg)) { Text("Ir a la Tienda", fontWeight = FontWeight.SemiBold) } },
                        dismissButton = { TextButton(onClick = { showAlarmLimitDialog = false }) { Text("Cancelar", color = CleanTextDim) } }
                    )
                }
                alarmPendingDelete?.let { alarm ->
                    val timeFormatted = String.format("%02d:%02d", alarm.hour, alarm.minute)
                    AlertDialog(
                        onDismissRequest = { alarmPendingDelete = null }, containerColor = CleanSurface,
                        title = { Text("Eliminar alarma", color = CleanText, fontWeight = FontWeight.SemiBold) },
                        text = {
                            val base = when (AppLang.language) {
                                "en" -> "Are you sure you want to delete the $timeFormatted alarm"
                                "fr" -> "Voulez-vous vraiment supprimer l'alarme de $timeFormatted"
                                else -> "¿Seguro que quieres eliminar la alarma de las $timeFormatted"
                            }
                            Text(base + (if (!alarm.label.isNullOrBlank()) " (${alarm.label})" else "") + "?", color = CleanTextDim)
                        },
                        confirmButton = { Button(onClick = { performDelete(alarm); alarmPendingDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = WakeRed)) { Text("Eliminar", color = Color.White, fontWeight = FontWeight.SemiBold) } },
                        dismissButton = { TextButton(onClick = { alarmPendingDelete = null }) { Text("Cancelar", color = CleanTextDim) } }
                    )
                }
                rewardDialog?.let { info ->
                    if (info.achievements.isNotEmpty()) AchievementUnlockedOverlay(info = info, onDismiss = { currentLevel = streakManager.getLevel(); currentXp = streakManager.getXp(); points = streakManager.getPoints(); currentStreak = streakManager.getStreak(); rewardDialog = null })
                    else XpRewardDialog(info = info, onDismiss = { rewardDialog = null })
                }
                if (readingMode) Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33FF6600), Color(0x22FF8800)))))
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
    streakManager: StreakManager,
    onGoToShop: (() -> Unit)? = null
) {
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
        Card(
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(22.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Editar Alarma", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(CleanSurfaceVariant).clickable { onDismiss() }, contentAlignment = Alignment.Center) { Text("✕", color = CleanTextDim, fontSize = 14.sp) }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    NumberPicker(value = hour, range = 0..23, onValueChange = { hour = it })
                    Text(":", fontSize = 30.sp, fontWeight = FontWeight.Light, color = CleanTextDim, modifier = Modifier.padding(horizontal = 8.dp))
                    NumberPicker(value = minute, range = 0..59, onValueChange = { minute = it })
                }
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("¿Para qué es esta alarma?") },
                    placeholder = { Text("Trabajo, Instituto, Gym...", color = CleanTextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CleanText, unfocusedBorderColor = CleanSurfaceVariant, focusedTextColor = CleanText, unfocusedTextColor = CleanText, focusedLabelColor = CleanText, unfocusedLabelColor = CleanTextDim, cursorColor = WakeBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text("Minijuego", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ChallengeCatalog.all) { type ->
                        val isSelected = selectedType == type.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = type.id },
                            label = { Text("${type.emoji} ${tr(type.label)}", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText),
                            border = null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Rondas", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items((1..maxRounds).toList()) { r ->
                        FilterChip(
                            selected = rounds == r,
                            onClick = { rounds = r },
                            label = { Text("$r", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText),
                            border = null
                        )
                    }
                    if (!hasExtraRounds) {
                        item(key = "more_rounds_lock") {
                            FilterChip(
                                selected = false,
                                onClick = { onGoToShop?.invoke() },
                                label = { Text("+4 🔒", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = CleanSurfaceVariant.copy(alpha = 0.7f),
                                    labelColor = CleanTextDim
                                ),
                                border = null
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Dificultad", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(listOf("EASY", "MEDIUM", "HARD", "EXTREME")) { diff ->
                        FilterChip(
                            selected = selectedDifficulty == diff,
                            onClick = { selectedDifficulty = diff },
                            label = { Text(diff, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText),
                            border = null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Color", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(10.dp))
                ColorPickerRow(
                    selectedId = color,
                    allUnlocked = streakManager.hasExtraColors(),
                    circleSize = 36,
                    onSelect = { color = it },
                    onLockedClick = { onGoToShop?.invoke() }
                )
                Spacer(modifier = Modifier.height(26.dp))
                Button(
                    onClick = { val finalTypes = List(rounds) { selectedType }; onSave(alarm.copy(hour = hour, minute = minute, colorHex = color, challengeTypes = finalTypes, difficulty = selectedDifficulty, rounds = finalTypes.size, label = label.trim().ifBlank { null })) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanText, contentColor = CleanBg),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Guardar Cambios", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onDelete() }, modifier = Modifier.fillMaxWidth().height(46.dp)) { Text("Eliminar Alarma", color = WakeRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
fun LevelRewardsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CleanSurface,
        title = { Text("Recompensas de Nivel", color = CleanText, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text("Sube de nivel completando desafíos para ganar recompensas directas. Las monedas (🪙) solo se obtienen al desbloquear Logros:", fontSize = 13.5.sp, color = CleanTextDim, lineHeight = 19.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CleanSurfaceVariant).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚡", fontSize = 20.sp); Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Puntos de Energía", color = CleanText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Gana +50 🪙 por cada nivel alcanzado.", color = CleanTextDim, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido", color = CleanText, fontWeight = FontWeight.SemiBold) } }
    )
}

@Composable
fun XpRewardDialog(info: RewardInfo, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { /* bloquea cierre */ }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CleanSurface)
                .border(1.dp, CleanSurfaceVariant, RoundedCornerShape(22.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = if (info.levelUp) "🎉" else "⭐", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (info.levelUp) "NIVEL ALCANZADO" else "DESAFÍO COMPLETADO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CleanTextDim,
                    letterSpacing = 1.6.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("+${info.xpGained} XP", fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                Spacer(modifier = Modifier.height(26.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanText, contentColor = CleanBg),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Continuar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
            }
        }
    }
}

@Composable
fun WakeBottomBar(current: AppSection, onSelect: (AppSection) -> Unit) {
    Surface(color = Color.Transparent, tonalElevation = 0.dp, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppSection.entries.forEach { section ->
                val selected = section == current
                val color = if (selected) CleanText else CleanTextDim
                val stroke = if (selected) 2.2.dp else 1.7.dp
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { onSelect(section) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
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
                                val palettePath = Path().apply {
                                    moveTo(w * 0.56f, h * 0.86f)
                                    cubicTo(w * 0.16f, h * 0.88f, w * 0.08f, h * 0.56f, w * 0.22f, h * 0.30f)
                                    cubicTo(w * 0.36f, h * 0.06f, w * 0.82f, h * 0.08f, w * 0.88f, h * 0.38f)
                                    cubicTo(w * 0.90f, h * 0.50f, w * 0.78f, h * 0.54f, w * 0.70f, h * 0.50f)
                                    cubicTo(w * 0.60f, h * 0.46f, w * 0.54f, h * 0.58f, w * 0.62f, h * 0.70f)
                                    cubicTo(w * 0.66f, h * 0.78f, w * 0.62f, h * 0.85f, w * 0.56f, h * 0.86f)
                                    close()
                                }
                                drawPath(palettePath, color = color, style = Stroke(width = s, join = StrokeJoin.Round))
                                drawCircle(color = color, radius = w * 0.068f, center = Offset(w * 0.36f, h * 0.32f))
                                drawCircle(color = color, radius = w * 0.068f, center = Offset(w * 0.28f, h * 0.52f))
                                drawCircle(color = color, radius = w * 0.068f, center = Offset(w * 0.56f, h * 0.26f))
                                drawLine(color = color, start = Offset(w * 0.78f, h * 0.85f), end = Offset(w * 0.50f, h * 0.57f), strokeWidth = s * 1.2f, cap = StrokeCap.Round)
                                drawLine(color = color, start = Offset(w * 0.50f, h * 0.57f), end = Offset(w * 0.41f, h * 0.48f), strokeWidth = s * 1.9f, cap = StrokeCap.Round)
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
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("⏰", fontSize = 52.sp, color = CleanTextDim.copy(alpha = 0.45f))
    }
}

@Composable
fun AlarmCard(alarm: AlarmItem, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val accentColor = getAlarmColor(alarm.colorHex, WakeBlue)
    val timeFormatted = String.format("%02d:%02d", alarm.hour, alarm.minute)
    val typeLabel = remember(alarm.challengeTypes, AppLang.language) { challengeTitle(alarm.challengeTypes.firstOrNull() ?: "MATH") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CleanSurfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CleanSurface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(if (alarm.isEnabled) accentColor else CleanSurfaceVariant))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timeFormatted,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Light,
                        color = if (alarm.isEnabled) CleanText else CleanTextDim,
                        letterSpacing = (-1).sp
                    )
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
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CleanBg,
                            checkedTrackColor = CleanText,
                            uncheckedThumbColor = CleanTextDim,
                            uncheckedTrackColor = CleanSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onDelete() }, contentAlignment = Alignment.Center) { Text("🗑", fontSize = 16.sp) }
                }
            }
        }
    }
}

private enum class AchRarity(
    val label: String,
    val emoji: String,
    val color: Color,
    val glow: Color
) {
    COMMON("Común", "⚪", Color(0xFF9EA7B0), Color(0xFFB8C0C8)),
    RARE("Raro", "🔵", Color(0xFF6B8EAE), Color(0xFF8AA8C4)),
    EPIC("Épico", "🟣", Color(0xFF8B739E), Color(0xFFA790BA)),
    LEGENDARY("Legendario", "🟡", Color(0xFFB5945B), Color(0xFFCCB07C))
}

private fun rarityOf(required: Int): AchRarity = when {
    required >= 100 -> AchRarity.LEGENDARY
    required >= 30  -> AchRarity.EPIC
    required >= 10  -> AchRarity.RARE
    else            -> AchRarity.COMMON
}

@Composable
fun AchievementIngot(
    achievement: Achievement,
    unlocked: Boolean,
    progress: Int
) {
    val isActuallyUnlocked = unlocked || (progress >= achievement.required)
    val rarity = rarityOf(achievement.required)
    val accent = rarity.color

    val progressFraction = (progress.toFloat() / achievement.required.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = if (isActuallyUnlocked) 1f else progressFraction,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "progress_${achievement.id}"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActuallyUnlocked) CleanSurface else CleanSurface.copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                color = if (isActuallyUnlocked) accent.copy(alpha = 0.45f) else CleanSurfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActuallyUnlocked) accent.copy(alpha = 0.14f) else CleanSurfaceVariant)
                    .border(
                        width = 1.dp,
                        color = if (isActuallyUnlocked) accent.copy(alpha = 0.35f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isActuallyUnlocked) achievement.emoji else "🔒",
                    fontSize = if (isActuallyUnlocked) 22.sp else 17.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = achievement.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActuallyUnlocked) CleanText else CleanTextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActuallyUnlocked) accent.copy(alpha = 0.14f) else CleanSurfaceVariant)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${rarity.emoji} ${rarity.label}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isActuallyUnlocked) accent else CleanTextDim
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = achievement.description,
                    fontSize = 12.5.sp,
                    color = CleanTextDim,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isActuallyUnlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✓", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Completado", color = accent, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(CleanSurfaceVariant)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(CleanText.copy(alpha = 0.65f))
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "$progress/${achievement.required}", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = CleanTextDim)
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
    val unlockedCount = remember(streak, points) {
        AchievementCatalog.all.count { achievementManager.isUnlocked(it.id) }
    }
    val unlockedFraction = if (totalAch > 0) unlockedCount.toFloat() / totalAch else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Tu Progreso", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = OnBgText, letterSpacing = (-0.5).sp)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, CleanSurfaceVariant, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    ProfileStatRow("Nivel", "$level")
                    HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))
                    ProfileStatRow("XP", "$xp / $xpNeeded")
                    HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))
                    ProfileStatRow("Días Distintos", "$streak ${tr("días")}")
                    HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))
                    ProfileStatRow("Puntos de Energía", "$points")
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CleanSurface)
                    .border(1.dp, CleanSurfaceVariant, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(CleanSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) { Text("🏆", fontSize = 18.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("LOGROS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanTextDim, letterSpacing = 1.6.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$unlockedCount de $totalAch desbloqueados", fontSize = 13.sp, color = CleanText)
                        }
                        Text("${(unlockedFraction * 100).toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(CleanSurfaceVariant)) {
                        Box(
                            modifier = Modifier.fillMaxWidth(unlockedFraction).fillMaxHeight().clip(RoundedCornerShape(3.dp))
                                .background(CleanText)
                        )
                    }
                }
            }
        }

        items(AchievementCatalog.all, key = { it.id }) { ach ->
            val unlocked = achievementManager.isUnlocked(ach.id)
            val progress = achievementManager.getProgress(ach, streak, points)
            AchievementIngot(achievement = ach, unlocked = unlocked, progress = progress)
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
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CleanSurface,
        title = { Text("¿Qué es WakeApp?", color = CleanText, fontWeight = FontWeight.SemiBold) },
        text = { Column { Text("WakeApp es tu despertador inteligente. En lugar de un simple botón, cada alarma te lanza desafíos mentales o físicos que debes superar para desactivarla.", fontSize = 14.sp, color = CleanTextDim, lineHeight = 20.sp) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido", color = CleanText, fontWeight = FontWeight.SemiBold) } }
    )
}

@Composable
fun CustomTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, String?) -> Unit,
    selectedChallengeType: String,
    onChallengeTypeSelected: (String) -> Unit,
    selectedDifficulty: String,
    onDifficultySelected: (String) -> Unit,
    selectedRounds: Int,
    onRoundsChange: (Int) -> Unit,
    soundName: String?,
    onPickSound: () -> Unit,
    onClearSound: () -> Unit,
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    streakManager: StreakManager,
    onGoToShop: (() -> Unit)? = null
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
                    NumberPicker(value = selectedHour, range = 0..23, onValueChange = { selectedHour = it })
                    Text(":", fontSize = 30.sp, fontWeight = FontWeight.Light, color = CleanTextDim, modifier = Modifier.padding(horizontal = 8.dp))
                    NumberPicker(value = selectedMinute, range = 0..59, step = 1, onValueChange = { selectedMinute = it })
                }
                Spacer(modifier = Modifier.height(18.dp))
                OutlinedTextField(
                    value = alarmLabel,
                    onValueChange = { alarmLabel = it },
                    label = { Text("¿Para qué es esta alarma?") },
                    placeholder = { Text("Trabajo, Instituto, Gym...", color = CleanTextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CleanText, unfocusedBorderColor = CleanSurfaceVariant, focusedTextColor = CleanText, unfocusedTextColor = CleanText, focusedLabelColor = CleanText, unfocusedLabelColor = CleanTextDim, cursorColor = WakeBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                Text("Minijuego", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ChallengeCatalog.all) { type ->
                        val isSelected = selectedChallengeType == type.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onChallengeTypeSelected(type.id) },
                            label = { Text("${type.emoji} ${tr(type.label)}", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Rondas", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items((1..maxRounds).toList()) { r ->
                        FilterChip(
                            selected = selectedRounds == r,
                            onClick = { onRoundsChange(r) },
                            label = { Text("$r", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText),
                            border = null
                        )
                    }
                    if (!hasExtraRounds) {
                        item(key = "more_rounds_lock") {
                            FilterChip(
                                selected = false,
                                onClick = { onGoToShop?.invoke() },
                                label = { Text("+4 🔒", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = CleanSurfaceVariant.copy(alpha = 0.7f),
                                    labelColor = CleanTextDim
                                ),
                                border = null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Dificultad", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(listOf("EASY", "MEDIUM", "HARD", "EXTREME")) { diff ->
                        FilterChip(
                            selected = selectedDifficulty == diff,
                            onClick = { onDifficultySelected(diff) },
                            label = { Text(diff, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CleanText, selectedLabelColor = CleanBg, containerColor = CleanSurfaceVariant, labelColor = CleanText),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Color de la Alarma", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(10.dp))
                ColorPickerRow(
                    selectedId = selectedColor,
                    allUnlocked = streakManager.hasExtraColors(),
                    circleSize = 38,
                    onSelect = onColorSelected,
                    onLockedClick = { onGoToShop?.invoke() }
                )

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
            if (centeredIndex != null && centeredIndex in items.indices) {
                val newValue = items[centeredIndex]
                if (newValue != value) onValueChange(newValue)
            }
        }
    }
    Box(modifier = Modifier.height(boxHeight).width(80.dp), contentAlignment = Alignment.Center) {
        LazyColumn(state = listState, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = verticalPadding), flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)) {
            items(items) { item ->
                val isSelected = item == value
                Box(modifier = Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = String.format("%02d", item), fontSize = if (isSelected) 32.sp else 20.sp, fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Light, color = if (isSelected) CleanText else CleanTextDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
            if (resId != 0) { val mediaPlayer = MediaPlayer.create(context, resId); mediaPlayer.start(); mediaPlayer.setOnCompletionListener { it.release() } }
        } catch (_: Exception) {}
    }
    var queue by remember { mutableStateOf(info.achievements.toList()) }
    val current = queue.firstOrNull()
    if (current == null) { LaunchedEffect(Unit) { onDismiss() }; return }
    Dialog(onDismissRequest = { /* bloquea cierre */ }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CleanSurface)
                .border(1.dp, CleanSurfaceVariant, RoundedCornerShape(22.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = current.emoji, fontSize = 52.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "LOGRO DESBLOQUEADO", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanTextDim, letterSpacing = 1.6.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = current.title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = CleanText, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
                Text("+1 NIVEL", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = CleanText)
                Spacer(modifier = Modifier.height(6.dp))
                Text("+${current.rewardPoints} ${tr("PUNTOS")}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = CleanTextDim)
                Spacer(modifier = Modifier.height(26.dp))
                Button(
                    onClick = { if (queue.size > 1) { queue = queue.drop(1) } else { onDismiss() } },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanText, contentColor = CleanBg),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(if (queue.size > 1) "Siguiente" else "Continuar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
            }
        }
    }
}

fun scheduleAlarm(context: Context, alarm: AlarmItem) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val types = if (alarm.challengeTypes.isNotEmpty()) alarm.challengeTypes else listOf("MATH")
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
    } catch (e: SecurityException) { e.printStackTrace(); try { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent) } catch (_: Exception) {} }
}

fun cancelAlarm(context: Context, alarm: AlarmItem) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, alarm.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    alarmManager.cancel(pendingIntent)
}

// ============================================================
// ==============  TIENDA LIMPIA · MEJORAS + AVATARES + TEMAS ==
// ============================================================

private data class JpgAvatarShopItem(
    val id: String,
    val resName: String,
    val title: String,
    val price: Int,
    val accent: Color
)

private val JpgAvatarCatalog = listOf(
    JpgAvatarShopItem("avatar_0", "avatar0", "Por defecto", 0, Color(0xFF90A4AE)),
    JpgAvatarShopItem("avatar_1", "avatar1", "Caballero", 250, Color(0xFF42A5F5)),
    JpgAvatarShopItem("avatar_2", "avatar2", "Eclipse", 300, Color(0xFF26C6DA)),
    JpgAvatarShopItem("avatar_3", "avatar3", "Británico gordo", 350, Color(0xFF66BB6A)),
    JpgAvatarShopItem("avatar_4", "avatar4", "Julius", 400, Color(0xFFFFCA28)),
    JpgAvatarShopItem("avatar_5", "avatar5", "Avatar 5", 450, Color(0xFFFF7043)),
    JpgAvatarShopItem("avatar_6", "avatar6", "Avatar 6", 500, Color(0xFFAB47BC)),
    JpgAvatarShopItem("avatar_7", "avatar7", "Avatar 7", 550, Color(0xFFEC407A)),
    JpgAvatarShopItem("avatar_8", "avatar8", "Avatar 8", 600, Color(0xFF5C6BC0)),
    JpgAvatarShopItem("avatar_9", "avatar9", "Avatar 9", 700, Color(0xFF29B6F6)),
    JpgAvatarShopItem("avatar_10", "avatar10", "Avatar 10", 800, Color(0xFFFFA726))
)

@Composable
fun ShopSection(
    points: Int,
    streakManager: StreakManager,
    profileManager: ProfileManager,
    isLoadingAd: Boolean,
    onWatchRewardedAd: () -> Unit,
    onBuy: (Int) -> Unit
) {
    var hasInfinite by remember { mutableStateOf(streakManager.hasInfiniteAlarms()) }
    var hasColors by remember { mutableStateOf(streakManager.hasExtraColors()) }
    var hasRounds by remember { mutableStateOf(streakManager.hasExtraRounds()) }

    val shopContext = LocalContext.current
    val avatarPrefs = remember {
        shopContext.getSharedPreferences("wakeapp_jpg_avatars", Context.MODE_PRIVATE)
    }
    val ownedAvatars = remember {
        mutableStateMapOf<String, Boolean>().apply {
            JpgAvatarCatalog.forEach { item ->
                val defaultOwned = item.id == "avatar_0"
                put(item.id, avatarPrefs.getBoolean("unlocked_${item.id}", defaultOwned))
            }
        }
    }
    var equippedAvatarId by remember {
        mutableStateOf(avatarPrefs.getString("equipped_avatar", "avatar_0") ?: "avatar_0")
    }

    val themeManager = remember { ThemeManager(shopContext) }
    val ownedThemes = remember {
        mutableStateMapOf<String, Boolean>().apply {
            ThemeCatalog.purchasable.forEach { put(it.id, themeManager.isUnlocked(it.id)) }
        }
    }

    var selectedFilter by remember { mutableStateOf("TODO") }

    val unlockedUpgradesCount = listOf(hasInfinite, hasColors, hasRounds).count { it }
    val unlockedAvatarsCount = ownedAvatars.values.count { it }
    val unlockedThemesCount = ownedThemes.values.count { it }

    val totalUnlockableCount = 3 + JpgAvatarCatalog.size + ThemeCatalog.purchasable.size
    val totalUnlockedCount = unlockedUpgradesCount + unlockedAvatarsCount + unlockedThemesCount
    val collectionRatio = if (totalUnlockableCount > 0) {
        (totalUnlockedCount.toFloat() / totalUnlockableCount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 76.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ShopHero(
                points = points,
                unlockedCount = totalUnlockedCount,
                totalCount = totalUnlockableCount,
                collectionRatio = collectionRatio,
                isLoadingAd = isLoadingAd,
                onWatchRewardedAd = onWatchRewardedAd
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("TODO", "MEJORAS", "AVATARES", "TEMAS")) { tab ->
                    val active = selectedFilter == tab
                    FilterChip(
                        selected = active,
                        onClick = { selectedFilter = tab },
                        label = {
                            Text(
                                text = tab,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WakeBlue,
                            selectedLabelColor = CleanText,
                            containerColor = CleanSurface,
                            labelColor = CleanTextDim
                        ),
                        border = null
                    )
                }
            }
        }

        if (selectedFilter == "TODO" || selectedFilter == "MEJORAS") {
            item {
                ShopSectionHeader(
                    title = "MEJORAS DE SISTEMA",
                    subtitle = "Ventajas permanentes para tus alarmas",
                    badge = "$unlockedUpgradesCount/3",
                    emoji = "⚡"
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ShopCard(
                        emoji = "♾️",
                        tag = "ILIMITADO",
                        title = "Alarmas Infinitas",
                        description = "Elimina el límite de 4 alarmas y crea tantas como necesites.",
                        price = 1500,
                        owned = hasInfinite,
                        affordable = points >= 1500,
                        accent = Color(0xFF9C7CF4)
                    ) {
                        if (!hasInfinite && points >= 1500) {
                            streakManager.unlockInfiniteAlarms()
                            hasInfinite = true
                            onBuy(1500)
                        }
                    }

                    ShopCard(
                        emoji = "🎨",
                        tag = "CROMÁTICA",
                        title = "Nuevos Marcadores",
                        description = "Desbloquea toda la paleta de colores para diferenciar tus alarmas.",
                        price = 1200,
                        owned = hasColors,
                        affordable = points >= 1200,
                        accent = Color(0xFFFF5E8A)
                    ) {
                        if (!hasColors && points >= 1200) {
                            streakManager.unlockExtraColors()
                            hasColors = true
                            onBuy(1200)
                        }
                    }

                    ShopCard(
                        emoji = "🔄",
                        tag = "RONDAS",
                        title = "Más Asaltos",
                        description = "Amplía el máximo de rondas por alarma hasta 10 desafíos.",
                        price = 1500,
                        owned = hasRounds,
                        affordable = points >= 1500,
                        accent = Color(0xFF26C6DA)
                    ) {
                        if (!hasRounds && points >= 1500) {
                            streakManager.unlockExtraRounds()
                            hasRounds = true
                            onBuy(1500)
                        }
                    }
                }
            }
        }

        if (selectedFilter == "TODO" || selectedFilter == "AVATARES") {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ShopSectionHeader(
                    title = "AVATARES",
                    subtitle = "Elige tu avatar para el perfil",
                    badge = "$unlockedAvatarsCount/${JpgAvatarCatalog.size}",
                    emoji = "🖼️"
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(JpgAvatarCatalog, key = { it.id }) { av ->
                        val isOwned = ownedAvatars[av.id] == true || av.id == "avatar_0"
                        val isEquipped = equippedAvatarId == av.id
                        val canAfford = points >= av.price
                        val resId = remember(av.resName) {
                            shopContext.resources.getIdentifier(av.resName, "drawable", shopContext.packageName)
                        }

                        Box(
                            modifier = Modifier
                                .width(156.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(CleanSurface)
                                .border(
                                    width = 1.5.dp,
                                    color = when {
                                        isEquipped -> WakeBlue
                                        isOwned -> av.accent.copy(alpha = 0.65f)
                                        else -> CleanSurfaceVariant
                                    },
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(92.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(av.accent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (resId != 0) {
                                        Image(
                                            painter = painterResource(id = resId),
                                            contentDescription = av.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(74.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, CleanText.copy(alpha = 0.35f), CircleShape)
                                        )
                                    } else {
                                        Text("🖼️", fontSize = 30.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = av.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                when {
                                    isEquipped -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(WakeBlue.copy(alpha = 0.2f))
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("✓ Equipado", color = WakeBlue, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    isOwned -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(CleanSurfaceVariant)
                                                .clickable {
                                                    equippedAvatarId = av.id
                                                    avatarPrefs.edit().putString("equipped_avatar", av.id).apply()
                                                }
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Equipar", color = CleanText, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    else -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🪙 ${av.price}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = CleanText)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (canAfford) av.accent else CleanSurfaceVariant)
                                                    .clickable(enabled = canAfford) {
                                                        avatarPrefs.edit().putBoolean("unlocked_${av.id}", true).apply()
                                                        ownedAvatars[av.id] = true
                                                        equippedAvatarId = av.id
                                                        avatarPrefs.edit().putString("equipped_avatar", av.id).apply()
                                                        onBuy(av.price)
                                                    }
                                                    .padding(horizontal = 11.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    if (canAfford) "Comprar" else "🔒",
                                                    color = if (canAfford) Color.White else CleanTextDim,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedFilter == "TODO" || selectedFilter == "TEMAS") {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ShopSectionHeader(
                    title = "TEMAS VISUALES",
                    subtitle = "Cambia el acento de la app desde Personalización",
                    badge = "$unlockedThemesCount/${ThemeCatalog.purchasable.size}",
                    emoji = "🌈"
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(ThemeCatalog.purchasable, key = { it.id }) { theme ->
                        ThemePreviewCard(
                            theme = theme,
                            owned = ownedThemes[theme.id] == true,
                            affordable = points >= theme.price,
                            onBuy = {
                                if (ownedThemes[theme.id] != true && points >= theme.price) {
                                    themeManager.unlock(theme.id)
                                    ownedThemes[theme.id] = true
                                    onBuy(theme.price)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopHero(
    points: Int,
    unlockedCount: Int,
    totalCount: Int,
    collectionRatio: Float,
    isLoadingAd: Boolean,
    onWatchRewardedAd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        CleanSurface,
                        WakeBlue.copy(alpha = 0.16f),
                        CleanSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            )
            .border(1.5.dp, WakeBlue.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "BILLETERA Y TIENDA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextDim,
                    letterSpacing = 1.8.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CleanBg.copy(alpha = 0.55f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Colección $unlockedCount/$totalCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CleanText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WakeBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🪙", fontSize = 26.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "$points",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CleanText,
                    letterSpacing = (-1.5).sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "monedas",
                    fontSize = 14.sp,
                    color = CleanTextDim,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(CleanSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(collectionRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(WakeBlue)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onWatchRewardedAd,
                enabled = !isLoadingAd,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WakeBlue,
                    contentColor = CleanText,
                    disabledContainerColor = CleanSurfaceVariant,
                    disabledContentColor = CleanTextDim
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (isLoadingAd) "⏳ Cargando anuncio..." else "🎬  Ver anuncio recompensado (+50 🪙)",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ShopSectionHeader(
    title: String,
    subtitle: String,
    badge: String? = null,
    emoji: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CleanSurface)
                .border(1.dp, CleanSurfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanText,
                    letterSpacing = 1.4.sp
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(CleanSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            badge,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanText
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = CleanTextDim, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun ShopCard(
    emoji: String,
    tag: String,
    title: String,
    description: String,
    price: Int,
    owned: Boolean,
    affordable: Boolean,
    accent: Color,
    onBuy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = if (owned) 0.16f else 0.08f),
                        CleanSurface
                    )
                )
            )
            .border(
                width = 1.5.dp,
                color = if (owned) accent.copy(alpha = 0.65f) else CleanSurfaceVariant,
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(accent.copy(alpha = 0.18f))
                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(emoji, fontSize = 23.sp) }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accent.copy(alpha = 0.16f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CleanText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                description,
                fontSize = 13.sp,
                color = CleanTextDim,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.65f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (owned) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.16f))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✓", color = accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Activo en tu cuenta", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CleanSurfaceVariant.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("🪙", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("$price", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CleanText)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (affordable) accent else CleanSurfaceVariant)
                            .clickable(enabled = affordable) { onBuy() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            if (affordable) "Desbloquear" else "Faltan 🪙",
                            color = if (affordable) Color.White else CleanTextDim,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    theme: AppTheme,
    owned: Boolean,
    affordable: Boolean,
    onBuy: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(182.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CleanSurface)
            .border(
                width = 1.5.dp,
                color = if (owned) theme.primary.copy(alpha = 0.65f) else CleanSurfaceVariant,
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                theme.primary,
                                theme.primary.copy(alpha = 0.45f),
                                CleanBg.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(11.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("TEMA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Column {
                        Box(modifier = Modifier.fillMaxWidth(0.75f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.7f)))
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.45f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.4f)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(11.dp))
            Text(theme.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CleanText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(11.dp))

            if (owned) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.primary.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("✓", color = theme.primary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Desbloqueado", color = theme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${theme.price}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CleanText)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (affordable) theme.primary else CleanSurfaceVariant)
                            .clickable(enabled = affordable) { onBuy() }
                            .padding(horizontal = 13.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (affordable) "Comprar" else "🔒",
                            color = if (affordable) Color.White else CleanTextDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}