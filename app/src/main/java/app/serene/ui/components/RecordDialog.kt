package app.serene.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.serene.R
import app.serene.audio.Narrator
import app.serene.ui.clock
import app.serene.ui.theme.U
import app.serene.ui.theme.UType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/** Microphone capture into filesDir/audio as AAC (.m4a). */
private class Recorder(private val context: Context) {
    private var rec: MediaRecorder? = null
    var name: String = ""
        private set

    fun start(): Boolean = runCatching {
        name = "${UUID.randomUUID()}.m4a"
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(96_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(Narrator.file(context, name).path)
        r.prepare()
        r.start()
        rec = r
    }.isSuccess

    /** 0..1, for the level meter. */
    fun level(): Float = (rec?.let { runCatching { it.maxAmplitude }.getOrDefault(0) } ?: 0) / 32767f

    /** Returns the file name, or null when the take was too short to keep. */
    fun stop(): String? {
        val r = rec ?: return null
        rec = null
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        if (!ok) {
            Narrator.delete(context, name)
            return null
        }
        return name
    }

    fun cancel() {
        stop()?.let { Narrator.delete(context, it) }
    }
}

/**
 * Record your own voice: tap to start, tap to stop, listen back, keep or try again.
 * Calls [onDone] with the new file name; the dialog deletes takes the user didn't keep.
 */
@Composable
fun RecordDialog(text: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val context = LocalContext.current
    val recorder = remember { Recorder(context) }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var recording by remember { mutableStateOf(false) }
    var take by remember { mutableStateOf<String?>(null) }
    var seconds by remember { mutableIntStateOf(0) }
    var level by remember { mutableFloatStateOf(0f) }
    var failed by remember { mutableStateOf(false) }
    val narrator = remember { Narrator(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.RECORD_AUDIO) }
    LaunchedEffect(recording) {
        while (recording) {
            delay(100)
            level = level * 0.6f + recorder.level() * 0.4f
        }
    }
    LaunchedEffect(recording) {
        seconds = 0
        while (recording) {
            delay(1000)
            seconds++
            // A vision rarely needs more than a few minutes; stop runaway takes.
            if (seconds >= 600) { take = recorder.stop(); recording = false }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            narrator.stop()
            recorder.cancel()
        }
    }

    fun discardTake() {
        take?.let { Narrator.delete(context, it) }
        take = null
    }

    AlertDialog(
        onDismissRequest = { discardTake(); onDismiss() },
        containerColor = U.c.bg,
        title = { Text(stringResource(R.string.record_title), style = UType.Strong.copy(fontSize = 19.sp)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (text.isNotBlank()) {
                    Text(text, style = UType.Quote.copy(fontSize = 17.sp, lineHeight = 24.sp, textAlign = TextAlign.Center))
                    Spacer(Modifier.height(18.dp))
                }
                Box(contentAlignment = Alignment.Center) {
                    val ring = U.c.accent
                    Canvas(Modifier.size(120.dp)) {
                        if (recording) drawCircle(ring.copy(alpha = 0.18f), size.minDimension / 2 * (0.62f + 0.38f * level.coerceIn(0f, 1f)))
                    }
                    Box(
                        Modifier.size(76.dp).clip(CircleShape).background(if (recording) U.c.danger else U.c.accent)
                            .clickable(enabled = granted) {
                                if (recording) {
                                    take = recorder.stop()
                                    recording = false
                                    failed = take == null
                                } else {
                                    narrator.stop()
                                    discardTake()
                                    failed = !recorder.start()
                                    recording = !failed
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Icon(if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic, null, Modifier.size(34.dp), tint = U.c.onAccent) }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        !granted -> stringResource(R.string.record_permission)
                        failed -> stringResource(R.string.record_failed)
                        recording -> clock(seconds)
                        take != null -> stringResource(R.string.record_done, clock(seconds))
                        else -> stringResource(R.string.record_hint)
                    },
                    style = UType.Muted, textAlign = TextAlign.Center,
                )
                if (take != null && !recording) {
                    TextButton(onClick = { take?.let { t -> scope.launch { narrator.playFile(t, 1f) } } }) {
                        Text(stringResource(R.string.record_listen), color = U.c.accent, style = UType.Button)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = take != null && !recording, onClick = {
                val t = take!!
                take = null
                onDone(t)
                onDismiss()
            }) { Text(stringResource(R.string.save), color = if (take != null && !recording) U.c.accent else U.c.faint, style = UType.Button) }
        },
        dismissButton = {
            TextButton(onClick = { discardTake(); onDismiss() }) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) }
        },
    )
}
