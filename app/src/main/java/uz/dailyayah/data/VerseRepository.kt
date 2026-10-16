package uz.dailyayah.data

import android.content.Context
import java.time.LocalDate
import kotlin.random.Random

/** Which translation(s) to show under the Arabic text. */
enum class TranslationMode { ENGLISH, RUSSIAN, BOTH }

/**
 * Picks the verse of the day, caches it, and stores user settings.
 * The cache means the widget still shows a verse when the phone is offline.
 */
class VerseRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("daily_ayah", Context.MODE_PRIVATE)

    /** Same date -> same verse, on every phone. */
    fun dailyAyahNumber(date: LocalDate = LocalDate.now()): Int =
        Random(date.toEpochDay()).nextInt(1, QuranApi.TOTAL_AYAHS + 1)

    /** Cached verse of the day, if it is for today. */
    fun cachedToday(): Verse? {
        if (prefs.getString(KEY_DATE, null) != LocalDate.now().toString()) return null
        return prefs.getString(KEY_VERSE, null)?.let { Verse.fromJson(it) }
    }

    /** Last verse we ever saved, even from an earlier day (offline fallback). */
    fun lastSaved(): Verse? = prefs.getString(KEY_VERSE, null)?.let { Verse.fromJson(it) }

    /** Returns today's verse, downloading it only if the cache is stale. */
    suspend fun today(): Verse {
        cachedToday()?.let { return it }
        val today = LocalDate.now()
        val verse = QuranApi.fetchAyah(dailyAyahNumber(today))
        prefs.edit()
            .putString(KEY_DATE, today.toString())
            .putString(KEY_VERSE, verse.toJson())
            .apply()
        return verse
    }

    suspend fun random(): Verse = QuranApi.fetchAyah(Random.nextInt(1, QuranApi.TOTAL_AYAHS + 1))

    var translationMode: TranslationMode
        get() = runCatching {
            TranslationMode.valueOf(prefs.getString(KEY_MODE, null) ?: defaultMode().name)
        }.getOrDefault(defaultMode())
        set(value) = prefs.edit().putString(KEY_MODE, value.name).apply()

    /** Russian if the phone is in Russian, otherwise English. */
    private fun defaultMode(): TranslationMode =
        if (java.util.Locale.getDefault().language == "ru") TranslationMode.RUSSIAN
        else TranslationMode.ENGLISH

    fun translationFor(verse: Verse): String = when (translationMode) {
        TranslationMode.ENGLISH -> verse.english
        TranslationMode.RUSSIAN -> verse.russian
        TranslationMode.BOTH -> "${verse.english}\n\n${verse.russian}"
    }

    private companion object {
        const val KEY_DATE = "cache_date"
        const val KEY_VERSE = "cache_verse"
        const val KEY_MODE = "translation_mode"
    }
}
