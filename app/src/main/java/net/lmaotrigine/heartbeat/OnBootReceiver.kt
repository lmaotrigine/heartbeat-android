package net.lmaotrigine.heartbeat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager

class OnBootReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) {
                return
            }
            if (context != null) {
                val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                if (defaultSharedPreferences.getBoolean("start_on_boot", false)) {
                    val intent2 = Intent(context, ForegroundService::class.java)
                    context.applicationContext.startForegroundService(intent2)
                }
            }
        }
    }
}
