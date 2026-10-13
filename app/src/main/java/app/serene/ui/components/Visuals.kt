package app.serene.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.serene.ui.theme.U
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** From a heavy slate blue to a warm gold. */
fun moodColor(m: Int): Color = when (m) {
    1 -> Color(0xFF7D86A8)
    2 -> Color(0xFF6FA2D6)
    3 -> Color(0xFF8BB8A0)
    4 -> Color(0xFF55B486)
    else -> Color(0xFFF0B13E)
}

/** A simple drawn face: the mouth curves with the mood. */
@Composable
fun MoodFace(mood: Int, size: Dp = 44.dp, filled: Boolean = true, modifier: Modifier = Modifier) {
    val color = moodColor(mood)
    val ink = if (filled) Color.White else color
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        val c = center
        if (filled) drawCircle(color, r, c) else drawCircle(color.copy(alpha = 0.16f), r, c)
        val stroke = r * 0.13f
        drawCircle(ink, r * 0.09f, Offset(c.x - r * 0.33f, c.y - r * 0.18f))
        drawCircle(ink, r * 0.09f, Offset(c.x + r * 0.33f, c.y - r * 0.18f))
        // -1 frown .. +1 smile
        val curve = (mood - 3) / 2f
        val y = c.y + r * 0.3f
        val p = Path().apply {
            moveTo(c.x - r * 0.38f, y - curve * r * 0.08f)
            quadraticTo(c.x, y + curve * r * 0.34f, c.x + r * 0.38f, y - curve * r * 0.08f)
        }
        drawPath(p, ink, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/** Mood over the last [days] days: dots joined where days are consecutive. */
@Composable
fun MoodChart(byDay: Map<LocalDate, Float>, today: LocalDate, days: Int = 14, modifier: Modifier = Modifier) {
    val line = U.c.line
    val muted = U.c.accent
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val step = w / (days - 1)
        for (m in 1..5) {
            val y = h - (m - 1) / 4f * h
            drawLine(line, Offset(0f, y), Offset(w, y), 1f * density)
        }
        var prev: Offset? = null
        for (i in 0 until days) {
            val d = today.minusDays((days - 1 - i).toLong())
            val v = byDay[d]
            if (v == null) {
                prev = null
                continue
            }
            val p = Offset(i * step, h - (v - 1) / 4f * h)
            prev?.let { drawLine(muted.copy(alpha = 0.5f), it, p, 2.5f * density, cap = StrokeCap.Round) }
            prev = p
        }
        for (i in 0 until days) {
            val d = today.minusDays((days - 1 - i).toLong())
            val v = byDay[d] ?: continue
            val p = Offset(i * step, h - (v - 1) / 4f * h)
            drawCircle(moodColor(Math.round(v)), 5.5f * density, p)
        }
    }
}

/** Last five weeks of practice, one square per day, darker for longer sessions. */
@Composable
fun PracticeCalendar(byDay: Map<LocalDate, Int>, today: LocalDate, modifier: Modifier = Modifier) {
    val accent = U.c.accent
    val empty = U.c.surface2
    val ring = U.c.line
    Canvas(modifier) {
        val cols = 7
        val rows = 5
        val gap = 5f * density
        val cell = minOf((size.width - gap * (cols - 1)) / cols, (size.height - gap * (rows - 1)) / rows)
        val start = today.minusDays((today.dayOfWeek.value - 1 + 7L * (rows - 1)).toLong())
        for (i in 0 until cols * rows) {
            val d = start.plusDays(i.toLong())
            if (d.isAfter(today)) break
            val x = (i % cols) * (cell + gap)
            val y = (i / cols) * (cell + gap)
            val s = byDay[d] ?: 0
            val k = if (s == 0) 0f else (0.3f + 0.7f * (s / 1200f).coerceAtMost(1f))
            drawRoundRect(
                if (s == 0) empty else accent.copy(alpha = k), Offset(x, y), Size(cell, cell),
                androidx.compose.ui.geometry.CornerRadius(cell * 0.28f),
            )
            if (d == today) drawRoundRect(ring, Offset(x, y), Size(cell, cell), androidx.compose.ui.geometry.CornerRadius(cell * 0.28f), style = Stroke(1.5f * density))
        }
    }
}

/** Wheel of balance: the latest scores filled, the previous check as a faint outline. */
@Composable
fun BalanceWheel(scores: List<Int>, previous: List<Int>?, modifier: Modifier = Modifier) {
    val line = U.c.line
    val accent = U.c.accent
    val peach = U.c.peach
    Canvas(modifier) {
        val n = 6
        val c = center
        val r = size.minDimension / 2 * 0.92f
        fun at(i: Int, v: Float) = Offset(
            c.x + r * v * cos(-PI / 2 + i * 2 * PI / n).toFloat(),
            c.y + r * v * sin(-PI / 2 + i * 2 * PI / n).toFloat(),
        )
        for (ring in 1..5) {
            val p = Path()
            for (i in 0 until n) at(i, ring / 5f).let { if (i == 0) p.moveTo(it.x, it.y) else p.lineTo(it.x, it.y) }
            p.close()
            drawPath(p, line, style = Stroke(1f * density))
        }
        for (i in 0 until n) drawLine(line, c, at(i, 1f), 1f * density)
        fun shape(v: List<Int>) = Path().apply {
            for (i in 0 until n) at(i, (v.getOrElse(i) { 0 } / 10f).coerceIn(0.04f, 1f)).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) }
            close()
        }
        previous?.let { drawPath(shape(it), peach.copy(alpha = 0.8f), style = Stroke(1.5f * density)) }
        drawPath(shape(scores), accent.copy(alpha = 0.25f))
        drawPath(shape(scores), accent, style = Stroke(2.5f * density))
        for (i in 0 until n) drawCircle(accent, 3.5f * density, at(i, (scores.getOrElse(i) { 0 } / 10f).coerceIn(0.04f, 1f)))
    }
}
