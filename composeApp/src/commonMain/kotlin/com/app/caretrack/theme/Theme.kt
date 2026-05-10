package com.app.caretrack.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val lightColors = CareTrackColors()
private val darkColors = DarkCareTrackColors()

object CareTrackColor {
    val Primary = Color(lightColors.primary)
    val OnPrimary = Color(lightColors.onPrimary)
    val PrimaryContainer = Color(lightColors.primaryContainer)
    val OnPrimaryContainer = Color(lightColors.onPrimaryContainer)
    val Secondary = Color(lightColors.secondary)
    val OnSecondary = Color(lightColors.onSecondary)
    val SecondaryContainer = Color(lightColors.secondaryContainer)
    val OnSecondaryContainer = Color(lightColors.onSecondaryContainer)
    val Tertiary = Color(lightColors.tertiary)
    val OnTertiary = Color(lightColors.onTertiary)
    val TertiaryContainer = Color(lightColors.tertiaryContainer)
    val OnTertiaryContainer = Color(lightColors.onTertiaryContainer)
    val Error = Color(lightColors.error)
    val OnError = Color(lightColors.onError)
    val ErrorContainer = Color(lightColors.errorContainer)
    val OnErrorContainer = Color(lightColors.onErrorContainer)
    val Background = Color(lightColors.background)
    val OnBackground = Color(lightColors.onBackground)
    val Surface = Color(lightColors.surface)
    val OnSurface = Color(lightColors.onSurface)
    val SurfaceVariant = Color(lightColors.surfaceVariant)
    val OnSurfaceVariant = Color(lightColors.onSurfaceVariant)
    val Outline = Color(lightColors.outline)
    val OutlineVariant = Color(lightColors.outlineVariant)
}

data class CareTrackColors(
    val primary: Long = 0xFF006B5E,
    val onPrimary: Long = 0xFFFFFFFF,
    val primaryContainer: Long = 0xFF6FF7E3,
    val onPrimaryContainer: Long = 0xFF00201B,
    val secondary: Long = 0xFF4B635C,
    val onSecondary: Long = 0xFFFFFFFF,
    val secondaryContainer: Long = 0xFFCCE8DF,
    val onSecondaryContainer: Long = 0xFF07201A,
    val tertiary: Long = 0xFF426278,
    val onTertiary: Long = 0xFFFFFFFF,
    val tertiaryContainer: Long = 0xFFC9E6FF,
    val onTertiaryContainer: Long = 0xFF001E2E,
    val error: Long = 0xFFBA1A1A,
    val onError: Long = 0xFFFFFFFF,
    val errorContainer: Long = 0xFFFFDAD6,
    val onErrorContainer: Long = 0xFF410002,
    val background: Long = 0xFFFAFDFA,
    val onBackground: Long = 0xFF191C1B,
    val surface: Long = 0xFFFAFDFA,
    val onSurface: Long = 0xFF191C1B,
    val surfaceVariant: Long = 0xFFDBE5E0,
    val onSurfaceVariant: Long = 0xFF3F4945,
    val outline: Long = 0xFF6F7975,
    val outlineVariant: Long = 0xFFBFC9C4
)

data class DarkCareTrackColors(
    val primary: Long = 0xFF4Ddbc7,
    val onPrimary: Long = 0xFF00382E,
    val primaryContainer: Long = 0xFF005144,
    val onPrimaryContainer: Long = 0xFF6FF7E3,
    val secondary: Long = 0xFFB1CCC3,
    val onSecondary: Long = 0xFF1D352F,
    val secondaryContainer: Long = 0xFF334B45,
    val onSecondaryContainer: Long = 0xFFCCE8DF,
    val tertiary: Long = 0xFFAACBE3,
    val onTertiary: Long = 0xFF103447,
    val tertiaryContainer: Long = 0xFF2A4A60,
    val onTertiaryContainer: Long = 0xFFC9E6FF,
    val error: Long = 0xFFFFB4AB,
    val onError: Long = 0xFF690005,
    val errorContainer: Long = 0xFF93000A,
    val onErrorContainer: Long = 0xFFFFDAD6,
    val background: Long = 0xFF191C1B,
    val onBackground: Long = 0xFFE1E3E0,
    val surface: Long = 0xFF191C1B,
    val onSurface: Long = 0xFFE1E3E0,
    val surfaceVariant: Long = 0xFF3F4945,
    val onSurfaceVariant: Long = 0xFFBFC9C4,
    val outline: Long = 0xFF89938F,
    val outlineVariant: Long = 0xFF3F4945
)

private val LightColorScheme = lightColorScheme(
    primary = Color(lightColors.primary),
    onPrimary = Color(lightColors.onPrimary),
    primaryContainer = Color(lightColors.primaryContainer),
    onPrimaryContainer = Color(lightColors.onPrimaryContainer),
    secondary = Color(lightColors.secondary),
    onSecondary = Color(lightColors.onSecondary),
    secondaryContainer = Color(lightColors.secondaryContainer),
    onSecondaryContainer = Color(lightColors.onSecondaryContainer),
    tertiary = Color(lightColors.tertiary),
    onTertiary = Color(lightColors.onTertiary),
    tertiaryContainer = Color(lightColors.tertiaryContainer),
    onTertiaryContainer = Color(lightColors.onTertiaryContainer),
    error = Color(lightColors.error),
    onError = Color(lightColors.onError),
    errorContainer = Color(lightColors.errorContainer),
    onErrorContainer = Color(lightColors.onErrorContainer),
    background = Color(lightColors.background),
    onBackground = Color(lightColors.onBackground),
    surface = Color(lightColors.surface),
    onSurface = Color(lightColors.onSurface),
    surfaceVariant = Color(lightColors.surfaceVariant),
    onSurfaceVariant = Color(lightColors.onSurfaceVariant),
    outline = Color(lightColors.outline),
    outlineVariant = Color(lightColors.outlineVariant)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(darkColors.primary),
    onPrimary = Color(darkColors.onPrimary),
    primaryContainer = Color(darkColors.primaryContainer),
    onPrimaryContainer = Color(darkColors.onPrimaryContainer),
    secondary = Color(darkColors.secondary),
    onSecondary = Color(darkColors.onSecondary),
    secondaryContainer = Color(darkColors.secondaryContainer),
    onSecondaryContainer = Color(darkColors.onSecondaryContainer),
    tertiary = Color(darkColors.tertiary),
    onTertiary = Color(darkColors.onTertiary),
    tertiaryContainer = Color(darkColors.tertiaryContainer),
    onTertiaryContainer = Color(darkColors.onTertiaryContainer),
    error = Color(darkColors.error),
    onError = Color(darkColors.onError),
    errorContainer = Color(darkColors.errorContainer),
    onErrorContainer = Color(darkColors.onErrorContainer),
    background = Color(darkColors.background),
    onBackground = Color(darkColors.onBackground),
    surface = Color(darkColors.surface),
    onSurface = Color(darkColors.onSurface),
    surfaceVariant = Color(darkColors.surfaceVariant),
    onSurfaceVariant = Color(darkColors.onSurfaceVariant),
    outline = Color(darkColors.outline),
    outlineVariant = Color(darkColors.outlineVariant)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun CareTrackTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}