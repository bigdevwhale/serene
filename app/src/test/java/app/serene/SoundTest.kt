package app.serene

import app.serene.audio.Generators
import app.serene.audio.Soundscape
import app.serene.content.Ambience
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Soundscapes stay audible, never clip and never blow up into NaN over a long stretch.
 * `./gradlew testDebugUnitTest -Ppreview=<dir>` also writes each one as a WAV to listen to.
 */
class SoundTest {
    @Test
    fun `every soundscape has a calm, steady level`() {
        val dir = System.getProperty("preview")?.takeIf { it.isNotEmpty() }?.let(::File)?.apply { mkdirs() }
        for (a in Ambience.entries - Ambience.NONE) {
            val gen = Generators.of(a, Random(1))
            val n = Soundscape.RATE * 30
            val out = FloatArray(n) { tanh(gen.next() * 1.2f) }
            assertTrue(a.name, out.all { !it.isNaN() })
            val rms = sqrt(out.sumOf { (it * it).toDouble() } / n)
            val peak = out.maxOf { abs(it) }
            println("%-7s rms %.3f peak %.3f".format(a.name, rms, peak))
            assertTrue("${a.name} too quiet: $rms", rms > 0.04)
            assertTrue("${a.name} too loud: $rms", rms < 0.45)
            dir?.let { wav(File(it, "${a.name.lowercase()}.wav"), out) }
        }
    }

    private fun wav(f: File, pcm: FloatArray) {
        RandomAccessFile(f, "rw").use { r ->
            r.setLength(0)
            fun i32(v: Int) = r.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
            fun i16(v: Int) = r.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
            r.writeBytes("RIFF"); i32(36 + pcm.size * 2); r.writeBytes("WAVEfmt ")
            i32(16); i16(1); i16(1); i32(Soundscape.RATE); i32(Soundscape.RATE * 2); i16(2); i16(16)
            r.writeBytes("data"); i32(pcm.size * 2)
            val buf = ByteArray(pcm.size * 2)
            pcm.forEachIndexed { i, v -> val s = (v * 0.85f * Short.MAX_VALUE).toInt(); buf[2 * i] = s.toByte(); buf[2 * i + 1] = (s shr 8).toByte() }
            r.write(buf)
        }
    }
}
