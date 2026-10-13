package app.serene.ui.visions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.serene.R
import app.serene.container
import app.serene.data.Vision
import app.serene.audio.Narrator
import app.serene.ui.components.AudioPicker
import app.serene.ui.components.ConfirmDialog
import app.serene.ui.components.PrimaryButton
import app.serene.ui.components.SectionTitle
import app.serene.ui.components.TopBar
import app.serene.ui.components.UTextField
import app.serene.ui.components.cardBrush
import app.serene.ui.components.cardGradients
import app.serene.ui.theme.U
import app.serene.ui.theme.UType
import kotlinx.coroutines.launch

@Composable
fun EditVisionScreen(id: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<Vision?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }
    var affirmation by rememberSaveable { mutableStateOf("") }
    var image by rememberSaveable { mutableStateOf("") }
    var audio by rememberSaveable { mutableStateOf("") }
    var hue by rememberSaveable { mutableIntStateOf((0 until cardGradients.size).random()) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (id > 0) container.db.dao().vision(id)?.let { v ->
            loaded = v
            title = v.title; details = v.details; affirmation = v.affirmation; image = v.image; hue = v.hue; audio = v.audio
        }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            VisionImages.import(context, uri)?.let { name ->
                // A replaced photo that was never saved is just litter.
                if (image.isNotEmpty() && image != loaded?.image) VisionImages.delete(context, image)
                image = name
            }
        }
    }
    val img = rememberVisionImage(image)

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (id > 0) R.string.vision_edit else R.string.vision_new), onBack = onDone) {
            if (loaded != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Box(
                Modifier.fillMaxWidth().height(190.dp).clip(U.RLg).background(cardBrush(hue))
                    .clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center,
            ) {
                if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(CircleShape).background(Color(0x66000000)).padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(18.dp), tint = Color.White)
                    Text(" " + stringResource(if (img != null) R.string.photo_change else R.string.photo_add), style = UType.Small.copy(color = Color.White))
                }
            }
            if (img == null) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cardGradients.indices.forEach { i ->
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(cardBrush(i))
                                .border(2.dp, if (i == hue) U.c.ink else Color.Transparent, CircleShape)
                                .clickable { hue = i },
                        )
                    }
                }
            } else {
                TextButton(onClick = {
                    if (image != loaded?.image) VisionImages.delete(context, image)
                    image = ""
                }) { Text(stringResource(R.string.photo_remove), color = U.c.muted, style = UType.Button) }
            }

            SectionTitle(stringResource(R.string.vision_what))
            UTextField(title, { title = it }, placeholder = stringResource(R.string.vision_what_hint))
            SectionTitle(stringResource(R.string.vision_details))
            Text(stringResource(R.string.vision_details_text), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            UTextField(details, { details = it }, placeholder = stringResource(R.string.vision_details_hint), minLines = 3)
            SectionTitle(stringResource(R.string.vision_affirmation))
            UTextField(affirmation, { affirmation = it }, placeholder = stringResource(R.string.vision_affirmation_hint))
            SectionTitle(stringResource(R.string.vision_voice))
            Text(stringResource(R.string.vision_voice_text), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            AudioPicker(audio, listOf(title, details, affirmation).filter { it.isNotBlank() }.joinToString(". ")) { new ->
                if (audio != loaded?.audio) Narrator.delete(context, audio)
                audio = new
            }
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            stringResource(R.string.save),
            onClick = {
                val prev = loaded
                val v = Vision(
                    id = prev?.id ?: 0, title = title.trim(), details = details.trim(), affirmation = affirmation.trim(),
                    image = image, hue = hue, createdAt = prev?.createdAt ?: System.currentTimeMillis(), audio = audio,
                )
                if (prev != null && prev.audio.isNotEmpty() && prev.audio != audio) Narrator.delete(context, prev.audio)
                if (prev != null && prev.image.isNotEmpty() && prev.image != image) VisionImages.delete(context, prev.image)
                container.write { if (prev != null) dao().updateVision(v) else dao().insertVision(v) }
                onDone()
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 12.dp),
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.vision_delete), loaded?.title.orEmpty(), stringResource(R.string.delete),
            onConfirm = {
                loaded?.let { v ->
                    VisionImages.delete(context, v.image)
                    Narrator.delete(context, v.audio)
                    if (audio != v.audio) Narrator.delete(context, audio)
                    if (image != v.image) VisionImages.delete(context, image)
                    container.write { dao().deleteVision(v) }
                }
                onDone()
            },
            onDismiss = { confirmDelete = false }, danger = true,
        )
    }
}
