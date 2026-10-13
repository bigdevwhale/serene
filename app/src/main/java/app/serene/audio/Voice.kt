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

/**
 * The phone's own offline text-to-speech, slowed down and softened. When no voice is available
 * for the language, sessions still run: lines appear on screen with time to read them.
 */
class Voice(context: Context) {
    enum class State { LOADING, READY, MISSING_LANGUAGE, UNAVAILABLE }

    private val app = context.applicationContext
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val _state = MutableStateFlow(State.LOADING)
    val state: StateFlow<State> = _state
    private var counter = 0
    private var locale: Locale? = null

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
        // A calm, slightly lower voice.
        tts.setPitch(0.92f)
    }

    val canSpeak: Boolean get() = _state.value == State.READY

    /** Speaks [text] and suspends until it has been said (or the time it should have taken). */
    suspend fun say(text: String, rate: Float) {
        if (!canSpeak) return
        val id = "u${counter++}"
        val done = CompletableDeferred<Unit>()
        pending[id] = done
        tts.setSpeechRate(rate)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f) }
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, id) != TextToSpeech.SUCCESS) {
            pending.remove(id)
            return
        }
        // A stuck engine must not freeze the session.
        val words = text.split(' ').size
        withTimeoutOrNull(4000L + (words * 900L / rate).toLong()) { done.await() }
        pending.remove(id)
    }

    fun stop() {
        runCatching { tts.stop() }
        pending.values.forEach { it.complete(Unit) }
        pending.clear()
    }
}
