package uz.dailyayah.data

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
}
