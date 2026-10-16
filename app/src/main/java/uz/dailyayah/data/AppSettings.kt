package uz.dailyayah.data

import android.content.Context
import uz.dailyayah.style.WidgetStyle

/** App-wide settings: renewal interval and the main screen's text style. */
object AppSettings {

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun interval(context: Context): RenewInterval =
        runCatching { RenewInterval.valueOf(prefs(context).getString("interval", null)!!) }
            .getOrDefault(RenewInterval.HOUR_1)

    fun setInterval(context: Context, value: RenewInterval) {
        prefs(context).edit().putString("interval", value.name).apply()
    }

    /** true: tapping a widget opens Tafsiri Hilol; false: opens this app. */
    fun tapOpensHilol(context: Context): Boolean = prefs(context).getBoolean("tap_hilol", true)

    fun setTapOpensHilol(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("tap_hilol", value).apply()
    }

    fun appStyle(context: Context): WidgetStyle =
        prefs(context).getString("app_style", null)?.let { WidgetStyle.fromJson(it) }
            ?: defaultAppStyle()

    fun setAppStyle(context: Context, style: WidgetStyle) {
        prefs(context).edit().putString("app_style", style.toJson()).apply()
    }

    fun defaultAppStyle() = WidgetStyle(
        translations = WidgetStyle.defaultTranslation(),
        arabicSize = 26,
        textSize = 17,
        corner = 20,
        opacity = 100,
    )
}
