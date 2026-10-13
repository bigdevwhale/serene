package app.serene.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import app.serene.audio.Soundscape
import app.serene.container
import app.serene.content.Ambience
import app.serene.content.Scene
import app.serene.data.PracticeKind
import app.serene.data.Settings
import app.serene.ui.ambienceLabel
import app.serene.ui.clock
import app.serene.ui.components.GlassButton
import app.serene.ui.components.KeepScreenOn
import app.serene.ui.components.SceneBackdrop
import app.serene.ui.components.logSession
import app.serene.ui.minutesLabel
import app.serene.ui.theme.Inter
import app.serene.ui.theme.Lora
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun sceneFor(a: Ambience): Scene = when (a) {
    Ambience.OCEAN -> Scene.OCEAN
    Ambience.STREAM, Ambience.FOREST -> Scene.FOREST
    Ambience.MUSIC, Ambience.BOAT -> Scene.MEADOW
    Ambience.NIGHT, Ambience.STORM, Ambience.CITY, Ambience.TRAIN, Ambience.CAFE -> Scene.NIGHT
    Ambience.WHITE, Ambience.PINK -> Scene.SPACE
    Ambience.WIND -> Scene.MOUNTAIN
    Ambience.DRONE -> Scene.LIGHT
    Ambience.BROWN -> Scene.SPACE
    Ambience.RAIN, Ambience.FIRE, Ambience.NONE -> Scene.NIGHT
}

/** Silent meditation: a duration, a soundscape and a bowl to mark the time. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(settings: Settings, onClose: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    var minutes by remember { mutableIntStateOf(settings.timerMinutes) }
    var ambience by remember { mutableStateOf(runCatching { Ambience.valueOf(settings.timerAmbience) }.getOrDefault(Ambience.RAIN)) }
    var interval by remember { mutableIntStateOf(settings.timerInterval) }
    var running by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    val total = minutes * 60

    fun close() {
        if (running) logSession(context, PracticeKind.TIMER, "", elapsed)
        onClose()
    }
    BackHandler { close() }
    KeepScreenOn(running && !paused)
    LaunchedEffect(Unit) { Chimes.prepare(Chime.BOWL, Chime.GONG) }
    DisposableEffect(Unit) { onDispose { Soundscape.stop() } }

    // Preview the sound while choosing it.
    LaunchedEffect(ambience, running, paused, done) {
        when {
            done -> Soundscape.stop()
            running && paused -> Soundscape.stop()
            else -> Soundscape.play(ambience, settings.ambienceVolume * if (running) 1f else 0.7f)
        }
    }
    LaunchedEffect(running, paused) {
        if (!running || paused) return@LaunchedEffect
        if (elapsed == 0 && settings.bells) Chimes.play(Chime.BOWL)
        while (elapsed < total) {
            delay(1000)
            elapsed++
            if (interval > 0 && elapsed < total && elapsed % (interval * 60) == 0 && settings.bells) Chimes.play(Chime.BOWL)
        }
        if (settings.bells) Chimes.play(Chime.GONG)
        logSession(context, PracticeKind.TIMER, "", elapsed)
        running = false
        done = true
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF12111A))) {
        SceneBackdrop(sceneFor(ambience))
        Box(Modifier.fillMaxSize().background(Color(0x55000000)))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton(Icons.Rounded.Close, stringResource(R.string.close), { close() }, size = 44.dp)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    done -> Done(elapsed, onClose)
                    running -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(280.dp)) {
                                val w = 6.dp.toPx()
                                val arc = Size(size.width - w, size.height - w)
                                drawArc(Color.White.copy(alpha = 0.15f), -90f, 360f, false, Offset(w / 2, w / 2), arc, style = Stroke(w))
                                drawArc(Color.White, -90f, 360f * elapsed / total, false, Offset(w / 2, w / 2), arc, style = Stroke(w, cap = StrokeCap.Round))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(clock(total - elapsed), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 56.sp, color = Color.White))
                                Text(ambienceLabel(ambience), color = Color.White.copy(alpha = 0.7f), fontFamily = Inter, fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.height(40.dp))
                        GlassButton(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, stringResource(if (paused) R.string.resume else R.string.pause), { paused = !paused }, size = 72.dp, strong = true)
                    }
                    else -> Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.timer_title), style = TextStyle(fontFamily = Lora, fontSize = 32.sp, color = Color.White, textAlign = TextAlign.Center))
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.timer_text), color = Color.White.copy(alpha = 0.8f), fontFamily = Inter, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Label(stringResource(R.string.timer_duration))
                        Pills(listOf(3, 5, 10, 15, 20, 30, 45, 60), minutes, { minutesLabel(it) }) { minutes = it }
                        Label(stringResource(R.string.timer_sound))
                        Pills(Ambience.entries, ambience, { ambienceLabel(it) }) { ambience = it }
                        Label(stringResource(R.string.timer_interval))
                        Pills(listOf(0, 1, 2, 5, 10), interval, { if (it == 0) stringResource(R.string.off) else minutesLabel(it) }) { interval = it }
                        Spacer(Modifier.height(32.dp))
                        GlassButton(Icons.Rounded.PlayArrow, stringResource(R.string.begin), {
                            container.scope.launch {
                                container.settings.update { it.copy(timerMinutes = minutes, timerAmbience = ambience.name, timerInterval = interval) }
                            }
                            running = true
                        }, size = 84.dp, strong = true)
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(), color = Color.White.copy(alpha = 0.65f), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.4.sp,
        modifier = Modifier.padding(top = 26.dp, bottom = 10.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Pills(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier.clip(CircleShape)
                    .background(if (on) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.14f))
                    .clickable { onSelect(o) }
                    .padding(horizontal = 15.dp, vertical = 9.dp),
            ) {
                Text(label(o), color = if (on) Color(0xFF1F1D2B) else Color.White, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
}
