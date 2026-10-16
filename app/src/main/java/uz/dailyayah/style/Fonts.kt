package uz.dailyayah.style

import android.content.Context
import android.graphics.Typeface

/** A selectable font: either bundled in assets/fonts or a system family. */
data class FontOption(
    val key: String,
    val label: String,
    val asset: String? = null,
    val family: String? = null,
)

object Fonts {

    val arabic: List<FontOption> = listOf(
        FontOption("amiri", "Amiri", asset = "fonts/amiri.ttf"),
        FontOption("scheherazade", "Scheherazade", asset = "fonts/scheherazade.ttf"),
        FontOption("naskh", "Noto Naskh", asset = "fonts/naskh.ttf"),
        FontOption("lateef", "Lateef", asset = "fonts/lateef.ttf"),
        FontOption("kufi", "Noto Kufi", asset = "fonts/kufi.ttf"),
        FontOption("reemkufi", "Reem Kufi", asset = "fonts/reemkufi.ttf"),
        FontOption("ruqaa", "Ruqaa", asset = "fonts/ruqaa.ttf"),
        FontOption("system", "System", family = "sans-serif"),
    )

    val text: List<FontOption> = listOf(
        FontOption("sans", "Sans", family = "sans-serif"),
        FontOption("serif", "Serif", family = "serif"),
        FontOption("light", "Light", family = "sans-serif-light"),
        FontOption("medium", "Medium", family = "sans-serif-medium"),
        FontOption("condensed", "Condensed", family = "sans-serif-condensed"),
        FontOption("mono", "Mono", family = "monospace"),
        FontOption("casual", "Casual", family = "casual"),
        FontOption("cursive", "Cursive", family = "cursive"),
    )

    private val cache = HashMap<String, Typeface>()

    private fun base(context: Context, option: FontOption): Typeface = synchronized(cache) {
        cache.getOrPut(option.key) {
            option.asset?.let { path ->
                runCatching { Typeface.createFromAsset(context.applicationContext.assets, path) }.getOrNull()
            } ?: Typeface.create(option.family ?: "serif", Typeface.NORMAL)
        }
    }

    /** True if a bundled font was actually downloaded at build time. */
    private val availability = HashMap<String, Boolean>()

    fun isAvailable(context: Context, option: FontOption): Boolean {
        val path = option.asset ?: return true
        return synchronized(availability) {
            availability.getOrPut(path) {
                runCatching { context.applicationContext.assets.open(path).close(); true }.getOrDefault(false)
            }
        }
    }

    fun availableArabic(context: Context): List<FontOption> = arabic.filter { isAvailable(context, it) }

    fun arabicTypeface(context: Context, key: String, bold: Boolean): Typeface {
        val option = arabic.firstOrNull { it.key == key && isAvailable(context, it) }
            ?: availableArabic(context).first()
        return Typeface.create(base(context, option), if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    fun textTypeface(context: Context, key: String, bold: Boolean, italic: Boolean): Typeface {
        val option = text.firstOrNull { it.key == key } ?: text[0]
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        return Typeface.create(base(context, option), style)
    }

    private val arabicChars = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

    fun containsArabic(text: String): Boolean = arabicChars.containsMatchIn(text)
}
