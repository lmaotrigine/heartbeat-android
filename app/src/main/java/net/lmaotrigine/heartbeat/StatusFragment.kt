package net.lmaotrigine.heartbeat

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import net.lmaotrigine.heartbeat.databinding.FragmentStatusBinding


class StatusFragment : Fragment() {
    companion object {
        private var shouldRun = false
        const val ACTION_UPDATE: String = "ACTION_UPDATE"
        var Status: String = "Unknown"
        var SubStatus: String = "Unknown"
        var updateReceiver: UpdateReceiver? = null
        val intentFilter: IntentFilter = IntentFilter(ACTION_UPDATE)
        @SuppressLint("StaticFieldLeak") // FIXME: sorry :( but this is my first project
        private var fragmentStatusBinding: FragmentStatusBinding? = null
        private val binding get() = fragmentStatusBinding!!
    }

    private var serviceBinding: IBinder? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        fragmentStatusBinding = FragmentStatusBinding.inflate(inflater, container, false)
        return binding.root

    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonStartStop.setOnClickListener{
            shouldRun = !shouldRun
            if (shouldRun) {
                val service = ForegroundService()
                serviceBinding = service.Binder()
                val intent = Intent(requireContext(), service::class.java)
                requireContext().applicationContext.startForegroundService(intent)
                binding.textViewStatus.text = "Starting"
                binding.buttonStartStop.text = getText(R.string.stop_hb).toString()
            } else {
                requireContext().applicationContext.stopService(Intent(requireContext(), ForegroundService()::class.java))
                binding.textViewStatus.text = "Stopped"
                binding.buttonStartStop.text = getText(R.string.start_hb).toString()
            }
        }
        val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (defaultSharedPreferences.getBoolean("showAds", false)) {
            val adView = view.findViewById<AdView>(R.id.adView)
            MobileAds.initialize(requireContext())
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        }
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(updateReceiver)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag") // guarded by SDK check
    override fun onResume() {
        super.onResume()
        if (updateReceiver == null) updateReceiver = UpdateReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requireContext().registerReceiver(
                updateReceiver, intentFilter,
                Context.RECEIVER_NOT_EXPORTED,
            )
        else
            requireContext().registerReceiver(updateReceiver, intentFilter)
    }

    override fun onDestroy() {
        try {
            requireContext().unregisterReceiver(updateReceiver)
        } catch (e: Exception) {
            Log.i("StatusFragment.kt:onDestroy", "Receiver not registered, no need to unregister")
        }
        updateReceiver = null
        super.onDestroy()
    }

    class UpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                if (intent.action == ACTION_UPDATE) {
                    Status = intent.extras?.getString("status").toString()
                    SubStatus = intent.extras?.getString("sub_status").toString()
                    binding.textViewStatus.text = Status
                    binding.textViewSubStatus.text = SubStatus
                    when (Status) {
                        "Running" -> binding.textViewStatus.setTextColor(context!!.getColor(R.color.running_green))
                        else -> binding.textViewStatus.setTextColor(context!!.getColor(R.color.not_running_red))
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentStatusBinding = null
    }
}
