package uz.dailyayah.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.random.Random
import uz.dailyayah.data.QuranData

/**
 * Shared logic for all widget types. Work runs on a background thread
 * (goAsync) because drawing the widget image and loading the Quran text
 * should not block the main thread.
 */
abstract class BaseVerseWidget : AppWidgetProvider() {

    abstract val kind: WidgetKind

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        Thread {
            try {
                handle(app, intent)
            } catch (e: Exception) {
                Log.e("DailyAyah", "Widget update failed", e)
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun handle(context: Context, intent: Intent) {
        val manager = AppWidgetManager.getInstance(context)
        val singleId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val ids: IntArray = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            ?: if (singleId != AppWidgetManager.INVALID_APPWIDGET_ID) intArrayOf(singleId)
            else manager.getAppWidgetIds(ComponentName(context, javaClass))

        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED -> {
                ids.forEach { WidgetUpdater.update(context, manager, it, kind) }
                RenewScheduler.schedule(context)
            }
            AppWidgetManager.ACTION_APPWIDGET_ENABLED -> RenewScheduler.schedule(context)
            AppWidgetManager.ACTION_APPWIDGET_DELETED -> WidgetStore.delete(context, ids)
            WidgetUpdater.ACTION_NEXT_VERSE,
            WidgetUpdater.ACTION_PREV_VERSE,
            WidgetUpdater.ACTION_FOLLOWING_VERSE -> if (singleId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val current = WidgetStore.ayahFor(context, singleId)
                val total = QuranData.TOTAL_AYAHS
                val target = when (intent.action) {
                    WidgetUpdater.ACTION_PREV_VERSE -> if (current <= 1) total else current - 1
                    WidgetUpdater.ACTION_FOLLOWING_VERSE -> if (current >= total) 1 else current + 1
                    else -> Random.nextInt(1, total + 1)
                }
                WidgetStore.setOverride(context, singleId, target)
                WidgetUpdater.update(context, manager, singleId, kind)
            }
        }
    }
}

class VerseWidgetLarge : BaseVerseWidget() {
    override val kind get() = WidgetKind.LARGE
}

class VerseWidgetCompact : BaseVerseWidget() {
    override val kind get() = WidgetKind.COMPACT
}

class ArabicWidget : BaseVerseWidget() {
    override val kind get() = WidgetKind.ARABIC
}

class DhikrWidget : BaseVerseWidget() {
    override val kind get() = WidgetKind.DHIKR
}
