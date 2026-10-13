package app.serene.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume

/**
 * The studio narrator: every stock line was recorded ahead of time by a neural voice
 * (tools/narrate.py) and ships in assets/voice/<lang>/<hash>.webm, where the hash is taken from the
 * exact text. A line that was edited, or the user's own words, has no recording, and the caller
 * falls back to the phone's voice. The user's own recordings live in filesDir/audio.
 */
class Narrator(context: Context) {
    private val app = context.applicationContext
    private val index = HashMap<String, Set<String>>()
    @Volatile private var player: MediaPlayer? = null
    private val lengths = HashMap<String, Map<String, Float>>()

    /** Length of the recording of [text] in seconds, from durations.json written by the tool. */
    fun seconds(text: String, lang: String): Float? = synchronized(lengths) {
        lengths.getOrPut(lang) {
            runCatching {
                val json = org.json.JSONObject(app.assets.open("voice/$lang/durations.json").use { it.readBytes().decodeToString() })
                json.keys().asSequence().associateWith { json.getDouble(it).toFloat() }
            }.getOrDefault(emptyMap())
        }[key(text)]
    }

    private fun available(lang: String): Set<String> = synchronized(index) {
        index.getOrPut(lang) { runCatching { app.assets.list("voice/$lang")?.toSet() }.getOrNull().orEmpty() }
    }

    fun has(text: String, lang: String): Boolean = "${key(text)}.webm" in available(lang)

    /** Plays the recording of [text]; returns false when there is none. */
    suspend fun say(text: String, lang: String, speed: Float): Boolean {
        val name = "${key(text)}.webm"
        if (name !in available(lang)) return false
        val fd = withContext(Dispatchers.IO) { runCatching { app.assets.openFd("voice/$lang/$name") }.getOrNull() } ?: return false
        play(speed) { it.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length); fd.closeQuietly() }
        return true
    }

    /** Plays one of the user's own recordings. */
    suspend fun playFile(name: String, speed: Float): Boolean {
        val f = file(app, name)
        if (!f.exists()) return false
        play(speed) { it.setDataSource(f.path) }
        return true
    }

    private suspend fun play(speed: Float, source: (MediaPlayer) -> Unit) {
        stop()
        val mp = MediaPlayer()
        player = mp
        try {
            suspendCancellableCoroutine { cont ->
                val ok = runCatching {
                    mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    source(mp)
                    mp.setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                    mp.setOnErrorListener { _, _, _ -> if (cont.isActive) cont.resume(Unit); true }
                    mp.prepare()
                    if (speed != 1f) mp.playbackParams = mp.playbackParams.setSpeed(speed)
                    mp.start()
                }.isSuccess
                if (!ok && cont.isActive) cont.resume(Unit)
                cont.invokeOnCancellation { runCatching { mp.stop() } }
            }
        } finally {
            runCatching { mp.release() }
            if (player === mp) player = null
        }
    }

    fun stop() {
        player?.let { runCatching { it.stop() } }
    }

    private fun AssetFileDescriptor.closeQuietly() = runCatching { close() }

    companion object {
        /** Must match tools/narrate.py: first 16 hex digits of SHA-1 over the trimmed UTF-8 text. */
        fun key(text: String): String =
            MessageDigest.getInstance("SHA-1").digest(text.trim().toByteArray()).joinToString("") { "%02x".format(it) }.take(16)

        fun file(context: Context, name: String) = File(File(context.filesDir, "audio").apply { mkdirs() }, name)

        /** Copies a picked audio file into private storage; returns its new name. */
        suspend fun import(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
            runCatching {
                val type = context.contentResolver.getType(uri).orEmpty()
                val ext = when {
                    "mpeg" in type || "mp3" in type -> "mp3"
                    "ogg" in type -> "ogg"
                    "wav" in type -> "wav"
                    "aac" in type -> "aac"
                    "webm" in type -> "webm"
                    else -> "m4a"
                }
                val name = "${UUID.randomUUID()}.$ext"
                context.contentResolver.openInputStream(uri)!!.use { input -> file(context, name).outputStream().use { input.copyTo(it) } }
                name
            }.getOrNull()
        }

        fun delete(context: Context, name: String) {
            if (name.isNotEmpty()) file(context, name).delete()
        }
    }
}
