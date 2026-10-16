package uz.dailyayah.data

import android.content.Context
import org.json.JSONArray

/** One surah's metadata. */
data class Surah(
    val number: Int,
    val nameAr: String,
    val nameEn: String,
    val meaning: String,
    val ayahCount: Int,
)

/**
 * The whole Quran, read from assets (bundled at build time), so the app and
 * widgets work fully offline. Loaded once per process and kept in memory.
 */
object QuranData {

    const val TOTAL_AYAHS = 6236

    private class Store(
        val arabic: List<String>,
        val english: List<String>,
        val russian: List<String>,
        val surahs: List<Surah>,
        val surahOfAyah: IntArray,      // index: global ayah number (1-based)
        val firstAyahOfSurah: IntArray, // index: surah number (1-based)
    )

    @Volatile
    private var store: Store? = null

    private fun load(context: Context): Store {
        store?.let { return it }
        synchronized(this) {
            store?.let { return it }
            val assets = context.applicationContext.assets
            fun lines(name: String): List<String> =
                assets.open("quran/$name").bufferedReader(Charsets.UTF_8).use { it.readLines() }

            val arabic = lines("ar.txt")
            val english = lines("en.txt")
            val russian = lines("ru.txt")

            val json = assets.open("quran/surahs.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val array = JSONArray(json)
            val surahs = ArrayList<Surah>(array.length())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                surahs += Surah(
                    number = o.getInt("number"),
                    nameAr = o.getString("name"),
                    nameEn = o.getString("englishName"),
                    meaning = o.optString("meaning"),
                    ayahCount = o.getInt("ayahs"),
                )
            }

            val surahOfAyah = IntArray(TOTAL_AYAHS + 1)
            val firstAyah = IntArray(surahs.size + 2)
            var n = 1
            for (s in surahs) {
                firstAyah[s.number] = n
                repeat(s.ayahCount) { surahOfAyah[n++] = s.number }
            }

            return Store(arabic, english, russian, surahs, surahOfAyah, firstAyah).also { store = it }
        }
    }

    /** Loads the data in advance (call from a background thread). */
    fun preload(context: Context) {
        load(context)
    }

    fun isLoaded(): Boolean = store != null

    fun surahs(context: Context): List<Surah> = load(context).surahs

    /** Global ayah number (1..6236) -> verse. */
    fun verse(context: Context, number: Int): Verse {
        val s = load(context)
        val n = number.coerceIn(1, TOTAL_AYAHS)
        val surahNumber = s.surahOfAyah[n]
        val surah = s.surahs[surahNumber - 1]
        return Verse(
            number = n,
            surahNumber = surahNumber,
            numberInSurah = n - s.firstAyahOfSurah[surahNumber] + 1,
            surahNameEn = surah.nameEn,
            surahNameAr = surah.nameAr,
            arabic = s.arabic.getOrElse(n - 1) { "" },
            english = s.english.getOrElse(n - 1) { "" },
            russian = s.russian.getOrElse(n - 1) { "" },
        )
    }
}
