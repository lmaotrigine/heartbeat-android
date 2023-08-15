package net.lmaotrigine.heartbeat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import io.noties.markwon.Markwon
import net.lmaotrigine.heartbeat.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding:ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        val text = getText(R.string.about)
        val textView = findViewById<TextView>(R.id.textViewAboutText)
        val markwon = Markwon.create(this)
        markwon.setMarkdown(textView, text.toString())
        textView.movementMethod = LinkMovementMethod.getInstance()
        val navController = findNavController(R.id.nav_host_fragment_content_heartbeat_status)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_heartbeat_status)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
