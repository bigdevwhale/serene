package app.serene.ui.visions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Photos are copied into the app's private storage, so the board survives the gallery being cleaned. */
object VisionImages {
    private fun dir(context: Context) = File(context.filesDir, "visions").apply { mkdirs() }

    fun file(context: Context, name: String) = File(dir(context), name)

    /** Copies a picked image, scaled down to at most 1600 px on the long side. Returns the file name. */
    suspend fun import(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
            val bmp = context.contentResolver.openInputStream(uri)!!.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }!!
            val name = "${UUID.randomUUID()}.jpg"
            file(context, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            name
        }.getOrNull()
    }

    fun delete(context: Context, name: String) {
        if (name.isNotEmpty()) file(context, name).delete()
    }
}

@Composable
fun rememberVisionImage(name: String): ImageBitmap? {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, name) {
        value = if (name.isEmpty()) null else withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(VisionImages.file(context, name).path)?.asImageBitmap() }.getOrNull()
        }
    }
    return image
}
