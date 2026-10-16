package uz.dailyayah.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import uz.dailyayah.data.VerseRepository
import uz.dailyayah.widget.VerseWidgetProvider
import java.util.concurrent.TimeUnit

/** Downloads today's verse (if needed) and redraws every widget. */
class DailyVerseWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        VerseRepository(applicationContext).today()
        VerseWidgetProvider.updateAll(applicationContext)
        Result.success()
    } catch (e: Exception) {
        // Still redraw with whatever is cached, then let WorkManager retry.
        VerseWidgetProvider.updateAll(applicationContext)
        if (runAttemptCount < 5) Result.retry() else Result.failure()
    }

    companion object {
        private const val PERIODIC = "daily_verse_periodic"
        private const val NOW = "daily_verse_now"

        private val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Checks every few hours, so the widget changes soon after midnight. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyVerseWorker>(3, TimeUnit.HOURS)
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        /** Runs once as soon as there is a connection. */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<DailyVerseWorker>()
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOW, ExistingWorkPolicy.REPLACE, request
            )
        }
    }
}
