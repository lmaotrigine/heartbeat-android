package net.lmaotrigine.heartbeat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.noties.markwon.Markwon
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

val buildConfig = mapOf(
    "commit" to BuildConfig.COMMIT_SHA,
    "commit_count" to BuildConfig.COMMIT_COUNT,
    "build_time" to formattedBuildTime(),
    "branch" to BuildConfig.BRANCH
)

class AboutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)
        val button = view.findViewById<Button>(R.id.buttonReportBug)
        button.setOnClickListener {
            startActivity(Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/lmaotrigine/heartbeat-android/issues/new")
            ))
        }
        val textView = view.findViewById<TextView>(R.id.textViewAboutText)
        val text = getText(R.string.about).toString().format(buildConfig)
        val markwon = Markwon.create(requireContext())
        markwon.setMarkdown(textView, text)
        textView.movementMethod = LinkMovementMethod.getInstance()
        return view
    }

}

fun formattedBuildTime(): String {
    return try {
        val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.UK)
        inputDf.timeZone = TimeZone.getTimeZone("UTC")
        val buildTime = inputDf.parse(BuildConfig.BUILD_TIME)
        val outputDf =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        outputDf.timeZone = TimeZone.getDefault()
        outputDf.format(buildTime!!)
    } catch (e: Exception) {
        BuildConfig.BUILD_TIME
    }
}

// why does this language suck?
// I cannot believe the stdlib doesn't have (a better version of) this (hack)
// FIXME: This is unsafe. Yes I feel bad.
fun String.format(values: Map<String, String>): String {
    val result = StringBuilder()
    val withoutBraces = "\\$([a-zA-Z_][a-zA-Z0-9_]*)"
    val withBraces = "\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}"
    val matches = Regex("$withoutBraces|$withBraces|\\\$\\{'(\\\$)'\\}").findAll(this)
    var pos = 0
    matches.forEach {
        val range = it.range
        val placeholder = this.substring(range)
        val key = it.groups.filterNotNull()[1].value
        val newText = if (" \${'\$'}" == placeholder) "$" else values[key] ?: throw IllegalArgumentException("Could not find $placeholder in passed values.")
        result.append(this.substring(pos, range.first)).append(newText)
        pos = range.last + 1
    }
    result.append(this.substring(pos))
    return result.toString()
}
