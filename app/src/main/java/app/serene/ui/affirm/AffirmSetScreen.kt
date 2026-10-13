package app.serene.ui.affirm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.R
import app.serene.container
import app.serene.content.Affirmation
import app.serene.content.Library
import app.serene.data.Favorite
import app.serene.data.MyAffirmation
import app.serene.data.Settings
import app.serene.ui.Go
import app.serene.ui.components.ConfirmDialog
import app.serene.ui.components.PrimaryButton
import app.serene.ui.components.SoftButton
import app.serene.ui.components.TopBar
import app.serene.ui.components.UTextField
import app.serene.ui.library
import app.serene.ui.theme.U
import app.serene.ui.theme.UType
import kotlinx.coroutines.launch

@Composable
fun AffirmSetScreen(setId: String, settings: Settings, go: Go) {
    val context = LocalContext.current
    val container = context.container
    val dao = container.db.dao()
    val lib = library()
    val items = rememberAffirmations(setId, settings)
    val favorites by remember { dao.observeFavorites() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val fav = remember(favorites) { favorites.map { it.key }.toSet() }
    val title = when (setId) {
        Library.MINE -> stringResource(R.string.mine)
        FAVORITES -> stringResource(R.string.favorites)
        else -> lib.set(setId)?.title.orEmpty()
    }
    var draft by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Affirmation?>(null) }
    var deleting by remember { mutableStateOf<Affirmation?>(null) }
    val stock = lib.set(setId) != null
    val inFocus = setId in settings.focus

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(title, onBack = { go.back() }) {
            if (stock) {
                IconButton(onClick = {
                    container.scope.launch { container.settings.update { it.copy(focus = if (inFocus) it.focus - setId else it.focus + setId) } }
                }) { Icon(if (inFocus) Icons.Rounded.Star else Icons.Rounded.StarBorder, stringResource(R.string.focus_toggle), tint = if (inFocus) U.c.peach else U.c.muted) }
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)) {
            if (setId == Library.MINE) item {
                Text(stringResource(R.string.mine_hint), style = UType.Sub, modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(12.dp))
                UTextField(draft, { draft = it }, placeholder = stringResource(R.string.mine_placeholder), minLines = 2)
                Spacer(Modifier.height(10.dp))
                SoftButton(stringResource(R.string.add), onClick = {
                    val t = draft.trim()
                    if (t.isNotEmpty()) {
                        container.write { dao().insertMine(MyAffirmation(text = t)) }
                        draft = ""
                    }
                }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(18.dp))
            }
            if (items.isNotEmpty()) item {
                PrimaryButton(stringResource(R.string.play_all), { go.play(setId) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
                Spacer(Modifier.height(14.dp))
            } else if (setId == FAVORITES) item {
                Text(stringResource(R.string.favorites_empty), style = UType.Sub, modifier = Modifier.padding(4.dp))
            }
            items(items, key = { it.key }) { a ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd)
                        .clickable { go.play(setId, a.key) }.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(a.text, style = UType.Quote.copy(fontSize = 17.sp, lineHeight = 24.sp), modifier = Modifier.weight(1f).padding(vertical = 6.dp))
                    if (setId == Library.MINE) {
                        IconButton(onClick = { editing = a }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = U.c.faint) }
                    }
                    val on = a.key in fav
                    IconButton(onClick = { container.write { if (on) dao().unfavorite(a.key) else dao().favorite(Favorite(a.key)) } }) {
                        Icon(if (on) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, stringResource(R.string.favorite), tint = if (on) U.c.rose else U.c.faint)
                    }
                }
            }
        }
    }

    editing?.let { a ->
        var text by remember(a.key) { mutableStateOf(a.text) }
        val id = a.key.removePrefix("m:").toLongOrNull() ?: 0L
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = U.c.bg,
            title = { Text(stringResource(R.string.edit), style = UType.Strong) },
            text = { UTextField(text, { text = it }, minLines = 3) },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) container.write { dao().updateMine(id, text.trim()) }
                    editing = null
                }) { Text(stringResource(R.string.save), color = U.c.accent, style = UType.Button) }
            },
            dismissButton = {
                Row {
                    IconButton(onClick = { deleting = a; editing = null }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.danger) }
                    Box(Modifier.width(4.dp))
                    TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) }
                }
            },
        )
    }
    deleting?.let { a ->
        ConfirmDialog(
            stringResource(R.string.delete_affirmation), a.text, stringResource(R.string.delete),
            onConfirm = { a.key.removePrefix("m:").toLongOrNull()?.let { id -> container.write { dao().deleteMine(id) } } },
            onDismiss = { deleting = null }, danger = true,
        )
    }
}
