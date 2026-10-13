package app.serene.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.serene.MainActivity
import app.serene.R
import app.serene.container
import app.serene.content.Library
import app.serene.notify.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Home-screen card with the affirmation of the day; a tap opens the affirmations tab. */
class AffirmationWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AffirmationWidget::class.java))
            if (ids.isEmpty()) return
            val c = context.container
            val lib = Library.of(context.resources.configuration.locales[0])
            val a = lib.daily(LocalDate.now(), c.settings.now().focus, c.db.dao().favorites().map { it.key }, c.db.dao().mine())
            val views = RemoteViews(context.packageName, R.layout.widget_affirmation)
            views.setTextViewText(R.id.text, a.text)
            views.setOnClickPendingIntent(
                R.id.root,
                PendingIntent.getActivity(
                    context, 5000,
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(Reminders.EXTRA_ROUTE, "affirm"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            manager.updateAppWidget(ids, views)
        }
    }
}
