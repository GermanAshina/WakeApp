package com.german.wakeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StreakCalendarDialog(
    streak: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val streakManager = remember { StreakManager(context) }
    val wakeHistory = remember { streakManager.getWakeHistory() }

    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_MONTH)
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    // Filtrar los días del historial que pertenecen a este mes y año
    val wakeDays = remember(wakeHistory) {
        val days = mutableListOf<Int>()
        wakeHistory.forEach { key ->
            // La clave es: year * 1000 + dayOfYear
            val year = (key / 1000).toInt()
            val dayOfYear = (key % 1000).toInt()

            if (year == currentYear) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.DAY_OF_YEAR, dayOfYear)
                }
                if (cal.get(Calendar.MONTH) == currentMonth) {
                    days.add(cal.get(Calendar.DAY_OF_MONTH))
                }
            }
        }
        days
    }

    val firstDayOfMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }.get(Calendar.DAY_OF_WEEK)

    val emptyDays = if (firstDayOfMonth == Calendar.SUNDAY) 6 else firstDayOfMonth - 2
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Cabecera con el mes y los días totales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(calendar.time).replaceFirstChar { it.uppercase() },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanText
                    )

                    Text(
                        text = "$streak días",
                        color = WakeBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                        Text(day, color = CleanTextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emptyDays) { Box(modifier = Modifier.size(32.dp)) }
                    items(daysInMonth) { index ->
                        val day = index + 1
                        val isAwake = wakeDays.contains(day)
                        val isToday = day == today

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isAwake -> WakeBlue
                                        isToday -> CleanSurfaceVariant
                                        else -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                color = when {
                                    isAwake -> CleanText
                                    isToday -> CleanText
                                    else -> CleanTextDim
                                },
                                fontSize = 14.sp,
                                fontWeight = if (isAwake || isToday) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanSurfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Cerrar", color = CleanText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}