package app.serene.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.R
import app.serene.container
import app.serene.data.BalanceCheck
import app.serene.domain.Area
import app.serene.ui.areaLabel
import app.serene.ui.components.BalanceWheel
import app.serene.ui.components.PrimaryButton
import app.serene.ui.components.TopBar
import app.serene.ui.components.UCard
import app.serene.ui.dateLabel
import app.serene.ui.theme.U
import app.serene.ui.theme.UType

/** Wheel of balance: how full each area of life feels right now, 1 to 10. */
@Composable
fun BalanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val history by remember { container.db.dao().observeBalance() }.collectAsStateWithLifecycle(initialValue = null)
    val scores = remember { mutableStateListOf(5, 5, 5, 5, 5, 5) }
    var seeded by remember { mutableStateOf(false) }
    LaunchedEffect(history) {
        val h = history ?: return@LaunchedEffect
        if (!seeded) {
            h.lastOrNull()?.values()?.forEachIndexed { i, v -> if (i < scores.size) scores[i] = v }
            seeded = true
        }
    }
    val previous = history?.lastOrNull()

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.balance_title), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Text(stringResource(R.string.balance_text), style = UType.Sub, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(16.dp))
            UCard {
                BalanceWheel(scores.toList(), previous?.values(), Modifier.size(220.dp).align(Alignment.CenterHorizontally))
                if (previous != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.balance_previous, dateLabel(previous.at)), style = UType.Small.copy(color = U.c.peach), modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
            Spacer(Modifier.height(12.dp))
            Area.entries.forEachIndexed { i, a ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(areaLabel(a), style = UType.Strong, modifier = Modifier.weight(1f))
                    Text(scores[i].toString(), style = UType.Strong.copy(color = U.c.accent))
                }
                Slider(
                    value = scores[i].toFloat(),
                    onValueChange = { scores[i] = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = U.c.accent, activeTrackColor = U.c.accent, inactiveTrackColor = U.c.accentSoft, activeTickColor = U.c.accentSoft, inactiveTickColor = U.c.line),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton(
            stringResource(R.string.save),
            onClick = {
                val s = scores.joinToString(",")
                container.write { dao().insertBalance(BalanceCheck(at = System.currentTimeMillis(), scores = s)) }
                onBack()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 12.dp),
        )
    }
}
