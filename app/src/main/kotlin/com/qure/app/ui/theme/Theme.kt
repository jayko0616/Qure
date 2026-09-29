package com.qure.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val QrYellow = Color(0xFFFFC800)

val RiskSafe = Color(0xFF35C759)
val RiskCaution = Color(0xFFFFB020)
val RiskDanger = Color(0xFFFF453A)

private val QureDark = darkColorScheme(
    primary = QrYellow,
    onPrimary = Color(0xFF201A00),
    secondary = Color(0xFFB8C4D0),
    background = Color(0xFF0B0D10),
    onBackground = Color(0xFFE6E9EE),
    surface = Color(0xFF15181D),
    onSurface = Color(0xFFE6E9EE),
    surfaceVariant = Color(0xFF232830),
    onSurfaceVariant = Color(0xFFA8B2BE),
    error = RiskDanger,
)

private val QureLight = lightColorScheme(
    primary = Color(0xFF7A5A00),
    background = Color(0xFFFAFAFC),
    surface = Color(0xFFFFFFFF),
    error = RiskDanger,
)

@Composable
fun QureTheme(forceDark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (forceDark || isSystemInDarkTheme()) QureDark else QureLight,
        content = content,
    )
}
