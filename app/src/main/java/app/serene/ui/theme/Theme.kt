package app.serene.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.serene.R
import app.serene.data.ThemeMode

/** Twilight lavender on warm paper: quiet enough to stay out of the way. */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val peach: Color,
    val peachSoft: Color,
    val sage: Color,
    val sageSoft: Color,
    val sky: Color,
    val skySoft: Color,
    val rose: Color,
    val roseSoft: Color,
    val danger: Color,
    val dark: Boolean,
)

val LightPalette = Palette(
    bg = Color(0xFFF5F3EF),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFFAF8F5),
    line = Color(0xFFE8E3DC),
    ink = Color(0xFF1F1D2B),
    muted = Color(0xFF6A6677),
    faint = Color(0xFFA39FAD),
    accent = Color(0xFF6B5BD2),
    accentSoft = Color(0xFFECE8FB),
    onAccent = Color(0xFFFFFFFF),
    peach = Color(0xFFD27A52),
    peachSoft = Color(0xFFFBEADF),
    sage = Color(0xFF3F9574),
    sageSoft = Color(0xFFE0F0E8),
    sky = Color(0xFF3E83C4),
    skySoft = Color(0xFFE0ECF8),
    rose = Color(0xFFC6557A),
    roseSoft = Color(0xFFF8E3EA),
    danger = Color(0xFFD9443F),
    dark = false,
)

val DarkPalette = Palette(
    bg = Color(0xFF12111A),
    surface = Color(0xFF1B1A25),
    surface2 = Color(0xFF211F2C),
    line = Color(0xFF2D2B3A),
    ink = Color(0xFFEEECF5),
    muted = Color(0xFFA8A4B8),
    faint = Color(0xFF6F6B80),
    accent = Color(0xFFA397FF),
    accentSoft = Color(0xFF2C2750),
    onAccent = Color(0xFF17132E),
    peach = Color(0xFFF0A27E),
    peachSoft = Color(0xFF3A2A22),
    sage = Color(0xFF6CCBA5),
    sageSoft = Color(0xFF1C3329),
    sky = Color(0xFF7DB6F0),
    skySoft = Color(0xFF1A2A3C),
    rose = Color(0xFFF08AA8),
    roseSoft = Color(0xFF3A2030),
    danger = Color(0xFFFF6B63),
    dark = true,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** A soft serif for the words that matter: affirmations and spoken lines. */
val Lora = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
)

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(28.dp)
    val RLg = RoundedCornerShape(24.dp)
    val RMd = RoundedCornerShape(18.dp)
    val RSm = RoundedCornerShape(12.dp)
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Medium, fontSize = 34.sp, letterSpacing = (-0.01).em, lineHeight = 40.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.025).em, lineHeight = 32.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.12.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = (-0.01).em, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.02).em, color = U.c.ink)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val Quote: TextStyle @Composable get() = TextStyle(fontFamily = Lora, fontSize = 23.sp, lineHeight = 32.sp, color = U.c.ink)
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun SereneTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = p.accent,
                onPrimary = p.onAccent,
                primaryContainer = p.accentSoft,
                onPrimaryContainer = p.ink,
                secondaryContainer = p.accentSoft,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 14.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
