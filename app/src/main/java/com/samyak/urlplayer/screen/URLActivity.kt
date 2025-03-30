package com.samyak.urlplayer.screen

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.samyak.urlplayer.R

import com.samyak.urlplayer.models.Videos
import com.samyak2403.custom_toast.TastyToast
import android.view.inputmethod.InputMethodManager
import android.content.Context

class URLActivity : AppCompatActivity() {
    private lateinit var titleEditText: EditText
    private lateinit var urlEditText: EditText
    private lateinit var userAgentEditText: EditText
    private lateinit var titleLayout: TextInputLayout
    private lateinit var urlLayout: TextInputLayout
    private lateinit var pinRadioButton: RadioButton
    private lateinit var pinGroup: LinearLayout
    private lateinit var pinEditTexts: List<EditText>

    companion object {
        private val SUPPORTED_VIDEO_EXTENSIONS = listOf(
            ".m3u8",  // HLS streams
            ".mp4",   // MP4 videos
            ".avi",   // AVI videos
            ".mkv",   // MKV videos
            ".m3u",   // Playlist format
            ".ts",    // Transport streams
            ".mov",   // QuickTime videos
            ".webm",  // WebM videos
            ".mpd"    // DASH streams
        )

        private val SUPPORTED_PROTOCOLS = listOf(
            "http://",  // HTTP
            "https://", // HTTPS
            "rtmp://",  // RTMP
            "rtsp://",  // RTSP
            "udp://",   // UDP
            "rtp://",   // RTP
            "mms://",   // MMS
            "srt://"    // SRT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_urlactivity)

        setupToolbar()
        initializeViews()
        setupClickListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.add_url)
        }
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        
        // Set navigation icon color to white
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
    }

    private fun initializeViews() {
        // Initialize EditTexts
        titleEditText = findViewById(R.id.titleEditText)
        urlEditText = findViewById(R.id.urlEditText)
        userAgentEditText = findViewById(R.id.userAgentEditText)

        // Initialize TextInputLayouts
        titleLayout = titleEditText.parent.parent as TextInputLayout
        urlLayout = urlEditText.parent.parent as TextInputLayout

        // Initialize PIN components
        pinRadioButton = findViewById(R.id.pin)
        pinGroup = findViewById(R.id.PinGroup)
        pinEditTexts = listOf(
            findViewById(R.id.etPin1),
            findViewById(R.id.etPin2),
            findViewById(R.id.etPin3),
            findViewById(R.id.etPin4)
        )

        // Set PIN group visibility based on radio button
        pinGroup.visibility = View.GONE
        pinRadioButton.setOnCheckedChangeListener { _, isChecked ->
            pinGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Setup PIN input behavior (auto-advance to next field)
        setupPinInputBehavior()

        // Clear errors on text change
        titleEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) titleLayout.error = null
        }
        urlEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) urlLayout.error = null
        }
    }

    private fun setupPinInputBehavior() {
        // Auto-advance to next PIN field when a digit is entered
        for (i in 0 until pinEditTexts.size - 1) {
            val currentEditText = pinEditTexts[i]
            val nextEditText = pinEditTexts[i + 1]
            
            currentEditText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (s?.length == 1) {
                        nextEditText.requestFocus()
                    }
                }
            })
        }
        
        // Add listener for the last PIN field to hide keyboard when filled
        pinEditTexts.last().addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == 1) {
                    // Hide keyboard after last digit
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(pinEditTexts.last().windowToken, 0)
                }
            }
        })
    }

    private fun setupClickListeners() {
        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            if (validateInputs()) {
                saveChannelDetails()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val title = titleEditText.text.toString().trim()
        val url = urlEditText.text.toString().trim()
        var isValid = true

        // Validate title
        if (title.isEmpty()) {
            titleLayout.error = getString(R.string.error_title_required)
            isValid = false
        } else if (title.length < 3) {
            titleLayout.error = getString(R.string.error_title_too_short)
            isValid = false
        }

        // Validate URL
        if (url.isEmpty()) {
            urlLayout.error = getString(R.string.error_url_required)
            isValid = false
        } else if (!isValidStreamUrl(url)) {
            showUrlError()
            isValid = false
        }

        // Validate PIN if enabled
        if (pinRadioButton.isChecked) {
            val pin = getPinFromInputs()
            if (pin.length != 4) {
                TastyToast.show(this, "Please enter a 4-digit PIN", TastyToast.Type.WARNING)
                isValid = false
            }
        }

        return isValid
    }

    private fun getPinFromInputs(): String {
        val pinBuilder = StringBuilder()
        for (pinEditText in pinEditTexts) {
            pinBuilder.append(pinEditText.text.toString())
        }
        return pinBuilder.toString()
    }

    /**
     * Validate whether a string is a valid URL.
     */
    private fun isValidUrl(url: String): Boolean {
        return SUPPORTED_PROTOCOLS.any { protocol ->
            url.lowercase().startsWith(protocol)
        }
    }

    /**
     * Validate whether a string is a valid stream URL.
     */
    private fun isValidStreamUrl(url: String): Boolean {
        if (!isValidUrl(url)) return false

        val lowercaseUrl = url.lowercase()
        
        // Check for supported file extensions
        if (SUPPORTED_VIDEO_EXTENSIONS.any { ext -> lowercaseUrl.endsWith(ext) }) {
            return true
        }

        // Enhanced check for PHP-based streams with query parameters
        if (lowercaseUrl.contains(".php") && lowercaseUrl.contains("?")) {
            return true
        }
        
        // Enhanced check for streaming keywords in the URL
        return lowercaseUrl.contains("stream") || 
               lowercaseUrl.contains("live") || 
               lowercaseUrl.contains("video") ||
               lowercaseUrl.contains("play") ||
               lowercaseUrl.contains("cricket") ||
               lowercaseUrl.contains("sport") ||
               lowercaseUrl.contains("match") ||
               lowercaseUrl.contains("tv") ||
               lowercaseUrl.contains("channel") ||
               lowercaseUrl.contains("tata") ||
               lowercaseUrl.contains("id=")
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

    private fun showUrlError() {
        urlLayout.error = getString(R.string.error_invalid_url)
        urlLayout.isErrorEnabled = true
    }

    private fun saveChannelDetails() {
        try {
            val title = titleEditText.text.toString().trim()
            val url = urlEditText.text.toString().trim()
            val userAgent = userAgentEditText.text.toString().trim()

            // Get existing channels
            val sharedPreferences = getSharedPreferences("M3U8Links", MODE_PRIVATE)
            val currentLinks = sharedPreferences.getStringSet("links", mutableSetOf()) ?: mutableSetOf()
            
            // Check for duplicate titles
            if (currentLinks.any { it.split("###")[0] == title }) {
                titleLayout.error = getString(R.string.error_title_exists)
                return
            }

            // Create Videos object
            val video = Videos(
                name = title,
                url = url,
                userAgent = if (userAgent.isNotEmpty()) userAgent else null
            )

            // Get PIN if enabled
            val pin = if (pinRadioButton.isChecked) {
                getPinFromInputs()
            } else {
                ""
            }

            // Add new channel with URL type detection
            val urlType = detectUrlType(url)
            val isLiveStream = urlType == "LIVE" || 
                              urlType == "HLS" || 
                              url.lowercase().contains("live") ||
                              url.lowercase().contains("cricket") ||
                              url.lowercase().contains("match") ||
                              url.lowercase().contains("tv") ||
                              url.lowercase().contains("tata") ||
                              (url.lowercase().contains(".php") && url.lowercase().contains("?"))
            
            val newLinks = currentLinks.toMutableSet()
            
            // Format: title###url###urlType###userAgent###isLiveStream###pin
            val channelData = buildString {
                append("${video.name}###${video.url}###$urlType")
                if (!video.userAgent.isNullOrEmpty()) {
                    append("###${video.userAgent}")
                } else {
                    append("###")
                }
                append("###$isLiveStream###$pin")
            }
            
            newLinks.add(channelData)
            
            // Save to SharedPreferences
            sharedPreferences.edit().apply {
                putStringSet("links", newLinks)
                apply()
            }

            showSuccessAndFinish()
        } catch (e: Exception) {
            showError(e.message ?: getString(R.string.error_saving_channel))
        }
    }

    private fun showSuccessAndFinish() {
        TastyToast.show(this,  getString(R.string.success_channel_saved), TastyToast.Type.SUCCESS)
        setResult(RESULT_OK)
        finish()
    }

    private fun showError(message: String) {
        TastyToast.show(this, message, TastyToast.Type.DEFAULT)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}