package com.german.wakeapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileAvatar(
    profileManager: ProfileManager,
    size: Int = 60,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val avatarId = profileManager.equippedAvatar
    val avatar = ProfileCatalog.byId(avatarId) ?: ProfileCatalog.avatars.first()
    val resName = if (avatar.renderKey.startsWith("drawable:")) {
        avatar.renderKey.removePrefix("drawable:")
    } else {
        "avatar0"
    }
    val resId = remember(resName) {
        context.resources.getIdentifier(resName, "drawable", context.packageName)
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(avatar.primaryColor, avatar.secondaryColor)))
            .border(1.5.dp, CleanSurfaceVariant, CircleShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = avatar.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = avatar.previewEmoji, fontSize = (size * 0.45f).sp)
        }
    }
}