package uz.dailyayah.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import uz.dailyayah.MainActivity
import uz.dailyayah.R
import uz.dailyayah.data.VerseRepository
import uz.dailyayah.work.DailyVerseWorker

/** Home-screen widget showing the verse of the day. */
class VerseWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        DailyVerseWorker.schedulePeriodic(context)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Draw the cached verse immediately, then fetch a fresh one in the background.
        ids.forEach { manager.updateAppWidget(it, buildViews(context)) }
        DailyVerseWorker.runNow(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            updateAll(context)
            DailyVerseWorker.runNow(context)
        }
    }

    companion object {
        private const val ACTION_REFRESH = "uz.dailyayah.action.REFRESH"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VerseWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val repo = VerseRepository(context)
            val verse = repo.cachedToday() ?: repo.lastSaved()
            val views = RemoteViews(context.packageName, R.layout.widget_verse)

            if (verse == null) {
                views.setTextViewText(R.id.widget_arabic, "")
                views.setViewVisibility(R.id.widget_arabic, View.GONE)
                views.setTextViewText(R.id.widget_translation, context.getString(R.string.widget_loading))
                views.setTextViewText(R.id.widget_reference, "")
            } else {
                views.setViewVisibility(R.id.widget_arabic, View.VISIBLE)
                views.setTextViewText(R.id.widget_arabic, verse.arabic)
                views.setTextViewText(R.id.widget_translation, repo.translationFor(verse))
                views.setTextViewText(R.id.widget_reference, verse.reference)
            }

            // Tap the widget -> open the app.
            val openApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)

            // Refresh button.
            val refresh = PendingIntent.getBroadcast(
                context, 1,
                Intent(context, VerseWidgetProvider::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refresh)

            return views
        }
    }
}
