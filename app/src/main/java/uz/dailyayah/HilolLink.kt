package uz.dailyayah

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import uz.dailyayah.data.Verse

/**
 * Opens the official "Tafsiri Hilol" app (Hilol Nashr) for a verse.
 * Tafsiri Hilol does not publish a link format for opening a specific
 * verse, so we open the app and tell the reader which surah and verse to go to.
 */
object HilolLink {

    const val PACKAGE = "uz.hilal.tafsir"

    const val EXTRA_SURAH = "uz.dailyayah.extra.SURAH"
    const val EXTRA_AYAH_IN_SURAH = "uz.dailyayah.extra.AYAH_IN_SURAH"
    const val EXTRA_SURAH_NAME = "uz.dailyayah.extra.SURAH_NAME"

    fun isInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(PACKAGE) != null

    /** Intent for the small hand-off screen used by widgets. */
    fun widgetIntent(context: Context, verse: Verse): Intent =
        Intent(context, OpenInHilolActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            .putExtra(EXTRA_SURAH, verse.surahNumber)
            .putExtra(EXTRA_AYAH_IN_SURAH, verse.numberInSurah)
            .putExtra(EXTRA_SURAH_NAME, verse.surahNameEn)

    fun open(activity: Activity, surah: Int, ayah: Int, surahName: String) {
        val launch = activity.packageManager.getLaunchIntentForPackage(PACKAGE)
        if (launch != null) {
            Toast.makeText(
                activity,
                activity.getString(R.string.hilol_goto, surahName, surah, ayah),
                Toast.LENGTH_LONG
            ).show()
            activity.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        // Not installed: show it in the Play Store.
        Toast.makeText(activity, R.string.hilol_not_installed, Toast.LENGTH_LONG).show()
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun open(activity: Activity, verse: Verse) =
        open(activity, verse.surahNumber, verse.numberInSurah, verse.surahNameEn)
}

/** Invisible screen a widget tap goes through: opens Tafsiri Hilol and closes. */
class OpenInHilolActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HilolLink.open(
            this,
            intent.getIntExtra(HilolLink.EXTRA_SURAH, 1),
            intent.getIntExtra(HilolLink.EXTRA_AYAH_IN_SURAH, 1),
            intent.getStringExtra(HilolLink.EXTRA_SURAH_NAME).orEmpty(),
        )
        finish()
        overridePendingTransition(0, 0)
    }
}
