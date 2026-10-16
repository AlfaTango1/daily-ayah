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

    fun appStyle(context: Context): WidgetStyle =
        prefs(context).getString("app_style", null)?.let { WidgetStyle.fromJson(it) }
            ?: defaultAppStyle()

    fun setAppStyle(context: Context, style: WidgetStyle) {
        prefs(context).edit().putString("app_style", style.toJson()).apply()
    }

    fun defaultAppStyle() = WidgetStyle(
        translation = WidgetStyle.defaultTranslation(),
        arabicSize = 26,
        textSize = 17,
        corner = 20,
        opacity = 100,
    )
}
