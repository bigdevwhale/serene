package app.serene.ui.visions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.R
import app.serene.container
import app.serene.content.Library
import app.serene.data.Vision
import app.serene.ui.Go
import app.serene.ui.components.PrimaryButton
import app.serene.ui.components.TopBar
import app.serene.ui.components.cardBrush
import app.serene.ui.theme.Lora
import app.serene.ui.theme.U
import app.serene.ui.theme.UType

@Composable
fun VisionsScreen(go: Go) {
    val dao = LocalContext.current.container.db.dao()
    val visions by remember { dao.observeVisions() }.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.visions_title), onBack = { go.back() })
        LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                Text(stringResource(R.string.visions_text), style = UType.Sub, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp))
            }
            items(visions, key = { it.id }) { v -> VisionTile(v) { go.vision(v.id) } }
            item {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(0.8f).clip(U.RMd).border(1.5.dp, U.c.line, U.RMd).clickable { go.vision(0) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(30.dp), tint = U.c.accent)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.vision_add), style = UType.Button.copy(color = U.c.accent))
                    }
                }
            }
        }
        if (visions.isNotEmpty()) {
            PrimaryButton(
                stringResource(R.string.visualize_now), { go.guided(Library.VISIONS_ID) },
                Modifier.fillMaxWidth().padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 12.dp),
                icon = Icons.Rounded.PlayArrow,
            )
        }
    }
}

@Composable
private fun VisionTile(v: Vision, onClick: () -> Unit) {
    val img = rememberVisionImage(v.image)
    Box(Modifier.fillMaxWidth().aspectRatio(0.8f).clip(U.RMd).background(cardBrush(v.hue)).clickable(onClick = onClick)) {
        if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)), startY = 150f)))
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(v.title, style = TextStyle(fontFamily = Lora, fontSize = 17.sp, lineHeight = 21.sp, color = Color.White), maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (v.affirmation.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(v.affirmation, style = UType.Small.copy(color = Color.White.copy(alpha = 0.85f)), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
