package app.serene.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.serene.MainActivity
import app.serene.R
import app.serene.container
import app.serene.content.Library
import app.serene.widget.AffirmationWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Two gentle daily nudges: an affirmation in the morning, an invitation to unwind at night. */
object Reminders {
    private const val CHANNEL_ID = "reminders_v1"
    const val EXTRA_ROUTE = "route"
    private const val ACTION_MORNING = "app.serene.notify.MORNING"
    private const val ACTION_EVENING = "app.serene.notify.EVENING"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_desc)
            }
        )
    }

    private fun pi(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        context, code,
        Intent(context, ReminderReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Next time the clock shows [minutes] after midnight, strictly after now. */
    fun nextAt(minutes: Int, now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate().atTime(minutes / 60, minutes % 60)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        for ((action, code, at) in listOf(Triple(ACTION_MORNING, 1, s.morningAt), Triple(ACTION_EVENING, 2, s.eveningAt))) {
            val p = pi(context, action, code)
            if (at < 0) {
                am.cancel(p)
                continue
            }
            val ms = nextAt(at, now).atZone(zone).toInstant().toEpochMilli()
            // A reminder does not need to-the-second precision; this avoids the exact-alarm permission.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, p)
        }
    }

    suspend fun fire(context: Context, action: String) {
        if (!canNotify(context)) return
        val c = context.container
        val s = c.settings.now()
        val lib = Library.of(context.resources.configuration.locales[0])
        when (action) {
            ACTION_MORNING -> {
                val a = lib.daily(LocalDate.now(), s.focus, c.db.dao().favorites().map { it.key }, c.db.dao().mine())
                notify(context, 1, context.getString(R.string.notif_morning_title), a.text, "affirm")
            }
            ACTION_EVENING -> notify(context, 2, context.getString(R.string.notif_evening_title), context.getString(R.string.notif_evening_text), "guided/sleep_wind_down")
        }
    }

    private fun notify(context: Context, id: Int, title: String, text: String, route: String) {
        val open = PendingIntent.getActivity(
            context, 100 + id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_ROUTE, route),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    internal val actions = setOf(ACTION_MORNING, ACTION_EVENING)
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in Reminders.actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Reminders.fire(context, action)
                Reminders.reschedule(context)
                AffirmationWidget.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}
