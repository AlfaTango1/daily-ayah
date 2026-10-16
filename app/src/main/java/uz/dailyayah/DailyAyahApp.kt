package uz.dailyayah

import android.app.Application
import uz.dailyayah.widget.RenewScheduler

class DailyAyahApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RenewScheduler.schedule(this)
    }
}
