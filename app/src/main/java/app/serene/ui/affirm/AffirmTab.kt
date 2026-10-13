package app.serene.ui.affirm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.R
import app.serene.container
import app.serene.content.Affirmation
import app.serene.content.Library
import app.serene.data.Settings
import app.serene.ui.Go
import app.serene.ui.components.IconDisc
import app.serene.ui.components.cardBrush
import app.serene.ui.components.cardGradients
import app.serene.ui.library
import app.serene.ui.theme.Inter
import app.serene.ui.theme.Lora
import app.serene.ui.theme.U
import app.serene.ui.theme.UType

const val FAVORITES = "favorites"
const val MIX = "mix"

/** The affirmations behind a list id: a stock theme, "mine", "favorites" or "mix". */
@Composable
fun rememberAffirmations(setId: String, settings: Settings): List<Affirmation> {
    val dao = LocalContext.current.container.db.dao()
    val lib = library()
    val favorites by remember { dao.observeFavorites() }.collectAsStateWithLifecycle(initialValue = null)
    val mine by remember { dao.observeMine() }.collectAsStateWithLifecycle(initialValue = null)
    val f = favorites ?: return emptyList()
    val m = mine ?: return emptyList()
    return remember(setId, lib, f, m, settings.focus) {
        when (setId) {
            Library.MINE -> m.map { Affirmation("m:${it.id}", it.text, Library.MINE, it.audio) }
            FAVORITES -> f.mapNotNull { lib.affirmation(it.key, m) }
            MIX -> lib.mix(settings.focus, f.map { it.key }, m).let { all ->
                // A fresh, short playlist each day rather than all 240 lines.
                val day = java.time.LocalDate.now().toEpochDay()
                all.shuffled(kotlin.random.Random(day)).take(15)
            }
            else -> lib.set(setId)?.let(lib::affirmations).orEmpty()
        }
    }
}

@Composable
fun AffirmTab(settings: Settings, go: Go) {
    val dao = LocalContext.current.container.db.dao()
    val lib = library()
    val favorites by remember { dao.observeFavorites() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val mine by remember { dao.observeMine() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val favCount = favorites.count { !it.key.startsWith("g:") }

    LazyVerticalGrid(
        GridCells.Fixed(2),
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Text(stringResource(R.string.tab_affirm), style = UType.Title, modifier = Modifier.padding(top = 18.dp, start = 4.dp))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.affirm_sub), style = UType.Sub, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().clip(U.RLg).background(Brush.linearGradient(listOf(Color(0xFF5B4FC4), Color(0xFF9A7FD6), Color(0xFFE39A7B))))
                        .clickable { go.play(MIX) }.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.mix_title), style = TextStyle(fontFamily = Lora, fontSize = 24.sp, color = Color.White))
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.mix_text), color = Color.White.copy(alpha = 0.85f), fontFamily = Inter, fontSize = 13.5.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF5B4FC4))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        item {
            SmallTile(Icons.Rounded.Favorite, stringResource(R.string.favorites), pluralStringResource(R.plurals.affirmations, favCount, favCount), U.c.rose, U.c.roseSoft) { go.set(FAVORITES) }
        }
        item {
            SmallTile(Icons.Rounded.Edit, stringResource(R.string.mine), pluralStringResource(R.plurals.affirmations, mine.size, mine.size), U.c.sage, U.c.sageSoft) { go.set(Library.MINE) }
        }
        item(span = { GridItemSpan(2) }) {
            Text(stringResource(R.string.themes).uppercase(), style = UType.Eyebrow, modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp))
        }
        itemsIndexed(lib.sets, key = { _, s -> s.id }) { i, s ->
            Column(
                Modifier.fillMaxWidth().height(118.dp).clip(U.RMd).background(cardBrush(i)).clickable { go.set(s.id) }.padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(s.title, style = TextStyle(fontFamily = Lora, fontSize = 19.sp, lineHeight = 23.sp, color = Color.White))
                Text(
                    if (s.id in settings.focus) "★  " + pluralStringResource(R.plurals.affirmations, s.items.size, s.items.size)
                    else pluralStringResource(R.plurals.affirmations, s.items.size, s.items.size),
                    color = Color.White.copy(alpha = 0.85f), fontFamily = Inter, fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SmallTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, color: Color, soft: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(U.RMd).background(soft).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDisc(icon, 38.dp, color, U.c.surface.copy(alpha = 0.7f))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = UType.Strong.copy(fontSize = 15.sp))
            Text(sub, style = UType.Small.copy(color = U.c.muted))
        }
    }
}

/** Index into [cardGradients] for a list id, so a theme keeps its colour everywhere. */
fun gradientIndex(setId: String, lib: Library): Int = when (setId) {
    Library.MINE -> 2
    FAVORITES -> 4
    MIX -> 0
    else -> lib.sets.indexOfFirst { it.id == setId }.coerceAtLeast(0) % cardGradients.size
}
