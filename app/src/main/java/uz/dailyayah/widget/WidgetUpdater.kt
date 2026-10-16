package uz.dailyayah.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import uz.dailyayah.HilolLink
import uz.dailyayah.MainActivity
import uz.dailyayah.R
import uz.dailyayah.WidgetEditorActivity
import uz.dailyayah.data.AppSettings
import uz.dailyayah.data.QuranData
import uz.dailyayah.style.ContentMode
import uz.dailyayah.style.Themes
import kotlin.math.sqrt

/** Builds and pushes the RemoteViews for widgets. Call from a background thread. */
object WidgetUpdater {

    const val ACTION_NEXT_VERSE = "uz.dailyayah.action.NEXT_VERSE"
    const val EXTRA_AYAH = "uz.dailyayah.extra.AYAH"

    /** Upper bound for the widget image, to stay within launcher memory limits. */
    private const val MAX_PIXELS = 1_300_000f

    fun idsFor(context: Context, kind: WidgetKind): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, kind.provider))

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        for (kind in WidgetKind.entries) {
            for (id in idsFor(context, kind)) update(context, manager, id, kind)
        }
    }

    fun update(context: Context, manager: AppWidgetManager, widgetId: Int, kind: WidgetKind) {
        val style = WidgetStore.style(context, widgetId, kind)
        val isCustom = style.content == ContentMode.CUSTOM_TEXT
        val ayah = WidgetStore.ayahFor(context, widgetId)
        val verse = if (isCustom) null else runCatching { QuranData.verse(context, ayah) }.getOrNull()

        // Widget size in dp (portrait), from the launcher.
        val options = manager.getAppWidgetOptions(widgetId)
        var widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        var heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        if (widthDp <= 0 || heightDp <= 0) {
            widthDp = kind.defaultWidthDp
            heightDp = kind.defaultHeightDp
        }
        var scale = context.resources.displayMetrics.density
        val area = widthDp.toFloat() * heightDp * scale * scale
        if (area > MAX_PIXELS) scale = sqrt(MAX_PIXELS / (widthDp.toFloat() * heightDp))

        val bitmap = WidgetRenderer.render(
            context, style, verse,
            (widthDp * scale).toInt(), (heightDp * scale).toInt(), scale
        )

        val theme = Themes.byKey(style.theme)
        val views = RemoteViews(context.packageName, R.layout.widget_verse)
        views.setImageViewBitmap(R.id.widget_image, bitmap)
        views.setContentDescription(
            R.id.widget_image,
            if (isCustom) style.customText else verse?.let { "${it.reference}. ${style.translationOf(it)}" }
        )
        views.setInt(R.id.widget_next, "setColorFilter", theme.accent)
        views.setInt(R.id.widget_edit, "setColorFilter", theme.accent)
        views.setViewVisibility(R.id.widget_next, if (isCustom) View.GONE else View.VISIBLE)

        // Tap the widget: open the app on this verse.
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (verse != null) open.putExtra(EXTRA_AYAH, verse.number)
        // ...or in Tafsiri Hilol, if the user chose that.
        val tapIntent = if (verse != null && AppSettings.tapOpensHilol(context)) HilolLink.widgetIntent(context, verse) else open
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(context, widgetId, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        // Show another verse in this widget.
        val next = Intent(context, kind.provider)
            .setAction(ACTION_NEXT_VERSE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        views.setOnClickPendingIntent(
            R.id.widget_next,
            PendingIntent.getBroadcast(context, widgetId, next, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        // Edit this widget's fonts, colours and content.
        val edit = Intent(context, WidgetEditorActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .putExtra(WidgetEditorActivity.EXTRA_FROM_WIDGET, true)
        views.setOnClickPendingIntent(
            R.id.widget_edit,
            PendingIntent.getActivity(context, 100_000 + widgetId, edit, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        manager.updateAppWidget(widgetId, views)
    }
}
