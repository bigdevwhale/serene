package app.serene.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.serene.R
import app.serene.data.Settings
import app.serene.notify.Reminders
import app.serene.ui.affirm.AffirmPlayer
import app.serene.ui.affirm.AffirmSetScreen
import app.serene.ui.affirm.AffirmTab
import app.serene.ui.journal.BalanceScreen
import app.serene.ui.journal.CheckInScreen
import app.serene.ui.journal.JournalTab
import app.serene.ui.onboarding.OnboardingScreen
import app.serene.ui.practice.BreathScreen
import app.serene.ui.practice.GuidedPlayer
import app.serene.ui.practice.PracticeTab
import app.serene.ui.practice.TimerScreen
import app.serene.ui.settings.SettingsScreen
import app.serene.ui.theme.Inter
import app.serene.ui.theme.U
import app.serene.ui.today.TodayTab
import app.serene.ui.visions.EditVisionScreen
import app.serene.ui.visions.VisionsScreen
import kotlinx.coroutines.flow.MutableStateFlow

enum class Tab { TODAY, PRACTICE, AFFIRM, JOURNAL }

/** Everything a tab can open. */
class Go(private val nav: NavHostController) {
    fun guided(id: String) = nav.navigate("guided/$id")
    fun breathe(id: String) = nav.navigate("breathe/$id")
    fun timer() = nav.navigate("timer")
    fun set(id: String) = nav.navigate("set/$id")
    fun play(set: String, start: String = "") = nav.navigate("play/$set?start=$start")
    fun checkIn(id: Long = 0, mood: Int = 0) = nav.navigate("checkin/$id?mood=$mood")
    fun balance() = nav.navigate("balance")
    fun visions() = nav.navigate("visions")
    fun vision(id: Long) = nav.navigate("vision/$id")
    fun settings() = nav.navigate("settings")
    fun back() = nav.popBackStack()
}

@Composable
fun SereneRoot(openRoute: MutableStateFlow<String?>, settings: Settings) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val go = androidx.compose.runtime.remember(nav) { Go(nav) }
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val start = androidx.compose.runtime.remember { if (settings.onboarded) "main" else "welcome" }

    val requested by openRoute.collectAsStateWithLifecycle()
    LaunchedEffect(requested, settings.onboarded) {
        val route = requested ?: return@LaunchedEffect
        if (!settings.onboarded) return@LaunchedEffect
        nav.popBackStack("main", inclusive = false)
        when {
            route == "affirm" -> tab = Tab.AFFIRM
            route.startsWith("guided/") -> nav.navigate(route)
        }
        openRoute.value = null
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context) && !app.serene.BuildConfig.DEMO) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NavHost(
        navController = nav,
        startDestination = start,
        modifier = Modifier.background(U.c.bg),
        enterTransition = { fadeIn(tween(260)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = { fadeOut(tween(200)) },
    ) {
        composable("welcome") {
            OnboardingScreen { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } }
        }
        composable("main") {
            MainTabs(tab, { tab = it }, go, settings)
        }
        composable("guided/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
            GuidedPlayer(e.arguments?.getString("id").orEmpty(), settings, onClose = { go.back() })
        }
        composable("breathe/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
            BreathScreen(e.arguments?.getString("id").orEmpty(), settings, onClose = { go.back() })
        }
        composable("timer", enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
            TimerScreen(settings, onClose = { go.back() })
        }
        composable("set/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
            AffirmSetScreen(e.arguments?.getString("id").orEmpty(), settings, go)
        }
        composable(
            "play/{set}?start={start}",
            arguments = listOf(navArgument("set") { type = NavType.StringType }, navArgument("start") { type = NavType.StringType; defaultValue = "" }),
        ) { e ->
            AffirmPlayer(e.arguments?.getString("set").orEmpty(), e.arguments?.getString("start").orEmpty(), settings, onClose = { go.back() })
        }
        composable(
            "checkin/{id}?mood={mood}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("mood") { type = NavType.IntType; defaultValue = 0 }),
        ) { e ->
            CheckInScreen(e.arguments?.getLong("id") ?: 0L, e.arguments?.getInt("mood") ?: 0, onDone = { go.back() })
        }
        composable("balance") { BalanceScreen(onBack = { go.back() }) }
        composable("visions") { VisionsScreen(go) }
        composable("vision/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            EditVisionScreen(e.arguments?.getLong("id") ?: 0L, onDone = { go.back() })
        }
        composable("settings") { SettingsScreen(settings, onBack = { go.back() }) }
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, settings: Settings) {
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.TODAY -> TodayTab(settings, go, onTab)
                Tab.PRACTICE -> PracticeTab(settings, go)
                Tab.AFFIRM -> AffirmTab(settings, go)
                Tab.JOURNAL -> JournalTab(settings, go)
            }
        }
        HorizontalDivider(color = U.c.line)
        Row(
            Modifier.fillMaxWidth().background(U.c.surface).navigationBarsPadding().height(66.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.WbSunny, stringResource(R.string.tab_today), tab == Tab.TODAY) { onTab(Tab.TODAY) }
            NavItem(Icons.Rounded.SelfImprovement, stringResource(R.string.tab_practice), tab == Tab.PRACTICE) { onTab(Tab.PRACTICE) }
            NavItem(Icons.Rounded.AutoAwesome, stringResource(R.string.tab_affirm), tab == Tab.AFFIRM) { onTab(Tab.AFFIRM) }
            NavItem(Icons.Rounded.Book, stringResource(R.string.tab_journal), tab == Tab.JOURNAL) { onTab(Tab.JOURNAL) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.accent else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.accentSoft else androidx.compose.ui.graphics.Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Inter, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
    Spacer(Modifier.width(0.dp))
}
