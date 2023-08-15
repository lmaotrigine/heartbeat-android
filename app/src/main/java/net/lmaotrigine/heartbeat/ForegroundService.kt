package net.lmaotrigine.heartbeat

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import androidx.preference.PreferenceManager
import net.lmaotrigine.heartbeat.StatusFragment.Companion.ACTION_UPDATE
import java.io.FileDescriptor
import java.time.Instant

class ForegroundService : Service() {
    class Transaction {
        companion object {
            const val GET_STATUS: Int = 1
            const val GET_LAST_PING: Int = 2
            const val GET_LAST_ERROR: Int = 3
        }
    }
    var running: Boolean = false
    var since: Instant? = Instant.now()
    private val bgService = BackgroundService()
    var lastError = ""
    private var context: Context? = null
    private var notificationChannel: NotificationChannel? = null
    private var notification: Notification? = null
    private var notificationManager: NotificationManager? = null
    private var status = ""
    private var subStatus = ""
    private var defaultSharedPreferences: SharedPreferences? = null

    override fun onBind(intent: Intent?): IBinder {
        return Binder()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        context = baseContext
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context!!)
        if (notificationChannel == null) {
            createNotificationChannel()
        }
        if (notification == null) createNotification()
        startForeground(1, notification)
        context?.registerReceiver(bgService, IntentFilter(Intent.ACTION_TIME_TICK))
        context?.registerReceiver(bgService, IntentFilter(Intent.ACTION_SCREEN_ON))
        context?.registerReceiver(bgService, IntentFilter(Intent.ACTION_SCREEN_OFF))
        context?.registerReceiver(bgService, IntentFilter(Intent.ACTION_USER_PRESENT))
        running = true
        since = Instant.now()
        bgService.since = since
        bgService.notification = notification
        val notificationUpdateReceiver = NotificationUpdateReceiver()
        notificationUpdateReceiver.foregroundService = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context?.registerReceiver(
                notificationUpdateReceiver, IntentFilter(ACTION_UPDATE),
                RECEIVER_NOT_EXPORTED,
            )
        } else
            context?.registerReceiver(
                notificationUpdateReceiver, IntentFilter(ACTION_UPDATE)
            )
        return START_STICKY
    }

    class NotificationUpdateReceiver : BroadcastReceiver() {
        var foregroundService: ForegroundService? = null

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                if (intent.action == ACTION_UPDATE) {
                    if (intent.extras != null) {
                        foregroundService?.status = intent.extras!!.getString("status").toString()
                        foregroundService?.subStatus = intent.extras!!.getString("sub_status").toString()
                        foregroundService?.createNotification()
                        foregroundService?.notificationManager?.notify(1, foregroundService!!.notification)
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Foreground Service"
        val descriptionText = "Foreground Service"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        notificationChannel = NotificationChannel("fgService", name, importance)
        notificationChannel!!.description = descriptionText
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager!!.createNotificationChannel(notificationChannel!!)
    }

    private fun createNotification() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(defaultSharedPreferences?.getString("server_url", "https://hb.5ht2.me"))
        val notificationStatus = if (status != "") status else "Heartbeat Status"
        val notificationSubStatus = if (subStatus != "") subStatus else "Heartbeat is running"
        val pendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE)
        }
        notification = Notification.Builder(context, "fgService").setContentTitle(notificationStatus).setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(pendingIntent).setStyle(Notification.BigTextStyle().bigText(notificationSubStatus)).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        context?.unregisterReceiver(bgService)
        stopSelf()
    }

    inner class Binder : IBinder {

        override fun getInterfaceDescriptor(): String {
            return "HeartbeatService"
        }

        override fun pingBinder(): Boolean {
            return this@ForegroundService.running
        }

        override fun isBinderAlive(): Boolean {
            return this@ForegroundService.running
        }

        override fun queryLocalInterface(descriptor: String): IInterface? {
            return null
        }

        override fun dump(fd: FileDescriptor, args: Array<out String>?) {
            throw NotImplementedError()
        }

        override fun dumpAsync(fd: FileDescriptor, args: Array<out String>?) {
            throw NotImplementedError()
        }

        override fun transact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            when (code) {
                Transaction.GET_STATUS -> {
                    if (running) reply?.writeString("Running\nsince ${since.toString()}")
                    else reply?.writeString("Stopped\nsince ${since.toString()}")
                    reply?.setDataPosition(0)
                    return true
                }
                Transaction.GET_LAST_PING -> {
                    reply?.writeInt(bgService.lastUpdateTimestamp)
                    reply?.setDataPosition(0)
                    return true
                }
                Transaction.GET_LAST_ERROR -> {
                    return if (lastError != "") {
                        reply?.writeString(lastError)
                        reply?.setDataPosition(0)
                        true
                    } else false
                }
                else -> return false
            }
        }

        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) {
            TODO("Not yet implemented")
        }

        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int): Boolean {
            TODO("Not yet implemented")
        }
    }
}
