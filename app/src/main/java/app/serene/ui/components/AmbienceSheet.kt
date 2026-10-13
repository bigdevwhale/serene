package app.serene.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serene.R
import app.serene.audio.Soundscape
import app.serene.content.Ambience
import app.serene.ui.ambienceLabel
import app.serene.ui.theme.Inter

private val SheetBg = Color(0xFF1E1B2E)

/**
 * Background sound for one session: the one it was written for, any other soundscape, or
 * silence, plus the volume. Choices preview live while the sheet is open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbienceSheet(
    designed: Ambience?,
    selected: Ambience,
    custom: Boolean,
    volume: Float,
    onSelect: (Ambience?) -> Unit,
    onVolume: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var level by remember { mutableFloatStateOf(volume) }
    ModalBottomSheet(
        onDismissRequest = { onVolume(level); onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg,
        contentColor = Color.White,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(stringResource(R.string.ambience), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = Color.White)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolumeDown, null, tint = Color.White.copy(alpha = 0.7f))
                Slider(
                    value = level,
                    onValueChange = { level = it; Soundscape.setVolume(it) },
                    onValueChangeFinished = { onVolume(level) },
                    valueRange = 0.05f..1f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.2f)),
                )
                Icon(Icons.Rounded.VolumeUp, null, tint = Color.White.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(8.dp))
            if (designed != null) {
                Option(stringResource(R.string.ambience_designed, ambienceLabel(designed)), !custom) { onSelect(null) }
            }
            Ambience.entries.forEach { a ->
                Option(ambienceLabel(a), custom && a == selected || designed == null && a == selected) { onSelect(a) }
            }
        }
    }
}

@Composable
private fun Option(label: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(if (on) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontFamily = Inter, fontSize = 16.sp, color = Color.White, modifier = Modifier.weight(1f))
        if (on) Icon(Icons.Rounded.Check, null, Modifier.size(20.dp), tint = Color.White)
        else Spacer(Modifier.width(20.dp))
    }
}
