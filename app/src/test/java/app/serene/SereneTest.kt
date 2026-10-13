package app.serene

import app.serene.content.Library
import app.serene.content.estimateMinutes
import app.serene.data.MyAffirmation
import app.serene.data.PracticeKind
import app.serene.data.Session
import app.serene.data.Vision
import app.serene.domain.Pattern
import app.serene.domain.StatsCalc
import app.serene.notify.Reminders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

class SereneTest {
    private val en = Library.of(Locale.ENGLISH)
    private val ru = Library.of(Locale.forLanguageTag("ru"))

    @Test
    fun `languages carry the same sessions and aligned affirmations`() {
        assertEquals(en.guided.map { it.id }, ru.guided.map { it.id })
        assertEquals(en.guided.map { it.scene to it.ambience }, ru.guided.map { it.scene to it.ambience })
        assertEquals(en.sets.map { it.id }, ru.sets.map { it.id })
        en.sets.zip(ru.sets).forEach { (a, b) -> assertEquals(a.id, a.items.size, b.items.size) }
        assertEquals(10, en.meditations.size)
        assertEquals(10, en.visualizations.size)
    }

    @Test
    fun `spoken text has no digits or symbols the voice would stumble on`() {
        for (lib in listOf(en, ru)) for (g in lib.guided) for (s in g.steps) {
            assertTrue("${g.id}: ${s.text}", s.text.none { it.isDigit() || it in "\"$#%&*" })
            assertTrue(s.pause in 0..60)
        }
    }

    @Test
    fun `sessions last roughly what they promise`() {
        val targets = mapOf("calm_now" to 3, "body_scan" to 12, "sleep_wind_down" to 12, "future_self" to 12, "release_worries" to 7)
        for ((id, minutes) in targets) for (lib in listOf(en, ru)) {
            val m = lib.guided(id)!!.estimateMinutes(lib.ru)
            assertTrue("$id: $m", m in (minutes - 2)..(minutes + 2))
        }
    }

    @Test
    fun `favourite keys resolve in either language`() {
        val mine = listOf(MyAffirmation(id = 7, text = "Mine"))
        assertEquals(en.sets.first().items[3], en.affirmation("a:${en.sets.first().id}:3", mine)!!.text)
        assertEquals(ru.sets.first().items[3], ru.affirmation("a:${ru.sets.first().id}:3", mine)!!.text)
        assertEquals("Mine", en.affirmation("m:7", mine)!!.text)
        assertEquals(null, en.affirmation("m:8", mine))
        assertEquals(null, en.affirmation("a:nope:1", mine))
    }

    @Test
    fun `daily affirmation is stable for a day and draws from focus`() {
        val day = LocalDate.of(2026, 10, 13)
        val a = en.daily(day, setOf("calm"), emptyList(), emptyList())
        assertEquals(a, en.daily(day, setOf("calm"), emptyList(), emptyList()))
        assertEquals("calm", a.setId)
        val days = (0L until 30).map { en.daily(day.plusDays(it), emptySet(), emptyList(), emptyList()).key }.toSet()
        assertTrue(days.size > 20)
    }

    @Test
    fun `vision session tags lines with their vision`() {
        val g = en.visionSession(listOf(Vision(id = 1, title = "A garden", details = "Green", affirmation = "I grow"), Vision(id = 2, title = "Calm")))
        assertTrue(g.steps.any { it.tag == 0 && it.text == "A garden" })
        assertTrue(g.steps.any { it.tag == 1 && it.text.contains("Calm") })
        assertTrue(g.steps.first().tag == -1 && g.steps.last().tag == -1)
    }

    @Test
    fun `streak counts consecutive days and survives until tonight`() {
        val today = LocalDate.of(2026, 10, 13)
        fun at(d: LocalDate) = d.atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val sessions = listOf(4, 3, 1).map { Session(at = at(today.minusDays(it.toLong())), kind = PracticeKind.BREATH, seconds = 120) }
        val s = StatsCalc.of(sessions, today, ZoneOffset.UTC)
        assertEquals(1, s.streak) // yesterday only; the gap before it breaks the run
        assertEquals(2, s.best)
        assertEquals(6, s.totalMinutes)
        val s2 = StatsCalc.of(sessions + Session(at = at(today), kind = PracticeKind.TIMER, seconds = 600), today, ZoneOffset.UTC)
        assertEquals(2, s2.streak)
        assertEquals(600, s2.todaySeconds)
    }

    @Test
    fun `narration keys match the recording tool`() {
        // tools/narrate.py: sha1(text.strip()).hexdigest()[:16]
        assertEquals("a9993e364706816a", app.serene.audio.Narrator.key("abc"))
        assertEquals("a9993e364706816a", app.serene.audio.Narrator.key("  abc "))
    }

    @Test
    fun `vision session keeps the user's words apart from stock lines`() {
        val g = ru.visionSession(listOf(Vision(id = 1, title = "Дом у моря", affirmation = "Мне спокойно", audio = "x.m4a"), Vision(id = 2, title = "Сад")))
        assertTrue(g.steps.any { it.audio == "x.m4a" && it.tag == 0 })
        assertTrue(g.steps.none { it.text.contains("Дом у моря") && it.audio.isEmpty() })
        assertTrue(g.steps.any { it.text == "Сад" && it.tag == 1 })
    }

    @Test
    fun `breathing patterns fill the chosen time`() {
        val calm = Pattern.byId("calm")
        assertEquals(10f, calm.cycle)
        assertEquals(18, calm.cycles(3))
        assertEquals(3, Pattern.byId("relax").cycles(0))
        assertEquals("4 · 7 · 8", Pattern.byId("relax").rhythm())
        assertNotNull(Pattern.all.firstOrNull { it.id == "sigh" })
    }

    @Test
    fun `reminders land on the next occurrence`() {
        val now = LocalDateTime.of(2026, 10, 13, 10, 0)
        assertEquals(LocalDateTime.of(2026, 10, 14, 9, 0), Reminders.nextAt(9 * 60, now))
        assertEquals(LocalDateTime.of(2026, 10, 13, 22, 0), Reminders.nextAt(22 * 60, now))
        assertEquals(LocalDateTime.of(2026, 10, 14, 10, 0), Reminders.nextAt(10 * 60, now))
    }
}
