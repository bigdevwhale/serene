package app.serene.ui.practice

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.R
import app.serene.container
import app.serene.content.Guided
import app.serene.content.Scene
import app.serene.content.estimateMinutes
import app.serene.data.Settings
import app.serene.domain.Pattern
import app.serene.ui.Go
import app.serene.ui.components.IconDisc
import app.serene.ui.components.SceneBackdrop
import app.serene.ui.components.Segmented
import app.serene.ui.library
import app.serene.ui.minutesLabel
import app.serene.ui.patternText
import app.serene.ui.patternTitle
import app.serene.ui.theme.U
import app.serene.ui.theme.UType

private enum class Section { MEDITATE, VISUALIZE, BREATHE }

@Composable
fun PracticeTab(settings: Settings, go: Go) {
    val context = LocalContext.current
    val dao = context.container.db.dao()
    val lib = library()
    var section by rememberSaveable { mutableStateOf(Section.MEDITATE) }
    val sessions by remember { dao.observeSessions() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val favorites by remember { dao.observeFavorites() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val visions by remember { dao.observeVisions() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val done = remember(sessions) { sessions.map { it.ref }.toSet() }
    val fav = remember(favorites) { favorites.map { it.key }.toSet() }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)) {
        item {
            Text(stringResource(R.string.tab_practice), style = UType.Title, modifier = Modifier.padding(top = 18.dp, start = 4.dp))
            Spacer(Modifier.height(16.dp))
            Segmented(
                Section.entries, section,
                label = {
                    stringResource(
                        when (it) {
                            Section.MEDITATE -> R.string.sec_meditate
                            Section.VISUALIZE -> R.string.sec_visualize
                            Section.BREATHE -> R.string.sec_breathe
                        }
                    )
                },
                onSelect = { section = it },
            )
            Spacer(Modifier.height(14.dp))
        }
        when (section) {
            Section.MEDITATE -> {
                item {
                    WideCard(Icons.Rounded.Timer, stringResource(R.string.timer_title), stringResource(R.string.timer_card), U.c.sky, U.c.skySoft) { go.timer() }
                    Spacer(Modifier.height(10.dp))
                }
                items(lib.meditations, key = { it.id }) { g ->
                    GuidedCard(g, lib.ru, g.id in done, "g:${g.id}" in fav) { go.guided(g.id) }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Section.VISUALIZE -> {
                item {
                    WideCard(
                        Icons.Rounded.Dashboard, stringResource(R.string.visions_title),
                        if (visions.isEmpty()) stringResource(R.string.visions_card_empty) else stringResource(R.string.visions_card, visions.size),
                        U.c.peach, U.c.peachSoft,
                    ) { go.visions() }
                    Spacer(Modifier.height(10.dp))
                }
                items(lib.visualizations, key = { it.id }) { g ->
                    GuidedCard(g, lib.ru, g.id in done, "g:${g.id}" in fav) { go.guided(g.id) }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Section.BREATHE -> {
                items(Pattern.all, key = { it.id }) { p ->
                    Row(
                        Modifier.fillMaxWidth().clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd)
                            .clickable { go.breathe(p.id) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(64.dp).clip(U.RSm), contentAlignment = Alignment.Center) {
                            SceneBackdrop(Scene.NIGHT, animate = false)
                            Text(p.rhythm(), style = UType.Small.copy(color = Color.White), modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xE6141230)).padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(patternTitle(p.id), style = UType.Strong)
                            Spacer(Modifier.height(3.dp))
                            Text(patternText(p.id), style = UType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun GuidedCard(g: Guided, ru: Boolean, done: Boolean, favorite: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(68.dp).clip(U.RSm)) { SceneBackdrop(g.scene, animate = false) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(g.title, style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(g.subtitle, style = UType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(minutesLabel(g.estimateMinutes(ru)), style = UType.Small)
                if (done) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(14.dp), tint = U.c.sage)
                if (favorite) Icon(Icons.Rounded.Favorite, null, Modifier.size(14.dp), tint = U.c.rose)
            }
        }
    }
}

@Composable
fun WideCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, color: Color, soft: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(U.RMd).background(soft).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDisc(icon, 46.dp, color, U.c.surface.copy(alpha = 0.7f))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong)
            Spacer(Modifier.height(2.dp))
            Text(text, style = UType.Muted)
        }
    }
}
