package uz.dailyayah

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import uz.dailyayah.data.AppSettings
import uz.dailyayah.data.QuranData
import uz.dailyayah.data.RenewInterval
import uz.dailyayah.data.Verse
import uz.dailyayah.data.VerseSchedule
import uz.dailyayah.databinding.ActivityMainBinding
import uz.dailyayah.style.ContentMode
import uz.dailyayah.style.Fonts
import uz.dailyayah.style.TextAlign
import uz.dailyayah.style.Themes
import uz.dailyayah.style.WidgetStyle
import uz.dailyayah.widget.RenewScheduler
import uz.dailyayah.widget.WidgetKind
import uz.dailyayah.widget.WidgetStore
import uz.dailyayah.widget.WidgetUpdater

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var current: Verse? = null
    private var currentNumber = 0
    private var following = true // true: show the scheduled verse; false: user is browsing
    private val intervalChips = HashMap<Int, RenewInterval>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPrev.setOnClickListener { browse(currentNumber - 1) }
        binding.btnNext.setOnClickListener { browse(currentNumber + 1) }
        binding.btnNow.setOnClickListener { following = true; showScheduled() }
        binding.btnRandom.setOnClickListener { browse(Random.nextInt(1, QuranData.TOTAL_AYAHS + 1)) }
        binding.btnShare.setOnClickListener { share() }
        binding.btnCopy.setOnClickListener { copy() }
        binding.btnTextStyle.setOnClickListener {
            startActivity(Intent(this, WidgetEditorActivity::class.java).putExtra(WidgetEditorActivity.EXTRA_APP_STYLE, true))
        }

        buildIntervalChips()
        buildWidgetKinds()
        handleOpenIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        buildPlacedWidgets()
        // Style may have changed in the editor; the scheduled verse may have changed too.
        if (following) showScheduled() else current?.let { show(it) }
    }

    private fun handleOpenIntent(intent: Intent?) {
        val ayah = intent?.getIntExtra(WidgetUpdater.EXTRA_AYAH, 0) ?: 0
        if (ayah > 0) browse(ayah)
    }

    // ---- Verse display -----------------------------------------------------------

    private fun showScheduled() = load { VerseSchedule.currentAyah(this) }

    private fun browse(number: Int) {
        following = false
        val n = when {
            number < 1 -> QuranData.TOTAL_AYAHS
            number > QuranData.TOTAL_AYAHS -> 1
            else -> number
        }
        load { n }
    }

    private fun load(number: () -> Int) {
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val verse = withContext(Dispatchers.IO) { runCatching { QuranData.verse(this@MainActivity, number()) }.getOrNull() }
            binding.progress.visibility = View.GONE
            if (verse != null) show(verse)
        }
    }

    private fun show(verse: Verse) {
        current = verse
        currentNumber = verse.number
        val style = AppSettings.appStyle(this)
        binding.title.setText(if (following) R.string.verse_now else R.string.browsing)
        binding.subtitle.text = getString(R.string.renews, getString(AppSettings.interval(this).labelRes).lowercase())
        binding.surahName.text = "${verse.surahNameAr}  ·  ${verse.surahNameEn}"
        binding.arabic.text = verse.arabic
        binding.translation.text = style.translationOf(verse)
        binding.reference.text = verse.reference
        applyStyle(style)
        binding.card.visibility = View.VISIBLE
    }

    private fun applyStyle(style: WidgetStyle) {
        val theme = Themes.byKey(style.theme)
        val density = resources.displayMetrics.density
        binding.card.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Themes.withOpacity(theme.bgStart, style.opacity.coerceAtLeast(60)),
                Themes.withOpacity(theme.bgEnd, style.opacity.coerceAtLeast(60)),
            )
        ).apply { cornerRadius = style.corner * density }

        binding.arabic.typeface = Fonts.arabicTypeface(this, style.arabicFont, style.bold)
        binding.arabic.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.arabicSize * 1.15f)
        binding.arabic.setTextColor(theme.text)

        binding.translation.typeface = Fonts.textTypeface(this, style.textFont, style.bold, style.italic)
        binding.translation.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.textSize * 1.15f)
        binding.translation.setTextColor(theme.text)

        binding.surahName.typeface = Fonts.arabicTypeface(this, style.arabicFont, true)
        binding.surahName.setTextColor(theme.accent)
        binding.reference.setTextColor(theme.accent)

        val alignment = when (style.align) {
            TextAlign.START -> View.TEXT_ALIGNMENT_TEXT_START
            TextAlign.CENTER -> View.TEXT_ALIGNMENT_CENTER
            TextAlign.END -> View.TEXT_ALIGNMENT_TEXT_END
        }
        binding.arabic.textAlignment = alignment
        binding.translation.textAlignment = alignment

        binding.arabic.visibility = if (style.content == ContentMode.TRANSLATION_ONLY) View.GONE else View.VISIBLE
        binding.translation.visibility = if (style.content == ContentMode.ARABIC_ONLY) View.GONE else View.VISIBLE
        binding.surahName.visibility = if (style.showSurahName) View.VISIBLE else View.GONE
        binding.reference.visibility = if (style.showReference) View.VISIBLE else View.GONE
    }

    private fun shareText(verse: Verse): String {
        val style = AppSettings.appStyle(this)
        return "${verse.arabic}\n\n${style.translationOf(verse)}\n\n— ${verse.reference}"
    }

    private fun share() {
        val verse = current ?: return
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText(verse))
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun copy() {
        val verse = current ?: return
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText(verse.reference, shareText(verse)))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Settings ----------------------------------------------------------------

    private fun buildIntervalChips() {
        val selected = AppSettings.interval(this)
        for (interval in RenewInterval.entries) {
            val chip = layoutInflater.inflate(R.layout.item_chip, binding.intervalGroup, false) as Chip
            chip.id = View.generateViewId()
            chip.setText(interval.labelRes)
            binding.intervalGroup.addView(chip)
            intervalChips[chip.id] = interval
            if (interval == selected) binding.intervalGroup.check(chip.id)
        }
        binding.intervalGroup.setOnCheckedStateChangeListener { _, ids ->
            val interval = ids.firstOrNull()?.let { intervalChips[it] } ?: return@setOnCheckedStateChangeListener
            if (interval == AppSettings.interval(this)) return@setOnCheckedStateChangeListener
            AppSettings.setInterval(this, interval)
            val app = applicationContext
            Thread {
                RenewScheduler.schedule(app)
                runCatching { WidgetUpdater.updateAll(app) }
            }.start()
            if (following) showScheduled() else current?.let { show(it) }
        }
    }

    // ---- Widgets -----------------------------------------------------------------

    private fun swatch(view: View, themeKey: String) {
        val theme = Themes.byKey(themeKey)
        view.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(theme.bgStart, theme.bgEnd))
            .apply { cornerRadius = 10 * resources.displayMetrics.density }
    }

    private fun buildWidgetKinds() {
        val manager = getSystemService(AppWidgetManager::class.java)
        for (kind in WidgetKind.entries) {
            val row = layoutInflater.inflate(R.layout.item_widget_row, binding.kindsContainer, false)
            row.findViewById<TextView>(R.id.rowTitle).setText(kind.titleRes)
            row.findViewById<TextView>(R.id.rowDesc).setText(kind.descriptionRes)
            swatch(row.findViewById(R.id.rowSwatch), kind.defaultStyle().theme)
            row.findViewById<Button>(R.id.rowButton).apply {
                setText(R.string.add)
                setOnClickListener {
                    val provider = ComponentName(this@MainActivity, kind.provider)
                    if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(provider, null, null)
                    else Toast.makeText(this@MainActivity, R.string.add_widget_manual, Toast.LENGTH_LONG).show()
                }
            }
            binding.kindsContainer.addView(row)
        }
    }

    private fun buildPlacedWidgets() {
        binding.placedContainer.removeAllViews()
        var count = 0
        for (kind in WidgetKind.entries) {
            WidgetUpdater.idsFor(this, kind).forEachIndexed { index, id ->
                count++
                val style = WidgetStore.style(this, id, kind)
                val row = layoutInflater.inflate(R.layout.item_widget_row, binding.placedContainer, false)
                row.findViewById<TextView>(R.id.rowTitle).text = "${getString(kind.titleRes)} #${index + 1}"
                row.findViewById<TextView>(R.id.rowDesc).text = when (style.content) {
                    ContentMode.CUSTOM_TEXT -> style.customText.take(40)
                    ContentMode.ARABIC_ONLY -> getString(R.string.content_arabic)
                    ContentMode.TRANSLATION_ONLY -> getString(R.string.content_translation)
                    ContentMode.ARABIC_AND_TRANSLATION -> getString(R.string.content_both)
                }
                swatch(row.findViewById(R.id.rowSwatch), style.theme)
                row.findViewById<Button>(R.id.rowButton).apply {
                    setText(R.string.edit)
                    setOnClickListener {
                        startActivity(
                            Intent(this@MainActivity, WidgetEditorActivity::class.java)
                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        )
                    }
                }
                binding.placedContainer.addView(row)
            }
        }
        binding.noWidgets.visibility = if (count == 0) View.VISIBLE else View.GONE
    }
}
