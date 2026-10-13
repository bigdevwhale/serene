package app.serene

import android.app.Application
import android.content.Context
import app.serene.audio.Narrator
import app.serene.audio.Voice
import app.serene.data.DemoSeed
import app.serene.data.SereneDatabase
import app.serene.data.SettingsStore
import app.serene.data.voiceMap
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
    val voice: Voice by lazy {
        Voice(context).also { v -> scope.launch { settings.settings.collect { v.choose(it.voiceMap()) } } }
    }

    val narrator: Narrator by lazy { Narrator(context) }

    /**
     * Says [text] with the best voice there is: the user's own recording, then the studio
     * narration, then the phone's voice. Returns false when nothing could be heard, so the
     * caller can leave the line on screen for reading instead.
     */
    suspend fun speak(text: String, s: app.serene.data.Settings, lang: String, audio: String = ""): Boolean {
        if (audio.isNotEmpty() && narrator.playFile(audio, s.voiceRate)) return true
        if (s.studioVoice && narrator.say(text, lang, s.voiceRate)) return true
        if (!voice.canSpeak) return false
        voice.say(text, s.voiceRate)
        return true
    }

    fun stopSpeaking() {
        narrator.stop()
        voice.stop()
    }

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
        app.serene.content.Spoken.lookup = { text, ru -> container.narrator.seconds(text, if (ru) "ru" else "en") }
        Reminders.ensureChannel(this)
        app.serene.audio.Soundscape.init(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@SereneApp, container.db, container.settings)
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as SereneApp).container
