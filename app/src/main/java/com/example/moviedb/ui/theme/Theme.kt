package com.example.moviedb.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Amber80,
    onPrimary = Color(0xFF3E2600),
    primaryContainer = AmberDarkContainer,
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Indigo80,
    onSecondary = Color(0xFF1F226E),
    secondaryContainer = IndigoDarkContainer,
    onSecondaryContainer = Color(0xFFDEE0FF),
    tertiary = Rose80,
    onTertiary = Color(0xFF680020),
    tertiaryContainer = Color(0xFF93003A),
    onTertiaryContainer = Color(0xFFFFDAD9),
    background = MidnightBlue,
    onBackground = Color(0xFFE8E8F0),
    surface = MidnightSurface,
    onSurface = Color(0xFFE8E8F0),
    surfaceVariant = MidnightVariant,
    onSurfaceVariant = Color(0xFFA8A8C0),
    outline = Color(0xFF585870),
    outlineVariant = Color(0xFF2C2C40),
    error = Rose80,
    onError = Color(0xFF680020),
    errorContainer = Color(0xFF93003A),
    onErrorContainer = Color(0xFFFFDAD9),
    inverseSurface = Color(0xFFE8E8F0),
    inverseOnSurface = Color(0xFF1C1C28),
    inversePrimary = Amber40,
)

private val LightColorScheme = lightColorScheme(
    primary = Amber40,
    onPrimary = Color.White,
    primaryContainer = AmberContainer,
    onPrimaryContainer = Color(0xFF2D1600),
    secondary = Indigo40,
    onSecondary = Color.White,
    secondaryContainer = IndigoContainer,
    onSecondaryContainer = Color(0xFF00105A),
    tertiary = Rose40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD9),
    onTertiaryContainer = Color(0xFF410002),
    background = Color(0xFFF8F6F2),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0ECE8),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF7C7678),
    outlineVariant = Color(0xFFCAC4D0),
    error = Rose40,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Amber80,
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
