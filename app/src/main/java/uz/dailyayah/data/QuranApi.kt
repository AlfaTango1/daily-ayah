package uz.dailyayah.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal client for the free Al-Quran Cloud API (https://alquran.cloud/api).
 * One request returns the Arabic text plus both translations.
 */
object QuranApi {

    const val TOTAL_AYAHS = 6236

    private const val BASE = "https://api.alquran.cloud/v1"
    private const val ED_ARABIC = "quran-uthmani"
    private const val ED_ENGLISH = "en.sahih"     // Sahih International
    private const val ED_RUSSIAN = "ru.kuliev"    // Elmir Kuliev

    suspend fun fetchAyah(number: Int): Verse = withContext(Dispatchers.IO) {
        require(number in 1..TOTAL_AYAHS) { "Ayah number out of range: $number" }
        val url = URL("$BASE/ayah/$number/editions/$ED_ARABIC,$ED_ENGLISH,$ED_RUSSIAN")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw java.io.IOException("HTTP ${conn.responseCode}")
            }
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parse(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(body: String): Verse {
        val data = JSONObject(body).getJSONArray("data")
        var arabic = ""
        var english = ""
        var russian = ""
        val first = data.getJSONObject(0)
        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            when (item.getJSONObject("edition").getString("identifier")) {
                ED_ARABIC -> arabic = item.getString("text")
                ED_ENGLISH -> english = item.getString("text")
                ED_RUSSIAN -> russian = item.getString("text")
            }
        }
        val surah = first.getJSONObject("surah")
        return Verse(
            number = first.getInt("number"),
            surahNumber = surah.getInt("number"),
            numberInSurah = first.getInt("numberInSurah"),
            surahNameEn = surah.getString("englishName"),
            surahNameAr = surah.getString("name"),
            arabic = arabic,
            english = english,
            russian = russian,
        )
    }
}
