package uz.dailyayah.data

import android.content.Context
import uz.dailyayah.R
import java.util.TimeZone
import kotlin.random.Random

/** How often the verse changes. */
enum class RenewInterval(val minutes: Int, val labelRes: Int) {
    MIN_30(30, R.string.interval_30m),
    HOUR_1(60, R.string.interval_1h),
    HOURS_3(180, R.string.interval_3h),
    HOURS_6(360, R.string.interval_6h),
    DAILY(1440, R.string.interval_daily),
}

/**
 * Decides which verse is shown right now. Time is split into periods
 * (e.g. hours, aligned to the local clock); each period maps to one verse,
 * so all widgets agree and the choice survives restarts.
 */
object VerseSchedule {

    private fun localMinutes(now: Long): Long {
        val offset = TimeZone.getDefault().getOffset(now)
        return (now + offset) / 60_000L
    }

    private fun periodIndex(context: Context, now: Long = System.currentTimeMillis()): Long =
        localMinutes(now) / AppSettings.interval(context).minutes

    /** Identifies the current period; changes whenever a new verse is due. */
    fun currentPeriodKey(context: Context): String =
        "${AppSettings.interval(context).minutes}:${periodIndex(context)}"

    /** Global ayah number scheduled for the current period. */
    fun currentAyah(context: Context): Int {
        val interval = AppSettings.interval(context)
        val seed = periodIndex(context) * 7919L + interval.minutes
        return Random(seed).nextInt(1, QuranData.TOTAL_AYAHS + 1)
    }

    /** Wall-clock time (ms) when the next period starts. */
    fun nextBoundaryMillis(context: Context): Long {
        val now = System.currentTimeMillis()
        val minutes = AppSettings.interval(context).minutes
        val nextLocalMinute = (localMinutes(now) / minutes + 1) * minutes
        val offset = TimeZone.getDefault().getOffset(now)
        return nextLocalMinute * 60_000L - offset
    }
}
