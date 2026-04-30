package com.example.moviedb.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary                = CinemaGreen,
    onPrimary              = Color.White,
    primaryContainer       = Dark700,
    onPrimaryContainer     = TextPrimaryDark,

    secondary              = TextTertiary,
    onSecondary            = Dark800,
    secondaryContainer     = Dark700,
    onSecondaryContainer   = TextSecondaryDark,

    tertiary               = ErrorRed,
    onTertiary             = Color.White,
    tertiaryContainer      = Color(0xFF7F1D1D),
    onTertiaryContainer    = ErrorContainer,

    background             = Dark900,
    onBackground           = TextPrimaryDark,

    surface                = Dark800,
    onSurface              = TextPrimaryDark,
    surfaceVariant         = Dark700,
    onSurfaceVariant       = TextTertiary,

    outline                = Dark600,
    outlineVariant         = Dark700,

    error                  = ErrorRed,
    onError                = Color.White,
    errorContainer         = Color(0xFF7F1D1D),
    onErrorContainer       = ErrorContainer,

    inverseSurface         = TextPrimaryDark,
    inverseOnSurface       = Dark900,
    inversePrimary         = CinemaGreenDark,
)

private val LightColorScheme = lightColorScheme(
    primary                = CinemaGreen,
    onPrimary              = Color.White,
    primaryContainer       = CinemaGreenLight,
    onPrimaryContainer     = CinemaGreenDeep,

    secondary              = TextSecondaryLight,
    onSecondary            = Color.White,
    secondaryContainer     = LightChip,
    onSecondaryContainer   = TextPrimaryLight,

    tertiary               = ErrorRedDark,
    onTertiary             = Color.White,
    tertiaryContainer      = ErrorContainer,
    onTertiaryContainer    = ErrorOnContainer,

    background             = Light100,
    onBackground           = TextPrimaryLight,

    surface                = Light50,
    onSurface              = TextPrimaryLight,
    surfaceVariant         = Light75,
    onSurfaceVariant       = TextSecondaryLight,

    outline                = LightBorder,
    outlineVariant         = LightChip,

    error                  = ErrorRedDark,
    onError                = Color.White,
    errorContainer         = ErrorContainer,
    onErrorContainer       = ErrorOnContainer,

    inverseSurface         = TextPrimaryLight,
    inverseOnSurface       = Light50,
    inversePrimary         = CinemaGreen,
)

@Composable
fun MovieDbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
