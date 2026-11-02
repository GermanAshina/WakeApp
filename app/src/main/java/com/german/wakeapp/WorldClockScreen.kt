package com.german.wakeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.german.wakeapp.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class WorldCity(
    val name: String,
    val country: String,
    val flag: String,
    val timeZoneId: String
)

object WorldCityCatalog {
    val all = listOf(
        WorldCity("Madrid", "España", "🇪🇸", "Europe/Madrid"),
        WorldCity("Londres", "Reino Unido", "🇬🇧", "Europe/London"),
        WorldCity("París", "Francia", "🇫🇷", "Europe/Paris"),
        WorldCity("Berlín", "Alemania", "🇩🇪", "Europe/Berlin"),
        WorldCity("Roma", "Italia", "🇮🇹", "Europe/Rome"),
        WorldCity("Moscú", "Rusia", "🇷🇺", "Europe/Moscow"),
        WorldCity("Nueva York", "EE.UU.", "🇺🇸", "America/New_York"),
        WorldCity("Los Ángeles", "EE.UU.", "🇺🇸", "America/Los_Angeles"),
        WorldCity("Ciudad de México", "México", "🇲🇽", "America/Mexico_City"),
        WorldCity("Bogotá", "Colombia", "🇨🇴", "America/Bogota"),
        WorldCity("Buenos Aires", "Argentina", "🇦🇷", "America/Argentina/Buenos_Aires"),
        WorldCity("São Paulo", "Brasil", "🇧🇷", "America/Sao_Paulo"),
        WorldCity("Santiago", "Chile", "🇨🇱", "America/Santiago"),
        WorldCity("Tokio", "Japón", "🇯🇵", "Asia/Tokyo"),
        WorldCity("Pekín", "China", "🇨🇳", "Asia/Shanghai"),
        WorldCity("Seúl", "Corea del Sur", "🇰🇷", "Asia/Seoul"),
        WorldCity("Bangkok", "Tailandia", "🇹🇭", "Asia/Bangkok"),
        WorldCity("Nueva Delhi", "India", "🇮🇳", "Asia/Kolkata"),
        WorldCity("Dubái", "EAU", "🇦🇪", "Asia/Dubai"),
        WorldCity("Estambul", "Turquía", "🇹🇷", "Europe/Istanbul"),
        WorldCity("El Cairo", "Egipto", "🇪🇬", "Africa/Cairo"),
        WorldCity("Johannesburgo", "Sudáfrica", "🇿🇦", "Africa/Johannesburg"),
        WorldCity("Lagos", "Nigeria", "🇳🇬", "Africa/Lagos"),
        WorldCity("Sídney", "Australia", "🇦🇺", "Australia/Sydney"),
        WorldCity("Auckland", "Nueva Zelanda", "🇳🇿", "Pacific/Auckland"),
        WorldCity("Honolulú", "EE.UU.", "🇺🇸", "Pacific/Honolulu"),
        WorldCity("Anchorage", "EE.UU.", "🇺🇸", "America/Anchorage"),
        WorldCity("Reykjavik", "Islandia", "🇮🇸", "Atlantic/Reykjavik"),
        WorldCity("Lisboa", "Portugal", "🇵🇹", "Europe/Lisbon"),
        WorldCity("Ámsterdam", "Países Bajos", "🇳🇱", "Europe/Amsterdam")
    )
}

@Composable
fun WorldClockSection() {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }

    val localZone = remember { TimeZone.getDefault() }
    val localFormatter = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = localZone }
    }
    val localSecondsFormatter = remember {
        SimpleDateFormat("ss", Locale.getDefault()).apply { timeZone = localZone }
    }
    val localDateFormatter = remember {
        SimpleDateFormat("EEE d MMM", Locale("es", "ES")).apply { timeZone = localZone }
    }
    val zoneName = remember {
        val tz = TimeZone.getDefault()
        tz.getDisplayName(tz.inDaylightTime(Date()), TimeZone.SHORT)
    }
    val date = Date(now)
    val localTime = localFormatter.format(date)
    val localSeconds = localSecondsFormatter.format(date)
    val localDate = localDateFormatter.format(date)

    // Todo dentro de la misma LazyColumn -> un solo bloque deslizante
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
    ) {
        // 1. Título
        item {
            Text(
                "HORAS DEL MUNDO",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = AmberAccent,
                letterSpacing = 2.sp
            )
        }

        // 2. Círculo central
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .shadow(24.dp, CircleShape, ambientColor = RedAccent, spotColor = RedAccent)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(CardBackground, SurfaceDark)
                            )
                        )
                        .border(2.dp, RedAccent.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🇪🇸", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = localTime,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = ":$localSeconds",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberAccent,
                                modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "España · $zoneName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = localDate,
                            fontSize = 11.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 3. Nota zona horaria
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🌍 Todas las horas están en tu zona horaria ($zoneName)",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 4. Espacio antes de la lista de ciudades
        item { Spacer(modifier = Modifier.height(10.dp)) }

        // 5. Ciudades
        items(WorldCityCatalog.all, key = { it.timeZoneId }) { city ->
            WorldClockRow(city, now)
        }
    }
}

@Composable
private fun WorldClockRow(city: WorldCity, now: Long) {
    val tz = remember(city.timeZoneId) { TimeZone.getTimeZone(city.timeZoneId) }
    val formatter = remember(city.timeZoneId) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = tz }
    }
    val secondsFormatter = remember(city.timeZoneId) {
        SimpleDateFormat("ss", Locale.getDefault()).apply { timeZone = tz }
    }
    val dateFormatter = remember(city.timeZoneId) {
        SimpleDateFormat("EEE d MMM", Locale("es", "ES")).apply { timeZone = tz }
    }

    val date = Date(now)
    val timeStr = formatter.format(date)
    val secStr = secondsFormatter.format(date)
    val dateStr = dateFormatter.format(date)

    val hour = SimpleDateFormat("H", Locale.getDefault()).apply { timeZone = tz }
        .format(date).toIntOrNull() ?: 0

    val isNight = hour in 0..6 || hour >= 21
    val accent = when {
        hour in 6..9 -> AmberAccent
        hour in 10..19 -> SuccessGreen
        hour in 20..21 -> Color(0xFFE08E3C)
        else -> Color(0xFF7A88C9)
    }

    val emoji = when {
        hour in 6..8 -> "🌅"
        hour in 9..18 -> "☀️"
        hour in 19..20 -> "🌇"
        else -> "🌙"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground.copy(alpha = 0.8f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(city.flag, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                city.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                "${city.country} · $dateStr",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                timeStr,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = if (isNight) TextWhite.copy(alpha = 0.85f) else accent,
                letterSpacing = (-1).sp
            )
            Text(
                ":$secStr",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accent.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(emoji, fontSize = 18.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}