package com.german.wakeapp

import androidx.compose.ui.graphics.Color

// ============================================================
//  CATÁLOGO DE PERFIL — Solo Avatares
// ============================================================

enum class ProfileCategory(val label: String, val emoji: String) {
    AVATAR("Avatar", "🖼️")
}

sealed class UnlockRequirement {
    data class Purchase(val price: Int) : UnlockRequirement()
    data class Achievement(val achievementId: String) : UnlockRequirement()
    data class Streak(val days: Int) : UnlockRequirement()
    data class Level(val level: Int) : UnlockRequirement()
    object Free : UnlockRequirement()
}

data class ProfileItem(
    val id: String,
    val category: ProfileCategory,
    val name: String,
    val description: String = "",
    val previewEmoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val requirement: UnlockRequirement,
    val renderKey: String = ""
)

object ProfileCatalog {

    val avatars = listOf(
        ProfileItem("avatar_0", ProfileCategory.AVATAR, "Por defecto", "", "🖼️", Color(0xFF90A4AE), Color(0xFF455A64), UnlockRequirement.Free, "drawable:avatar0"),
        ProfileItem("avatar_1", ProfileCategory.AVATAR, "Caballero", "", "🖼️", Color(0xFF42A5F5), Color(0xFF1565C0), UnlockRequirement.Purchase(250), "drawable:avatar1"),
        ProfileItem("avatar_2", ProfileCategory.AVATAR, "Eclipse", "", "🖼️", Color(0xFF26C6DA), Color(0xFF00838F), UnlockRequirement.Purchase(300), "drawable:avatar2"),
        ProfileItem("avatar_3", ProfileCategory.AVATAR, "Británico Gordo", "", "🖼️", Color(0xFF66BB6A), Color(0xFF2E7D32), UnlockRequirement.Purchase(350), "drawable:avatar3"),
        ProfileItem("avatar_4", ProfileCategory.AVATAR, "Julius", "", "🖼️", Color(0xFFFFCA28), Color(0xFFF57F17), UnlockRequirement.Purchase(400), "drawable:avatar4"),
        ProfileItem("avatar_5", ProfileCategory.AVATAR, "Avatar 5", "", "🖼️", Color(0xFFFF7043), Color(0xFFD84315), UnlockRequirement.Purchase(450), "drawable:avatar5"),
        ProfileItem("avatar_6", ProfileCategory.AVATAR, "Avatar 6", "", "🖼️", Color(0xFFAB47BC), Color(0xFF6A1B9A), UnlockRequirement.Purchase(500), "drawable:avatar6"),
        ProfileItem("avatar_7", ProfileCategory.AVATAR, "Avatar 7", "", "🖼️", Color(0xFFEC407A), Color(0xFFAD1457), UnlockRequirement.Purchase(550), "drawable:avatar7"),
        ProfileItem("avatar_8", ProfileCategory.AVATAR, "Avatar 8", "", "🖼️", Color(0xFF5C6BC0), Color(0xFF283593), UnlockRequirement.Purchase(600), "drawable:avatar8"),
        ProfileItem("avatar_9", ProfileCategory.AVATAR, "Avatar 9", "", "🖼️", Color(0xFF29B6F6), Color(0xFF0277BD), UnlockRequirement.Purchase(700), "drawable:avatar9"),
        ProfileItem("avatar_10", ProfileCategory.AVATAR, "Avatar 10", "", "🖼️", Color(0xFFFFA726), Color(0xFFEF6C00), UnlockRequirement.Purchase(800), "drawable:avatar10")
    )

    fun all(): List<ProfileItem> = avatars
    fun byId(id: String): ProfileItem? = avatars.firstOrNull { it.id == id }
    fun byCategory(category: ProfileCategory): List<ProfileItem> = when (category) {
        ProfileCategory.AVATAR -> avatars
    }

    fun defaultFor(category: ProfileCategory): String = when (category) {
        ProfileCategory.AVATAR -> "avatar_0"
    }
}