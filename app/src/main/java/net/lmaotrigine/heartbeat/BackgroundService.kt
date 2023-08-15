package net.lmaotrigine.heartbeat

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

class BackgroundService : BroadcastReceiver() {
    private var screenOn = true

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            screenOn = when (intent.action) {
                Intent.ACTION_SCREEN_ON -> true
                Intent.ACTION_USER_PRESENT -> true
                Intent.ACTION_SCREEN_OFF -> false
                else -> screenOn
            }
            val screenStatus =
                if (screenOn) "Screen is on, sending ping..." else "Screen is off, sleeping"
            Log.v("ScreenStatus", screenStatus)
            if (screenOn) {
                ping(context)
            }
        }
    }
    var since:Instant? = null
    var lastUpdateTimestamp: Int = 0
    var notification: Notification? = null

    override fun peekService(myContext: Context?, service: Intent?): IBinder {
        return super.peekService(myContext, service)
    }

    private fun ping(context: Context?) {
        Thread {
            try {
                if (context == null) {
                    throw Exception()
                }
                val defaultSharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context)
                val urlString = defaultSharedPreferences.getString("server_url", "")
                val url = URL("${urlString}/api/beat")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doInput = true
                connection.doOutput = true
                connection.setRequestProperty(
                    "Authorization",
                    defaultSharedPreferences.getString("server_authorization", "")
                )
                connection.connect()
                Log.i("BGService-Ping", "Connected")
                val reader = InputStreamReader(connection.inputStream)
                val response = reader.readText()
                Log.i("BGService-Response", response)
                connection.disconnect()
                Log.i("BGService-Ping", "Successful ping!")
                lastUpdateTimestamp = response.trim().toInt()
                val updateIntent = Intent(StatusFragment.ACTION_UPDATE)
                val bundle = Bundle()
                val status = "Running"
                val subStatus = "As of ${since.toString()}"
                bundle.putString("status", status)
                bundle.putString("sub_status", subStatus)
                updateIntent.putExtras(bundle)
                context.sendBroadcast(updateIntent)
            } catch (e: Exception) {
                Log.e("BGService-Ping", e.toString())
                Log.e("BGService-Ping", e.stackTraceToString())
            }
        }.start()
    }
}