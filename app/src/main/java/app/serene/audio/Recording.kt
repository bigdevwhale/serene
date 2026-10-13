package app.serene.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A recorded soundscape: assets/ambience/<name>.ogg, mono at the mixer's rate. */
object Recording {
    /** Loudness lift to sit level with the synthesised sounds (the files are normalised to -22 LUFS). */
    private const val GAIN = 2.4f
    private val cache = HashMap<String, ShortArray>()

    /** Decodes the whole file to PCM once; a few megabytes, kept while the app runs. */
    fun load(context: Context, name: String): ShortArray? = synchronized(cache) {
        cache[name]?.let { return it }
        val pcm = runCatching { decode(context, name) }.getOrNull()?.takeIf { it.size > Soundscape.RATE } ?: return null
        // Only the most recent one stays: the user hears one soundscape at a time.
        cache.clear()
        cache[name] = pcm
        pcm
    }

    private fun decode(context: Context, name: String): ShortArray {
        val fd = context.assets.openFd("ambience/$name.ogg")
        val ex = MediaExtractor()
        ex.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        fd.close()
        ex.selectTrack(0)
        val format = ex.getTrackFormat(0)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var out = ShortArray(Soundscape.RATE * 60)
        var n = 0
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        try {
            while (true) {
                if (!inputDone) {
                    val i = codec.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val size = ex.readSampleData(codec.getInputBuffer(i)!!, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(i, 0, size, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10_000)
                if (o >= 0) {
                    val buf = codec.getOutputBuffer(o)!!.order(java.nio.ByteOrder.nativeOrder()).asShortBuffer()
                    val frames = info.size / 2 / channels
                    if (n + frames > out.size) out = out.copyOf(maxOf(out.size * 2, n + frames))
                    for (f in 0 until frames) {
                        var acc = 0
                        for (c in 0 until channels) acc += buf.get(info.offset / 2 + f * channels + c)
                        out[n++] = (acc / channels).toShort()
                    }
                    codec.releaseOutputBuffer(o, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
        } finally {
            codec.stop()
            codec.release()
            ex.release()
        }
        return out.copyOf(n)
    }

    /**
     * Plays [pcm] forever as segments of 8–20 s taken from random places, each fading into the
     * next over 1.5 s with equal power, so neither a loop point nor a repeat can be heard.
     */
    fun endless(pcm: ShortArray, rnd: Random): Gen {
        val rate = Soundscape.RATE
        val xf = (rate * 1.5f).toInt().coerceAtMost(pcm.size / 4)
        fun segment(): IntArray {
            val len = (rate * (8 + rnd.nextFloat() * 12)).toInt().coerceAtMost(pcm.size - 1).coerceAtLeast(xf * 2)
            val start = if (pcm.size - len <= 0) 0 else rnd.nextInt(pcm.size - len)
            return intArrayOf(start, len)
        }
        var cur = segment()
        var next = segment()
        var i = 0
        return Gen {
            val (start, len) = cur.let { it[0] to it[1] }
            val fadeAt = len - xf
            var v = pcm[start + i] / 32768f
            if (i >= fadeAt) {
                val t = (i - fadeAt).toFloat() / xf
                v = v * cos(t * PI.toFloat() / 2) + pcm[next[0] + (i - fadeAt)] / 32768f * sin(t * PI.toFloat() / 2)
            }
            if (++i >= len) {
                cur = next
                next = segment()
                i = xf
            }
            v * GAIN
        }
    }
}
