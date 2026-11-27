package com.german.wakeapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ProfileScreen(
    profileManager: ProfileManager,
    streakManager: StreakManager,
    achievementManager: AchievementManager,
    points: Int,
    level: Int,
    xp: Int,
    streak: Int,
    onClose: () -> Unit,
    onSpendPoints: (Int) -> Unit
) {
    var customizingCategory by remember { mutableStateOf<ProfileCategory?>(null) }
    var showCalendar by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf(false) }

    val totalWakeUps = profileManager.getTotalWakeUps()
    val bestStreak = profileManager.getBestStreak()
    val alarmsCreated = profileManager.getAlarmsCreated()
    val unlockedProfile = profileManager.getProfileUnlockedCount()
    val totalProfile = profileManager.getProfileTotalCount()

    androidx.activity.compose.BackHandler(enabled = true) { onClose() }

    OnboardingDarkBlueBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // ============ CABECERA ============
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(CleanSurface).border(1.dp, CleanSurfaceVariant, CircleShape).clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) { Text("←", color = CleanText, fontSize = 20.sp) }
                    Text("Perfil", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CleanText)
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }

            // ============ AVATAR + NOMBRE ============
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileAvatar(profileManager = profileManager, size = 96, onClick = { customizingCategory = ProfileCategory.AVATAR })
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profileManager.getName().ifBlank { "WakeApp User" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanText
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(CleanSurfaceVariant).clickable { editingName = true },
                            contentAlignment = Alignment.Center
                        ) { Text("✎", fontSize = 12.sp, color = CleanText) }
                    }
                }
            }

            // ============ ESTADÍSTICAS SIMPLES ============
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("ESTADÍSTICAS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CleanTextDim, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        ProfileRow("Días despertado", "$totalWakeUps")
                        ProfileRow("Mejor racha", "$bestStreak")
                        ProfileRow("Nivel actual", "$level")
                        ProfileRow("Puntos acumulados", "$points")
                        ProfileRow("Alarmas creadas", "$alarmsCreated")
                    }
                }
            }

            // ============ PERSONALIZAR ============
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("PERSONALIZAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CleanTextDim, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    val cat = ProfileCategory.AVATAR
                    val equippedItem = ProfileCatalog.byId(profileManager.getEquipped(cat))
                    ProfileCategoryRow(cat, equippedItem?.name ?: "-", unlockedProfile, totalProfile) { customizingCategory = cat }
                }
            }

            // ============ ACCIONES ============
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showCalendar = true }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CleanSurface), shape = RoundedCornerShape(14.dp)) {
                        Text("📅  Ver calendario", color = CleanText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = WakeBlue), shape = RoundedCornerShape(14.dp)) {
                        Text("Cerrar", color = CleanText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (customizingCategory != null) {
        ProfileCustomizationSheet(
            category = customizingCategory!!,
            profileManager = profileManager,
            points = points,
            onSpendPoints = onSpendPoints,
            onDismiss = { customizingCategory = null }
        )
    }

    if (editingName) {
        var nameText by remember { mutableStateOf(profileManager.getName()) }
        AlertDialog(
            onDismissRequest = { editingName = false }, containerColor = CleanSurface,
            title = { Text("Tu nombre", color = CleanText, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { if (it.length <= 15) nameText = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WakeBlue, unfocusedBorderColor = CleanSurfaceVariant, focusedTextColor = CleanText, unfocusedTextColor = CleanText, cursorColor = WakeBlue),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = { Button(onClick = { profileManager.setName(nameText.trim()); editingName = false }, colors = ButtonDefaults.buttonColors(containerColor = WakeBlue)) { Text("Guardar", color = CleanText, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text("Cancelar", color = CleanTextDim) } }
        )
    }

    if (showCalendar) {
        StreakCalendarDialog(streak = streak, onDismiss = { showCalendar = false })
    }
}

// Fila simple para las estadísticas
@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = CleanTextDim)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CleanText)
    }
    HorizontalDivider(color = CleanSurfaceVariant.copy(alpha = 0.5f))
}

@Composable
private fun ProfileCategoryRow(category: ProfileCategory, equippedName: String, unlockedCount: Int, totalCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CleanSurface).border(1.dp, CleanSurfaceVariant, RoundedCornerShape(16.dp)).clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(CleanSurfaceVariant), contentAlignment = Alignment.Center) { Text(category.emoji, fontSize = 20.sp) }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(category.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CleanText)
            Text("Equipado: $equippedName", fontSize = 12.sp, color = CleanTextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$unlockedCount/$totalCount", fontSize = 12.sp, color = CleanTextDim, fontWeight = FontWeight.Bold)
            Text("›", fontSize = 18.sp, color = CleanTextDim)
        }
    }
}

@Composable
private fun ProfileCustomizationSheet(
    category: ProfileCategory,
    profileManager: ProfileManager,
    points: Int,
    onSpendPoints: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val items = ProfileCatalog.byCategory(category)
    val equippedId = profileManager.getEquipped(category)

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = CleanSurface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(category.emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category.label, fontSize = 18.sp, fontWeight = FontWeight.Black, color = CleanText)
                    }
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(CleanSurfaceVariant).clickable { onDismiss() }, contentAlignment = Alignment.Center) { Text("✕", color = CleanTextDim, fontSize = 14.sp) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { it.id }) { item ->
                        ProfileItemRow(
                            item = item,
                            isEquipped = item.id == equippedId,
                            isUnlocked = profileManager.isUnlocked(item.id),
                            points = points,
                            onEquip = { profileManager.setEquipped(category, item.id) },
                            onBuy = { cost ->
                                if (profileManager.unlock(item.id)) {
                                    onSpendPoints(cost)
                                    profileManager.setEquipped(category, item.id)
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
private fun ProfileItemRow(
    item: ProfileItem,
    isEquipped: Boolean,
    isUnlocked: Boolean,
    points: Int,
    onEquip: () -> Unit,
    onBuy: (Int) -> Unit
) {
    val context = LocalContext.current
    val req = item.requirement
    val canAfford = req is UnlockRequirement.Purchase && points >= req.price
    val buyable = req is UnlockRequirement.Purchase && !isUnlocked
    val resName = if (item.renderKey.startsWith("drawable:")) item.renderKey.removePrefix("drawable:") else "avatar1"
    val resId = remember(resName) { context.resources.getIdentifier(resName, "drawable", context.packageName) }

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (isEquipped) item.primaryColor.copy(alpha = 0.12f) else CleanSurfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, if (isEquipped) item.primaryColor.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(enabled = isUnlocked) { onEquip() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(item.primaryColor, item.secondaryColor))), contentAlignment = Alignment.Center) {
            if (resId != 0) {
                Image(painter = painterResource(id = resId), contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(item.previewEmoji, fontSize = 18.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CleanText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.description, fontSize = 11.sp, color = CleanTextDim, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        when {
            isEquipped -> Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(item.primaryColor.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("✓ Equipado", color = item.primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            isUnlocked -> Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(CleanSurface).clickable { onEquip() }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Equipar", color = CleanText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            buyable -> Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if (canAfford) item.primaryColor else CleanSurface).clickable(enabled = canAfford) { if (req is UnlockRequirement.Purchase) onBuy(req.price) }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(text = if (canAfford) "🪙 ${req.price}" else "🔒 ${req.price}", color = if (canAfford) Color.White else CleanTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            else -> {
                val label = when (val r = req) {
                    is UnlockRequirement.Achievement -> "🏆"
                    is UnlockRequirement.Streak -> "🔥 ${r.days}d"
                    is UnlockRequirement.Level -> "⭐ Nv.${r.level}"
                    else -> "🔒"
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(CleanSurface).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(label, color = CleanTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}