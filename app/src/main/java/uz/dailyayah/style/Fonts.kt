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
        FontOption("amiriquran", "Amiri Quran", asset = "fonts/amiriquran.ttf"),
        FontOption("scheherazade", "Scheherazade", asset = "fonts/scheherazade.ttf"),
        FontOption("naskh", "Noto Naskh", asset = "fonts/naskh.ttf"),
        FontOption("lateef", "Lateef", asset = "fonts/lateef.ttf"),
        FontOption("harmattan", "Harmattan", asset = "fonts/harmattan.ttf"),
        FontOption("mirza", "Mirza", asset = "fonts/mirza.ttf"),
        FontOption("nastaliq", "Nastaliq", asset = "fonts/nastaliq.ttf"),
        FontOption("gulzar", "Gulzar", asset = "fonts/gulzar.ttf"),
        FontOption("ruqaa", "Ruqaa", asset = "fonts/ruqaa.ttf"),
        FontOption("kufi", "Noto Kufi", asset = "fonts/kufi.ttf"),
        FontOption("reemkufi", "Reem Kufi", asset = "fonts/reemkufi.ttf"),
        FontOption("kufam", "Kufam", asset = "fonts/kufam.ttf"),
        FontOption("elmessiri", "El Messiri", asset = "fonts/elmessiri.ttf"),
        FontOption("rakkas", "Rakkas", asset = "fonts/rakkas.ttf"),
        FontOption("lalezar", "Lalezar", asset = "fonts/lalezar.ttf"),
        FontOption("marhey", "Marhey", asset = "fonts/marhey.ttf"),
        FontOption("cairo", "Cairo", asset = "fonts/cairo.ttf"),
        FontOption("tajawal", "Tajawal", asset = "fonts/tajawal.ttf"),
        FontOption("system", "System", family = "sans-serif"),
    )

    /** Fonts for translations; the bundled ones cover Latin and Cyrillic. */
    val text: List<FontOption> = listOf(
        FontOption("sans", "Sans", family = "sans-serif"),
        FontOption("serif", "Serif", family = "serif"),
        FontOption("lora", "Lora", asset = "fonts/lora.ttf"),
        FontOption("merriweather", "Merriweather", asset = "fonts/merriweather.ttf"),
        FontOption("ptserif", "PT Serif", asset = "fonts/ptserif.ttf"),
        FontOption("ebgaramond", "EB Garamond", asset = "fonts/ebgaramond.ttf"),
        FontOption("cormorant", "Cormorant", asset = "fonts/cormorant.ttf"),
        FontOption("playfair", "Playfair", asset = "fonts/playfair.ttf"),
        FontOption("philosopher", "Philosopher", asset = "fonts/philosopher.ttf"),
        FontOption("montserrat", "Montserrat", asset = "fonts/montserrat.ttf"),
        FontOption("comfortaa", "Comfortaa", asset = "fonts/comfortaa.ttf"),
        FontOption("marck", "Marck Script", asset = "fonts/marck.ttf"),
        FontOption("caveat", "Caveat", asset = "fonts/caveat.ttf"),
        FontOption("light", "Light", family = "sans-serif-light"),
        FontOption("medium", "Medium", family = "sans-serif-medium"),
        FontOption("condensed", "Condensed", family = "sans-serif-condensed"),
        FontOption("mono", "Mono", family = "monospace"),
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

    fun availableText(context: Context): List<FontOption> = text.filter { isAvailable(context, it) }

    fun arabicTypeface(context: Context, key: String, bold: Boolean): Typeface {
        val option = arabic.firstOrNull { it.key == key && isAvailable(context, it) }
            ?: availableArabic(context).first()
        return Typeface.create(base(context, option), if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    fun textTypeface(context: Context, key: String, bold: Boolean, italic: Boolean): Typeface {
        val option = text.firstOrNull { it.key == key && isAvailable(context, it) } ?: text[0]
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
