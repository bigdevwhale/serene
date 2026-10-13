package app.serene.domain

import app.serene.data.JournalEntry
import app.serene.data.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun dayOf(ms: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

/** Practice totals for the journal header and the daily goal ring. */
data class Stats(
    val streak: Int,
    val best: Int,
    val totalMinutes: Int,
    val sessions: Int,
    val todaySeconds: Int,
    /** Seconds practised per day. */
    val byDay: Map<LocalDate, Int>,
)

object StatsCalc {
    /**
     * A day counts when the user practised at all. The streak survives until the end of today:
     * yesterday's streak is still alive in the morning before today's practice.
     */
    fun of(sessions: List<Session>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Stats {
        val byDay = sessions.groupBy { dayOf(it.at, zone) }.mapValues { (_, l) -> l.sumOf { it.seconds } }
        val days = byDay.keys
        var streak = 0
        var d = if (today in days) today else today.minusDays(1)
        while (d in days) {
            streak++
            d = d.minusDays(1)
        }
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (day in days.sorted()) {
            run = if (prev != null && prev.plusDays(1) == day) run + 1 else 1
            best = maxOf(best, run)
            prev = day
        }
        return Stats(
            streak = streak,
            best = best,
            totalMinutes = sessions.sumOf { it.seconds } / 60,
            sessions = sessions.size,
            todaySeconds = byDay[today] ?: 0,
            byDay = byDay,
        )
    }

    /** Average mood per day (only days with a rated check-in). */
    fun moodByDay(entries: List<JournalEntry>, zone: ZoneId = ZoneId.systemDefault()): Map<LocalDate, Float> =
        entries.filter { it.mood != null }
            .groupBy { dayOf(it.at, zone) }
            .mapValues { (_, l) -> l.map { it.mood!! }.average().toFloat() }
}

/** The six areas of the wheel of balance, in drawing order. */
enum class Area { BODY, MIND, PEOPLE, WORK, REST, JOY }
