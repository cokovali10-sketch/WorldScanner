package org.worldscanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Minecraft / dev-tool inspired dark palette.
 *
 * - `primary`   -> grass green, used for actions and highlights
 * - `secondary` -> enchanting-table cyan, used for links / secondary actions
 * - `tertiary`  -> experience gold, used for decorative accents
 * - `error`     -> redstone red, used for filter validation errors
 */
object MinecraftColors {
    val GrassGreen = Color(0xFF63B73B)
    val EnchantCyan = Color(0xFF3EC6E0)
    val ExperienceGold = Color(0xFFE8C468)
    val RedstoneRed = Color(0xFFDA5A5A)

    val BackgroundDeep = Color(0xFF10151D)
    val SurfaceDark = Color(0xFF1A212C)
    val SurfaceVariant = Color(0xFF232C39)
    val TextPrimary = Color(0xFFE6EBF2)
    val TextSecondary = Color(0xFF9AA7B8)
}

private val MinecraftDarkColorScheme = darkColorScheme(
    primary = MinecraftColors.GrassGreen,
    onPrimary = Color(0xFF0C1407),
    primaryContainer = Color(0xFF1E3320),
    onPrimaryContainer = MinecraftColors.GrassGreen,

    secondary = MinecraftColors.EnchantCyan,
    onSecondary = Color(0xFF06222B),
    secondaryContainer = Color(0xFF14313A),
    onSecondaryContainer = MinecraftColors.EnchantCyan,

    tertiary = MinecraftColors.ExperienceGold,
    onTertiary = Color(0xFF2A1F05),

    error = MinecraftColors.RedstoneRed,
    onError = Color(0xFF2A0505),
    errorContainer = Color(0xFF3A1515),
    onErrorContainer = MinecraftColors.RedstoneRed,

    background = MinecraftColors.BackgroundDeep,
    onBackground = MinecraftColors.TextPrimary,
    surface = MinecraftColors.SurfaceDark,
    onSurface = MinecraftColors.TextPrimary,
    surfaceVariant = MinecraftColors.SurfaceVariant,
    onSurfaceVariant = MinecraftColors.TextSecondary,
    outline = Color(0xFF3A4656),
)

/** Monospace family used for the SNBT filter and coordinates. */
val MonoFontFamily = FontFamily.Monospace

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.4.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
    ),
)

/** Applies the dark Minecraft theme to [content]. */
@Composable
fun WorldScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MinecraftDarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
