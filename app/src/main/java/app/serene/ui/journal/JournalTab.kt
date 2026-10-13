package app.serene.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.R
import app.serene.container
import app.serene.data.JournalEntry
import app.serene.data.Settings
import app.serene.domain.Area
import app.serene.domain.StatsCalc
import app.serene.domain.dayOf
import app.serene.ui.Go
import app.serene.ui.areaLabel
import app.serene.ui.components.BalanceWheel
import app.serene.ui.components.MoodChart
import app.serene.ui.components.MoodFace
import app.serene.ui.components.PracticeCalendar
import app.serene.ui.components.SectionTitle
import app.serene.ui.components.SoftButton
import app.serene.ui.components.UCard
import app.serene.ui.dayLabel
import app.serene.ui.moodLabel
import app.serene.ui.theme.U
import app.serene.ui.theme.UType
import app.serene.ui.timeLabel
import java.time.LocalDate

@Composable
fun JournalTab(settings: Settings, go: Go) {
    val dao = LocalContext.current.container.db.dao()
    val sessions by remember { dao.observeSessions() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val journal by remember { dao.observeJournal() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val balance by remember { dao.observeBalance() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val today = LocalDate.now()
    val stats = remember(sessions, today) { StatsCalc.of(sessions, today) }
    val moods = remember(journal) { StatsCalc.moodByDay(journal) }
    val byDay = remember(journal) { journal.groupBy { dayOf(it.at) }.toList() }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)) {
        item {
            Text(stringResource(R.string.tab_journal), style = UType.Title, modifier = Modifier.padding(top = 18.dp, start = 4.dp))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(pluralStringResource(R.plurals.days_n, stats.streak, stats.streak), stringResource(R.string.stat_streak), Modifier.weight(1f))
                Stat(stats.totalMinutes.toString(), stringResource(R.string.stat_minutes), Modifier.weight(1f))
                Stat(stats.sessions.toString(), stringResource(R.string.stat_sessions), Modifier.weight(1f))
            }
            if (stats.best > stats.streak) {
                Text(stringResource(R.string.stat_best, stats.best), style = UType.Small, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            }

            SectionTitle(stringResource(R.string.how_are_you))
            UCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (1..5).forEach { m ->
                        Column(Modifier.clip(U.RSm).clickable { go.checkIn(mood = m) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            MoodFace(m, 46.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(moodLabel(m), style = UType.Small.copy(fontSize = 11.sp, color = U.c.muted))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                SoftButton(stringResource(R.string.write_gratitude), { go.checkIn() }, Modifier.fillMaxWidth())
            }

            SectionTitle(stringResource(R.string.mood_chart))
            UCard {
                if (moods.isEmpty()) Text(stringResource(R.string.mood_chart_empty), style = UType.Muted)
                else MoodChart(moods, today, modifier = Modifier.fillMaxWidth().height(110.dp).padding(horizontal = 6.dp, vertical = 6.dp))
            }

            SectionTitle(stringResource(R.string.practice_calendar))
            UCard {
                PracticeCalendar(stats.byDay, today, Modifier.fillMaxWidth().aspectRatio(7f / 5f))
            }

            SectionTitle(stringResource(R.string.balance_title))
            UCard(onClick = { go.balance() }) {
                val last = balance.lastOrNull()
                if (last == null) {
                    Text(stringResource(R.string.balance_empty), style = UType.Sub)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BalanceWheel(last.values(), balance.getOrNull(balance.size - 2)?.values(), Modifier.size(130.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Area.entries.forEachIndexed { i, a ->
                                Text("${areaLabel(a)} · ${last.values().getOrElse(i) { 0 }}", style = UType.Muted)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.balance_check), style = UType.Button.copy(color = U.c.accent))
            }

            if (byDay.isNotEmpty()) SectionTitle(stringResource(R.string.entries))
        }
        items(byDay, key = { it.first.toEpochDay() }) { (day, entries) ->
            Text(dayLabel(day), style = UType.Strong.copy(fontSize = 14.sp, color = U.c.muted), modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 8.dp))
            entries.forEach { e ->
                EntryCard(e) { go.checkIn(e.id) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    UCard(modifier, padding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
        Text(value, style = UType.Number.copy(fontSize = 19.sp), maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, style = UType.Small.copy(color = U.c.muted), maxLines = 1)
    }
}

@Composable
private fun EntryCard(e: JournalEntry, onClick: () -> Unit) {
    UCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (e.mood != null) {
                MoodFace(e.mood, 30.dp)
                Spacer(Modifier.width(10.dp))
                Text(moodLabel(e.mood), style = UType.Strong.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
            } else {
                Text(stringResource(R.string.gratitude), style = UType.Strong.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
            }
            Text(timeLabel(e.at), style = UType.Small)
        }
        val lines = e.gratitude.lines().filter { it.isNotBlank() }
        if (lines.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            lines.forEach { l ->
                Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(U.c.peach))
                    Spacer(Modifier.width(10.dp))
                    Text(l.trim(), style = UType.Body)
                }
            }
        }
        if (e.note.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(e.note, style = UType.Sub)
        }
    }
}
