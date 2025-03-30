package com.samyak.urlplayer.screen

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.samyak.urlplayer.R

import com.samyak.urlplayer.models.Videos
import com.samyak2403.custom_toast.TastyToast

class UpdateActivity : AppCompatActivity() {
    private lateinit var titleEditText: EditText
    private lateinit var urlEditText: EditText
    private lateinit var userAgentEditText: EditText
    private lateinit var titleLayout: TextInputLayout
    private lateinit var urlLayout: TextInputLayout
    private lateinit var changePinButton: Button

    private var originalTitle: String? = null
    private var originalUrl: String? = null
    private var originalUserAgent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)

        setupToolbar()
        initializeViews()
        loadChannelData()
        setupClickListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.update_channel)
        }

        // Set both title and navigation icon color to white
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
    }

    private fun initializeViews() {
        titleEditText = findViewById(R.id.titleEditText)
        urlEditText = findViewById(R.id.urlEditText)
        userAgentEditText = findViewById(R.id.userAgentEditText)

        titleLayout = findViewById(R.id.titleInputLayout)
        urlLayout = findViewById(R.id.urlInputLayout)
        changePinButton = findViewById(R.id.changePinButton)

        // Clear errors on focus
        titleEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) titleLayout.error = null
        }
        urlEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) urlLayout.error = null
        }

        changePinButton.setOnClickListener {
            startActivity(Intent(this, PinManagementActivity::class.java))
            finish()
        }
    }

    private fun loadChannelData() {
        originalTitle = intent.getStringExtra("TITLE")
        originalUrl = intent.getStringExtra("URL")
        originalUserAgent = intent.getStringExtra("USER_AGENT")

        titleEditText.setText(originalTitle)
        urlEditText.setText(originalUrl)
        userAgentEditText.setText(originalUserAgent)
    }

    private fun setupClickListeners() {
        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            if (validateInputs()) {
                updateChannel()
            }
        }

        findViewById<MaterialButton>(R.id.deleteButton).setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun validateInputs(): Boolean {
        val title = titleEditText.text.toString().trim()
        val url = urlEditText.text.toString().trim()
        var isValid = true

        if (title.isEmpty()) {
            titleLayout.error = getString(R.string.error_title_required)
            isValid = false
        } else if (title.length < 3) {
            titleLayout.error = getString(R.string.error_title_too_short)
            isValid = false
        }

        if (url.isEmpty()) {
            urlLayout.error = getString(R.string.error_url_required)
            isValid = false
        } else if (!isValidUrl(url)) {
            urlLayout.error = getString(R.string.error_invalid_url)
            isValid = false
        }

        return isValid
    }

    private fun isValidUrl(url: String): Boolean {
        return Patterns.WEB_URL.matcher(url).matches() ||
                url.startsWith("rtmp://") ||
                url.startsWith("rtsp://") ||
                url.startsWith("udp://") ||
                url.startsWith("rtp://") ||
                url.contains(".php?") // Added support for PHP-based streams
    }

    private fun detectUrlType(url: String): String {
        val lowercaseUrl = url.lowercase()
        return when {
            lowercaseUrl.endsWith(".m3u8") -> "HLS"
            lowercaseUrl.contains(".m3u8?") -> "HLS" // Added support for m3u8 with query params
            lowercaseUrl.endsWith(".mp4") -> "MP4"
            lowercaseUrl.endsWith(".avi") -> "AVI"
            lowercaseUrl.endsWith(".mkv") -> "MKV"
            lowercaseUrl.endsWith(".m3u") -> "M3U"
            lowercaseUrl.endsWith(".mpd") -> "DASH"
            lowercaseUrl.endsWith(".ts") -> "TS"
            lowercaseUrl.endsWith(".mov") -> "MOV"
            lowercaseUrl.endsWith(".webm") -> "WEBM"
            lowercaseUrl.startsWith("rtmp://") -> "RTMP"
            lowercaseUrl.startsWith("rtsp://") -> "RTSP"
            lowercaseUrl.startsWith("udp://") -> "UDP"
            lowercaseUrl.startsWith("rtp://") -> "RTP"
            lowercaseUrl.startsWith("mms://") -> "MMS"
            lowercaseUrl.startsWith("srt://") -> "SRT"
            // Add special detection for PHP-based streams
            lowercaseUrl.contains(".php") && lowercaseUrl.contains("?") -> "HLS"
            // Add special detection for live content
            lowercaseUrl.contains("live") ||
                    lowercaseUrl.contains("stream") ||
                    lowercaseUrl.contains("cricket") ||
                    lowercaseUrl.contains("match") ||
                    lowercaseUrl.contains("tv") ||
                    lowercaseUrl.contains("tata") -> "LIVE"

            else -> "HTTP"
        }
    }

    private fun updateChannel() {
        try {
            val title = titleEditText.text.toString().trim()
            val url = urlEditText.text.toString().trim()
            val userAgent = userAgentEditText.text.toString().trim()

            val sharedPreferences = getSharedPreferences("M3U8Links", MODE_PRIVATE)
            val currentLinks =
                sharedPreferences.getStringSet("links", mutableSetOf()) ?: mutableSetOf()
            val newLinks = currentLinks.toMutableSet()

            // Remove old entry
            newLinks.removeIf { it.startsWith("$originalTitle###") }

            // Create Videos object
            val video = Videos(
                name = title,
                url = url,
                userAgent = if (userAgent.isNotEmpty()) userAgent else null
            )

            // Add updated entry with URL type
            val urlType = detectUrlType(url)
            val isLiveStream = urlType == "LIVE" ||
                    urlType == "HLS" ||
                    url.lowercase().contains("live") ||
                    url.lowercase().contains("cricket") ||
                    url.lowercase().contains("match") ||
                    url.lowercase().contains("tv") ||
                    url.lowercase().contains("tata") ||
                    (url.lowercase().contains(".php") && url.lowercase().contains("?"))

            // Format: title###url###urlType###userAgent###isLiveStream
            val channelData = buildString {
                append("${video.name}###${video.url}###$urlType")
                if (!video.userAgent.isNullOrEmpty()) {
                    append("###${video.userAgent}")
                } else {
                    append("###")
                }
                append("###$isLiveStream")
            }

            newLinks.add(channelData)

            // Save changes
            sharedPreferences.edit().apply {
                putStringSet("links", newLinks)
                apply()
            }
            TastyToast.show(
                this,
                getString(R.string.success_channel_updated),
                TastyToast.Type.SUCCESS
            )
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            TastyToast.show(this, getString(R.string.error_updating_channel), TastyToast.Type.ERROR)
        }
    }

    private fun deleteChannel() {
        try {
            val sharedPreferences = getSharedPreferences("M3U8Links", MODE_PRIVATE)
            val currentLinks =
                sharedPreferences.getStringSet("links", mutableSetOf()) ?: mutableSetOf()
            val newLinks = currentLinks.toMutableSet()

            // Remove channel
            newLinks.removeIf { it.startsWith("$originalTitle###") }

            // Save changes
            sharedPreferences.edit().apply {
                putStringSet("links", newLinks)
                apply()
            }
            TastyToast.show(
                this,
                getString(R.string.success_channel_deleted),
                TastyToast.Type.SUCCESS
            )

            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            TastyToast.show(this, getString(R.string.error_deleting_channel), TastyToast.Type.ERROR)

        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_channel))
            .setMessage(getString(R.string.delete_channel_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> deleteChannel() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}