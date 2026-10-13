package app.serene.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serene.R
import app.serene.audio.Chime
import app.serene.audio.Chimes
import app.serene.container
import app.serene.content.Scene
import app.serene.data.PracticeKind
import app.serene.data.Settings
import app.serene.domain.Pattern
import app.serene.domain.PhaseType
import app.serene.ui.clock
import app.serene.ui.components.GlassButton
import app.serene.ui.components.KeepScreenOn
import app.serene.ui.components.SceneBackdrop
import app.serene.ui.components.logSession
import app.serene.ui.components.tick
import app.serene.ui.minutesLabel
import app.serene.ui.patternText
import app.serene.ui.patternTitle
import app.serene.ui.phaseLabel
import app.serene.ui.theme.Inter
import app.serene.ui.theme.Lora
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

private val Breath = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

@Composable
fun BreathScreen(id: String, settings: Settings, onClose: () -> Unit) {
    val context = LocalContext.current
    val pattern = remember(id) { Pattern.byId(id) }
    var minutes by remember { mutableIntStateOf(settings.breathMinutes) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(PhaseType.INHALE) }
    var left by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(0.45f) }

    fun close() {
        if (running) logSession(context, PracticeKind.BREATH, pattern.id, elapsed)
        onClose()
    }
    BackHandler { close() }
    KeepScreenOn(running)
    LaunchedEffect(Unit) { Chimes.prepare(Chime.INHALE, Chime.EXHALE, Chime.HOLD, Chime.BOWL) }

    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            elapsed++
        }
    }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        delay(600)
        repeat(pattern.cycles(minutes)) {
            for (p in pattern.phases) {
                phase = p.type
                if (settings.bells) when (p.type) {
                    PhaseType.INHALE -> Chimes.play(Chime.INHALE)
                    PhaseType.EXHALE -> Chimes.play(Chime.EXHALE)
                    PhaseType.HOLD, PhaseType.REST, PhaseType.TOP_UP -> Chimes.play(Chime.HOLD)
                }
                if (settings.haptics) tick(context)
                val target = when (p.type) {
                    PhaseType.INHALE -> if (pattern.phases.any { it.type == PhaseType.TOP_UP }) 0.88f else 1f
                    PhaseType.TOP_UP -> 1f
                    PhaseType.EXHALE -> 0.45f
                    else -> scale.value
                }
                val ms = (p.seconds * 1000).toLong()
                val counter = launch {
                    var s = ceil(p.seconds).toInt()
                    while (s > 0) {
                        left = s
                        delay(if (s == ceil(p.seconds).toInt()) ms - (s - 1) * 1000L else 1000L)
                        s--
                    }
                }
                scale.animateTo(target, tween(ms.toInt(), easing = Breath))
                counter.join()
            }
        }
        logSession(context, PracticeKind.BREATH, pattern.id, elapsed)
        if (settings.bells) Chimes.play(Chime.BOWL)
        running = false
        done = true
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF12111A))) {
        SceneBackdrop(Scene.NIGHT)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton(Icons.Rounded.Close, stringResource(R.string.close), { close() }, size = 44.dp)
                Spacer(Modifier.weight(1f))
                if (running) Text(clock(elapsed), color = Color.White.copy(alpha = 0.7f), fontFamily = Inter, fontSize = 14.sp)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    done -> Done(elapsed, onClose)
                    !running -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(patternTitle(pattern.id), style = TextStyle(fontFamily = Lora, fontSize = 34.sp, color = Color.White, textAlign = TextAlign.Center))
                        Spacer(Modifier.height(8.dp))
                        Text(pattern.rhythm(), color = Color.White.copy(alpha = 0.7f), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(14.dp))
                        Text(patternText(pattern.id), color = Color.White.copy(alpha = 0.85f), fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(28.dp))
                        Pills(listOf(1, 3, 5, 10), minutes, { minutesLabel(it) }) { m ->
                            minutes = m
                            context.container.scope.launch { context.container.settings.update { it.copy(breathMinutes = m) } }
                        }
                        Spacer(Modifier.height(36.dp))
                        GlassButton(Icons.Rounded.PlayArrow, stringResource(R.string.begin), { running = true }, size = 84.dp, strong = true)
                    }
                    else -> Box(contentAlignment = Alignment.Center) {
                        Orb(scale.value, Modifier.size(320.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedContent(phase, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) }, label = "phase") {
                                Text(phaseLabel(it), style = TextStyle(fontFamily = Lora, fontSize = 30.sp, color = Color.White))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(left.toString(), color = Color.White.copy(alpha = 0.8f), fontFamily = Inter, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A glowing sphere that grows with the inhale; faint rings mark the full breath. */
@Composable
private fun Orb(scale: Float, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = center
        drawCircle(Color.White.copy(alpha = 0.10f), r, c, style = Stroke(1.5f * density))
        drawCircle(Color.White.copy(alpha = 0.06f), r * 0.45f, c, style = Stroke(1.5f * density))
        val cur = r * scale
        drawCircle(Brush.radialGradient(listOf(Color(0x55B9A8FF), Color.Transparent), c, cur * 1.35f), cur * 1.35f, c)
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFFEDE6FF), Color(0xFFB9A8FF), Color(0xFF7D6BE0)), Offset(c.x - cur * 0.25f, c.y - cur * 0.3f), cur * 1.4f),
            cur, c,
        )
    }
}
