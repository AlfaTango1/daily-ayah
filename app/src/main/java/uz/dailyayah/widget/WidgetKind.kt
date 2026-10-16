package uz.dailyayah.widget

import uz.dailyayah.R
import uz.dailyayah.style.ContentMode
import uz.dailyayah.style.TextAlign
import uz.dailyayah.style.WidgetStyle

/** The widget types offered in the launcher's widget list. */
enum class WidgetKind(
    val provider: Class<out BaseVerseWidget>,
    val titleRes: Int,
    val descriptionRes: Int,
    val defaultWidthDp: Int,
    val defaultHeightDp: Int,
) {
    LARGE(VerseWidgetLarge::class.java, R.string.widget_large, R.string.widget_large_desc, 320, 180),
    COMPACT(VerseWidgetCompact::class.java, R.string.widget_compact, R.string.widget_compact_desc, 170, 170),
    ARABIC(ArabicWidget::class.java, R.string.widget_arabic, R.string.widget_arabic_desc, 320, 120),
    DHIKR(DhikrWidget::class.java, R.string.widget_dhikr, R.string.widget_dhikr_desc, 250, 110);

    fun defaultStyle(): WidgetStyle {
        val translation = WidgetStyle.defaultTranslation()
        return when (this) {
            LARGE -> WidgetStyle(translations = translation)
            COMPACT -> WidgetStyle(
                content = ContentMode.TRANSLATION_ONLY,
                translations = translation,
                textSize = 13,
                textFont = "serif",
                align = TextAlign.START,
                theme = "sand",
                showSurahName = false,
            )
            ARABIC -> WidgetStyle(
                content = ContentMode.ARABIC_ONLY,
                translations = translation,
                arabicSize = 26,
                theme = "midnight",
            )
            DHIKR -> WidgetStyle(
                content = ContentMode.CUSTOM_TEXT,
                translations = translation,
                customText = DEFAULT_DHIKR,
                arabicSize = 26,
                theme = "gold",
                showSurahName = false,
                showReference = false,
            )
        }
    }

    companion object {
        const val DEFAULT_DHIKR = "سُبْحَانَ ٱللَّٰهِ وَبِحَمْدِهِۦ"

        fun forProviderClass(className: String?): WidgetKind? =
            entries.firstOrNull { it.provider.name == className }
    }
}
