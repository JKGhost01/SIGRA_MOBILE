package com.sigra.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.sigra.app.R

val Teal = Color(0xFF087F78)
val TealDark = Color(0xFF065F5A)
val Coral = Color(0xFFFF6A5C)
val Amber = Color(0xFFFFB547)
val Ink = Color(0xFF152538)
val Paper = Color(0xFFF3F6FA)
val Muted = Color(0xFF536579)
val DarkPaper = Color(0xFF070B12)
val DarkSurface = Color(0xFF0F1622)
val DarkLine = Color(0xFF26384F)

val DisplayFont = FontFamily(
    Font(R.font.chakra_petch_medium, FontWeight.Medium),
    Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
    Font(R.font.chakra_petch_bold, FontWeight.Bold),
)
val ReadingFont = FontFamily(Font(R.font.ibm_plex_sans_variable))
val DataFont = FontFamily(
    Font(R.font.ibm_plex_mono_regular),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

private val EsquemaClaro = lightColorScheme(
    primary = Teal, onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F4F0), onPrimaryContainer = TealDark,
    secondary = Color(0xFF326C9A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EEF8), onSecondaryContainer = Ink,
    tertiary = Color(0xFF8A5700),
    background = Paper, onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFE7EDF4), onSurfaceVariant = Muted,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F9FC),
    surfaceContainer = Color(0xFFEDF2F7), surfaceContainerHigh = Color(0xFFE7EDF4),
    surfaceContainerHighest = Color(0xFFDFE7F0),
    outline = Color(0xFF7B8A9C), outlineVariant = Color(0xFFD5DFEA),
    error = Color(0xFFB3261E), onErrorContainer = Color(0xFF410002),
)
private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF4BE3D8), onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF123731), onPrimaryContainer = Color(0xFF8FF0E8),
    secondary = Color(0xFF82C5F1), onSecondary = Color(0xFF082E48),
    secondaryContainer = Color(0xFF132B40), onSecondaryContainer = Color(0xFFC6E7FF),
    tertiary = Amber,
    background = DarkPaper, onBackground = Color(0xFFE8EEF6),
    surface = DarkSurface, onSurface = Color(0xFFE8EEF6),
    surfaceVariant = Color(0xFF1C2636), onSurfaceVariant = Color(0xFFA4B1C4),
    surfaceContainerLowest = Color(0xFF06080D), surfaceContainerLow = Color(0xFF0C1119),
    surfaceContainer = DarkSurface, surfaceContainerHigh = Color(0xFF152031),
    surfaceContainerHighest = Color(0xFF1C293C),
    outline = Color(0xFF526580), outlineVariant = DarkLine,
    error = Color(0xFFFF8A7E), errorContainer = Color(0xFF4A201F), onErrorContainer = Color(0xFFFFDAD5),
)

private val Tipografia = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFont, fontSize = 48.sp, lineHeight = 54.sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontFamily = DisplayFont, fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold),
    displaySmall = TextStyle(fontFamily = DisplayFont, fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontFamily = DisplayFont, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = DisplayFont, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = ReadingFont, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = ReadingFont, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = ReadingFont, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = ReadingFont, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = ReadingFont, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = ReadingFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = ReadingFont, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = DataFont, fontSize = 10.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
)

@Composable
fun SigraTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Tipografia,
        shapes = Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(18.dp)),
        content = content,
    )
}
