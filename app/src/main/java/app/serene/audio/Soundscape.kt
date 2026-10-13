package app.serene.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import app.serene.content.Ambience
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Endless background sound. Recordings are decoded once and played as shuffled segments that
 * crossfade into each other, so even a short file never audibly loops; music, the warm tone and
 * deep noise are synthesised on the fly. One mixer handles fades, volume and ducking for both.
 */
object Soundscape {
    const val RATE = 32000
    private const val CHUNK = 1024

    @Volatile private var worker: Worker? = null
    @Volatile private var volume = 0.6f
    @Volatile private var duck = 1f
    private lateinit var app: Context

    fun init(context: Context) {
        app = context.applicationContext
    }

    val playing: Ambience? get() = worker?.takeIf { !it.stopping }?.ambience

    fun play(ambience: Ambience, volume: Float) {
        this.volume = volume
        if (worker?.ambience == ambience && worker?.stopping == false) return
        stop()
        if (ambience == Ambience.NONE) return
        worker = Worker(ambience).also { it.start() }
    }

    fun setVolume(v: Float) {
        volume = v
    }

    /** Much softer while the voice speaks, so every word stays clear over the ocean. */
    fun duck(on: Boolean) {
        duck = if (on) 0.2f else 1f
    }

    fun stop() {
        worker?.stopping = true
        worker = null
    }

    private class Worker(val ambience: Ambience) : Thread("soundscape") {
        @Volatile var stopping = false

        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val min = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(maxOf(min, CHUNK * 2) * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return
            val rnd = Random(System.nanoTime())
            val gen = ambience.file?.let { f -> Recording.load(app, f)?.let { Recording.endless(it, rnd) } } ?: Generators.of(ambience, rnd)
            if (stopping) {
                track.release()
                return
            }
            val buf = ShortArray(CHUNK)
            var fade = 0f
            var level = volume * duck
            track.play()
            try {
                while (true) {
                    val fadeStep = if (stopping) -1f / (RATE * 1.2f) else 1f / (RATE * 3f)
                    val target = volume * duck
                    for (i in 0 until CHUNK) {
                        fade = (fade + fadeStep).coerceIn(0f, 1f)
                        // Glide toward the target level so ducking never clicks.
                        // About 60 ms down when the voice starts, a slower 0.4 s swell back after it.
                        level += (target - level) * (if (target < level) 0.0005f else 0.00008f)
                        val x = tanh(gen.next() * 1.2f) * fade * level
                        buf[i] = (x * 0.85f * Short.MAX_VALUE).toInt().toShort()
                    }
                    track.write(buf, 0, CHUNK)
                    if (stopping && fade <= 0f) break
                }
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }
    }
}

/** One sample at a time, roughly -1..1. */
fun interface Gen {
    fun next(): Float
}

object Generators {
    private const val R = Soundscape.RATE.toFloat()

    private fun coef(hz: Float) = 1f - exp(-2f * PI.toFloat() * hz / R)

    fun of(a: Ambience, rnd: Random): Gen = when (a) {
        Ambience.BROWN -> brown(rnd).lowpass(900f).gain(1.1f)
        Ambience.MUSIC -> music(rnd)
        Ambience.DRONE -> drone(rnd).gain(0.85f)
        // Recordings are decoded by Soundscape; silence stands in if one cannot be.
        else -> Gen { 0f }
    }

    fun white(rnd: Random) = Gen { rnd.nextFloat() * 2 - 1 }

    /** Paul Kellet's economy pink filter. */
    fun pink(rnd: Random): Gen {
        var b0 = 0f; var b1 = 0f; var b2 = 0f
        return Gen {
            val w = rnd.nextFloat() * 2 - 1
            b0 = 0.99765f * b0 + w * 0.0990460f
            b1 = 0.96300f * b1 + w * 0.2965164f
            b2 = 0.57000f * b2 + w * 1.0526913f
            (b0 + b1 + b2 + w * 0.1848f) * 0.22f
        }
    }

    fun brown(rnd: Random): Gen {
        var b = 0f
        return Gen {
            b = (b + 0.02f * (rnd.nextFloat() * 2 - 1)) / 1.02f
            b * 3.5f
        }
    }

    fun Gen.lowpass(hz: Float): Gen {
        val a = coef(hz)
        var y = 0f
        return Gen { y += a * (next() - y); y }
    }

    fun Gen.highpass(hz: Float): Gen {
        val a = coef(hz)
        var y = 0f
        return Gen { val x = next(); y += a * (x - y); x - y }
    }

    fun Gen.gain(g: Float) = Gen { next() * g }

    /** A value that wanders smoothly between 0 and 1, retargeting every [minS]..[maxS] seconds. */
    class Drift(private val rnd: Random, private val minS: Float, private val maxS: Float, smoothHz: Float) {
        private val a = coef(smoothHz)
        private var target = rnd.nextFloat()
        private var left = 0
        var value = target
            private set

        fun next(): Float {
            if (--left <= 0) {
                target = rnd.nextFloat()
                left = ((minS + rnd.nextFloat() * (maxS - minS)) * R).toInt()
            }
            value += a * (target - value)
            return value
        }
    }

    /**
     * Gentle generative music: unhurried notes from a pentatonic scale, so no two ever clash,
     * each a soft bell-like tone with a long fade, over a quiet pad and a little echo for space.
     */
    private fun music(rnd: Random): Gen {
        // A minor pentatonic across two octaves.
        val scale = floatArrayOf(220f, 261.63f, 293.66f, 329.63f, 392f, 440f, 523.25f, 587.33f, 659.26f, 783.99f)
        val max = 10
        val ph = FloatArray(max * 3)
        val step = FloatArray(max * 3)
        val amp = FloatArray(max)
        val dec = FloatArray(max)
        val age = IntArray(max)
        var n = 0
        var wait = (R * 0.5f).toInt()
        var last = 4
        val parts = floatArrayOf(1f, 2f, 3.01f)
        val partAmp = floatArrayOf(1f, 0.32f, 0.1f)
        fun note(i: Int, a: Float) {
            if (n == max) return
            for (p in 0 until 3) {
                ph[n * 3 + p] = 0f
                step[n * 3 + p] = 2f * PI.toFloat() * scale[i] * parts[p] / R
            }
            amp[n] = a
            dec[n] = exp(-1f / (R * (2.2f + rnd.nextFloat() * 1.6f)))
            age[n] = 0
            n++
        }
        val pad = drone(rnd)
        // Two feedback delays, darkened: a small, soft room.
        val d1 = FloatArray((R * 0.37f).toInt())
        val d2 = FloatArray((R * 0.53f).toInt())
        var i1 = 0
        var i2 = 0
        var lp = 0f
        return Gen {
            if (--wait <= 0) {
                // A melody that mostly steps, sometimes leaps, with an occasional soft second voice.
                last = (last + rnd.nextInt(-2, 3)).coerceIn(0, scale.size - 1)
                note(last, 0.1f + rnd.nextFloat() * 0.08f)
                if (rnd.nextFloat() < 0.25f) note((last - 2).coerceAtLeast(0), 0.06f)
                wait = ((0.9f + rnd.nextFloat() * 2.4f) * R).toInt()
            }
            var x = 0f
            var k = 0
            while (k < n) {
                val attack = minOf(1f, age[k] / (R * 0.012f))
                var v = 0f
                for (p in 0 until 3) {
                    v += sin(ph[k * 3 + p]) * partAmp[p]
                    ph[k * 3 + p] += step[k * 3 + p]
                }
                x += v * amp[k] * attack
                amp[k] *= dec[k]
                age[k]++
                if (amp[k] < 0.0004f) {
                    n--
                    for (p in 0 until 3) { ph[k * 3 + p] = ph[n * 3 + p]; step[k * 3 + p] = step[n * 3 + p] }
                    amp[k] = amp[n]; dec[k] = dec[n]; age[k] = age[n]
                } else k++
            }
            val e1 = d1[i1]
            val e2 = d2[i2]
            lp += 0.25f * ((e1 + e2) * 0.5f - lp)
            d1[i1] = x + lp * 0.55f
            d2[i2] = x + lp * 0.45f
            i1 = (i1 + 1) % d1.size
            i2 = (i2 + 1) % d2.size
            (x + lp * 0.6f) * 1.6f + pad.next() * 0.18f
        }
    }

    /** A warm, slowly breathing chord on A: root, fifth and octaves, gently detuned. Pitched to carry on a phone speaker. */
    private fun drone(rnd: Random): Gen {
        val freqs = floatArrayOf(110f, 220f, 220.7f, 330f, 440f, 441.2f, 660f, 881f)
        val amps = floatArrayOf(0.14f, 0.2f, 0.13f, 0.13f, 0.09f, 0.07f, 0.04f, 0.02f)
        val phases = FloatArray(freqs.size)
        val swell = Array(freqs.size) { Drift(rnd, 5f, 14f, 0.12f) }
        val air = pink(rnd).lowpass(900f)
        return Gen {
            var x = 0f
            for (i in freqs.indices) {
                phases[i] += 2f * PI.toFloat() * freqs[i] / R
                if (phases[i] > 2f * PI.toFloat()) phases[i] -= 2f * PI.toFloat()
                x += sin(phases[i]) * amps[i] * (0.35f + 0.65f * swell[i].next())
            }
            x * 1.3f + air.next() * 0.05f
        }
    }
}
