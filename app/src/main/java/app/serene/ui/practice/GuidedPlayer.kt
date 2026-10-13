package app.serene.ui.practice

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VoiceOverOff
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.R
import app.serene.audio.Chime
import app.serene.audio.Chimes
import app.serene.audio.Soundscape
import app.serene.audio.Voice
import app.serene.container
import app.serene.content.Ambience
import app.serene.content.Guided
import app.serene.content.GuidedKind
import app.serene.content.Library
import app.serene.content.estimateMinutes
import app.serene.content.estimateSeconds
import app.serene.data.Favorite
import app.serene.data.PracticeKind
import app.serene.data.Settings
import app.serene.data.Vision
import app.serene.data.ambienceFor
import app.serene.data.withAmbience
import app.serene.ui.components.AmbienceSheet
import kotlinx.coroutines.launch
import app.serene.ui.clock
import app.serene.ui.components.GlassButton
import app.serene.ui.components.KeepScreenOn
import app.serene.ui.components.SceneBackdrop
import app.serene.ui.components.logSession
import app.serene.ui.library
import app.serene.ui.locale
import app.serene.ui.minutesLabel
import app.serene.ui.theme.Inter
import app.serene.ui.theme.Lora
import app.serene.ui.visions.rememberVisionImage
import kotlinx.coroutines.delay

private enum class Stage { INTRO, PLAYING, DONE }

@Composable
fun GuidedPlayer(id: String, settings: Settings, onClose: () -> Unit) {
    val context = LocalContext.current
    val lib = library()
    val visions by produceState<List<Vision>?>(null, id) {
        value = if (id == Library.VISIONS_ID) context.container.db.dao().visions() else emptyList()
    }
    val guided = remember(id, lib, visions) {
        if (id == Library.VISIONS_ID) visions?.takeIf { it.isNotEmpty() }?.let { lib.visionSession(it) } else lib.guided(id)
    }
    if (guided == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF12111A)))
        if (visions != null) LaunchedEffect(Unit) { onClose() }
        return
    }
    GuidedSession(guided, visions.orEmpty(), lib.ru, settings, onClose)
}

@Composable
private fun GuidedSession(g: Guided, visions: List<Vision>, ru: Boolean, settings: Settings, onClose: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val voice = container.voice
    val voiceState by voice.state.collectAsStateWithLifecycle()
    val loc = locale()
    LaunchedEffect(loc, voiceState == Voice.State.LOADING) { voice.setLanguage(loc) }

    var stage by remember { mutableStateOf(Stage.INTRO) }
    var paused by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(-1) }
    var jump by remember { mutableIntStateOf(0) }
    var inStep by remember { mutableFloatStateOf(0f) }
    var elapsed by remember { mutableIntStateOf(0) }
    var voiceOn by remember { mutableStateOf(settings.voice) }
    val custom = settings.ambienceFor(g.id)
    val ambience = custom ?: g.ambience
    var picking by remember { mutableStateOf(false) }

    val estimates = remember(g) { g.steps.map { it.estimateSeconds(ru) } }
    val total = estimates.sum()
    val before = if (index <= 0) 0f else estimates.take(index).sum()
    val progress = if (stage == Stage.DONE) 1f else ((before + inStep) / total).coerceIn(0f, 1f)

    val favKey = "g:${g.id}"
    val favorites by remember { container.db.dao().observeFavorites() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val isFavorite = favorites.any { it.key == favKey }

    val kind = if (g.id == Library.VISIONS_ID) PracticeKind.VISIONS else if (g.kind == GuidedKind.MEDITATION) PracticeKind.MEDITATION else PracticeKind.VISUALIZATION
    fun finishEarly() {
        if (stage == Stage.PLAYING) logSession(context, kind, g.id, elapsed)
        onClose()
    }
    BackHandler { finishEarly() }

    KeepScreenOn(stage == Stage.PLAYING && !paused)
    LaunchedEffect(Unit) { Chimes.prepare(Chime.BOWL, Chime.GONG) }
    DisposableEffect(Unit) {
        onDispose {
            container.stopSpeaking()
            Soundscape.stop()
        }
    }
    // Plays during the session, and as a preview while the sound is being chosen.
    LaunchedEffect(stage, ambience, picking) {
        if ((stage == Stage.PLAYING || picking) && ambience != Ambience.NONE) Soundscape.play(ambience, settings.ambienceVolume)
        else if (stage != Stage.DONE) Soundscape.stop()
    }

    // Wall-clock of actual practice, paused with the session.
    LaunchedEffect(stage, paused) {
        while (stage == Stage.PLAYING && !paused) {
            delay(1000)
            elapsed++
        }
    }

    // The script runner: restarts from the current line after pause, skip or a voice toggle.
    LaunchedEffect(stage, paused, jump, voiceOn) {
        if (stage != Stage.PLAYING || paused) return@LaunchedEffect
        if (index < 0) {
            if (settings.bells) Chimes.play(Chime.BOWL)
            delay(4000)
            index = 0
        }
        while (index < g.steps.size) {
            val step = g.steps[index]
            inStep = 0f
            val said = System.currentTimeMillis()
            Soundscape.duck(voiceOn)
            val heard = voiceOn && container.speak(step.text, settings, if (ru) "ru" else "en", step.audio)
            Soundscape.duck(false)
            if (!heard) {
                // Time to read the line on screen.
                delay((step.text.split(' ').size * 380L).coerceAtLeast(2500L))
            }
            val spoken = (System.currentTimeMillis() - said) / 1000f
            val estSpeech = estimates[index] - step.pause
            var waited = 0
            while (waited < step.pause * 10) {
                delay(100)
                waited++
                inStep = (minOf(spoken, estSpeech) + waited / 10f).coerceAtMost(estimates[index])
            }
            index++
        }
        if (settings.bells) Chimes.play(Chime.GONG)
        logSession(context, kind, g.id, elapsed)
        stage = Stage.DONE
        delay(2500)
        Soundscape.stop()
    }

    fun restartAt(i: Int) {
        container.stopSpeaking()
        index = i.coerceIn(0, g.steps.size - 1)
        jump++
    }

    val step = g.steps.getOrNull(index)
    val vision = step?.tag?.let { visions.getOrNull(it) }

    Box(Modifier.fillMaxSize().background(Color(0xFF12111A))) {
        SceneBackdrop(g.scene)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0x11000000), Color(0x88000000)))))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton(Icons.Rounded.Close, stringResource(R.string.close), { finishEarly() }, size = 44.dp)
                Spacer(Modifier.weight(1f))
                if (stage != Stage.DONE) {
                    GlassButton(if (ambience != Ambience.NONE) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff, stringResource(R.string.ambience), { picking = true }, size = 44.dp)
                    Spacer(Modifier.width(10.dp))
                    GlassButton(
                        if (voiceOn) Icons.Rounded.RecordVoiceOver else Icons.Rounded.VoiceOverOff, stringResource(R.string.voice),
                        { voiceOn = !voiceOn; container.stopSpeaking() }, size = 44.dp,
                    )
                }
                if (g.id != Library.VISIONS_ID) {
                    Spacer(Modifier.width(10.dp))
                    GlassButton(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, stringResource(R.string.favorite), {
                        container.write { if (isFavorite) dao().unfavorite(favKey) else dao().favorite(Favorite(favKey)) }
                    }, size = 44.dp)
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (stage) {
                    Stage.INTRO -> Intro(g, ru, voiceOn && !settings.studioVoice, voiceState) {
                        stage = Stage.PLAYING
                    }
                    Stage.PLAYING -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (vision != null && vision.image.isNotEmpty()) {
                            val img = rememberVisionImage(vision.image)
                            if (img != null) {
                                Image(img, null, Modifier.size(220.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp)), contentScale = ContentScale.Crop)
                                Spacer(Modifier.height(28.dp))
                            }
                        }
                        AnimatedContent(step?.text ?: "", transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(600)) }, label = "line") { text ->
                            Text(
                                text,
                                style = TextStyle(fontFamily = Lora, fontSize = 24.sp, lineHeight = 34.sp, color = Color.White, textAlign = TextAlign.Center),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Stage.DONE -> Done(elapsed, onClose)
                }
            }

            if (stage == Stage.PLAYING) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(clock(elapsed), color = Color.White.copy(alpha = 0.7f), fontFamily = Inter, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text("−" + clock(maxOf(0, (total - before - inStep).toInt())), color = Color.White.copy(alpha = 0.7f), fontFamily = Inter, fontSize = 13.sp)
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 22.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassButton(Icons.Rounded.SkipPrevious, stringResource(R.string.previous), { restartAt(index - 1) })
                    Spacer(Modifier.width(28.dp))
                    GlassButton(
                        if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, stringResource(if (paused) R.string.resume else R.string.pause),
                        {
                            paused = !paused
                            if (paused) {
                                container.stopSpeaking()
                                Soundscape.duck(true)
                            } else Soundscape.duck(false)
                        },
                        size = 72.dp, strong = true,
                    )
                    Spacer(Modifier.width(28.dp))
                    GlassButton(Icons.Rounded.SkipNext, stringResource(R.string.next), {
                        // Past the last line the runner rings the closing bell.
                        container.stopSpeaking()
                        index = (index + 1).coerceIn(0, g.steps.size)
                        jump++
                    })
                }
            }
        }
    }
    if (picking) AmbiencePicker(g.id, g.ambience, settings) {
        picking = false
        if (stage != Stage.PLAYING) Soundscape.stop()
    }
}

@Composable
private fun AmbiencePicker(id: String, designed: Ambience?, settings: Settings, onDismiss: () -> Unit) {
    val container = LocalContext.current.container
    val custom = settings.ambienceFor(id)
    AmbienceSheet(
        designed = designed,
        selected = custom ?: designed ?: Ambience.NONE,
        custom = custom != null,
        volume = settings.ambienceVolume,
        onSelect = { a ->
            if (a == Ambience.NONE) Soundscape.stop()
            container.scope.launch { container.settings.update { it.withAmbience(id, a) } }
        },
        onVolume = { v -> container.scope.launch { container.settings.update { it.copy(ambienceVolume = v) } } },
        onDismiss = onDismiss,
    )
}

@Composable
private fun Intro(g: Guided, ru: Boolean, voiceOn: Boolean, state: Voice.State, onStart: () -> Unit) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(if (g.kind == GuidedKind.MEDITATION) R.string.kind_meditation else R.string.kind_visualization).uppercase(),
            color = Color.White.copy(alpha = 0.75f), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 1.6.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(g.title, style = TextStyle(fontFamily = Lora, fontSize = 34.sp, lineHeight = 40.sp, color = Color.White, textAlign = TextAlign.Center))
        if (g.subtitle.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(g.subtitle, color = Color.White.copy(alpha = 0.82f), fontFamily = Inter, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(10.dp))
        Text(minutesLabel(g.estimateMinutes(ru)), color = Color.White.copy(alpha = 0.7f), fontFamily = Inter, fontSize = 14.sp)
        Spacer(Modifier.height(36.dp))
        GlassButton(Icons.Rounded.PlayArrow, stringResource(R.string.begin), onStart, size = 84.dp, strong = true)
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.intro_hint), color = Color.White.copy(alpha = 0.7f), fontFamily = Inter, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (voiceOn && (state == Voice.State.MISSING_LANGUAGE || state == Voice.State.UNAVAILABLE)) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.voice_missing), color = Color(0xFFFFE0B8), fontFamily = Inter, fontSize = 13.sp, textAlign = TextAlign.Center)
            TextButton(onClick = {
                runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }) { Text(stringResource(R.string.voice_settings), color = Color.White, fontFamily = Inter, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun Done(seconds: Int, onClose: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.done_title), style = TextStyle(fontFamily = Lora, fontSize = 34.sp, color = Color.White, textAlign = TextAlign.Center))
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.done_text, maxOf(1, (seconds + 30) / 60)),
            color = Color.White.copy(alpha = 0.82f), fontFamily = Inter, fontSize = 16.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))
        Box(
            Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).padding(horizontal = 36.dp, vertical = 14.dp),
        ) {
            TextButton(onClick = onClose) { Text(stringResource(R.string.finish), color = Color(0xFF1F1D2B), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
        }
    }
}
