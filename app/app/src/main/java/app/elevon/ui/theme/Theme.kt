package app.elevon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import app.elevon.data.ThemeMode

// Elevon design tokens (docs/design.md): graphite & paper neutrals, one
// signal-orange accent reserved for live state.
object ElevonColors {
    val GraphiteSurface = Color(0xFF101114)
    val GraphiteContainer = Color(0xFF17191D)
    val GraphiteHigh = Color(0xFF1E2126)
    val GraphiteOutline = Color(0xFF2E3238)
    val GraphiteOnSurface = Color(0xFFECEDEE)
    val GraphiteOnVariant = Color(0xFFA9AEB6)

    val PaperSurface = Color(0xFFFAFAF8)
    val PaperContainer = Color(0xFFF0EFEA)
    val PaperHigh = Color(0xFFE6E4DE)
    val PaperOutline = Color(0xFFD5D2CA)
    val PaperOnSurface = Color(0xFF1A1B1E)
    val PaperOnVariant = Color(0xFF5B6068)

    val AccentDark = Color(0xFFFF7A45)
    val AccentLight = Color(0xFFE4571F)
    val AccentContainerDark = Color(0xFF3A241A)
    val AccentContainerLight = Color(0xFFFFE0D2)
    val SuccessDark = Color(0xFF7AD98B)
    val SuccessLight = Color(0xFF1E7F35)
    val WarningDark = Color(0xFFF2C14E)
    val WarningLight = Color(0xFF9A6B00)
    val DangerDark = Color(0xFFF26D6D)
    val DangerLight = Color(0xFFB3261E)
}

private val GraphiteScheme = darkColorScheme(
    primary = ElevonColors.AccentDark,
    onPrimary = Color(0xFF2B1206),
    primaryContainer = ElevonColors.AccentContainerDark,
    onPrimaryContainer = Color(0xFFFFB597),
    secondary = ElevonColors.GraphiteOnVariant,
    onSecondary = ElevonColors.GraphiteSurface,
    secondaryContainer = ElevonColors.GraphiteHigh,
    onSecondaryContainer = ElevonColors.GraphiteOnSurface,
    tertiary = ElevonColors.SuccessDark,
    onTertiary = Color(0xFF0A2912),
    background = ElevonColors.GraphiteSurface,
    onBackground = ElevonColors.GraphiteOnSurface,
    surface = ElevonColors.GraphiteSurface,
    onSurface = ElevonColors.GraphiteOnSurface,
    surfaceContainer = ElevonColors.GraphiteContainer,
    surfaceContainerHigh = ElevonColors.GraphiteHigh,
    onSurfaceVariant = ElevonColors.GraphiteOnVariant,
    outline = ElevonColors.GraphiteOutline,
    outlineVariant = Color(0xFF23262B),
    error = ElevonColors.DangerDark,
    onError = Color(0xFF37060A),
    errorContainer = Color(0xFF3A1416),
    onErrorContainer = ElevonColors.DangerDark,
)

private val PaperScheme = lightColorScheme(
    primary = ElevonColors.AccentLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = ElevonColors.AccentContainerLight,
    onPrimaryContainer = Color(0xFF4A1A05),
    secondary = ElevonColors.PaperOnVariant,
    onSecondary = Color.White,
    secondaryContainer = ElevonColors.PaperHigh,
    onSecondaryContainer = ElevonColors.PaperOnSurface,
    tertiary = ElevonColors.SuccessLight,
    onTertiary = Color.White,
    background = ElevonColors.PaperSurface,
    onBackground = ElevonColors.PaperOnSurface,
    surface = ElevonColors.PaperSurface,
    onSurface = ElevonColors.PaperOnSurface,
    surfaceContainer = ElevonColors.PaperContainer,
    surfaceContainerHigh = ElevonColors.PaperHigh,
    onSurfaceVariant = ElevonColors.PaperOnVariant,
    outline = ElevonColors.PaperOutline,
    outlineVariant = Color(0xFFE3E0D8),
    error = ElevonColors.DangerLight,
    onError = Color.White,
    errorContainer = Color(0xFFFBE3E1),
    onErrorContainer = ElevonColors.DangerLight,
)

val ElevonTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun ElevonTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) GraphiteScheme else PaperScheme,
        typography = ElevonTypography,
        content = content,
    )
}
