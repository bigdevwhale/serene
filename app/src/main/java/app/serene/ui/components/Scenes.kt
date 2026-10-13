package app.serene.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.serene.content.Scene
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Sky colours from top to bottom. */
fun skyOf(scene: Scene): List<Color> = when (scene) {
    Scene.DAWN -> listOf(Color(0xFF2B2A5C), Color(0xFF8A5C94), Color(0xFFE79A8A), Color(0xFFF6C48F))
    Scene.OCEAN -> listOf(Color(0xFF0D2340), Color(0xFF1C4E72), Color(0xFF4F93AE), Color(0xFF9CCFD6))
    Scene.FOREST -> listOf(Color(0xFF0C231D), Color(0xFF1A4436), Color(0xFF3E7458), Color(0xFF9CC48E))
    Scene.NIGHT -> listOf(Color(0xFF080A1E), Color(0xFF15183F), Color(0xFF2B2660), Color(0xFF4A3A78))
    Scene.MOUNTAIN -> listOf(Color(0xFF1B2A4D), Color(0xFF4E5F94), Color(0xFFA89BC4), Color(0xFFE8C6CC))
    Scene.MEADOW -> listOf(Color(0xFF2E4E7A), Color(0xFF6E9CC7), Color(0xFFBFD9E6), Color(0xFFF2E3C2))
    Scene.LIGHT -> listOf(Color(0xFF2E2158), Color(0xFF7A4F9E), Color(0xFFD488A6), Color(0xFFF7C88E))
    Scene.SPACE -> listOf(Color(0xFF04050D), Color(0xFF0E1030), Color(0xFF221747), Color(0xFF3B1F55))
}

/** Seconds since the composable appeared, updated every frame. */
@Composable
fun rememberSeconds(): Float {
    val t by produceState(0f) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { value = (it - start) / 1e9f }
    }
    return t
}

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val speed: Float)

/**
 * A slowly living landscape behind a session: drifting light, and the scene's own shapes —
 * waves, trees, peaks, hills or stars. Everything moves slower than a calm breath.
 */
@Composable
fun SceneBackdrop(scene: Scene, modifier: Modifier = Modifier, animate: Boolean = true) {
    val t = if (animate) rememberSeconds() else 0f
    val stars = remember(scene) {
        val rnd = Random(scene.ordinal + 7)
        val n = when (scene) { Scene.SPACE -> 160; Scene.NIGHT -> 90; Scene.MOUNTAIN, Scene.DAWN -> 25; else -> 0 }
        List(n) { Star(rnd.nextFloat(), rnd.nextFloat().let { it * it } * 0.7f, 0.6f + rnd.nextFloat() * 1.6f, rnd.nextFloat() * 6.28f, 0.4f + rnd.nextFloat()) }
    }
    val motes = remember(scene) {
        val rnd = Random(scene.ordinal + 31)
        List(if (scene == Scene.FOREST || scene == Scene.LIGHT || scene == Scene.MEADOW) 26 else 0) {
            Star(rnd.nextFloat(), rnd.nextFloat(), 1.5f + rnd.nextFloat() * 2.5f, rnd.nextFloat() * 6.28f, 0.3f + rnd.nextFloat() * 0.7f)
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(skyOf(scene)))
        drawGlows(scene, t)

        stars.forEach { s ->
            val a = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * s.speed + s.phase))
            val k = if (scene == Scene.SPACE || scene == Scene.NIGHT) 1f else 0.5f
            drawCircle(Color.White.copy(alpha = a * k), s.r * density * 0.8f, Offset(s.x * w, s.y * h))
        }

        when (scene) {
            Scene.DAWN -> {
                hills(h * 0.78f, h * 0.03f, 1.3f, t * 0.01f, Color(0xFF5B3F6E).copy(alpha = 0.75f))
                hills(h * 0.85f, h * 0.025f, 2.1f, 1f + t * 0.006f, Color(0xFF34284C))
            }
            Scene.OCEAN -> {
                for (i in 0 until 4) {
                    val base = h * (0.66f + i * 0.085f)
                    val col = listOf(Color(0xFF2F6F93), Color(0xFF235A7E), Color(0xFF184867), Color(0xFF0F3651))[i]
                    waves(base, h * (0.012f + i * 0.004f), 1.6f + i * 0.5f, t * (0.25f + i * 0.08f) + i, col)
                }
            }
            Scene.FOREST -> {
                trees(h * 0.74f, h * 0.2f, 11, 3, Color(0xFF214A3A))
                trees(h * 0.86f, h * 0.28f, 8, 9, Color(0xFF0E2A21))
            }
            Scene.NIGHT -> {
                val moon = Offset(w * 0.74f, h * 0.18f)
                drawCircle(Brush.radialGradient(listOf(Color(0x55FFF3D6), Color.Transparent), moon, w * 0.25f), w * 0.25f, moon)
                drawCircle(Color(0xFFF6EBD0), w * 0.06f, moon)
                drawCircle(skyOf(scene)[1], w * 0.055f, Offset(moon.x + w * 0.028f, moon.y - w * 0.012f))
                hills(h * 0.86f, h * 0.02f, 1.1f, 0.4f, Color(0xFF0B0C1F))
            }
            Scene.MOUNTAIN -> {
                peaks(h * 0.62f, h * 0.24f, listOf(0f, 0.18f, 0.36f, 0.58f, 0.8f, 1f), listOf(0.2f, 0.75f, 0.35f, 1f, 0.45f, 0.15f), Color(0xFF6B6A9C))
                peaks(h * 0.68f, h * 0.18f, listOf(0f, 0.25f, 0.5f, 0.72f, 1f), listOf(0.5f, 0.9f, 0.3f, 0.8f, 0.4f), Color(0xFF3E4475))
                // The lake: a darker band with a few drifting glints.
                drawRect(Brush.verticalGradient(listOf(Color(0xFF39406E), Color(0xFF1E2448)), startY = h * 0.68f, endY = h), Offset(0f, h * 0.68f), Size(w, h * 0.32f))
                for (i in 0 until 6) {
                    val y = h * (0.72f + i * 0.045f)
                    val x = w * (0.5f + 0.25f * sin(t * 0.07f + i * 1.7f))
                    drawLine(Color.White.copy(alpha = 0.12f), Offset(x - w * 0.08f, y), Offset(x + w * 0.08f, y), 1.5f * density)
                }
            }
            Scene.MEADOW -> {
                hills(h * 0.72f, h * 0.035f, 1.2f, t * 0.01f, Color(0xFF8DB580).copy(alpha = 0.9f))
                hills(h * 0.8f, h * 0.03f, 1.9f, 2f + t * 0.008f, Color(0xFF5E9466))
                hills(h * 0.9f, h * 0.02f, 2.7f, 4f, Color(0xFF3F7550))
            }
            Scene.LIGHT -> {
                val c = Offset(w * 0.5f, h * 0.42f)
                val r = w * (0.32f + 0.03f * sin(t * 0.5f))
                drawCircle(Brush.radialGradient(listOf(Color(0xAAFFE7B8), Color(0x33FFD59A), Color.Transparent), c, r * 2.2f), r * 2.2f, c)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4DC), Color(0x00FFE7B8)), c, r * 0.7f), r * 0.7f, c)
            }
            Scene.SPACE -> {
                val c = Offset(w * 0.3f, h * 0.35f)
                drawCircle(Brush.radialGradient(listOf(Color(0x334F6BFF), Color.Transparent), c, w * 0.6f), w * 0.6f, c)
            }
        }

        motes.forEach { m ->
            // Fireflies and specks of light rise and sway.
            val y = ((m.y - t * 0.012f * m.speed) % 1f + 1f) % 1f
            val x = m.x + 0.02f * sin(t * 0.4f * m.speed + m.phase)
            val a = 0.25f + 0.55f * (0.5f + 0.5f * sin(t * 1.3f * m.speed + m.phase))
            val col = if (scene == Scene.FOREST) Color(0xFFE9F59A) else Color(0xFFFFF1CF)
            drawCircle(col.copy(alpha = a * 0.35f), m.r * density * 2.5f, Offset(x * w, h * (0.3f + y * 0.65f)))
            drawCircle(col.copy(alpha = a), m.r * density * 0.7f, Offset(x * w, h * (0.3f + y * 0.65f)))
        }
    }
}

private fun DrawScope.drawGlows(scene: Scene, t: Float) {
    val w = size.width
    val h = size.height
    val tint = when (scene) {
        Scene.DAWN, Scene.LIGHT -> Color(0x40FFC59A)
        Scene.OCEAN -> Color(0x3088E0E8)
        Scene.FOREST -> Color(0x30C8F0A0)
        Scene.NIGHT, Scene.SPACE -> Color(0x307A6CFF)
        Scene.MOUNTAIN -> Color(0x30FFD2E0)
        Scene.MEADOW -> Color(0x40FFF2C8)
    }
    for (i in 0 until 3) {
        val a = t * (0.03f + i * 0.012f) + i * 2.1f
        val c = Offset(w * (0.5f + 0.35f * cos(a)), h * (0.35f + 0.18f * sin(a * 1.3f)))
        val r = w * (0.45f + 0.1f * i)
        drawCircle(Brush.radialGradient(listOf(tint, Color.Transparent), c, r), r, c)
    }
}

/** Rolling hills along [base], filled to the bottom. */
private fun DrawScope.hills(base: Float, amp: Float, freq: Float, shift: Float, color: Color) {
    val w = size.width
    val p = Path().apply {
        moveTo(0f, size.height)
        var x = 0f
        while (x <= w + 8) {
            val k = x / w * 2 * PI.toFloat() * freq
            lineTo(x, base + amp * sin(k + shift) + amp * 0.5f * sin(k * 2.3f + shift * 1.7f))
            x += 8f
        }
        lineTo(w, size.height)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.waves(base: Float, amp: Float, freq: Float, phase: Float, color: Color) {
    val w = size.width
    val p = Path().apply {
        moveTo(0f, size.height)
        var x = 0f
        while (x <= w + 6) {
            val k = x / w * 2 * PI.toFloat() * freq
            lineTo(x, base + amp * sin(k + phase) + amp * 0.4f * sin(k * 2.7f - phase * 1.3f))
            x += 6f
        }
        lineTo(w, size.height)
        close()
    }
    drawPath(p, color)
    drawPath(p, Color.White.copy(alpha = 0.05f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f * density))
}

/** A row of fir trees standing on [base]. */
private fun DrawScope.trees(base: Float, height: Float, count: Int, seed: Int, color: Color) {
    val w = size.width
    val rnd = Random(seed)
    drawRect(color, Offset(0f, base), Size(w, size.height - base))
    for (i in 0..count) {
        val cx = w * (i + rnd.nextFloat() * 0.6f - 0.3f) / count
        val th = height * (0.65f + rnd.nextFloat() * 0.5f)
        val tw = th * 0.32f
        val p = Path()
        for (tier in 0 until 3) {
            val top = base - th + tier * th * 0.22f
            val bottom = base - th * 0.2f * (2 - tier) + th * 0.05f
            val half = tw * (0.55f + tier * 0.25f)
            p.moveTo(cx, top)
            p.lineTo(cx + half, bottom)
            p.lineTo(cx - half, bottom)
            p.close()
        }
        drawPath(p, color)
        drawRect(color, Offset(cx - tw * 0.06f, base - th * 0.2f), Size(tw * 0.12f, th * 0.2f))
    }
}

private fun DrawScope.peaks(base: Float, height: Float, xs: List<Float>, hs: List<Float>, color: Color) {
    val w = size.width
    val p = Path().apply {
        moveTo(0f, base)
        xs.zip(hs).forEach { (x, k) -> lineTo(x * w, base - height * k) }
        lineTo(w, base)
        close()
    }
    drawPath(p, color)
}

/** Card gradients for affirmation themes and vision cards without a photo. */
val cardGradients = listOf(
    listOf(Color(0xFF6B5BD2), Color(0xFFB69CF0)),
    listOf(Color(0xFFD27A6E), Color(0xFFF2B98E)),
    listOf(Color(0xFF3F8F7A), Color(0xFF9BD3B5)),
    listOf(Color(0xFF3E6FB0), Color(0xFF8EC0E8)),
    listOf(Color(0xFFB65A86), Color(0xFFF0A0B8)),
    listOf(Color(0xFF8A6A3A), Color(0xFFE6C17A)),
    listOf(Color(0xFF2F3E7A), Color(0xFF7A6FC0)),
    listOf(Color(0xFF5E8A3E), Color(0xFFC8DE8E)),
)

fun cardBrush(i: Int) = Brush.linearGradient(cardGradients[Math.floorMod(i, cardGradients.size)])
