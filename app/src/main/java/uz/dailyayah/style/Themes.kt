package uz.dailyayah.style

/** A colour preset for widgets and the verse card. */
data class Theme(
    val key: String,
    val bgStart: Int,
    val bgEnd: Int,
    val text: Int,
    val accent: Int,
)

object Themes {
    private fun c(v: Long): Int = v.toInt()

    val all: List<Theme> = listOf(
        Theme("emerald", c(0xFF0F4C4A), c(0xFF0A2E2D), c(0xFFFFF8EA), c(0xFFE3BC62)),
        Theme("night", c(0xFF1B2A4A), c(0xFF0B1222), c(0xFFEAF0FF), c(0xFF9DB8FF)),
        Theme("midnight", c(0xFF000000), c(0xFF151515), c(0xFFF5F5F5), c(0xFFD4AF37)),
        Theme("gold", c(0xFF5A3E12), c(0xFF2E1F08), c(0xFFFFF4D6), c(0xFFF0C75E)),
        Theme("rose", c(0xFF6B2E3E), c(0xFF3E1A24), c(0xFFFFEFF2), c(0xFFF2B8C6)),
        Theme("lavender", c(0xFF3B2F5C), c(0xFF1F1833), c(0xFFF1ECFF), c(0xFFC4B2FF)),
        Theme("sand", c(0xFFF7F0DF), c(0xFFEADBB8), c(0xFF2B2418), c(0xFF8A6212)),
        Theme("paper", c(0xFFFFFFFF), c(0xFFF1F1F1), c(0xFF1A1A1A), c(0xFF0F4C4A)),
        Theme("sky", c(0xFFDCEBFA), c(0xFFB8D4F0), c(0xFF0F2540), c(0xFF1F5FA8)),
        Theme("mint", c(0xFFE3F4EC), c(0xFFC2E6D4), c(0xFF113528), c(0xFF1E7A55)),
        // Inspired by the tiles of Samarkand and Bukhara.
        Theme("samarkand", c(0xFF143A7B), c(0xFF0A1F45), c(0xFFF4F1E6), c(0xFF45C3CF)),
        Theme("turquoise", c(0xFF0E7C86), c(0xFF074A52), c(0xFFF2FBFA), c(0xFFF3D27A)),
        Theme("ruby", c(0xFF7A1630), c(0xFF420A19), c(0xFFFFF1F3), c(0xFFF2C46B)),
        Theme("olive", c(0xFF4B5A2A), c(0xFF2A3315), c(0xFFF6F3E4), c(0xFFE6C77A)),
        Theme("parchment", c(0xFFF3E5C3), c(0xFFDCC593), c(0xFF3A2A12), c(0xFF8C2F1B)),
        Theme("dusk", c(0xFF3D2B56), c(0xFFB0563A), c(0xFFFFF4EA), c(0xFFFFD08A)),
    )

    fun byKey(key: String): Theme = all.firstOrNull { it.key == key } ?: all[0]

    /** Applies 0..100 opacity to a colour. */
    fun withOpacity(color: Int, opacity: Int): Int {
        val a = opacity.coerceIn(0, 100) * 255 / 100
        return (color and 0x00FFFFFF) or (a shl 24)
    }
}
