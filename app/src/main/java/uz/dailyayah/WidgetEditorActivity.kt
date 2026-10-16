package uz.dailyayah

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.dailyayah.data.AppSettings
import uz.dailyayah.data.QuranData
import uz.dailyayah.data.Verse
import uz.dailyayah.data.VerseSchedule
import uz.dailyayah.databinding.ActivityEditorBinding
import uz.dailyayah.style.ContentMode
import uz.dailyayah.style.Fonts
import uz.dailyayah.style.TextAlign
import uz.dailyayah.style.Themes
import uz.dailyayah.style.Lang
import uz.dailyayah.style.Patterns
import uz.dailyayah.style.Presets
import uz.dailyayah.style.WidgetStyle
import uz.dailyayah.widget.WidgetKind
import uz.dailyayah.widget.WidgetRenderer
import uz.dailyayah.widget.WidgetStore
import uz.dailyayah.widget.WidgetUpdater

/**
 * Edits the look of one home-screen widget, or (app mode) of the verse card
 * in the app. Shows a live preview of every change.
 */
class WidgetEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_APP_STYLE = "uz.dailyayah.extra.APP_STYLE"
        const val EXTRA_FROM_WIDGET = "uz.dailyayah.extra.FROM_WIDGET"
    }

    private lateinit var binding: ActivityEditorBinding
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var appMode = false
    private var kind = WidgetKind.LARGE
    private lateinit var style: WidgetStyle
    private var verse: Verse? = null
    private var controlsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appMode = intent.getBooleanExtra(EXTRA_APP_STYLE, false)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        // When the launcher opens us to configure a new widget, "back" must cancel.
        if (!appMode) {
            setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                finish()
                return
            }
            kind = WidgetStore.kindOf(this, widgetId)
            style = WidgetStore.style(this, widgetId, kind)
            binding.editorTitle.text = getString(R.string.editor_title_widget) + " · " + getString(kind.titleRes)
        } else {
            style = AppSettings.appStyle(this)
            binding.editorTitle.setText(R.string.editor_title_app)
            binding.chipCustom.visibility = View.GONE
        }

        buildFontChips()
        buildThemeSwatches()
        buildPatternChips()
        buildPresetChips()
        bindValues()
        attachListeners()

        binding.btnSave.setOnClickListener { save() }
        binding.btnReset.setOnClickListener {
            style = if (appMode) AppSettings.defaultAppStyle() else kind.defaultStyle()
            bindValues()
            refreshPreview()
        }

        lifecycleScope.launch {
            verse = withContext(Dispatchers.IO) {
                runCatching {
                    val n = if (appMode) VerseSchedule.currentAyah(this@WidgetEditorActivity)
                    else WidgetStore.ayahFor(this@WidgetEditorActivity, widgetId)
                    QuranData.verse(this@WidgetEditorActivity, n)
                }.getOrNull()
            }
            refreshPreview()
        }
        binding.previewFrame.post { refreshPreview() }
    }

    // ---- Building the controls ------------------------------------------------

    private fun newChip(group: ViewGroup, label: String): Chip {
        val chip = layoutInflater.inflate(R.layout.item_chip, group, false) as Chip
        chip.id = View.generateViewId()
        chip.text = label
        group.addView(chip)
        return chip
    }

    private val translationChips: Map<Lang, Int> by lazy {
        mapOf(
            Lang.UZ_LATN to R.id.trUzLatn,
            Lang.UZ_CYRL to R.id.trUzCyrl,
            Lang.RU to R.id.trRu,
            Lang.EN to R.id.trEn,
        )
    }

    private val arabicChipKeys = HashMap<Int, String>()
    private val textChipKeys = HashMap<Int, String>()

    private fun buildFontChips() {
        for (option in Fonts.availableArabic(this)) {
            val chip = newChip(binding.arabicFontGroup, "${option.label}  قرآن")
            chip.typeface = Fonts.arabicTypeface(this, option.key, false)
            arabicChipKeys[chip.id] = option.key
        }
        for (option in Fonts.availableText(this)) {
            val chip = newChip(binding.textFontGroup, "${option.label}  Aa Яя")
            chip.typeface = Fonts.textTypeface(this, option.key, false, false)
            textChipKeys[chip.id] = option.key
        }
    }

    private val patternChipKeys = HashMap<Int, String>()
    private val frameChipKeys = HashMap<Int, String>()

    private fun buildPatternChips() {
        for ((key, label) in Patterns.patterns) {
            patternChipKeys[newChip(binding.patternGroup, getString(label)).id] = key
        }
        for ((key, label) in Patterns.frames) {
            frameChipKeys[newChip(binding.frameGroup, getString(label)).id] = key
        }
    }

    /** One-tap looks; they are buttons, not a selection. */
    private fun buildPresetChips() {
        for (preset in Presets.all) {
            val chip = newChip(binding.presetGroup, getString(preset.labelRes))
            chip.isCheckable = false
            chip.setOnClickListener {
                style = preset.apply(style)
                bindValues()
                refreshPreview()
            }
        }
    }

    private val swatches = ArrayList<Pair<String, View>>()

    private fun buildThemeSwatches() {
        val size = (44 * resources.displayMetrics.density).toInt()
        val margin = (6 * resources.displayMetrics.density).toInt()
        for (theme in Themes.all) {
            val view = View(this)
            view.layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = margin }
            view.contentDescription = theme.key
            view.setOnClickListener {
                style = style.copy(theme = theme.key)
                updateSwatches()
                refreshPreview()
            }
            binding.themeRow.addView(view)
            swatches += theme.key to view
        }
    }

    private fun updateSwatches() {
        val density = resources.displayMetrics.density
        for ((key, view) in swatches) {
            val theme = Themes.byKey(key)
            view.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR, intArrayOf(theme.bgStart, theme.bgEnd)
            ).apply {
                shape = GradientDrawable.OVAL
                val selected = key == style.theme
                setStroke(((if (selected) 4 else 1) * density).toInt(), if (selected) theme.accent else 0x33000000)
            }
        }
    }

    // ---- Values <-> controls --------------------------------------------------

    private fun bindValues() {
        controlsReady = false
        binding.contentGroup.check(
            when (style.content) {
                ContentMode.ARABIC_AND_TRANSLATION -> R.id.chipBoth
                ContentMode.ARABIC_ONLY -> R.id.chipArabic
                ContentMode.TRANSLATION_ONLY -> R.id.chipTranslation
                ContentMode.CUSTOM_TEXT -> if (appMode) R.id.chipBoth else R.id.chipCustom
            }
        )
        binding.customText.setText(style.customText)
        binding.translationGroup.clearCheck()
        for ((lang, chipId) in translationChips) {
            if (lang in style.translations) binding.translationGroup.check(chipId)
        }
        (arabicChipKeys.entries.firstOrNull { it.value == style.arabicFont } ?: arabicChipKeys.entries.firstOrNull())
            ?.let { binding.arabicFontGroup.check(it.key) }
        (textChipKeys.entries.firstOrNull { it.value == style.textFont } ?: textChipKeys.entries.firstOrNull())
            ?.let { binding.textFontGroup.check(it.key) }
        binding.arabicSize.value = style.arabicSize.toFloat().coerceIn(binding.arabicSize.valueFrom, binding.arabicSize.valueTo)
        binding.textSize.value = style.textSize.toFloat().coerceIn(binding.textSize.valueFrom, binding.textSize.valueTo)
        binding.opacity.value = (style.opacity / 5 * 5).toFloat().coerceIn(0f, 100f)
        binding.corner.value = (style.corner / 2 * 2).toFloat().coerceIn(0f, 40f)
        binding.lineSpacing.value = (style.lineSpacing / 5 * 5).toFloat().coerceIn(70f, 200f)
        binding.patternIntensity.value = (style.patternIntensity / 2 * 2).toFloat().coerceIn(4f, 80f)
        binding.shadow.isChecked = style.shadow
        binding.ayahMark.isChecked = style.ayahMark
        (patternChipKeys.entries.firstOrNull { it.value == style.pattern } ?: patternChipKeys.entries.firstOrNull())
            ?.let { binding.patternGroup.check(it.key) }
        (frameChipKeys.entries.firstOrNull { it.value == style.frame } ?: frameChipKeys.entries.firstOrNull())
            ?.let { binding.frameGroup.check(it.key) }
        binding.bold.isChecked = style.bold
        binding.italic.isChecked = style.italic
        binding.showSurah.isChecked = style.showSurahName
        binding.showReference.isChecked = style.showReference
        binding.alignGroup.check(
            when (style.align) {
                TextAlign.START -> R.id.alStart
                TextAlign.CENTER -> R.id.alCenter
                TextAlign.END -> R.id.alEnd
            }
        )
        updateSwatches()
        updateVisibility()
        controlsReady = true
    }

    private fun attachListeners() {
        binding.contentGroup.setOnCheckedStateChangeListener { _, ids ->
            if (!controlsReady || ids.isEmpty()) return@setOnCheckedStateChangeListener
            val content = when (ids[0]) {
                R.id.chipArabic -> ContentMode.ARABIC_ONLY
                R.id.chipTranslation -> ContentMode.TRANSLATION_ONLY
                R.id.chipCustom -> ContentMode.CUSTOM_TEXT
                else -> ContentMode.ARABIC_AND_TRANSLATION
            }
            change(style.copy(content = content))
            updateVisibility()
        }
        binding.customText.doAfterTextChanged {
            if (controlsReady) change(style.copy(customText = it?.toString().orEmpty()))
        }
        binding.translationGroup.setOnCheckedStateChangeListener { _, ids ->
            if (!controlsReady || ids.isEmpty()) return@setOnCheckedStateChangeListener
            val selected = translationChips.filter { it.value in ids }.map { it.key }
            change(style.copy(translations = selected))
        }
        binding.arabicFontGroup.setOnCheckedStateChangeListener { _, ids ->
            val key = ids.firstOrNull()?.let { arabicChipKeys[it] } ?: return@setOnCheckedStateChangeListener
            if (controlsReady) change(style.copy(arabicFont = key))
        }
        binding.textFontGroup.setOnCheckedStateChangeListener { _, ids ->
            val key = ids.firstOrNull()?.let { textChipKeys[it] } ?: return@setOnCheckedStateChangeListener
            if (controlsReady) change(style.copy(textFont = key))
        }
        binding.arabicSize.addOnChangeListener { _, v, _ -> if (controlsReady) change(style.copy(arabicSize = v.toInt())) }
        binding.textSize.addOnChangeListener { _, v, _ -> if (controlsReady) change(style.copy(textSize = v.toInt())) }
        binding.opacity.addOnChangeListener { _, v, _ -> if (controlsReady) change(style.copy(opacity = v.toInt())) }
        binding.corner.addOnChangeListener { _, v, _ -> if (controlsReady) change(style.copy(corner = v.toInt())) }
        binding.patternGroup.setOnCheckedStateChangeListener { _, ids ->
            val key = ids.firstOrNull()?.let { patternChipKeys[it] } ?: return@setOnCheckedStateChangeListener
            if (controlsReady) change(style.copy(pattern = key))
            updateVisibility()
        }
        binding.frameGroup.setOnCheckedStateChangeListener { _, ids ->
            val key = ids.firstOrNull()?.let { frameChipKeys[it] } ?: return@setOnCheckedStateChangeListener
            if (controlsReady) change(style.copy(frame = key))
        }
        binding.patternIntensity.addOnChangeListener { _, v, _ -> if (controlsReady) change(style.copy(patternIntensity = v.toInt())) }
        binding.lineSpacing.addOnChangeListener { _, v, _ -> if (controlsReady) change(style.copy(lineSpacing = v.toInt())) }
        binding.shadow.setOnCheckedChangeListener { _, c -> if (controlsReady) change(style.copy(shadow = c)) }
        binding.ayahMark.setOnCheckedChangeListener { _, c -> if (controlsReady) change(style.copy(ayahMark = c)) }
        binding.bold.setOnCheckedChangeListener { _, c -> if (controlsReady) change(style.copy(bold = c)) }
        binding.italic.setOnCheckedChangeListener { _, c -> if (controlsReady) change(style.copy(italic = c)) }
        binding.showSurah.setOnCheckedChangeListener { _, c -> if (controlsReady) change(style.copy(showSurahName = c)) }
        binding.showReference.setOnCheckedChangeListener { _, c -> if (controlsReady) change(style.copy(showReference = c)) }
        binding.alignGroup.addOnButtonCheckedListener { _, id, checked ->
            if (!controlsReady || !checked) return@addOnButtonCheckedListener
            val align = when (id) {
                R.id.alStart -> TextAlign.START
                R.id.alEnd -> TextAlign.END
                else -> TextAlign.CENTER
            }
            change(style.copy(align = align))
        }
    }

    private fun updateVisibility() {
        val patterned = style.pattern != "none"
        binding.patternIntensity.visibility = if (patterned) View.VISIBLE else View.GONE
        binding.patternIntensityLabel.visibility = binding.patternIntensity.visibility
        val custom = style.content == ContentMode.CUSTOM_TEXT
        binding.customTextLayout.visibility = if (custom) View.VISIBLE else View.GONE
        binding.translationGroup.isEnabled = !custom && style.content != ContentMode.ARABIC_ONLY
        binding.translationGroup.alpha = if (binding.translationGroup.isEnabled) 1f else 0.5f
    }

    private fun change(newStyle: WidgetStyle) {
        style = newStyle
        refreshPreview()
    }

    // ---- Preview and saving ----------------------------------------------------

    private fun refreshPreview() {
        val frame = binding.previewFrame
        val availableW = frame.width - frame.paddingLeft - frame.paddingRight
        val availableH = frame.height - frame.paddingTop - frame.paddingBottom
        if (availableW <= 0 || availableH <= 0) return
        // Keep the widget type's proportions inside the preview area.
        val ratio = kind.defaultHeightDp.toFloat() / kind.defaultWidthDp
        var w = availableW
        var h = (w * ratio).toInt()
        if (h > availableH) {
            h = availableH
            w = (h / ratio).toInt()
        }
        val density = resources.displayMetrics.density
        // The preview box is smaller than a real widget: scale text down to match.
        val scale = density * (w / (kind.defaultWidthDp * density)).coerceAtMost(1f)
        binding.preview.setImageBitmap(
            WidgetRenderer.render(this, style, verse, w, h, scale, reserveIconSpace = !appMode)
        )
    }

    private fun save() {
        if (appMode) {
            AppSettings.setAppStyle(this, style)
            finish()
            return
        }
        WidgetStore.saveStyle(this, widgetId, style)
        val app = applicationContext
        val id = widgetId
        val widgetKind = kind
        Thread {
            runCatching { WidgetUpdater.update(app, AppWidgetManager.getInstance(app), id, widgetKind) }
        }.start()
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
