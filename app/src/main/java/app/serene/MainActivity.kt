package app.serene

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.data.Settings
import app.serene.notify.Reminders
import app.serene.ui.SereneRoot
import app.serene.ui.theme.SereneTheme
import app.serene.ui.theme.isDark
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : AppCompatActivity() {
    /** Screen requested from outside: a reminder or the widget. */
    private val openRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val dark = isDark((settings ?: Settings()).theme)
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            SereneTheme(dark) {
                // Wait for settings so the theme doesn't flash light-then-dark.
                if (settings != null) SereneRoot(openRoute = openRoute, settings = settings!!)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.getStringExtra(Reminders.EXTRA_ROUTE)?.let { openRoute.value = it }
    }
}
