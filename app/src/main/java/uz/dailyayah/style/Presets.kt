package uz.dailyayah.style

import uz.dailyayah.R

/** Ready-made looks. Applying one keeps the content, translations and custom text. */
object Presets {

    class Preset(val labelRes: Int, val apply: (WidgetStyle) -> WidgetStyle)

    val all: List<Preset> = listOf(
        Preset(R.string.preset_samarkand) {
            it.copy(
                theme = "samarkand", pattern = "star8", patternIntensity = 28, frame = "corners",
                arabicFont = "amiri", textFont = "lora", align = TextAlign.CENTER, opacity = 100, corner = 22,
            )
        },
        Preset(R.string.preset_bukhara) {
            it.copy(
                theme = "gold", pattern = "zellige", patternIntensity = 26, frame = "double",
                arabicFont = "naskh", textFont = "playfair", align = TextAlign.CENTER, opacity = 100, corner = 18,
            )
        },
        Preset(R.string.preset_mihrab) {
            it.copy(
                theme = "turquoise", pattern = "arabesque", patternIntensity = 18, frame = "mihrab",
                arabicFont = "amiriquran", textFont = "ebgaramond", align = TextAlign.CENTER, opacity = 100, corner = 26,
            )
        },
        Preset(R.string.preset_night) {
            it.copy(
                theme = "night", pattern = "hexstar", patternIntensity = 22, frame = "line",
                arabicFont = "scheherazade", textFont = "cormorant", align = TextAlign.CENTER, opacity = 95, corner = 24,
                shadow = true,
            )
        },
        Preset(R.string.preset_nastaliq) {
            it.copy(
                theme = "parchment", pattern = "lattice", patternIntensity = 16, frame = "corners",
                arabicFont = "nastaliq", textFont = "merriweather", align = TextAlign.CENTER, opacity = 100, corner = 16,
            )
        },
        Preset(R.string.preset_ruby) {
            it.copy(
                theme = "ruby", pattern = "star8", patternIntensity = 20, frame = "double",
                arabicFont = "ruqaa", textFont = "philosopher", align = TextAlign.CENTER, opacity = 100, corner = 20,
            )
        },
        Preset(R.string.preset_minimal) {
            it.copy(
                theme = "midnight", pattern = "none", frame = "none",
                arabicFont = "kufi", textFont = "montserrat", align = TextAlign.START, opacity = 90, corner = 28,
                shadow = false,
            )
        },
        Preset(R.string.preset_paper) {
            it.copy(
                theme = "paper", pattern = "dots", patternIntensity = 20, frame = "line",
                arabicFont = "amiri", textFont = "ptserif", align = TextAlign.START, opacity = 100, corner = 14,
            )
        },
    )
}
