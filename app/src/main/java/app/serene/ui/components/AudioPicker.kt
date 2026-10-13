package app.serene.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serene.R
import app.serene.audio.Narrator
import app.serene.container
import app.serene.ui.locale
import app.serene.ui.theme.U
import app.serene.ui.theme.UType
import kotlinx.coroutines.launch

/**
 * How the user's own words are voiced: by the phone's speech engine, or by an audio file they
 * upload (their own voice, a recording from a coach…). [audio] is a file name in filesDir/audio.
 * Replaced files are the caller's to delete on save, so a cancelled edit keeps the old one.
 */
@Composable
fun AudioPicker(audio: String, text: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val scope = rememberCoroutineScope()
    val loc = locale()
    var recording by remember { mutableStateOf(false) }
    if (recording) RecordDialog(text, onDismiss = { recording = false }, onDone = onChange)
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { Narrator.import(context, uri)?.let(onChange) }
    }
    Column(Modifier.fillMaxWidth().clip(U.RSm).background(U.c.surface2).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (audio.isEmpty()) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.AudioFile, null, tint = U.c.accent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (audio.isEmpty()) R.string.audio_tts else R.string.audio_own), style = UType.Strong.copy(fontSize = 14.5.sp))
                Text(stringResource(if (audio.isEmpty()) R.string.audio_tts_text else R.string.audio_own_text), style = UType.Small.copy(color = U.c.muted))
            }
            IconButton(onClick = {
                scope.launch {
                    container.stopSpeaking()
                    if (audio.isNotEmpty()) container.narrator.playFile(audio, 1f)
                    else {
                        val s = container.settings.now()
                        container.voice.setLanguage(loc)
                        container.voice.say(text.ifBlank { "…" }, s.voiceRate)
                    }
                }
            }) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.listen), tint = U.c.accent) }
            if (audio.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Rounded.Close, stringResource(R.string.audio_remove), tint = U.c.muted) }
        }
        Spacer(Modifier.height(2.dp))
        Row {
            Text(
                stringResource(R.string.audio_record),
                style = UType.Button.copy(color = U.c.accent),
                modifier = Modifier.clip(U.RSm).clickable { recording = true }.padding(vertical = 8.dp),
            )
            Spacer(Modifier.width(20.dp))
            Text(
                stringResource(if (audio.isEmpty()) R.string.audio_upload else R.string.audio_replace),
                style = UType.Button.copy(color = U.c.accent),
                modifier = Modifier.clip(U.RSm).clickable { pick.launch(arrayOf("audio/*")) }.padding(vertical = 8.dp),
            )
        }
    }
}
