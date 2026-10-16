package uz.dailyayah.style

import org.json.JSONObject
import uz.dailyayah.data.Verse
import java.util.Locale

enum class ContentMode { ARABIC_AND_TRANSLATION, ARABIC_ONLY, TRANSLATION_ONLY, CUSTOM_TEXT }

enum class TranslationMode { ENGLISH, RUSSIAN, BOTH }

enum class TextAlign { START, CENTER, END }

/** Everything the user can change about how a verse looks (per widget, or for the app). */
data class WidgetStyle(
    val content: ContentMode = ContentMode.ARABIC_AND_TRANSLATION,
    val translation: TranslationMode = TranslationMode.ENGLISH,
    val customText: String = "",
    val arabicFont: String = "amiri",
    val textFont: String = "sans",
    val arabicSize: Int = 22,
    val textSize: Int = 14,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val align: TextAlign = TextAlign.CENTER,
    val theme: String = "emerald",
    val opacity: Int = 95,
    val corner: Int = 24,
    val showSurahName: Boolean = true,
    val showReference: Boolean = true,
) {
    fun translationOf(verse: Verse): String = when (translation) {
        TranslationMode.ENGLISH -> verse.english
        TranslationMode.RUSSIAN -> verse.russian
        TranslationMode.BOTH -> "${verse.english}\n\n${verse.russian}"
    }

    fun toJson(): String = JSONObject()
        .put("content", content.name)
        .put("translation", translation.name)
        .put("customText", customText)
        .put("arabicFont", arabicFont)
        .put("textFont", textFont)
        .put("arabicSize", arabicSize)
        .put("textSize", textSize)
        .put("bold", bold)
        .put("italic", italic)
        .put("align", align.name)
        .put("theme", theme)
        .put("opacity", opacity)
        .put("corner", corner)
        .put("showSurahName", showSurahName)
        .put("showReference", showReference)
        .toString()

    companion object {
        /** Russian if the phone is in Russian, otherwise English. */
        fun defaultTranslation(): TranslationMode =
            if (Locale.getDefault().language == "ru") TranslationMode.RUSSIAN else TranslationMode.ENGLISH

        private inline fun <reified T : Enum<T>> enumOr(value: String?, fallback: T): T =
            runCatching { enumValueOf<T>(value!!) }.getOrDefault(fallback)

        fun fromJson(json: String): WidgetStyle {
            val d = WidgetStyle()
            return try {
                val o = JSONObject(json)
                WidgetStyle(
                    content = enumOr(o.optString("content"), d.content),
                    translation = enumOr(o.optString("translation"), d.translation),
                    customText = o.optString("customText", d.customText),
                    arabicFont = o.optString("arabicFont", d.arabicFont),
                    textFont = o.optString("textFont", d.textFont),
                    arabicSize = o.optInt("arabicSize", d.arabicSize),
                    textSize = o.optInt("textSize", d.textSize),
                    bold = o.optBoolean("bold", d.bold),
                    italic = o.optBoolean("italic", d.italic),
                    align = enumOr(o.optString("align"), d.align),
                    theme = o.optString("theme", d.theme),
                    opacity = o.optInt("opacity", d.opacity),
                    corner = o.optInt("corner", d.corner),
                    showSurahName = o.optBoolean("showSurahName", d.showSurahName),
                    showReference = o.optBoolean("showReference", d.showReference),
                )
            } catch (e: Exception) {
                d
            }
        }
    }
}
