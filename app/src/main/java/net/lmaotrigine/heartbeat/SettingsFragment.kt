package net.lmaotrigine.heartbeat

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {
    private var preferences: SharedPreferences? = null
    private val sharedPreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "ui_theme") {
            when (sharedPreferences.getString("ui_theme", "default")) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "default" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                else -> {
                    Log.w("heartbeat:ui_theme", "Invalid ui_theme value")
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (preferences == null) preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        preferences?.registerOnSharedPreferenceChangeListener(sharedPreferenceListener)
    }

    override fun onPause() {
        super.onPause()
        preferences?.unregisterOnSharedPreferenceChangeListener(sharedPreferenceListener)
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onDestroy() {
        try {
            preferences?.unregisterOnSharedPreferenceChangeListener(sharedPreferenceListener)
        } catch (e: Exception) {
            Log.i("SettingsFragment:onDestroy", "Preference change listener was not registered. Nothing to do")
        }
        super.onDestroy()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}
