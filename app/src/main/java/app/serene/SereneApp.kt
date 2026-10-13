package app.serene

import android.app.Application
import android.content.Context
import app.serene.audio.Voice
import app.serene.data.DemoSeed
import app.serene.data.SereneDatabase
import app.serene.data.SettingsStore
import app.serene.notify.Reminders
import app.serene.widget.AffirmationWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = SereneDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Created on first use: binding the speech engine takes a moment and is not needed for the home screen. */
    val voice: Voice by lazy { Voice(context) }

    /** Fire-and-forget so a write survives the screen being closed right after tapping save. */
    fun write(block: suspend SereneDatabase.() -> Unit) {
        scope.launch {
            db.block()
            changed()
        }
    }

    /** Re-arm reminders and refresh the home-screen widget. */
    suspend fun changed() {
        Reminders.reschedule(context)
        AffirmationWidget.refresh(context)
    }
}

class SereneApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.ensureChannel(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@SereneApp, container.db, container.settings)
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as SereneApp).container
