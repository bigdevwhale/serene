package app.serene.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

enum class Chime { BOWL, GONG, INHALE, EXHALE, HOLD }

/**
 * A singing bowl and soft breath cues, rendered once from sine partials on first use.
 * Plays on the media stream: the user started a session and expects to hear it.
 */
object Chimes {
    private const val RATE = 32000
    private val thread = Executors.newSingleThreadExecutor()
    private val tracks = HashMap<Chime, AudioTrack>()

    @Volatile var volume = 1f

    fun play(c: Chime) {
        thread.execute {
            runCatching {
                val t = tracks.getOrPut(c) { track(render(c)) }
                if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
                t.reloadStaticData()
                t.setVolume(volume)
                t.play()
            }
        }
    }

    /** Warms the cache so the first bell of a session starts on time. */
    fun prepare(vararg cs: Chime) {
        thread.execute { cs.forEach { c -> runCatching { tracks.getOrPut(c) { track(render(c)) } } } }
    }

    private fun track(pcm: ShortArray): AudioTrack {
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(pcm, 0, pcm.size)
        return t
    }

    /** A partial: frequency ratio to the fundamental, level, decay time in seconds. */
    private class Partial(val ratio: Float, val amp: Float, val decay: Float, val detune: Float = 0f)

    private fun bowl(f0: Float, seconds: Float, partials: List<Partial>, strike: Float): ShortArray {
        val n = (seconds * RATE).toInt()
        val out = FloatArray(n)
        for (p in partials) {
            // Each partial is a slightly detuned pair: the slow beating is what makes a bowl "sing".
            for (side in listOf(-1f, 1f)) {
                val hz = f0 * p.ratio + side * p.detune
                val w = 2f * PI.toFloat() * hz / RATE
                for (i in 0 until n) {
                    val t = i.toFloat() / RATE
                    val attack = minOf(1f, t / 0.006f)
                    out[i] += sin(w * i) * p.amp * 0.5f * attack * exp(-t / p.decay)
                }
            }
        }
        // The mallet: a tick of filtered noise.
        val rnd = Random(5)
        var y = 0f
        for (i in 0 until (0.02f * RATE).toInt()) {
            y += 0.3f * (rnd.nextFloat() * 2 - 1 - y)
            out[i] += y * strike * exp(-i / (0.004f * RATE))
        }
        return finish(out)
    }

    private fun tone(hz: Float, seconds: Float, amp: Float, attack: Float): ShortArray {
        val n = (seconds * RATE).toInt()
        val out = FloatArray(n)
        val w = 2f * PI.toFloat() * hz / RATE
        for (i in 0 until n) {
            val t = i.toFloat() / RATE
            val env = minOf(1f, t / attack) * exp(-t / (seconds * 0.3f))
            out[i] = (sin(w * i) + 0.25f * sin(2 * w * i) + 0.08f * sin(3.01f * w * i)) * env * amp
        }
        return finish(out)
    }

    private fun finish(out: FloatArray): ShortArray {
        val n = out.size
        val tail = (0.05f * RATE).toInt()
        return ShortArray(n) { i ->
            val fade = if (i > n - tail) (n - i).toFloat() / tail else 1f
            (tanh(out[i] * 0.9f) * fade * 0.8f * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun render(c: Chime): ShortArray = when (c) {
        Chime.BOWL -> bowl(
            262f, 9f,
            listOf(Partial(1f, 0.55f, 4.5f, 0.6f), Partial(2.76f, 0.3f, 3f, 1.1f), Partial(5.4f, 0.14f, 1.8f, 1.6f), Partial(8.93f, 0.06f, 1f, 2f)),
            strike = 0.25f,
        )
        Chime.GONG -> bowl(
            174f, 12f,
            listOf(Partial(1f, 0.6f, 6f, 0.4f), Partial(2.0f, 0.18f, 4f, 0.7f), Partial(2.76f, 0.25f, 3.5f, 0.9f), Partial(4.07f, 0.1f, 2f, 1.3f), Partial(5.4f, 0.07f, 1.5f, 1.7f)),
            strike = 0.35f,
        )
        Chime.INHALE -> tone(528f, 1.6f, 0.22f, 0.12f)
        Chime.EXHALE -> tone(396f, 1.8f, 0.22f, 0.12f)
        Chime.HOLD -> tone(440f, 0.9f, 0.12f, 0.05f)
    }
}
