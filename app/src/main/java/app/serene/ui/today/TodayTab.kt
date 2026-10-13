package app.serene.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.R
import app.serene.container
import app.serene.data.Favorite
import app.serene.data.Settings
import app.serene.domain.StatsCalc
import app.serene.domain.dayOf
import app.serene.ui.Go
import app.serene.ui.Tab
import app.serene.ui.components.MoodFace
import app.serene.ui.components.ProgressRing
import app.serene.ui.components.SectionTitle
import app.serene.ui.components.UCard
import app.serene.ui.components.rememberNow
import app.serene.ui.greeting
import app.serene.ui.library
import app.serene.ui.locale
import app.serene.ui.moodLabel
import app.serene.ui.practice.GuidedCard
import app.serene.ui.practice.WideCard
import app.serene.ui.theme.Inter
import app.serene.ui.theme.Lora
import app.serene.ui.theme.U
import app.serene.ui.theme.UType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What fits this hour: waking up, the middle of the day, unwinding, or sleep. */
fun suggestions(hour: Int, day: Long): List<String> {
    val pick = { l: List<String> -> l[Math.floorMod(day, l.size.toLong()).toInt()] }
    return when (hour) {
        in 5..10 -> listOf("morning_intention", pick(listOf("gratitude", "healing_light", "goal_achieved")))
        in 11..16 -> listOf(pick(listOf("breath_awareness", "letting_go", "inner_harmony")), pick(listOf("forest_walk", "mountain_lake", "ocean_shore", "safe_place")))
        in 17..20 -> listOf(pick(listOf("self_compassion", "loving_kindness", "gratitude")), pick(listOf("release_worries", "inner_garden", "future_self")))
        else -> listOf("sleep_wind_down", pick(listOf("starry_sky", "body_scan")))
    }
}

@Composable
fun TodayTab(settings: Settings, go: Go, onTab: (Tab) -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val dao = container.db.dao()
    val lib = library()
    val now = rememberNow(60_000)
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val hour = Instant.ofEpochMilli(now).atZone(zone).hour

    val sessions by remember { dao.observeSessions() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val journal by remember { dao.observeJournal() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val favorites by remember { dao.observeFavorites() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val mine by remember { dao.observeMine() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val visions by remember { dao.observeVisions() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val stats = remember(sessions, today) { StatsCalc.of(sessions, today) }
    val daily = remember(lib, today, settings.focus, favorites, mine) { lib.daily(today, settings.focus, favorites.map { it.key }, mine) }
    val todayMood = journal.firstOrNull { it.mood != null && dayOf(it.at) == today }?.mood
    val loc = locale()

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    DateTimeFormatter.ofPattern("EEEE, d MMMM", loc).format(today).replaceFirstChar { it.titlecase(loc) },
                    style = UType.Small.copy(color = U.c.muted),
                )
                Spacer(Modifier.height(2.dp))
                val g = greeting(hour)
                Text(if (settings.name.isBlank()) g else "$g, ${settings.name}", style = UType.Title)
            }
            IconButton(onClick = { go.settings() }) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
        }
        Spacer(Modifier.height(16.dp))

        // Affirmation of the day.
        val fav = favorites.any { it.key == daily.key }
        val voice = container.voice
        LaunchedEffect(loc) { voice.setLanguage(loc) }
        Column(
            Modifier.fillMaxWidth().clip(U.RXl)
                .background(Brush.linearGradient(listOf(Color(0xFF5B4FC4), Color(0xFF9A7FD6), Color(0xFFE39A7B))))
                .clickable { onTab(Tab.AFFIRM) }
                .padding(start = 22.dp, end = 10.dp, top = 20.dp, bottom = 8.dp),
        ) {
            Text(stringResource(R.string.affirmation_of_day).uppercase(), color = Color.White.copy(alpha = 0.75f), fontFamily = Inter, fontSize = 11.sp, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(12.dp))
            Text(daily.text, style = TextStyle(fontFamily = Lora, fontSize = 25.sp, lineHeight = 34.sp, color = Color.White), modifier = Modifier.padding(end = 12.dp))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { container.scope.launch { voice.say(daily.text, settings.voiceRate) } }) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.listen), tint = Color.White)
                }
                IconButton(onClick = { container.write { if (fav) dao().unfavorite(daily.key) else dao().favorite(Favorite(daily.key)) } }) {
                    Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, stringResource(R.string.favorite), tint = Color.White)
                }
            }
        }

        SectionTitle(stringResource(R.string.how_are_you))
        UCard {
            if (todayMood != null) {
                Row(Modifier.clickable { go.checkIn() }, verticalAlignment = Alignment.CenterVertically) {
                    MoodFace(todayMood, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.today_mood, moodLabel(todayMood)), style = UType.Strong)
                        Text(stringResource(R.string.today_mood_more), style = UType.Muted)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (1..5).forEach { m ->
                        Column(Modifier.clip(U.RSm).clickable { go.checkIn(mood = m) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            MoodFace(m, 44.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(moodLabel(m), style = UType.Small.copy(fontSize = 11.sp, color = U.c.muted))
                        }
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.today_goal))
        UCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val mins = stats.todaySeconds / 60
                ProgressRing(mins / settings.dailyGoal.toFloat(), Modifier.size(84.dp), stroke = 9.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(mins.toString(), style = UType.Number)
                        Text("/ ${settings.dailyGoal}", style = UType.Small)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (mins >= settings.dailyGoal) R.string.goal_done else R.string.goal_left, maxOf(0, settings.dailyGoal - mins)),
                        style = UType.Strong,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (stats.streak > 0) androidx.compose.ui.res.pluralStringResource(R.plurals.streak, stats.streak, stats.streak)
                        else stringResource(R.string.streak_none),
                        style = UType.Muted,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(Icons.Rounded.Spa, stringResource(R.string.calm_now), stringResource(R.string.calm_now_text), U.c.peach, U.c.peachSoft, Modifier.weight(1f)) { go.guided("calm_now") }
            QuickTile(Icons.Rounded.Air, stringResource(R.string.breathe), stringResource(R.string.breathe_text), U.c.sky, U.c.skySoft, Modifier.weight(1f)) { go.breathe("calm") }
        }

        SectionTitle(stringResource(R.string.for_now))
        val fav2 = favorites.map { it.key }.toSet()
        val done = sessions.map { it.ref }.toSet()
        suggestions(hour, today.toEpochDay()).mapNotNull { lib.guided(it) }.forEach { g ->
            GuidedCard(g, lib.ru, g.id in done, "g:${g.id}" in fav2) { go.guided(g.id) }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(2.dp))
        WideCard(
            Icons.Rounded.Dashboard, stringResource(R.string.visions_title),
            if (visions.isEmpty()) stringResource(R.string.visions_card_empty) else stringResource(R.string.visions_card, visions.size),
            U.c.accent, U.c.accentSoft,
        ) { go.visions() }
    }
}

@Composable
private fun QuickTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, color: Color, soft: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(U.RMd).background(soft).clickable(onClick = onClick).padding(16.dp)) {
        Box(Modifier.size(38.dp).clip(androidx.compose.foundation.shape.CircleShape).background(U.c.surface.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = color)
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = UType.Strong)
        Spacer(Modifier.height(2.dp))
        Text(text, style = UType.Muted)
    }
}
