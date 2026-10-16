package uz.dailyayah

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import uz.dailyayah.databinding.ActivityAboutBinding

/** Explains where the Arabic text and each translation come from. */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sources = listOf(
            R.string.about_arabic_title to R.string.about_arabic_body,
            R.string.about_uz_title to R.string.about_uz_body,
            R.string.about_ru_title to R.string.about_ru_body,
            R.string.about_en_title to R.string.about_en_body,
            R.string.about_data_title to R.string.about_data_body,
        )
        for ((title, body) in sources) {
            val card = layoutInflater.inflate(R.layout.item_source, binding.sources, false)
            card.findViewById<TextView>(R.id.sourceTitle).setText(title)
            card.findViewById<TextView>(R.id.sourceBody).setText(body)
            binding.sources.addView(card)
        }
    }
}
