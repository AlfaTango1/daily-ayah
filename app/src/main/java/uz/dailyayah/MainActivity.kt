package uz.dailyayah

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uz.dailyayah.data.TranslationMode
import uz.dailyayah.data.Verse
import uz.dailyayah.data.VerseRepository
import uz.dailyayah.databinding.ActivityMainBinding
import uz.dailyayah.widget.VerseWidgetProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: VerseRepository
    private var current: Verse? = null
    private var showingToday = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = VerseRepository(this)

        setUpTranslationToggle()

        binding.btnToday.setOnClickListener { loadToday() }
        binding.btnRandom.setOnClickListener { loadRandom() }
        binding.btnShare.setOnClickListener { share() }
        binding.btnCopy.setOnClickListener { copy() }
        binding.btnAddWidget.setOnClickListener { requestPinWidget() }
        binding.btnRetry.setOnClickListener { if (showingToday) loadToday() else loadRandom() }

        // Show the cached verse instantly, then refresh.
        (repo.cachedToday() ?: repo.lastSaved())?.let { show(it) }
        loadToday()
    }

    private fun setUpTranslationToggle() {
        val checkedId = when (repo.translationMode) {
            TranslationMode.ENGLISH -> R.id.mode_en
            TranslationMode.RUSSIAN -> R.id.mode_ru
            TranslationMode.BOTH -> R.id.mode_both
        }
        binding.modeGroup.check(checkedId)
        binding.modeGroup.addOnButtonCheckedListener { _, id, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            repo.translationMode = when (id) {
                R.id.mode_ru -> TranslationMode.RUSSIAN
                R.id.mode_both -> TranslationMode.BOTH
                else -> TranslationMode.ENGLISH
            }
            current?.let { show(it) }
            VerseWidgetProvider.updateAll(this)
        }
    }

    private fun loadToday() {
        showingToday = true
        load { repo.today() }
    }

    private fun loadRandom() {
        showingToday = false
        load { repo.random() }
    }

    private fun load(block: suspend () -> Verse) {
        binding.progress.visibility = View.VISIBLE
        binding.errorBox.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val verse = block()
                show(verse)
                if (showingToday) VerseWidgetProvider.updateAll(this@MainActivity)
            } catch (e: Exception) {
                if (current == null) binding.errorBox.visibility = View.VISIBLE
                else Toast.makeText(this@MainActivity, R.string.error_offline, Toast.LENGTH_SHORT).show()
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun show(verse: Verse) {
        current = verse
        binding.title.setText(if (showingToday) R.string.verse_of_the_day else R.string.random_verse)
        binding.surahArabic.text = verse.surahNameAr
        binding.reference.text = verse.reference
        binding.arabic.text = verse.arabic
        binding.translation.text = repo.translationFor(verse)
        binding.card.visibility = View.VISIBLE
        binding.errorBox.visibility = View.GONE
    }

    private fun shareText(verse: Verse): String =
        "${verse.arabic}\n\n${repo.translationFor(verse)}\n\n— ${verse.reference}"

    private fun share() {
        val verse = current ?: return
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, shareText(verse))
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun copy() {
        val verse = current ?: return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(verse.reference, shareText(verse)))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }
    }

    /** Asks the launcher to place the widget (Android 8+, if the launcher supports it). */
    private fun requestPinWidget() {
        val manager = getSystemService(AppWidgetManager::class.java)
        val provider = ComponentName(this, VerseWidgetProvider::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(provider, null, null)
        } else {
            Toast.makeText(this, R.string.add_widget_manual, Toast.LENGTH_LONG).show()
        }
    }
}
