package uz.dailyayah.data

import org.json.JSONObject

/** One ayah with its Arabic text and the two translations the app shows. */
data class Verse(
    val number: Int,          // global ayah number, 1..6236
    val surahNumber: Int,
    val numberInSurah: Int,
    val surahNameEn: String,
    val surahNameAr: String,
    val arabic: String,
    val english: String,
    val russian: String,
) {
    /** e.g. "Al-Baqara 2:255" */
    val reference: String get() = "$surahNameEn $surahNumber:$numberInSurah"

    fun toJson(): String = JSONObject()
        .put("number", number)
        .put("surahNumber", surahNumber)
        .put("numberInSurah", numberInSurah)
        .put("surahNameEn", surahNameEn)
        .put("surahNameAr", surahNameAr)
        .put("arabic", arabic)
        .put("english", english)
        .put("russian", russian)
        .toString()

    companion object {
        fun fromJson(json: String): Verse? = try {
            val o = JSONObject(json)
            Verse(
                number = o.getInt("number"),
                surahNumber = o.getInt("surahNumber"),
                numberInSurah = o.getInt("numberInSurah"),
                surahNameEn = o.getString("surahNameEn"),
                surahNameAr = o.getString("surahNameAr"),
                arabic = o.getString("arabic"),
                english = o.getString("english"),
                russian = o.getString("russian"),
            )
        } catch (e: Exception) {
            null
        }
    }
}
