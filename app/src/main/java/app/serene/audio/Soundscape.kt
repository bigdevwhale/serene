package app.serene.audio

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
 * Endless background sound, synthesised on the fly from filtered noise and sine waves.
 * Nothing loops, so there is no seam to notice after ten minutes; no audio files ship with the app.
 */
object Soundscape {
    const val RATE = 32000
    private const val CHUNK = 1024

    @Volatile private var worker: Worker? = null
    @Volatile private var volume = 0.6f
    @Volatile private var duck = 1f

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

    /** Softer while the voice speaks, so words stay clear over the ocean. */
    fun duck(on: Boolean) {
        duck = if (on) 0.55f else 1f
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
            val gen = Generators.of(ambience, Random(System.nanoTime()))
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
                        level += (target - level) * 0.0004f
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
        Ambience.NONE -> Gen { 0f }
        Ambience.BROWN -> brown(rnd).lowpass(900f).gain(1.1f)
        Ambience.RAIN -> rain(rnd).gain(0.85f)
        Ambience.OCEAN -> ocean(rnd).gain(0.55f)
        Ambience.STREAM -> stream(rnd).gain(0.6f)
        Ambience.WIND -> wind(rnd).gain(1.4f)
        Ambience.FOREST -> forest(rnd).gain(1.7f)
        Ambience.FIRE -> fire(rnd)
        Ambience.DRONE -> drone(rnd).gain(0.85f)
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

    /** Short decaying sine pings: raindrops, bubbles, crackles. */
    private class Pings(private val max: Int) {
        private val phase = FloatArray(max)
        private val step = FloatArray(max)
        private val sweep = FloatArray(max)
        private val amp = FloatArray(max)
        private val decay = FloatArray(max)
        private var n = 0

        fun add(hz: Float, amplitude: Float, ms: Float, sweepTo: Float = hz) {
            if (n == max) return
            phase[n] = 0f
            step[n] = 2f * PI.toFloat() * hz / R
            val samples = ms / 1000f * R
            // Glide in frequency over the ping's life (bubbles rise).
            sweep[n] = Math.pow((sweepTo / hz).toDouble(), 1.0 / samples).toFloat()
            amp[n] = amplitude
            decay[n] = exp(-5f / samples)
            n++
        }

        fun next(): Float {
            var out = 0f
            var i = 0
            while (i < n) {
                out += sin(phase[i]) * amp[i]
                phase[i] += step[i]
                step[i] *= sweep[i]
                amp[i] *= decay[i]
                if (amp[i] < 0.0005f) {
                    n--
                    phase[i] = phase[n]; step[i] = step[n]; sweep[i] = sweep[n]; amp[i] = amp[n]; decay[i] = decay[n]
                } else i++
            }
            return out
        }
    }

    private fun rain(rnd: Random): Gen {
        val hiss = pink(rnd).highpass(500f).lowpass(7000f)
        val body = brown(rnd).lowpass(300f)
        val drops = Pings(24)
        val heavy = Drift(rnd, 4f, 9f, 0.3f)
        return Gen {
            val h = heavy.next()
            if (rnd.nextFloat() < (25f + 50f * h) / R) {
                drops.add(1800f + rnd.nextFloat() * 3800f, 0.04f + rnd.nextFloat().let { it * it } * 0.22f, 6f + rnd.nextFloat() * 18f, 1400f + rnd.nextFloat() * 1500f)
            }
            hiss.next() * (0.8f + 0.35f * h) + body.next() * 0.35f + drops.next()
        }
    }

    private fun ocean(rnd: Random): Gen {
        val src = pink(rnd)
        val rumble = brown(rnd).lowpass(160f)
        var y = 0f
        var t = 0
        var len = 1
        var peak = 0.4f
        return Gen {
            if (++t >= len) {
                t = 0
                len = ((7f + rnd.nextFloat() * 5f) * R).toInt()
                peak = 0.6f + rnd.nextFloat() * 0.4f
            }
            val p = t.toFloat() / len
            // A wave gathers for 40% of its life, then breaks and drains away.
            val e = if (p < 0.4f) (p / 0.4f).let { it * it * (3 - 2 * it) } * peak else peak * exp(-(p - 0.4f) * 5f)
            val a = coef(250f + 2200f * e)
            y += a * (src.next() - y)
            y * (0.35f + 2.1f * e) + rumble.next() * 0.55f
        }
    }

    private fun stream(rnd: Random): Gen {
        val water = pink(rnd).highpass(350f).lowpass(3500f)
        val ripple = Drift(rnd, 0.03f, 0.12f, 25f)
        val bubbles = Pings(12)
        return Gen {
            if (rnd.nextFloat() < 14f / R) {
                val f = 500f + rnd.nextFloat() * 900f
                bubbles.add(f, 0.05f + rnd.nextFloat() * 0.08f, 25f + rnd.nextFloat() * 40f, f * 1.7f)
            }
            water.next() * (1.1f + 0.9f * ripple.next()) + bubbles.next()
        }
    }

    /** Noise through a drifting resonant band-pass: gusts that rise and fall. */
    private fun windGen(rnd: Random): Gen {
        val noise = white(rnd)
        val gust = Drift(rnd, 2f, 5f, 0.35f)
        var low = 0f
        var band = 0f
        return Gen {
            val g = gust.next()
            val f = 2f * sin(PI.toFloat() * (180f + 650f * g) / R)
            val q = 0.35f
            low += f * band
            val high = noise.next() * 0.6f - low - q * band
            band += f * high
            band * (0.25f + 0.9f * g)
        }
    }

    private fun wind(rnd: Random): Gen {
        val w = windGen(rnd)
        val bed = brown(rnd).lowpass(220f)
        return Gen { w.next() * 1.4f + bed.next() * 0.3f }
    }

    private fun forest(rnd: Random): Gen {
        val w = windGen(rnd)
        val leaves = pink(rnd).highpass(1500f)
        val leafy = Drift(rnd, 1.5f, 4f, 0.6f)
        // A bird phrase: a few chirps sweeping up or down, then a long silence.
        var wait = (R * 2).toInt()
        var chirpLeft = 0
        var chirpLen = 1
        var chirps = 0
        var gap = 0
        var phase = 0f
        var f0 = 3000f
        var f1 = 3600f
        var amp = 0.15f
        var vib = 0f
        fun newChirp() {
            chirpLen = ((0.05f + rnd.nextFloat() * 0.09f) * R).toInt()
            chirpLeft = chirpLen
            val up = rnd.nextBoolean()
            val span = 0.2f + rnd.nextFloat() * 0.45f
            f1 = if (up) f0 * (1 + span) else f0 * (1 - span * 0.5f)
        }
        return Gen {
            var bird = 0f
            if (chirpLeft > 0) {
                val p = 1f - chirpLeft.toFloat() / chirpLen
                val env = sin(PI.toFloat() * p).let { it * it }
                vib += 2f * PI.toFloat() * 28f / R
                val hz = (f0 + (f1 - f0) * p) * (1 + 0.025f * sin(vib))
                phase += 2f * PI.toFloat() * hz / R
                bird = sin(phase) * env * amp
                if (--chirpLeft == 0) {
                    gap = ((0.04f + rnd.nextFloat() * 0.12f) * R).toInt()
                    chirps--
                }
            } else if (chirps > 0) {
                if (--gap <= 0) newChirp()
            } else if (--wait <= 0) {
                wait = ((1.5f + rnd.nextFloat() * 5f) * R).toInt()
                chirps = 2 + rnd.nextInt(5)
                f0 = 2200f + rnd.nextFloat() * 2200f
                amp = 0.06f + rnd.nextFloat() * 0.12f
                newChirp()
            }
            w.next() * 0.7f + leaves.next() * 0.12f * leafy.next() + bird
        }
    }

    private fun fire(rnd: Random): Gen {
        val roar = brown(rnd).lowpass(700f)
        val hiss = pink(rnd).highpass(2500f)
        val flicker = Drift(rnd, 0.2f, 0.8f, 3f)
        val crackle = white(rnd).highpass(1200f)
        var burst = 0
        var burstAmp = 0f
        return Gen {
            if (burst <= 0 && rnd.nextFloat() < 9f / R) {
                burst = ((0.001f + rnd.nextFloat() * 0.005f) * R).toInt()
                burstAmp = rnd.nextFloat().let { it * it * it } * 1.2f + 0.05f
            }
            val c = if (burst-- > 0) crackle.next() * burstAmp else 0f
            roar.next() * (0.6f + 0.5f * flicker.next()) + hiss.next() * 0.08f + c * 1.3f
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
