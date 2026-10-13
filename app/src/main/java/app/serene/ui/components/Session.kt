package app.serene.ui.components

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.serene.container
import app.serene.data.PracticeKind
import app.serene.data.Session

/** Shorter than this and it was a peek, not a practice. */
private const val MIN_SECONDS = 30

fun logSession(context: Context, kind: PracticeKind, ref: String, seconds: Int) {
    if (seconds < MIN_SECONDS) return
    context.container.write { dao().insertSession(Session(at = System.currentTimeMillis() - seconds * 1000L, kind = kind, ref = ref, seconds = seconds)) }
}

/** Keeps the display awake while a session is on screen. */
@Composable
fun KeepScreenOn(on: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(on) {
        view.keepScreenOn = on
        onDispose { view.keepScreenOn = false }
    }
}

fun tick(context: Context, ms: Long = 18) {
    val v = context.getSystemService(Vibrator::class.java) ?: return
    runCatching { v.vibrate(VibrationEffect.createOneShot(ms, 60)) }
}

/** Round translucent button for the dark session screens. */
@Composable
fun GlassButton(icon: ImageVector, description: String?, onClick: () -> Unit, size: Dp = 48.dp, strong: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (strong) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size(size * 0.46f), tint = if (strong) Color(0xFF1F1D2B) else Color.White)
    }
}
