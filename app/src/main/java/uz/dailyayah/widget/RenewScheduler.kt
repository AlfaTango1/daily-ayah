package uz.dailyayah.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import uz.dailyayah.data.VerseSchedule

/** Wakes up at the start of each period (e.g. every hour) to show the next verse. */
object RenewScheduler {

    const val ACTION_RENEW = "uz.dailyayah.action.RENEW"

    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, UpdateReceiver::class.java).setAction(ACTION_RENEW)
        val pending = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Inexact but battery-friendly; no special permission needed.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC, VerseSchedule.nextBoundaryMillis(context) + 1_500, pending)
    }
}

/** Handles the renewal alarm, reboot and clock/time-zone changes. */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        Thread {
            try {
                WidgetUpdater.updateAll(app)
            } catch (e: Exception) {
                Log.e("DailyAyah", "Renewal failed", e)
            } finally {
                RenewScheduler.schedule(app)
                pending.finish()
            }
        }.start()
    }
}
