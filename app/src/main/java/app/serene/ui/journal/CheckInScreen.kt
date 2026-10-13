package app.serene.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serene.R
import app.serene.container
import app.serene.data.JournalEntry
import app.serene.ui.components.ConfirmDialog
import app.serene.ui.components.MoodFace
import app.serene.ui.components.PrimaryButton
import app.serene.ui.components.SectionTitle
import app.serene.ui.components.TopBar
import app.serene.ui.components.UTextField
import app.serene.ui.components.moodColor
import app.serene.ui.moodLabel
import app.serene.ui.theme.U
import app.serene.ui.theme.UType
import java.time.LocalDate

@Composable
fun CheckInScreen(id: Long, initialMood: Int, onDone: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    var loaded by remember { mutableStateOf<JournalEntry?>(null) }
    var mood by rememberSaveable { mutableStateOf(initialMood.takeIf { it in 1..5 }) }
    var g1 by rememberSaveable { mutableStateOf("") }
    var g2 by rememberSaveable { mutableStateOf("") }
    var g3 by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (id > 0) container.db.dao().entry(id)?.let { e ->
            loaded = e
            mood = e.mood
            val lines = e.gratitude.lines()
            g1 = lines.getOrElse(0) { "" }; g2 = lines.getOrElse(1) { "" }; g3 = lines.drop(2).joinToString(" ")
            note = e.note
        }
    }
    val prompts = stringArrayResource(R.array.gratitude_prompts)
    val prompt = remember { prompts[Math.floorMod(LocalDate.now().toEpochDay().toInt(), prompts.size)] }
    val canSave = mood != null || listOf(g1, g2, g3, note).any { it.isNotBlank() }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (id > 0) R.string.edit_entry else R.string.check_in), onBack = onDone) {
            if (loaded != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            SectionTitle(stringResource(R.string.how_are_you))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (1..5).forEach { m ->
                    val on = mood == m
                    Column(
                        Modifier.clip(U.RSm)
                            .background(if (on) moodColor(m).copy(alpha = 0.16f) else Color.Transparent)
                            .border(1.dp, if (on) moodColor(m).copy(alpha = 0.6f) else Color.Transparent, U.RSm)
                            .clickable { mood = if (on) null else m }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MoodFace(m, 48.dp, filled = on || mood == null)
                        Spacer(Modifier.height(4.dp))
                        Text(moodLabel(m), style = UType.Small.copy(fontSize = 11.sp, color = if (on) U.c.ink else U.c.muted))
                    }
                }
            }

            SectionTitle(stringResource(R.string.gratitude_title))
            Text(prompt, style = UType.Sub, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
            UTextField(g1, { g1 = it }, placeholder = stringResource(R.string.gratitude_1))
            Spacer(Modifier.height(8.dp))
            UTextField(g2, { g2 = it }, placeholder = stringResource(R.string.gratitude_2))
            Spacer(Modifier.height(8.dp))
            UTextField(g3, { g3 = it }, placeholder = stringResource(R.string.gratitude_3))

            SectionTitle(stringResource(R.string.note))
            UTextField(note, { note = it }, placeholder = stringResource(R.string.note_placeholder), minLines = 4)
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            stringResource(R.string.save),
            onClick = {
                val gratitude = listOf(g1, g2, g3).map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
                val prev = loaded
                container.write {
                    if (prev != null) dao().updateEntry(prev.copy(mood = mood, gratitude = gratitude, note = note.trim()))
                    else dao().insertEntry(JournalEntry(at = System.currentTimeMillis(), mood = mood, gratitude = gratitude, note = note.trim()))
                }
                onDone()
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 12.dp),
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.delete_entry), stringResource(R.string.delete_entry_text), stringResource(R.string.delete),
            onConfirm = { loaded?.let { e -> container.write { dao().deleteEntry(e) } }; onDone() },
            onDismiss = { confirmDelete = false }, danger = true,
        )
    }
}
