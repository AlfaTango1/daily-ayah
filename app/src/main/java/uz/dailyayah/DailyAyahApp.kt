package uz.dailyayah

import android.app.Application
import uz.dailyayah.work.DailyVerseWorker

class DailyAyahApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DailyVerseWorker.schedulePeriodic(this)
    }
}
