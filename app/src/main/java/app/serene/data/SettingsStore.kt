package app.serene.data

import android.content.Context
import app.serene.content.Ambience
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false,
    /** Used in the greeting; empty is fine. */
    val name: String = "",
    /** Affirmation set ids the user cares about most; empty means all. */
    val focus: Set<String> = emptySet(),
    val voice: Boolean = true,
    /** Recorded neural narration for stock lines; off means the phone's voice for everything. */
    val studioVoice: Boolean = true,
    /** Natural pace; the calm comes from pauses between sentences. */
    val voiceRate: Float = 1f,
    /** Chosen engine voice per language: "en=en-us-x-iol-local;ru=…". */
    val voices: String = "",
    /** Background sound chosen per session, overriding the one it was written for: "body_scan=RAIN;…". */
    val ambiences: String = "",
    /** 0..1, background sound level during sessions. */
    val ambienceVolume: Float = 0.45f,
    val bells: Boolean = true,
    val haptics: Boolean = true,
    val dailyGoal: Int = 10,
    /** Minutes after midnight; -1 when the reminder is off. */
    val morningAt: Int = 9 * 60,
    val eveningAt: Int = 22 * 60,
    val timerMinutes: Int = 10,
    val timerAmbience: String = "RAIN",
    val timerInterval: Int = 0,
    val breathMinutes: Int = 3,
)

fun Settings.voiceMap(): Map<String, String> =
    voices.split(';').mapNotNull { e -> e.split('=', limit = 2).takeIf { it.size == 2 && it[1].isNotEmpty() }?.let { it[0] to it[1] } }.toMap()

fun Settings.withVoice(lang: String, name: String): Settings =
    copy(voices = (voiceMap() + (lang to name)).entries.joinToString(";") { "${it.key}=${it.value}" })

private fun Settings.ambienceMap(): Map<String, String> =
    ambiences.split(';').mapNotNull { e -> e.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()

/** The user's background sound for a session, or null when it plays the one it was written for. */
fun Settings.ambienceFor(id: String): Ambience? =
    ambienceMap()[id]?.let { runCatching { Ambience.valueOf(it) }.getOrNull() }

fun Settings.withAmbience(id: String, a: Ambience?): Settings {
    val m = ambienceMap().toMutableMap()
    if (a == null) m.remove(id) else m[id] = a.name
    return copy(ambiences = m.entries.joinToString(";") { "${it.key}=${it.value}" })
}

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        theme = this[THEME]?.let { t -> runCatching { ThemeMode.valueOf(t) }.getOrNull() } ?: ThemeMode.SYSTEM,
        onboarded = this[ONBOARDED] ?: false,
        name = this[NAME] ?: "",
        focus = this[FOCUS] ?: emptySet(),
        voice = this[VOICE] ?: true,
        voiceRate = this[VOICE_RATE] ?: 1f,
        studioVoice = this[STUDIO_VOICE] ?: true,
        voices = this[VOICES] ?: "",
        ambiences = this[AMBIENCES] ?: "",
        ambienceVolume = this[AMBIENCE_VOLUME] ?: 0.45f,
        bells = this[BELLS] ?: true,
        haptics = this[HAPTICS] ?: true,
        dailyGoal = this[DAILY_GOAL] ?: 10,
        morningAt = this[MORNING_AT] ?: (9 * 60),
        eveningAt = this[EVENING_AT] ?: (22 * 60),
        timerMinutes = this[TIMER_MINUTES] ?: 10,
        timerAmbience = this[TIMER_AMBIENCE] ?: "RAIN",
        timerInterval = this[TIMER_INTERVAL] ?: 0,
        breathMinutes = this[BREATH_MINUTES] ?: 3,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[THEME] = s.theme.name
            it[ONBOARDED] = s.onboarded
            it[NAME] = s.name
            it[FOCUS] = s.focus
            it[VOICE] = s.voice
            it[VOICE_RATE] = s.voiceRate
            it[STUDIO_VOICE] = s.studioVoice
            it[VOICES] = s.voices
            it[AMBIENCES] = s.ambiences
            it[AMBIENCE_VOLUME] = s.ambienceVolume
            it[BELLS] = s.bells
            it[HAPTICS] = s.haptics
            it[DAILY_GOAL] = s.dailyGoal
            it[MORNING_AT] = s.morningAt
            it[EVENING_AT] = s.eveningAt
            it[TIMER_MINUTES] = s.timerMinutes
            it[TIMER_AMBIENCE] = s.timerAmbience
            it[TIMER_INTERVAL] = s.timerInterval
            it[BREATH_MINUTES] = s.breathMinutes
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val FOCUS = stringSetPreferencesKey("focus")
        val VOICE = booleanPreferencesKey("voice")
        val VOICE_RATE = floatPreferencesKey("voice_rate_v2")
        val VOICES = stringPreferencesKey("voices")
        val AMBIENCES = stringPreferencesKey("ambiences")
        val STUDIO_VOICE = booleanPreferencesKey("studio_voice")
        val AMBIENCE_VOLUME = floatPreferencesKey("ambience_volume")
        val BELLS = booleanPreferencesKey("bells")
        val HAPTICS = booleanPreferencesKey("haptics")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val MORNING_AT = intPreferencesKey("morning_at")
        val EVENING_AT = intPreferencesKey("evening_at")
        val TIMER_MINUTES = intPreferencesKey("timer_minutes")
        val TIMER_AMBIENCE = stringPreferencesKey("timer_ambience")
        val TIMER_INTERVAL = intPreferencesKey("timer_interval")
        val BREATH_MINUTES = intPreferencesKey("breath_minutes")
    }
}
