package app.serene.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serene.R
import app.serene.container
import app.serene.content.Scene
import app.serene.ui.clockTime
import app.serene.ui.components.SceneBackdrop
import app.serene.ui.components.TimeDialog
import app.serene.ui.library
import app.serene.ui.theme.Inter
import app.serene.ui.theme.Lora
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val lib = library()
    var page by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var focus by rememberSaveable { mutableStateOf(setOf("calm")) }
    var morning by rememberSaveable { mutableIntStateOf(9 * 60) }
    var evening by rememberSaveable { mutableIntStateOf(22 * 60) }
    var morningOn by rememberSaveable { mutableStateOf(true) }
    var eveningOn by rememberSaveable { mutableStateOf(true) }
    var picking by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(page > 0) { page-- }

    fun finish() {
        container.scope.launch {
            container.settings.update {
                it.copy(
                    onboarded = true, name = name.trim(), focus = focus,
                    morningAt = if (morningOn) morning else -1, eveningAt = if (eveningOn) evening else -1,
                )
            }
            container.changed()
        }
        onDone()
    }

    val scene = listOf(Scene.DAWN, Scene.MEADOW, Scene.LIGHT, Scene.NIGHT)[page]
    Box(Modifier.fillMaxSize().background(Color(0xFF12111A))) {
        AnimatedContent(scene, transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(900)) }, label = "scene") { SceneBackdrop(it) }
        Box(Modifier.fillMaxSize().background(Color(0x33000000)))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 26.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                Spacer(Modifier.height(60.dp))
                when (page) {
                    0 -> {
                        Text(stringResource(R.string.app_name), style = TextStyle(fontFamily = Lora, fontSize = 52.sp, color = Color.White))
                        Spacer(Modifier.height(14.dp))
                        Text(stringResource(R.string.welcome_text), style = body(18.sp))
                        Spacer(Modifier.height(24.dp))
                        listOf(R.string.welcome_1, R.string.welcome_2, R.string.welcome_3, R.string.welcome_4).forEach {
                            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                                Box(Modifier.padding(top = 9.dp).size(6.dp).clip(CircleShape).background(Color.White))
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(it), style = body(15.sp))
                            }
                        }
                    }
                    1 -> {
                        Title(stringResource(R.string.ob_name_title))
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.ob_name_text), style = body(15.sp))
                        Spacer(Modifier.height(28.dp))
                        BasicTextField(
                            name, { name = it.take(30) },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = Lora, fontSize = 28.sp, color = Color.White),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth().clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).padding(horizontal = 22.dp, vertical = 14.dp),
                            decorationBox = { inner ->
                                if (name.isEmpty()) Text(stringResource(R.string.ob_name_hint), style = TextStyle(fontFamily = Lora, fontSize = 28.sp, color = Color.White.copy(alpha = 0.45f)))
                                inner()
                            },
                        )
                    }
                    2 -> {
                        Title(stringResource(R.string.ob_focus_title))
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.ob_focus_text), style = body(15.sp))
                        Spacer(Modifier.height(24.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lib.sets.forEach { s ->
                                val on = s.id in focus
                                Box(
                                    Modifier.clip(CircleShape)
                                        .background(if (on) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.14f))
                                        .clickable { focus = if (on) focus - s.id else focus + s.id }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                ) {
                                    Text(s.title, color = if (on) Color(0xFF1F1D2B) else Color.White, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                    else -> {
                        Title(stringResource(R.string.ob_remind_title))
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.ob_remind_text), style = body(15.sp))
                        Spacer(Modifier.height(24.dp))
                        ReminderRow(stringResource(R.string.remind_morning), stringResource(R.string.remind_morning_text), clockTime(morning), morningOn, { morningOn = it }) { picking = 1 }
                        Spacer(Modifier.height(12.dp))
                        ReminderRow(stringResource(R.string.remind_evening), stringResource(R.string.remind_evening_text), clockTime(evening), eveningOn, { eveningOn = it }) { picking = 2 }
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { i -> Box(Modifier.size(if (i == page) 22.dp else 7.dp, 7.dp).clip(CircleShape).background(Color.White.copy(alpha = if (i == page) 1f else 0.4f))) }
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clip(CircleShape).background(Color.White).clickable { if (page < 3) page++ else finish() }.padding(horizontal = 28.dp, vertical = 15.dp),
                ) {
                    Text(
                        stringResource(if (page == 0) R.string.ob_start else if (page < 3) R.string.next else R.string.ob_finish),
                        color = Color(0xFF1F1D2B), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    )
                }
            }
        }
    }

    if (picking != 0) {
        TimeDialog(if (picking == 1) morning else evening, onDismiss = { picking = 0 }) { m -> if (picking == 1) morning = m else evening = m }
    }
}

private fun body(size: androidx.compose.ui.unit.TextUnit) = TextStyle(fontFamily = Inter, fontSize = size, lineHeight = size * 1.45f, color = Color.White.copy(alpha = 0.9f))

@Composable
private fun Title(text: String) {
    Text(text, style = TextStyle(fontFamily = Lora, fontSize = 32.sp, lineHeight = 39.sp, color = Color.White, textAlign = TextAlign.Start))
}

@Composable
private fun ReminderRow(title: String, text: String, time: String, on: Boolean, onToggle: (Boolean) -> Unit, onTime: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.14f)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(text, color = Color.White.copy(alpha = 0.75f), fontFamily = Inter, fontSize = 13.sp)
            if (on) Text(time, color = Color.White, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, modifier = Modifier.padding(top = 6.dp).clickable(onClick = onTime))
        }
        Switch(on, onToggle, colors = SwitchDefaults.colors(checkedTrackColor = Color.White, checkedThumbColor = Color(0xFF6B5BD2)))
    }
}
