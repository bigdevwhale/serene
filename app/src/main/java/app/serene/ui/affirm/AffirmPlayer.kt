package app.serene.ui.affirm

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VoiceOverOff
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.R
import app.serene.audio.Soundscape
import app.serene.container
import app.serene.content.Ambience
import app.serene.data.Favorite
import app.serene.data.PracticeKind
import app.serene.data.Settings
import app.serene.ui.components.GlassButton
import app.serene.ui.components.KeepScreenOn
import app.serene.ui.components.cardGradients
import app.serene.ui.components.logSession
import app.serene.ui.library
import app.serene.ui.locale
import app.serene.ui.theme.Inter
import app.serene.ui.theme.Lora
import kotlinx.coroutines.delay

/** One affirmation per screen. In autoplay the voice says it, then leaves room to repeat it aloud. */
@Composable
fun AffirmPlayer(setId: String, start: String, settings: Settings, onClose: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val items = rememberAffirmations(setId, settings)
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color(0xFF12111A)))
        return
    }
    val lib = library()
    val voice = container.voice
    val loc = locale()
    val voiceState by voice.state.collectAsStateWithLifecycle()
    LaunchedEffect(loc, voiceState == app.serene.audio.Voice.State.LOADING) { voice.setLanguage(loc) }

    val first = remember(items) { items.indexOfFirst { it.key == start }.coerceAtLeast(0) }
    val pager = rememberPagerState(first) { items.size }
    // Autoplay starts by itself for the playlist, by tap for a single card.
    var auto by remember { mutableStateOf(start.isEmpty()) }
    var voiceOn by remember { mutableStateOf(settings.voice) }
    var elapsed by remember { mutableIntStateOf(0) }
    val favorites by remember { container.db.dao().observeFavorites() }.collectAsStateWithLifecycle(initialValue = emptyList())

    fun close() {
        logSession(context, PracticeKind.AFFIRMATIONS, setId, elapsed)
        onClose()
    }
    BackHandler { close() }
    KeepScreenOn(auto)
    DisposableEffect(Unit) {
        onDispose {
            voice.stop()
            Soundscape.stop()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsed++
        }
    }
    LaunchedEffect(auto) {
        if (auto) Soundscape.play(Ambience.DRONE, settings.ambienceVolume * 0.6f) else Soundscape.stop()
    }
    LaunchedEffect(auto, pager.currentPage, voiceOn) {
        if (!auto) {
            voice.stop()
            return@LaunchedEffect
        }
        delay(700)
        val a = items[pager.currentPage]
        if (voiceOn && voice.canSpeak) {
            Soundscape.duck(true)
            voice.say(a.text, settings.voiceRate)
            Soundscape.duck(false)
            // Room to say it back.
            delay(5500)
        } else delay(4000L + a.text.split(' ').size * 450L)
        if (pager.currentPage < items.size - 1) pager.animateScrollToPage(pager.currentPage + 1)
        else auto = false
    }

    val brush = Brush.linearGradient(cardGradients[gradientIndex(setId, lib)].let { listOf(it[0], it[1], Color(0xFF2B2550)) })
    Box(Modifier.fillMaxSize().background(brush)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            Box(Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                Text(
                    items[page].text,
                    style = TextStyle(fontFamily = Lora, fontSize = 30.sp, lineHeight = 41.sp, color = Color.White, textAlign = TextAlign.Center),
                )
            }
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton(Icons.Rounded.Close, stringResource(R.string.close), { close() }, size = 44.dp)
                Spacer(Modifier.weight(1f))
                Text("${pager.currentPage + 1} / ${items.size}", color = Color.White.copy(alpha = 0.8f), fontFamily = Inter, fontSize = 14.sp)
            }
            Spacer(Modifier.weight(1f))
            if (auto) {
                Text(
                    stringResource(R.string.repeat_hint), color = Color.White.copy(alpha = 0.75f), fontFamily = Inter, fontSize = 13.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
            }
            val a = items[pager.currentPage]
            val on = favorites.any { it.key == a.key }
            Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                GlassButton(if (on) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, stringResource(R.string.favorite), {
                    container.write { if (on) dao().unfavorite(a.key) else dao().favorite(Favorite(a.key)) }
                })
                Spacer(Modifier.width(20.dp))
                GlassButton(if (voiceOn) Icons.Rounded.RecordVoiceOver else Icons.Rounded.VoiceOverOff, stringResource(R.string.voice), {
                    voiceOn = !voiceOn
                    voice.stop()
                })
                Spacer(Modifier.width(20.dp))
                GlassButton(if (auto) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(R.string.autoplay), { auto = !auto }, size = 68.dp, strong = true)
                Spacer(Modifier.width(20.dp))
                GlassButton(Icons.Rounded.Share, stringResource(R.string.share), {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, a.text), null))
                })
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
