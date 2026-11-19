package com.german.wakeapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RedPrimary,
    secondary = RedAccent,
    tertiary = AmberAccent,
    background = DarkBackground,
    surface = SurfaceDark,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = DarkBackground,
    onBackground = TextWhite,
    onSurface = TextWhite
)

// Esquema claro de verdad: fondo blanco, texto oscuro
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1D9BF0),
    secondary = RedAccent,
    tertiary = AmberAccent,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF2F2F2),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF111111),
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111)
)

@Composable
fun WakeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accent: Color? = null,   // color del tema equipado (null = el de por defecto)
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val finalScheme = if (accent != null) colorScheme.copy(primary = accent) else colorScheme

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        content = content
    )
}