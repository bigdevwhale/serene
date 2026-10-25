package app.serene.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.BuildConfig
import app.serene.R
import app.serene.audio.Voice
import app.serene.container
import app.serene.data.Backup
import app.serene.data.Settings
import app.serene.data.ThemeMode
import app.serene.data.voiceMap
import app.serene.data.withVoice
import app.serene.notify.Reminders
import app.serene.ui.Language
import app.serene.ui.clockTime
import app.serene.ui.components.Chip
import app.serene.ui.components.CommunityLinks
import app.serene.ui.components.ConfirmDialog
import app.serene.ui.components.SectionTitle
import app.serene.ui.components.TimeDialog
import app.serene.ui.components.TopBar
import app.serene.ui.components.UCard
import app.serene.ui.components.UTextField
import app.serene.ui.locale
import app.serene.ui.minutesLabel
import app.serene.ui.theme.U
import app.serene.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: Settings, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    fun update(change: (Settings) -> Settings) {
        container.scope.launch {
            container.settings.update(change)
            container.changed()
        }
    }
    var canNotify by remember { mutableStateOf(Reminders.canNotify(context)) }
    // Re-read on resume so the permission card disappears after granting it in system settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canNotify = Reminders.canNotify(context) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canNotify = it }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var picking by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(settings.name) }
    var volume by remember { mutableFloatStateOf(settings.ambienceVolume) }

    val voice = container.voice
    val voiceState by voice.state.collectAsStateWithLifecycle()
    val loc = locale()
    LaunchedEffect(loc, voiceState == Voice.State.LOADING) { voice.setLanguage(loc) }
    val sample = stringResource(R.string.voice_sample)
    LaunchedEffect(name) {
        delay(400)
        if (name.trim() != settings.name) container.settings.update { it.copy(name = name.trim()) }
    }

    val exported = stringResource(R.string.backup_done)
    val failed = stringResource(R.string.backup_failed)
    val restored = stringResource(R.string.backup_restored)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        container.scope.launch {
            val ok = runCatching {
                val json = Backup.encode(container.db.dao().snapshot())
                context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }.isSuccess
            withContext(Dispatchers.Main) { Toast.makeText(context, if (ok) exported else failed, Toast.LENGTH_SHORT).show() }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            if (!canNotify) {
                Spacer(Modifier.height(8.dp))
                UCard(onClick = { if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text(stringResource(R.string.perm_title), style = UType.Strong.copy(color = U.c.accent))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.perm_text), style = UType.Muted)
                }
            }

            SectionTitle(stringResource(R.string.set_name))
            UTextField(name, { name = it.take(30) }, placeholder = stringResource(R.string.ob_name_hint))

            SectionTitle(stringResource(R.string.set_voice))
            UCard {
                SwitchRow(stringResource(R.string.set_voice_on), stringResource(R.string.set_voice_on_text), settings.voice) { v -> update { it.copy(voice = v) } }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = U.c.line)
                SwitchRow(stringResource(R.string.set_studio), stringResource(R.string.set_studio_text), settings.studioVoice) { v -> update { it.copy(studioVoice = v) } }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = U.c.line)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.set_voice_rate), style = UType.Muted)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.85f to R.string.rate_slow, 0.93f to R.string.rate_calm, 1.0f to R.string.rate_normal).forEach { (r, label) ->
                        Chip(stringResource(label), settings.voiceRate == r, { update { it.copy(voiceRate = r) } })
                    }
                }
                val options = remember(voiceState, loc) { voice.voices() }
                if (options.size > 1) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.set_voice_pick), style = UType.Muted)
                    Text(stringResource(R.string.set_voice_pick_note), style = UType.Small.copy(color = U.c.faint), modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.height(4.dp))
                    val selected = settings.voiceMap()[loc.language].orEmpty()
                    fun pick(name: String) {
                        val next = settings.withVoice(loc.language, name)
                        update { next }
                        voice.choose(next.voiceMap())
                        container.scope.launch { voice.say(sample, settings.voiceRate) }
                    }
                    VoiceRow(stringResource(R.string.voice_default), stringResource(R.string.voice_default_text), selected.isEmpty()) { pick("") }
                    options.forEachIndexed { i, o ->
                        VoiceRow(
                            stringResource(R.string.voice_n, i + 1),
                            stringResource(if (o.online) R.string.voice_online else R.string.voice_offline) +
                                if (o.quality >= android.speech.tts.Voice.QUALITY_HIGH) " · " + stringResource(R.string.voice_hq) else "",
                            o.name == selected,
                        ) { pick(o.name) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        val first = app.serene.content.Library.of(loc).sets.first().items.first()
                        container.scope.launch { container.speak(first, settings, loc.language) }
                    }) {
                        Text(stringResource(R.string.voice_test), color = U.c.accent, style = UType.Button)
                    }
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                    }) { Text(stringResource(R.string.voice_settings), color = U.c.accent, style = UType.Button) }
                }
                if (voiceState == Voice.State.MISSING_LANGUAGE || voiceState == Voice.State.UNAVAILABLE) {
                    Text(stringResource(R.string.voice_missing), style = UType.Muted.copy(color = U.c.peach))
                }
            }

            SectionTitle(stringResource(R.string.set_sound))
            UCard {
                Text(stringResource(R.string.set_ambience_volume), style = UType.Strong)
                Slider(
                    value = volume, onValueChange = { volume = it }, valueRange = 0.1f..1f,
                    onValueChangeFinished = { update { it.copy(ambienceVolume = volume) } },
                    colors = SliderDefaults.colors(thumbColor = U.c.accent, activeTrackColor = U.c.accent, inactiveTrackColor = U.c.accentSoft),
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = U.c.line)
                SwitchRow(stringResource(R.string.set_bells), stringResource(R.string.set_bells_text), settings.bells) { v -> update { it.copy(bells = v) } }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = U.c.line)
                SwitchRow(stringResource(R.string.set_haptics), stringResource(R.string.set_haptics_text), settings.haptics) { v -> update { it.copy(haptics = v) } }
            }

            SectionTitle(stringResource(R.string.set_reminders))
            UCard {
                ReminderRow(stringResource(R.string.remind_morning), settings.morningAt, { picking = 1 }) { on -> update { it.copy(morningAt = if (on) 9 * 60 else -1) } }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = U.c.line)
                ReminderRow(stringResource(R.string.remind_evening), settings.eveningAt, { picking = 2 }) { on -> update { it.copy(eveningAt = if (on) 22 * 60 else -1) } }
            }
            Spacer(Modifier.height(10.dp))
            UCard(onClick = {
                context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            }) {
                Text(stringResource(R.string.autostart_title), style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.autostart_text), style = UType.Muted)
            }

            SectionTitle(stringResource(R.string.set_goal))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 20, 30).forEach { m -> Chip(minutesLabel(m), settings.dailyGoal == m, { update { it.copy(dailyGoal = m) } }) }
            }

            SectionTitle(stringResource(R.string.set_appearance))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.theme_system), settings.theme == ThemeMode.SYSTEM, { update { it.copy(theme = ThemeMode.SYSTEM) } })
                Chip(stringResource(R.string.theme_light), settings.theme == ThemeMode.LIGHT, { update { it.copy(theme = ThemeMode.LIGHT) } })
                Chip(stringResource(R.string.theme_dark), settings.theme == ThemeMode.DARK, { update { it.copy(theme = ThemeMode.DARK) } })
            }

            SectionTitle(stringResource(R.string.language))
            val current = Language.current()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.lang_system), current == Language.SYSTEM, { Language.set(Language.SYSTEM) })
                Chip("English", current == Language.EN, { Language.set(Language.EN) })
                Chip("Русский", current == Language.RU, { Language.set(Language.RU) })
            }

            SectionTitle(stringResource(R.string.backup))
            UCard {
                Text(stringResource(R.string.backup_text), style = UType.Muted)
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(onClick = { export.launch("serene-${LocalDate.now()}.json") }) {
                        Text(stringResource(R.string.backup_export), color = U.c.accent, style = UType.Button)
                    }
                    TextButton(onClick = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                        Text(stringResource(R.string.backup_import), color = U.c.accent, style = UType.Button)
                    }
                }
            }

            CommunityLinks()

            SectionTitle(stringResource(R.string.about))
            UCard {
                Text(stringResource(R.string.about_text), style = UType.Sub)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = UType.Small)
            }
        }
    }

    if (picking != 0) {
        TimeDialog(
            (if (picking == 1) settings.morningAt else settings.eveningAt).takeIf { it >= 0 } ?: (9 * 60),
            onDismiss = { picking = 0 },
        ) { m -> val which = picking; update { if (which == 1) it.copy(morningAt = m) else it.copy(eveningAt = m) } }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.backup_import_title),
            text = stringResource(R.string.backup_import_text),
            confirm = stringResource(R.string.backup_import),
            danger = true,
            onConfirm = {
                container.scope.launch {
                    val ok = runCatching {
                        val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                        container.db.dao().replaceAll(Backup.decode(text))
                        container.changed()
                    }.isSuccess
                    withContext(Dispatchers.Main) { Toast.makeText(context, if (ok) restored else failed, Toast.LENGTH_SHORT).show() }
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
}

@Composable
private fun SwitchRow(title: String, text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong)
            Spacer(Modifier.height(2.dp))
            Text(text, style = UType.Muted)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = U.c.accent))
    }
}

@Composable
private fun ReminderRow(title: String, at: Int, onTime: () -> Unit, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable(enabled = at >= 0, onClick = onTime)) {
            Text(title, style = UType.Strong)
            Spacer(Modifier.height(2.dp))
            Text(if (at >= 0) clockTime(at) else stringResource(R.string.off), style = UType.Muted.copy(color = if (at >= 0) U.c.accent else U.c.muted))
        }
        Switch(checked = at >= 0, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = U.c.accent))
    }
}

@Composable
private fun VoiceRow(title: String, text: String, on: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.RadioButton(on, null, colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = U.c.accent))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong.copy(fontSize = 15.sp))
            Text(text, style = UType.Small.copy(color = U.c.muted))
        }
    }
}
