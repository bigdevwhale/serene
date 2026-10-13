package app.serene.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.serene.R
import app.serene.content.Ambience
import app.serene.content.Library
import app.serene.domain.Area
import app.serene.domain.PhaseType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

/** Content in the current app language; switches along with it. */
@Composable
fun library(): Library {
    val l = locale()
    return remember(l.language) { Library.of(l) }
}

@Composable
fun minutesLabel(minutes: Int): String = pluralStringResource(R.plurals.minutes, minutes, minutes)

/** "4:05" for timers. */
fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

@Composable
fun dateLabel(ms: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale())
        .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

@Composable
fun dayLabel(d: LocalDate): String {
    val today = LocalDate.now()
    return when (d) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> DateTimeFormatter.ofPattern(if (d.year == today.year) "EEEE, d MMMM" else "d MMMM yyyy", locale()).format(d)
            .replaceFirstChar { it.titlecase(locale()) }
    }
}

@Composable
fun timeLabel(ms: Long): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale())
        .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

@Composable
fun clockTime(minutes: Int): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale()).format(LocalTime.of(minutes / 60, minutes % 60))

@Composable
fun greeting(hour: Int): String = stringResource(
    when (hour) {
        in 5..11 -> R.string.greet_morning
        in 12..17 -> R.string.greet_day
        in 18..22 -> R.string.greet_evening
        else -> R.string.greet_night
    }
)

@Composable
fun patternTitle(id: String): String = stringResource(
    when (id) {
        "calm" -> R.string.breath_calm
        "box" -> R.string.breath_box
        "relax" -> R.string.breath_relax
        "coherent" -> R.string.breath_coherent
        "sigh" -> R.string.breath_sigh
        else -> R.string.breath_balance
    }
)

@Composable
fun patternText(id: String): String = stringResource(
    when (id) {
        "calm" -> R.string.breath_calm_text
        "box" -> R.string.breath_box_text
        "relax" -> R.string.breath_relax_text
        "coherent" -> R.string.breath_coherent_text
        "sigh" -> R.string.breath_sigh_text
        else -> R.string.breath_balance_text
    }
)

@Composable
fun phaseLabel(p: PhaseType): String = stringResource(
    when (p) {
        PhaseType.INHALE -> R.string.phase_inhale
        PhaseType.TOP_UP -> R.string.phase_top_up
        PhaseType.HOLD -> R.string.phase_hold
        PhaseType.EXHALE -> R.string.phase_exhale
        PhaseType.REST -> R.string.phase_rest
    }
)

@Composable
fun moodLabel(m: Int): String = stringResource(
    when (m) {
        1 -> R.string.mood_1
        2 -> R.string.mood_2
        3 -> R.string.mood_3
        4 -> R.string.mood_4
        else -> R.string.mood_5
    }
)

@Composable
fun areaLabel(a: Area): String = stringResource(
    when (a) {
        Area.BODY -> R.string.area_body
        Area.MIND -> R.string.area_mind
        Area.PEOPLE -> R.string.area_people
        Area.WORK -> R.string.area_work
        Area.REST -> R.string.area_rest
        Area.JOY -> R.string.area_joy
    }
)

@Composable
fun ambienceLabel(a: Ambience): String = stringResource(
    when (a) {
        Ambience.NONE -> R.string.amb_none
        Ambience.RAIN -> R.string.amb_rain
        Ambience.OCEAN -> R.string.amb_ocean
        Ambience.STREAM -> R.string.amb_stream
        Ambience.WIND -> R.string.amb_wind
        Ambience.MUSIC -> R.string.amb_music
        Ambience.FOREST -> R.string.amb_forest
        Ambience.NIGHT -> R.string.amb_night
        Ambience.STORM -> R.string.amb_storm
        Ambience.BOAT -> R.string.amb_boat
        Ambience.CITY -> R.string.amb_city
        Ambience.CAFE -> R.string.amb_cafe
        Ambience.TRAIN -> R.string.amb_train
        Ambience.WHITE -> R.string.amb_white
        Ambience.PINK -> R.string.amb_pink
        Ambience.FIRE -> R.string.amb_fire
        Ambience.DRONE -> R.string.amb_drone
        Ambience.BROWN -> R.string.amb_brown
    }
)
