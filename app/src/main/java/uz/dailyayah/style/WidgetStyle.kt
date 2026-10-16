package uz.dailyayah.style

import org.json.JSONArray
import org.json.JSONObject
import uz.dailyayah.R
import uz.dailyayah.data.Verse
import java.util.Locale

enum class ContentMode { ARABIC_AND_TRANSLATION, ARABIC_ONLY, TRANSLATION_ONLY, CUSTOM_TEXT }

/** Translations the app ships. Order here = order shown when several are selected. */
enum class Lang(val labelRes: Int) {
    UZ_LATN(R.string.lang_uz_latn),
    UZ_CYRL(R.string.lang_uz_cyrl),
    RU(R.string.lang_ru),
    EN(R.string.lang_en);

    fun textOf(verse: Verse): String = when (this) {
        UZ_LATN -> verse.uzbekLatin
        UZ_CYRL -> verse.uzbekCyrillic
        RU -> verse.russian
        EN -> verse.english
    }
}

enum class TextAlign { START, CENTER, END }

/** Everything the user can change about how a verse looks (per widget, or for the app). */
data class WidgetStyle(
    val content: ContentMode = ContentMode.ARABIC_AND_TRANSLATION,
    val translations: List<Lang> = listOf(Lang.EN),
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
    /** Selected translations, in a fixed order, separated by a blank line. */
    fun translationOf(verse: Verse): String =
        Lang.entries.filter { it in translations }
            .ifEmpty { listOf(Lang.EN) }
            .joinToString("\n\n") { it.textOf(verse) }

    fun toJson(): String = JSONObject()
        .put("content", content.name)
        .put("translations", JSONArray(translations.map { it.name }))
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
        /** Uzbek (Latin) on Uzbek phones, Russian on Russian phones, otherwise English. */
        fun defaultTranslation(): List<Lang> = when (Locale.getDefault().language) {
            "uz" -> listOf(Lang.UZ_LATN)
            "ru" -> listOf(Lang.RU)
            else -> listOf(Lang.EN)
        }

        private inline fun <reified T : Enum<T>> enumOr(value: String?, fallback: T): T =
            runCatching { enumValueOf<T>(value!!) }.getOrDefault(fallback)

        private fun readTranslations(o: JSONObject, fallback: List<Lang>): List<Lang> {
            o.optJSONArray("translations")?.let { array ->
                val list = (0 until array.length()).mapNotNull { i ->
                    runCatching { Lang.valueOf(array.getString(i)) }.getOrNull()
                }
                if (list.isNotEmpty()) return list
            }
            // Settings saved by version 2 ("translation": ENGLISH / RUSSIAN / BOTH).
            return when (o.optString("translation")) {
                "ENGLISH" -> listOf(Lang.EN)
                "RUSSIAN" -> listOf(Lang.RU)
                "BOTH" -> listOf(Lang.RU, Lang.EN)
                else -> fallback
            }
        }

        fun fromJson(json: String): WidgetStyle {
            val d = WidgetStyle(translations = defaultTranslation())
            return try {
                val o = JSONObject(json)
                WidgetStyle(
                    content = enumOr(o.optString("content"), d.content),
                    translations = readTranslations(o, d.translations),
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
