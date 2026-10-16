package uz.dailyayah.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import uz.dailyayah.data.VerseSchedule
import uz.dailyayah.style.WidgetStyle

/** Per-widget settings: its style, and a "next verse" the user tapped for. */
object WidgetStore {

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    fun kindOf(context: Context, widgetId: Int): WidgetKind {
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId)
        return WidgetKind.forProviderClass(info?.provider?.className) ?: WidgetKind.LARGE
    }

    fun style(context: Context, widgetId: Int, kind: WidgetKind = kindOf(context, widgetId)): WidgetStyle =
        prefs(context).getString("style_$widgetId", null)?.let { WidgetStyle.fromJson(it) }
            ?: kind.defaultStyle()

    fun saveStyle(context: Context, widgetId: Int, style: WidgetStyle) {
        prefs(context).edit().putString("style_$widgetId", style.toJson()).apply()
    }

    /** Verse for this widget: the scheduled one, unless the user asked for another this period. */
    fun ayahFor(context: Context, widgetId: Int): Int {
        val p = prefs(context)
        val period = VerseSchedule.currentPeriodKey(context)
        if (p.getString("override_period_$widgetId", null) == period) {
            val n = p.getInt("override_$widgetId", 0)
            if (n > 0) return n
        }
        return VerseSchedule.currentAyah(context)
    }

    fun setOverride(context: Context, widgetId: Int, ayah: Int) {
        prefs(context).edit()
            .putInt("override_$widgetId", ayah)
            .putString("override_period_$widgetId", VerseSchedule.currentPeriodKey(context))
            .apply()
    }

    fun delete(context: Context, widgetIds: IntArray) {
        val editor = prefs(context).edit()
        for (id in widgetIds) {
            editor.remove("style_$id").remove("override_$id").remove("override_period_$id")
        }
        editor.apply()
    }
}
