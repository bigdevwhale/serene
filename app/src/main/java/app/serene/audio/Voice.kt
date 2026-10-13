package app.serene.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import android.speech.tts.Voice as TtsVoice

/** A voice the user can pick: [name] is the engine's id. */
data class VoiceOption(val name: String, val online: Boolean, val quality: Int)

/**
 * The phone's own text-to-speech. Slowing a synthetic voice down makes it sound robotic, so
 * it speaks at a natural pace and the calm comes from pauses: each sentence is said on its own,
 * with a breath of silence after it. When no voice is available for the language, sessions
 * still run: lines appear on screen with time to read them.
 */
class Voice(context: Context) {
    enum class State { LOADING, READY, MISSING_LANGUAGE, UNAVAILABLE }

    private val app = context.applicationContext
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val _state = MutableStateFlow(State.LOADING)
    val state: StateFlow<State> = _state
    private var counter = 0
    private var locale: Locale? = null
    /** Chosen voice per language ("en" -> engine voice name). */
    private var chosen: Map<String, String> = emptyMap()

    private val tts: TextToSpeech = TextToSpeech(app) { status ->
        _state.value = if (status == TextToSpeech.SUCCESS) State.READY else State.UNAVAILABLE
        locale?.let { setLanguage(it) }
    }.apply {
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onDone(utteranceId: String) { pending.remove(utteranceId)?.complete(Unit) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) { pending.remove(utteranceId)?.complete(Unit) }
            override fun onError(utteranceId: String, errorCode: Int) { pending.remove(utteranceId)?.complete(Unit) }
            override fun onStop(utteranceId: String, interrupted: Boolean) { pending.remove(utteranceId)?.complete(Unit) }
        })
    }

    fun setLanguage(l: Locale) {
        locale = l
        if (_state.value == State.LOADING || _state.value == State.UNAVAILABLE) return
        val r = runCatching { tts.setLanguage(l) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        _state.value = if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) State.MISSING_LANGUAGE else State.READY
        tts.setPitch(1f)
        applyChosen()
    }

    /** Remembers the user's pick; [names] maps a language code to a voice name. */
    fun choose(names: Map<String, String>) {
        chosen = names
        applyChosen()
    }

    private fun applyChosen() {
        if (_state.value != State.READY) return
        val lang = locale?.language ?: return
        val name = chosen[lang]?.takeIf { it.isNotEmpty() } ?: return useDefault()
        runCatching { tts.voices?.firstOrNull { it.name == name } }.getOrNull()?.let { tts.voice = it }
    }

    /**
     * Voices for the current language, best first. Google ships most voices twice, "-local" and
     * "-network"; they are one voice to the listener, so each appears once, as the network
     * variant when there is one (it falls back to the local one without a connection).
     */
    fun voices(): List<VoiceOption> {
        if (_state.value != State.READY) return emptyList()
        val lang = locale?.language ?: return emptyList()
        val all: Set<TtsVoice> = runCatching { tts.voices }.getOrNull() ?: return emptyList()
        return all.filter { it.locale.language == lang && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .groupBy { it.name.removeSuffix("-local").removeSuffix("-network") }
            .values.map { twins -> twins.firstOrNull { it.isNetworkConnectionRequired } ?: twins.first() }
            // Prefer the user's own country (en-GB voices for a British phone, etc.).
            .sortedWith(compareByDescending<TtsVoice> { it.locale.country == locale?.country }.thenByDescending { it.quality }.thenBy { it.name })
            .map { VoiceOption(it.name, it.isNetworkConnectionRequired, it.quality) }
    }

    /** Back to the engine's default voice for the language. */
    fun useDefault() {
        locale?.let { runCatching { tts.setLanguage(it) } }
    }

    val current: String? get() = runCatching { tts.voice?.name }.getOrNull()

    val canSpeak: Boolean get() = _state.value == State.READY

    /** Speaks [text] sentence by sentence and suspends until it has been said. */
    suspend fun say(text: String, rate: Float) {
        if (!canSpeak) return
        tts.setSpeechRate(rate)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f) }
        val parts = sentences(text)
        var last: CompletableDeferred<Unit>? = null
        tts.stop()
        parts.forEachIndexed { i, part ->
            val id = "u${counter++}"
            val done = CompletableDeferred<Unit>()
            pending[id] = done
            if (tts.speak(part, TextToSpeech.QUEUE_ADD, params, id) != TextToSpeech.SUCCESS) {
                pending.remove(id)
                return
            }
            last = done
            // A short breath between sentences, longer after an ellipsis.
            if (i < parts.size - 1) tts.playSilentUtterance(if (part.endsWith("…") || part.endsWith("...")) 1100L else 650L, TextToSpeech.QUEUE_ADD, "s${counter++}")
        }
        // A stuck engine must not freeze the session.
        val words = text.split(' ').size
        withTimeoutOrNull(5000L + (words * 900L / rate).toLong() + parts.size * 1200L) { last?.await() }
    }

    fun stop() {
        runCatching { tts.stop() }
        pending.values.forEach { it.complete(Unit) }
        pending.clear()
    }

    companion object {
        private val boundary = Regex("(?<=[.!?…])\\s+")

        fun sentences(text: String): List<String> = text.trim().split(boundary).map { it.trim() }.filter { it.isNotEmpty() }
    }
}
