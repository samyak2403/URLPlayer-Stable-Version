package com.samyak.urlplayer.screen

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.Locale
import android.media.AudioManager
import android.content.res.Resources
import android.graphics.Typeface
import android.view.GestureDetector
import androidx.core.view.GestureDetectorCompat
import com.samyak.urlplayer.databinding.MoreFeaturesBinding
import kotlin.math.abs
import com.samyak.urlplayer.databinding.ActivityPlayerBinding
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.media.audiofx.LoudnessEnhancer
import com.samyak.urlplayer.databinding.BoosterBinding
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.*
import android.view.Gravity
import android.util.TypedValue
import android.widget.FrameLayout

import com.google.android.gms.cast.CastStatusCodes
import android.util.Log

import com.samyak.urlplayer.AdManage.showInterstitialAd
import android.util.Rational
import android.view.SurfaceView
import com.samyak.urlplayer.base.BaseActivity
import com.samyak.urlplayer.utils.LanguageManager
import android.widget.Button
import androidx.media3.common.C

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource

import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.rtmp.RtmpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.common.VideoSize
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import com.samyak.urlplayer.R
import java.net.HttpURLConnection
import java.net.URL

@UnstableApi
class PlayerActivity : BaseActivity(), GestureDetector.OnGestureListener {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var linearLayoutControlUp: LinearLayout
    private lateinit var linearLayoutControlBottom: LinearLayout

    // Custom controller views
    private lateinit var backButton: ImageButton
    private lateinit var videoTitle: TextView
    private lateinit var moreFeaturesButton: ImageButton
    private lateinit var playPauseButton: ImageButton
    private lateinit var repeatButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var fullScreenButton: ImageButton

    private var playbackPosition = 0L
    private var isPlayerReady = false
    private var isFullscreen: Boolean = true
    private var url: String? = null
    private var userAgent: String? = null
    private lateinit var trackSelector: DefaultTrackSelector
    private var currentQuality = "Auto"

    private data class VideoQuality(
        val height: Int,
        val width: Int,
        val bitrate: Int,
        val label: String,
        val description: String
    )

    private val availableQualities = listOf(
        VideoQuality(1080, 1920, 8_000_000, "1080p", "Full HD - Best quality"),
        VideoQuality(720, 1280, 5_000_000, "720p", "HD - High quality"),
        VideoQuality(480, 854, 2_500_000, "480p", "SD - Good quality"),
        VideoQuality(360, 640, 1_500_000, "360p", "SD - Normal quality"),
        VideoQuality(240, 426, 800_000, "240p", "Low - Basic quality"),
        VideoQuality(144, 256, 500_000, "144p", "Very Low - Minimal quality")
    )

    private var isManualQualityControl = false

    private lateinit var gestureDetectorCompat: GestureDetectorCompat
    private var minSwipeY: Float = 0f
    private var brightness: Int = 0
    private var volume: Int = 0
    private var audioManager: AudioManager? = null

    private var isLocked = false

    // Update supported formats with comprehensive streaming formats
    private val supportedFormats = mapOf(
        // Common video formats
        "mp4" to "video/mp4",
        "mkv" to "video/x-matroska",
        "webm" to "video/webm",
        "3gp" to "video/3gpp",
        "avi" to "video/x-msvideo",
        "mov" to "video/quicktime",
        "wmv" to "video/x-ms-wmv",
        "flv" to "video/x-flv",

        // Streaming formats
        "m3u8" to "application/x-mpegURL",  // Updated MIME type
        "m3u" to "application/x-mpegURL",   // Updated MIME type
        "ts" to "video/mp2t",
        "mpd" to "application/dash+xml",
        "ism" to "application/vnd.ms-sstr+xml",

        // Transport stream formats
        "mts" to "video/mp2t",
        "m2ts" to "video/mp2t",

        // Legacy formats
        "mp2" to "video/mpeg",
        "mpg" to "video/mpeg",
        "mpeg" to "video/mpeg",

        // Additional streaming formats
        "hls" to "application/x-mpegURL",  // Updated MIME type
        "dash" to "application/dash+xml",
        "smooth" to "application/vnd.ms-sstr+xml",

        // Playlist formats
        "pls" to "audio/x-scpls",
        "asx" to "video/x-ms-asf",
        "xspf" to "application/xspf+xml",

        // Add DASH format
        "mpd" to "application/dash+xml",

        // Add RTMP format
        "rtmp" to "video/rtmp",
        "rtmps" to "video/rtmps",
        
        // Add additional streaming formats
        "f4v" to "video/mp4",
        "f4m" to "application/adobe-f4m",
        "ssm" to "application/vnd.ms-sstr+xml",
        "vtt" to "text/vtt",
        "srt" to "application/x-subrip",
        "ttml" to "application/ttml+xml",
        "dfxp" to "application/ttaf+xml",
        "smil" to "application/smil+xml",
        "wvm" to "video/wvm",
        "isml" to "application/vnd.ms-sstr+xml",
        "m4s" to "video/iso.segment",
        "cmaf" to "video/mp4",
        "mss" to "application/vnd.ms-sstr+xml"
    )

    private var isPlaying = false

    // Add these properties
    private var position: Int = -1
    private var playerList: ArrayList<String> = ArrayList()

    // Add these properties if not already present
    private lateinit var loudnessEnhancer: LoudnessEnhancer
    private var boostLevel: Int = 0
    private var isBoostEnabled: Boolean = false

    private val maxBoostLevel = 15 // Maximum boost level (1500%)

    // Add these properties for casting
    private lateinit var castContext: CastContext
    private lateinit var sessionManager: SessionManager
    private var castSession: CastSession? = null
    private lateinit var mediaRouteButton: MediaRouteButton

    // Add after other properties
    private var screenHeight: Int = 0
    private var screenWidth: Int = 0

    // Add at the top with other properties
    private var isPipRequested = false

    // Add this property to track notch mode
    private var isNotchModeEnabled = true

    // Add this property to track if stream is live
    private var isLiveStream = false

    // Add these properties to track screen state before entering PiP
    private var prePipScreenMode = ScreenMode.FILL
    private var prePipNotchEnabled = true

    // Add this property to track if we're showing an ad
    private var isShowingAd = false

    private var liveStreamStartTime = 0L
    private var liveStreamDuration = 30 * 60 * 1000L // 30 minutes buffer by default


    // Add these properties to your class
    private var lastKnownLiveDuration: Long = 0
    private var lastLiveUpdateTime: Long = System.currentTimeMillis()
    private var lastPositionUpdateTime: Long = System.currentTimeMillis()
    private var isLiveTextAnimating: Boolean = false

    private val castSessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            // Save current playback position
            val position = player.currentPosition
            // Start casting
            loadRemoteMedia(position)
            // Pause local playback
            player.pause()
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            Toast.makeText(this@PlayerActivity, "Failed to start casting", Toast.LENGTH_SHORT).show()
        }

        override fun onSessionEnding(session: CastSession) {
            // Return to local playback
            val position = session.remoteMediaClient?.approximateStreamPosition ?: 0
            player.seekTo(position)
            player.playWhenReady = true
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession = null
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            castSession = session
        }
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    companion object {
        private const val INCREMENT_MILLIS = 5000L
        var pipStatus: Int = 0
    }

    // Add these properties at the top of the class
    private var playbackState = PlaybackState.IDLE
    private var wasPlayingBeforePause = false

    private enum class PlaybackState {
        IDLE, PLAYING, PAUSED, BUFFERING, ENDED
    }

    // Add this enum at the top of the class
    private enum class ScreenMode {
        FIT, FILL, ZOOM
    }

    // Add this property to track current screen mode
    private var currentScreenMode = ScreenMode.FILL

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Force landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Set up edge-to-edge display with notch support
        setupEdgeToEdgeDisplay()

        // Enable notch mode by default
        isNotchModeEnabled = true

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Handle different intent types
        handleIntent(intent)

        if (url == null) {
            Toast.makeText(this, "No valid URL provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views first
        initializeViews()

        // Initialize gesture and audio controls
        gestureDetectorCompat = GestureDetectorCompat(this, this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0

        setupPlayer()
        setupGestureControls()

        // Restore saved boost level
        boostLevel = getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
            .getInt("boost_level", 0)
        isBoostEnabled = getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
            .getBoolean("boost_enabled", false)

        // Initialize cast context
        try {
            castContext = CastContext.getSharedInstance(this)
            sessionManager = castContext.sessionManager
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Apply fullscreen mode by default
        playInFullscreen(enable = true)
    }

    private fun handleIntent(intent: Intent) {
        // First check for explicit URL extra (from our own app or other apps)
        url = intent.getStringExtra("URL")
        userAgent = intent.getStringExtra("USER_AGENT")

        // Always show ad when not in PiP mode
        if (shouldShowAd()) {
            isShowingAd = true
            showInterstitialAd {
                isShowingAd = false
                processUrlAndContinue(intent)
                // Auto-play after ad closes
                if (isPlayerReady) {
                    playVideo()
                }
            }
        } else {
            // Skip ad and continue directly when in PiP mode
            processUrlAndContinue(intent)
        }
    }

    private fun processUrlAndContinue(intent: Intent) {
        // If URL is null, try to get it from the data URI (VIEW intents)
        if (url == null && intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                url = uri.toString()

                // Try to extract title from URI path if no channel name provided
                if (intent.getStringExtra("CHANNEL_NAME") == null) {
                    val path = uri.path
                    if (path != null) {
                        val fileName = path.substringAfterLast('/')
                            .substringBeforeLast('.')
                            .replace("_", " ")
                            .replace("-", " ")
                            .capitalize(Locale.getDefault())

                        intent.putExtra("CHANNEL_NAME", fileName)
                    }
                }
            }
        }

        // Check RTMP support if needed
        if (url?.startsWith("rtmp://", ignoreCase = true) == true && !isRtmpSupported()) {
            Toast.makeText(
                this,
                "RTMP streaming is not supported on this device",
                Toast.LENGTH_LONG
            ).show()
        }

        // Enhanced handling for Hotstar-style URLs with query parameters
        if (url != null) {
            // Extract channel name from URL parameters if available
            var channelName: String? = null

            // Try to extract ID parameter (Hotstar style)
            val idMatch = Regex("(?:id|c|channel)=(\\w+)").find(url!!)
            if (idMatch != null) {
                val channelId = idMatch.groupValues[1]
                channelName = channelId.replace("_", " ")
                    .replace("-", " ")
                    .capitalize(Locale.getDefault())

                Log.d("PlayerActivity", "Extracted channel ID: $channelId")
            }
            // If no ID parameter, try to extract from path for m3u8 files
            else if (url!!.contains(".m3u8", ignoreCase = true)) {
                val pathParts = Uri.parse(url).path?.split("/") ?: emptyList()
                val streamName = pathParts.lastOrNull {
                    it.isNotEmpty() && !it.endsWith(".m3u8", ignoreCase = true)
                }

                if (streamName != null) {
                    channelName = streamName.replace("_", " ")
                        .replace("-", " ")
                        .capitalize(Locale.getDefault())

                    Log.d("PlayerActivity", "Extracted stream name from path: $streamName")
                }
            }

            // Set channel name if found and not already set
            if (channelName != null && intent.getStringExtra("CHANNEL_NAME") == null) {
                intent.putExtra("CHANNEL_NAME", channelName)
                Log.d("PlayerActivity", "Set channel name: $channelName")
            }
        }

        // Set default user agent if not provided
        if (userAgent == null) {
            // Use a more browser-like user agent for better compatibility with streaming services
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        }

        // Log the received intent data for debugging
        Log.d("PlayerActivity", "Received URL: $url")
        Log.d("PlayerActivity", "Channel Name: ${intent.getStringExtra("CHANNEL_NAME")}")
        Log.d("PlayerActivity", "User Agent: $userAgent")
    }

    private fun initializeViews() {
        // Initialize main views from binding
        playerView = binding.playerView
        progressBar = binding.progressBar
        errorTextView = binding.errorTextView
        linearLayoutControlUp = binding.linearLayoutControlUp
        linearLayoutControlBottom = binding.linearLayoutControlBottom

        // Setup player first
        setupPlayer()

        // Then initialize custom controller views and actions
        setupCustomControllerViews()
        setupCustomControllerActions()
    }

    private fun setupCustomControllerViews() {
        try {
            // Find all controller views from playerView
            backButton = playerView.findViewById(R.id.backBtn)
            videoTitle = playerView.findViewById(R.id.videoTitle)
            moreFeaturesButton = playerView.findViewById(R.id.moreFeaturesBtn)
            playPauseButton = playerView.findViewById(R.id.playPauseBtn)
            repeatButton = playerView.findViewById(R.id.repeatBtn)
            prevButton = playerView.findViewById(R.id.prevBtn)
            nextButton = playerView.findViewById(R.id.nextBtn)
            fullScreenButton = playerView.findViewById(R.id.fullScreenBtn)

            // Set initial title from intent
            val channelName = intent.getStringExtra("CHANNEL_NAME")
                ?: url?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: getString(R.string.video_name)

            videoTitle.text = channelName
            videoTitle.isSelected = true

            // Add cast button setup
            mediaRouteButton = playerView.findViewById(R.id.mediaRouteButton)
            CastButtonFactory.setUpMediaRouteButton(this, mediaRouteButton)

            // Move PiP button to controller layout
            // This assumes you have a pipButton in your player control layout
            val pipButton = playerView.findViewById<ImageButton>(R.id.pipModeBtn)
            pipButton?.setOnClickListener {
                enterPictureInPictureMode()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up controller views", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCustomControllerActions() {
        // Back button
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Play/Pause button
        playPauseButton.setOnClickListener {
            when (playbackState) {
                PlaybackState.PLAYING -> pauseVideo()
                PlaybackState.PAUSED, PlaybackState.ENDED -> playVideo()
                PlaybackState.BUFFERING -> {
                    wasPlayingBeforePause = !wasPlayingBeforePause
                    player.playWhenReady = wasPlayingBeforePause
                    updatePlayPauseButton(wasPlayingBeforePause)
                }
                else -> {
                    // Try to start playback for other states
                    playVideo()
                }
            }
        }

        // Previous/Next buttons (10 seconds skip)
        prevButton.setOnClickListener {
            player.seekTo(maxOf(0, player.currentPosition - 10000))
        }

        nextButton.setOnClickListener {
            player.seekTo(minOf(player.duration, player.currentPosition + 10000))
        }

        // Repeat button
        repeatButton.setOnClickListener {
            when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> {
                    player.setRepeatMode(Player.REPEAT_MODE_ONE)
                    repeatButton.setImageResource(R.drawable.repeat_one_icon)
                }
                Player.REPEAT_MODE_ONE -> {
                    player.setRepeatMode(Player.REPEAT_MODE_ALL)
                    repeatButton.setImageResource(R.drawable.repeat_all_icon)
                }
                else -> {
                    player.setRepeatMode(Player.REPEAT_MODE_OFF)
                    repeatButton.setImageResource(R.drawable.repeat_off_icon)
                }
            }
        }

        // Fullscreen button
        fullScreenButton.setOnClickListener {
            if (!isFullscreen) {
                isFullscreen = true
                playInFullscreen(enable = true)
            } else {
                // Cycle through modes when already fullscreen
                playInFullscreen(enable = true)
            }
        }

        // More Features button
        moreFeaturesButton.setOnClickListener {
            pauseVideo()
            showMoreFeaturesDialog()
        }

        // Lock button
        binding.lockButton.setOnClickListener {
            isLocked = !isLocked
            lockScreen(isLocked)
            binding.lockButton.setImageResource(
                if (isLocked) R.drawable.close_lock_icon
                else R.drawable.lock_open_icon
            )
        }

        // Add PiP button handler if it exists in the layout
        playerView.findViewById<ImageButton>(R.id.pipModeBtn)?.setOnClickListener {
            enterPictureInPictureMode()
        }
    }

    // Update the playVideo() method
    private fun playVideo() {
        if (!isPlayerReady) return

        try {
            when (playbackState) {
                PlaybackState.PAUSED, PlaybackState.ENDED -> {
                    player.play()
                    playbackState = PlaybackState.PLAYING
                    isPlaying = true
                    updatePlayPauseButton(true)
                }
                PlaybackState.BUFFERING -> {
                    wasPlayingBeforePause = true
                    player.playWhenReady = true
                    updatePlayPauseButton(true)
                }
                PlaybackState.IDLE -> {
                    // Try to restart playback if in IDLE state
                    player.prepare()
                    player.play()
                    updatePlayPauseButton(true)
                }
                else -> {
                    // Do nothing for other states
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing video: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Update the pauseVideo() method
    private fun pauseVideo() {
        try {
            when (playbackState) {
                PlaybackState.PLAYING, PlaybackState.BUFFERING -> {
                    player.pause()
                    playbackState = PlaybackState.PAUSED
                    isPlaying = false
                    updatePlayPauseButton(false)
                    wasPlayingBeforePause = false
                }
                else -> {
                    // Do nothing for other states
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error pausing video: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Update the playInFullscreen function
    private fun playInFullscreen(enable: Boolean) {
        if (enable) {
            when (currentScreenMode) {
                ScreenMode.FIT -> {
                    // Default fit mode
                    binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    fullScreenButton.setImageResource(R.drawable.fullscreen_exit_icon)
                    currentScreenMode = ScreenMode.FILL
                }
                ScreenMode.FILL -> {
                    // Stretch to fill
                    binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    fullScreenButton.setImageResource(R.drawable.fullscreen_exit_icon)
                    currentScreenMode = ScreenMode.ZOOM

                    // Enable notch mode when in FILL mode
                    if (!isNotchModeEnabled) {
                        toggleNotchMode()
                    }
                }
                ScreenMode.ZOOM -> {
                    // Zoom and crop
                    binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    fullScreenButton.setImageResource(R.drawable.fullscreen_exit_icon)
                    currentScreenMode = ScreenMode.FIT
                }
            }
        } else {
            // Reset to default fit mode
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            fullScreenButton.setImageResource(R.drawable.fullscreen_icon)
            currentScreenMode = ScreenMode.FIT

            // Disable notch mode
            if (isNotchModeEnabled) {
                toggleNotchMode()
            }
        }
    }

    private fun showSpeedDialog() {
        val dialogView = layoutInflater.inflate(R.layout.speed_dialog, null)
        val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
            .setView(dialogView)
            .create()

        var currentSpeed = player.playbackParameters.speed
        val speedText = dialogView.findViewById<TextView>(R.id.speedText)
        speedText.text = String.format("%.1fx", currentSpeed)

        dialogView.findViewById<ImageButton>(R.id.minusBtn).setOnClickListener {
            if (currentSpeed > 0.25f) {
                currentSpeed -= 0.25f
                speedText.text = String.format("%.1fx", currentSpeed)
                player.setPlaybackSpeed(currentSpeed)
            }
        }

        dialogView.findViewById<ImageButton>(R.id.plusBtn).setOnClickListener {
            if (currentSpeed < 3.0f) {
                currentSpeed += 0.25f
                speedText.text = String.format("%.1fx", currentSpeed)
                player.setPlaybackSpeed(currentSpeed)
            }
        }

        dialog.show()
    }

    private fun showQualityDialog() {
        val qualities = getAvailableQualities()
        val qualityItems = buildQualityItems(qualities)
        val currentIndex = (qualityItems.indexOfFirst { it.contains(currentQuality) }).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this, R.style.QualityDialogStyle)
            .setTitle(getString(R.string.select_quality))
            .setSingleChoiceItems(qualityItems.toTypedArray(), currentIndex) { dialog, which ->
                val selectedQuality = if (which == 0) "Auto" else qualities[which - 1].label
                isManualQualityControl = selectedQuality != "Auto"
                applyQuality(selectedQuality, qualities)
                dialog.dismiss()

                Toast.makeText(
                    this,
                    if (selectedQuality == "Auto") {
                        getString(R.string.auto_quality_enabled)
                    } else {
                        getString(R.string.quality_changed, selectedQuality)
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun buildQualityItems(qualities: List<VideoQuality>): List<String> {
        val items = mutableListOf("Auto (Recommended)")

        qualities.forEach { quality ->
            val currentFormat = player.videoFormat
            val isCurrent = when {
                currentFormat == null -> false
                !isManualQualityControl -> currentFormat.height == quality.height
                else -> currentQuality == quality.label
            }

            val qualityText = buildString {
                append(quality.label)
                append(" - ")
                append(quality.description)
                if (isCurrent) append(" ✓")
            }
            items.add(qualityText)
        }

        return items
    }

    private fun getAvailableQualities(): List<VideoQuality> {
        val tracks = mutableListOf<VideoQuality>()

        try {
            // Get current tracks using Media3 API
            val trackGroups = player.currentTracks.groups

            for (group in trackGroups) {
                // Only look at video tracks
                if (group.type == C.TRACK_TYPE_VIDEO) {
                    for (i in 0 until group.length) {
                        val trackFormat = group.getTrackFormat(i)

                        if (trackFormat.height > 0 && trackFormat.width > 0) {
                            availableQualities.find {
                                it.height == trackFormat.height
                            }?.let { tracks.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tracks.distinct().sortedByDescending { it.height }
    }

    private fun applyQuality(quality: String, availableTracks: List<VideoQuality>) {
        val parameters = trackSelector.buildUponParameters()

        when (quality) {
            "Auto" -> {
                parameters.clearVideoSizeConstraints()
                    .setForceHighestSupportedBitrate(false)
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
            }
            else -> {
                availableTracks.find { it.label == quality }?.let { track ->
                    parameters.setMaxVideoSize(track.width, track.height)
                        .setMinVideoSize(track.width/2, track.height/2)
                        .setMaxVideoBitrate(track.bitrate)
                        .setMinVideoBitrate(track.bitrate/2)
                        .setForceHighestSupportedBitrate(true)
                        .setAllowVideoMixedMimeTypeAdaptiveness(false)
                }
            }
        }

        try {
            val position = player.currentPosition
            val wasPlaying = player.isPlaying

            trackSelector.setParameters(parameters)
            currentQuality = quality

            // Save preferences
            getSharedPreferences("player_settings", Context.MODE_PRIVATE).edit().apply {
                putString("preferred_quality", quality)
                putBoolean("manual_quality_control", isManualQualityControl)
                apply()
            }

            // Restore playback state
            player.seekTo(position)
            player.playWhenReady = wasPlaying

        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.quality_change_failed, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun initializeQuality() {
        val prefs = getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        val savedQuality = prefs.getString("preferred_quality", "Auto") ?: "Auto"
        isManualQualityControl = prefs.getBoolean("manual_quality_control", false)

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    getAvailableQualities().let { tracks ->
                        if (tracks.isNotEmpty()) {
                            // If manual control is off, use Auto
                            val qualityToApply = if (isManualQualityControl) savedQuality else "Auto"
                            applyQuality(qualityToApply, tracks)
                            player.removeListener(this)
                        }
                    }
                }
            }
        })
    }

    private fun getCurrentQualityInfo(): String {
        val currentTrack = player.videoFormat
        return when {
            currentTrack == null -> "Unknown"
            !isManualQualityControl -> "Auto (${currentTrack.height}p)"
            else -> currentQuality
        }
    }

    // Enhance setupPlayer for better format support
    private fun setupPlayer() {
        try {
            if (::player.isInitialized) {
                player.release()
            }

            // Create Media3 track selector with improved parameters
            trackSelector = DefaultTrackSelector(this).apply {
                // Use more flexible parameters for streaming
                setParameters(buildUponParameters()
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setSelectUndeterminedTextLanguage(true)
                    .setTunnelingEnabled(true) // Enable tunneling for better performance
                    .setExceedRendererCapabilitiesIfNecessary(true) // Try to play content even if not fully supported
                    .setPreferredAudioLanguage("en") // Default to English audio
                )
            }

            // Get saved buffer settings or use defaults
            val prefs = getSharedPreferences("player_settings", Context.MODE_PRIVATE)
            val minBuffer = prefs.getInt("buffer_min_ms", 15000)  // Increased default
            val maxBuffer = prefs.getInt("buffer_max_ms", 60000)  // Increased default
            val playbackBuffer = prefs.getInt("playback_buffer_ms", 2500)  // Increased default
            val rebufferMs = prefs.getInt("rebuffer_ms", 5000)  // Increased default

            // Create Media3 player with enhanced buffer configuration
            player = ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            minBuffer,
                            maxBuffer,
                            playbackBuffer,
                            rebufferMs
                        )
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .build()
                )
                .setRenderersFactory(
                    DefaultRenderersFactory(this)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                )
                .build()

            playerView.player = player

            // Detect stream type from URL for better configuration
            val streamType = detectStreamType(url)
            
            // Create enhanced data source factory with improved headers
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
                .setDefaultRequestProperties(
                    when (streamType) {
                        StreamType.HOTSTAR -> createHotstarStreamingHeaders()
                        StreamType.RTMP -> createRtmpHeaders()
                        StreamType.DASH -> createDashHeaders()
                        StreamType.HLS -> createHlsHeaders()
                        else -> createStreamingHeaders()
                    }
                )

            // Create media item with enhanced configuration
            val mediaItem = createEnhancedMediaItem(url ?: return)

            // Create media source based on URL type with improved detection
            val mediaSource = createMediaSourceForUrl(url, mediaItem, dataSourceFactory)

            player.setMediaSource(mediaSource)
            player.seekTo(playbackPosition)
            player.playWhenReady = true
            player.prepare()

            // Configure player for specific stream types
            when (streamType) {
                StreamType.HOTSTAR -> configureHotstarPlayback()
                StreamType.RTMP -> configureRtmpPlayback()
                StreamType.DASH -> configureDashPlayback()
                StreamType.HLS -> configureHlsPlayback()
                StreamType.LIVE -> configureLivePlayback()
                StreamType.PROGRESSIVE -> configureProgressivePlayback()
            }

            // Set up player listeners with enhanced error handling
            setupEnhancedPlayerListeners()

        } catch (e: Exception) {
            handleSetupError(e)
        }
    }

    // Add this enum for stream types
    private enum class StreamType {
        HOTSTAR, RTMP, DASH, HLS, LIVE, PROGRESSIVE
    }

    // Add this method to detect stream type from URL
    private fun detectStreamType(url: String?): StreamType {
        if (url == null) return StreamType.PROGRESSIVE
        
        return when {
            isHotstarStyleStream(url) -> StreamType.HOTSTAR
            url.startsWith("rtmp://", ignoreCase = true) -> StreamType.RTMP
            url.contains(".mpd", ignoreCase = true) || 
                url.contains("/dash/", ignoreCase = true) -> StreamType.DASH
            url.contains(".m3u8", ignoreCase = true) || 
                url.contains("/hls/", ignoreCase = true) -> StreamType.HLS
            url.contains("/live/", ignoreCase = true) || 
                url.contains("stream", ignoreCase = true) -> StreamType.LIVE
            else -> StreamType.PROGRESSIVE
        }
    }

    // Add these methods for format-specific headers
    private fun createRtmpHeaders(): Map<String, String> {
        return mapOf(
            "Accept" to "*/*",
            "Connection" to "keep-alive"
        )
    }

    private fun createDashHeaders(): Map<String, String> {
        val host = Uri.parse(url)?.host ?: ""
        return mapOf(
            "Accept" to "application/dash+xml,video/*,*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Origin" to "https://$host",
            "Referer" to "https://$host",
            "Connection" to "keep-alive"
        )
    }

    private fun createHlsHeaders(): Map<String, String> {
        val host = Uri.parse(url)?.host ?: ""
        return mapOf(
            "Accept" to "application/vnd.apple.mpegurl,application/x-mpegURL,*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Origin" to "https://$host",
            "Referer" to "https://$host",
            "Connection" to "keep-alive"
        )
    }

    // Add these configuration methods
    private fun configureDashPlayback() {
        try {
            // DASH-specific configuration
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            
            // Configure track selection for DASH
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setForceHighestSupportedBitrate(false) // Allow adaptive bitrate
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
            )
            
            Log.d("PlayerActivity", "Configured player for DASH stream")
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error configuring DASH playback: ${e.message}")
        }
    }

    private fun configureHlsPlayback() {
        try {
            // HLS-specific configuration
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            
            // Configure track selection for HLS
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setForceHighestSupportedBitrate(false) // Allow adaptive bitrate
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
            )
            
            // Check if this is likely a live stream
            if (url?.contains("/live/", ignoreCase = true) == true || 
                url?.contains("stream", ignoreCase = true) == true) {
                isLiveStream = true
                configureLivePlayback()
            }
            
            Log.d("PlayerActivity", "Configured player for HLS stream")
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error configuring HLS playback: ${e.message}")
        }
    }

    private fun configureProgressivePlayback() {
        try {
            // Progressive-specific configuration
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            
            // Configure track selection for progressive playback
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setForceHighestSupportedBitrate(true) // Use highest quality for progressive
                    .setAllowVideoMixedMimeTypeAdaptiveness(false)
            )
            
            Log.d("PlayerActivity", "Configured player for progressive playback")
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error configuring progressive playback: ${e.message}")
        }
    }

    // Create better streaming headers based on URL
    private fun createStreamingHeaders(): Map<String, String> {
        val host = Uri.parse(url)?.host ?: ""
        val referer = "https://$host"

        return mapOf(
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Origin" to referer,
            "Referer" to referer,
            "Connection" to "keep-alive",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "cross-site",
            // Add headers that help with certain streaming services
            "X-Requested-With" to "com.samyak.urlplayer",
            "Pragma" to "no-cache",
            "Cache-Control" to "no-cache",
            // Add additional headers for better compatibility
            "DNT" to "1",
            "Upgrade-Insecure-Requests" to "1",
            "Accept-Encoding" to "gzip, deflate, br",
            // Add range support for better streaming
            "Range" to "bytes=0-"
        )
    }

    // Create enhanced media item with better configuration
    private fun createEnhancedMediaItem(url: String): MediaItem {
        return MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMimeType(getMimeType(url))
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .setMaxOffsetMs(30000) // 30 seconds max offset
                    .setTargetOffsetMs(5000) // Target 5 seconds behind live
                    .setMinOffsetMs(500) // Minimum 500ms behind live
                    .setMinPlaybackSpeed(0.97f)
                    .build()
            )
            .build()
    }

    // Improve createMediaSourceForUrl for better format handling
    private fun createMediaSourceForUrl(url: String?, mediaItem: MediaItem, dataSourceFactory: DefaultHttpDataSource.Factory): MediaSource {
        if (url == null) {
            Log.e("PlayerActivity", "URL is null when creating media source")
            return DefaultMediaSourceFactory(dataSourceFactory)
                .setLoadErrorHandlingPolicy(createEnhancedErrorPolicy())
                .createMediaSource(mediaItem)
        }

        Log.d("PlayerActivity", "Creating media source for URL: $url")

        return try {
            when {
                // RTMP streams
                url.startsWith("rtmp://", ignoreCase = true) ||
                        url.startsWith("rtmps://", ignoreCase = true) -> {
                    Log.d("PlayerActivity", "Detected RTMP stream: $url")
                    isLiveStream = true

                    val rtmpDataSourceFactory = RtmpDataSource.Factory()
                    DefaultMediaSourceFactory(rtmpDataSourceFactory)
                        .setLoadErrorHandlingPolicy(createEnhancedErrorPolicy())
                        .createMediaSource(mediaItem)
                }

                // HLS streams (m3u8)
                url.contains(".m3u8", ignoreCase = true) ||
                        url.contains("/hls/", ignoreCase = true) ||
                        url.contains("/live/", ignoreCase = true) ||
                        url.contains("/master", ignoreCase = true) ||
                        url.contains("/playlist", ignoreCase = true) -> {
                    Log.d("PlayerActivity", "Detected HLS stream: $url")
                    isLiveStream = true

                    HlsMediaSource.Factory(dataSourceFactory)
                        .setAllowChunklessPreparation(true)
                        .setLoadErrorHandlingPolicy(createEnhancedErrorPolicy())
                        .createMediaSource(mediaItem)
                }

                // DASH streams (mpd)
                url.endsWith(".mpd", ignoreCase = true) ||
                        url.contains("dash", ignoreCase = true) ||
                        url.contains("/manifest", ignoreCase = true) -> {
                    Log.d("PlayerActivity", "Detected DASH stream: $url")
                    
                    DashMediaSource.Factory(dataSourceFactory)
                        .setLoadErrorHandlingPolicy(createEnhancedErrorPolicy())
                        .createMediaSource(mediaItem)
                }

                // Smooth Streaming (ism/isml)
                url.contains(".ism", ignoreCase = true) ||
                        url.contains(".isml", ignoreCase = true) ||
                        url.contains("smooth", ignoreCase = true) ||
                        url.contains("mss", ignoreCase = true) -> {
                    Log.d("PlayerActivity", "Detected Smooth Streaming: $url")
                    
                    // For Smooth Streaming, use the appropriate factory
                    // Note: You need to add the SmoothStreaming extension dependency
                    try {
                        val smoothStreamingFactory = Class.forName("androidx.media3.exoplayer.smoothstreaming.SsMediaSource\$Factory")
                            .getConstructor(Class.forName("androidx.media3.datasource.DataSource\$Factory"))
                            .newInstance(dataSourceFactory)
                        
                        val createMediaSourceMethod = smoothStreamingFactory.javaClass
                            .getMethod("createMediaSource", MediaItem::class.java)
                        
                        createMediaSourceMethod.invoke(smoothStreamingFactory, mediaItem) as MediaSource
                    } catch (e: Exception) {
                        Log.e("PlayerActivity", "Smooth Streaming not supported, falling back to default: ${e.message}")
                        // Fall back to default if SmoothStreaming extension is not available
                        DefaultMediaSourceFactory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(createEnhancedErrorPolicy())
                            .createMediaSource(mediaItem)
                    }
                }

                // Transport streams (.ts)
                url.endsWith(".ts", ignoreCase = true) ||
                        url.endsWith(".mts", ignoreCase = true) ||
                        url.endsWith(".m2ts", ignoreCase = true) -> {
                    Log.d("PlayerActivity", "Detected Transport Stream: $url")
                    
                    // For TS files, use progressive media source with appropriate MIME type
                    val tsMediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(url))
                        .setMimeType("video/mp2t")
                        .build()
                    
                    DefaultMediaSourceFactory(dataSourceFactory)
                        .setLoadErrorHandlingPolicy(createEnhancedErrorPolicy())
                        .createMediaSource(tsMediaItem)
                }

                // Progressive playback for everything else
                else -> {
                    Log.d("PlayerActivity", "Using default media source for: $url")
                    DefaultMediaSourceFactory(dataSourceFactory)
                        .setLoadErrorHandlingPolicy(createEnhancedErrorPolicy())
                        .createMediaSource(mediaItem)
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error creating media source: ${e.message}")
            // Fallback to default media source factory
            DefaultMediaSourceFactory(dataSourceFactory)
                .setLoadErrorHandlingPolicy(createEnhancedErrorPolicy())
                .createMediaSource(mediaItem)
        }
    }

    // Configure player based on detected stream type
    private fun configurePlayerForStreamType() {
        if (isLiveStream) {
            configureLivePlayback()

            // Additional configuration for specific stream types
            when {
                isHotstarStyleStream(url) -> configureHotstarPlayback()
                url?.startsWith("rtmp://", ignoreCase = true) == true -> configureRtmpPlayback()
                isAkamaizedStream(url) -> configureAkamaizedPlayback()
            }
        }
    }

    // Add a new method for RTMP-specific configuration
    private fun configureRtmpPlayback() {
        try {
            // RTMP streams often need different buffering parameters
            player.setVideoSurfaceView(playerView.videoSurfaceView as SurfaceView?)

            // Use lower latency settings for RTMP
            playerView.controllerShowTimeoutMs = 3000

            // Configure track selection for RTMP
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setForceHighestSupportedBitrate(false) // Don't force highest quality for RTMP
                    .setMaxVideoBitrate(3_000_000) // Limit to 3Mbps for stability
            )

            Log.d("PlayerActivity", "Configured player for RTMP stream")
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error configuring RTMP playback: ${e.message}")
        }
    }

    // Add a method for Akamaized streams (often used by Hotstar)
    private fun configureAkamaizedPlayback() {
        try {
            // Akamaized streams often need special handling
            player.setVideoSurfaceView(playerView.videoSurfaceView as SurfaceView?)

            // Configure for higher quality
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setForceHighestSupportedBitrate(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
            )

            Log.d("PlayerActivity", "Configured player for Akamaized stream")
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error configuring Akamaized playback: ${e.message}")
        }
    }

    // Add this method to create an enhanced error handling policy
    private fun createEnhancedErrorPolicy(): DefaultLoadErrorHandlingPolicy {
        return object : DefaultLoadErrorHandlingPolicy(/* minLoadRetryCount= */ 8) {
            override fun getRetryDelayMsFor(
                loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
            ): Long {
                // Use exponential backoff with a base of 1000ms
                val baseDelay = 1000L
                val maxDelay = 15000L // 15 seconds max
                val errorCount = loadErrorInfo.errorCount

                // Calculate exponential backoff with jitter
                val exponentialDelay = minOf(
                    maxDelay,
                    baseDelay * (1L shl (errorCount - 1)) + (Math.random() * 1000).toLong()
                )

                // Log detailed error information for debugging
                Log.d("PlayerActivity", "Retry #$errorCount for error: ${loadErrorInfo.exception.message}, delay: $exponentialDelay ms")
                
                // For streaming errors, use shorter delays to recover faster
                return when {
                    // For network errors, use the calculated delay
                    loadErrorInfo.exception is java.net.SocketTimeoutException ||
                    loadErrorInfo.exception is java.io.IOException -> exponentialDelay
                    
                    // For parsing errors in streaming formats, retry faster
                    loadErrorInfo.exception.message?.contains("format", ignoreCase = true) == true ||
                    loadErrorInfo.exception.message?.contains("parse", ignoreCase = true) == true -> 
                        exponentialDelay / 2
                    
                    // Default to calculated delay
                    else -> exponentialDelay
                }
            }

            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                // Increase retry count for live streams and specific data types
                return when {
                    isLiveStream -> 10 // More retries for live content
                    dataType == C.DATA_TYPE_MEDIA -> 8 // More retries for media data
                    dataType == C.DATA_TYPE_MANIFEST -> 6 // More retries for manifests
                    else -> 5 // Default retry count
                }
            }
        }
    }



    // Add this method to try alternative RTMP playback approach
    private fun tryAlternativeRtmpPlayback() {
        try {
            // For RTMP streams, try with a different configuration
            if (url?.startsWith("rtmp://", ignoreCase = true) == true) {
                // Create a new media item with additional configuration
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(url))
                    .setLiveConfiguration(
                        MediaItem.LiveConfiguration.Builder()
                            .setMaxPlaybackSpeed(1.02f) // Slightly faster to avoid buffering
                            .setMaxOffsetMs(5000) // 5 seconds max offset
                            .setTargetOffsetMs(3000) // Target 3 seconds behind live
                            .setMinOffsetMs(500) // Minimum 500ms behind live
                            .setMinPlaybackSpeed(0.97f) // Allow slightly slower playback
                            .build()
                    )
                    .build()

                // Use the Media3 RTMP data source factory
                val rtmpDataSourceFactory = RtmpDataSource.Factory()
                val mediaSource = DefaultMediaSourceFactory(rtmpDataSourceFactory)
                    .createMediaSource(mediaItem)

                // Reset player and try again
                player.stop()
                player.clearMediaItems()
                player.setMediaSource(mediaSource)
                player.prepare()
                player.play()

                // Hide error views
                errorTextView.visibility = View.GONE
                progressBar.visibility = View.VISIBLE

                // Update UI
                Toast.makeText(this, "Trying alternative playback method...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Alternative RTMP playback failed: ${e.message}")
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        playPauseButton.setImageResource(
            if (isPlaying) R.drawable.pause_icon
            else R.drawable.play_icon
        )
    }

    private fun handlePlaybackEnded() {
        when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> {
                // Just replay current video
                player.seekTo(0)
                playVideo()
            }
            Player.REPEAT_MODE_ALL -> {
                // For single video, treat same as REPEAT_MODE_ONE
                player.seekTo(0)
                playVideo()
            }
            else -> {
                // Just stop at the end
                pauseVideo()
                // Optionally show replay button or end screen
                showPlaybackEndedUI()
            }
        }
    }

    private fun showPlaybackEndedUI() {
        try {
            // Show replay button with fallback to play icon
            playPauseButton.setImageResource(
                try {
                    R.drawable.replay_icon
                } catch (e: Exception) {
                    R.drawable.play_icon // Fallback to play icon
                }
            )

            playPauseButton.setOnClickListener {
                player.seekTo(0)
                playVideo()
                // Restore normal play/pause listener
                setupCustomControllerActions()
            }
        } catch (e: Exception) {
            // If anything fails, just show play icon
            playPauseButton.setImageResource(R.drawable.play_icon)
        }
    }

    private fun updateQualityInfo() {
        try {
            val videoFormat = player.videoFormat
            if (videoFormat != null) {
                val width = videoFormat.width
                val height = videoFormat.height
                val bitrate = videoFormat.bitrate / 1000 // Convert to kbps

                Log.d("PlayerActivity", "Current video quality: ${width}x${height} @ ${bitrate}kbps")

                // You could update a quality indicator in the UI here
            }
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error updating quality info: ${e.message}")
        }
    }

    private fun lockScreen(lock: Boolean) {
        linearLayoutControlUp.visibility = if (lock) View.INVISIBLE else View.VISIBLE
        linearLayoutControlBottom.visibility = if (lock) View.INVISIBLE else View.VISIBLE
        playerView.useController = !lock
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Always force landscape mode
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        // Reapply edge-to-edge settings
        setupEdgeToEdgeDisplay()

        // Reset screen dimensions and recalculate subtitle size
        screenWidth = 0
        screenHeight = 0
        applySubtitleStyle()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        playbackPosition = player.currentPosition
        outState.putLong("playbackPosition", playbackPosition)
        outState.putString("URL", url)
        outState.putString("USER_AGENT", userAgent)
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            player.playWhenReady = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || !isPlayerReady) {
            player.playWhenReady = true
        }
        if (audioManager == null) {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        audioManager?.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (brightness != 0) setScreenBrightness(brightness)

        // Auto-play after returning from ad if we were showing an ad
        if (isShowingAd) {
            isShowingAd = false
            if (isPlayerReady) {
                playVideo()
            }
        } else if (isPlaying) {
            playVideo()
        }

        if (::sessionManager.isInitialized) {
            sessionManager.addSessionManagerListener(castSessionManagerListener, CastSession::class.java)
        }

        // If you're restoring repeat mode, use setRepeatMode
        val savedRepeatMode = getSharedPreferences("player_settings", Context.MODE_PRIVATE)
            .getInt("repeat_mode", Player.REPEAT_MODE_OFF)
        player.setRepeatMode(savedRepeatMode)

        // Update repeat button icon based on current mode
        updateRepeatButtonIcon(player.repeatMode)
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            playbackPosition = player.currentPosition
            player.playWhenReady = false
        }
        if (isPlaying) {
            pauseVideo()
        }
        if (::sessionManager.isInitialized) {
            sessionManager.removeSessionManagerListener(castSessionManagerListener, CastSession::class.java)
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            playbackPosition = player.currentPosition
            player.playWhenReady = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        audioManager?.abandonAudioFocus(null)
        try {
            if (::loudnessEnhancer.isInitialized) {
                loudnessEnhancer.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Just finish the activity when back is pressed
        finish()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (isInPictureInPictureMode) {
            // Hide all UI controls when in PiP mode
            binding.playerView.hideController()
            binding.lockButton.visibility = View.GONE
            binding.brightnessIcon.visibility = View.GONE
            binding.volumeIcon.visibility = View.GONE

            // Disable controller completely to hide all UI elements
            playerView.useController = false

            // Ensure video is playing when entering PiP
            if (isPlayerReady && !isPlaying) {
                playVideo()
            }
        } else {
            // Reset PiP flag when exiting PiP mode
            isPipRequested = false

            // Show controls when exiting PiP mode
            binding.lockButton.visibility = View.VISIBLE
            playerView.useController = true

            // Force controller to update
            playerView.showController()

            // Restore previous screen mode and notch settings
            if (prePipScreenMode != currentScreenMode) {
                // Apply the saved screen mode
                when (prePipScreenMode) {
                    ScreenMode.FIT -> {
                        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    }
                    ScreenMode.FILL -> {
                        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    }
                    ScreenMode.ZOOM -> {
                        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    }
                }
                currentScreenMode = prePipScreenMode

                // Update fullscreen button icon
                fullScreenButton.setImageResource(R.drawable.fullscreen_exit_icon)
            }

            // Restore notch mode if needed
            if (prePipNotchEnabled != isNotchModeEnabled) {
                toggleNotchMode()
            }

            // Handle navigation based on pipStatus
            if (pipStatus != 0) {
                finish()
                val intent = Intent(this, PlayerActivity::class.java)
                when (pipStatus) {
                    1 -> intent.putExtra("class", "FolderActivity")
                    2 -> intent.putExtra("class", "SearchedVideos")
                    3 -> intent.putExtra("class", "AllVideos")
                }
                startActivity(intent)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        // Only enter PiP mode if player is initialized and we're not already in PiP mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            ::player.isInitialized &&
            !isInPictureInPictureMode &&
            isPlayerReady) {

            // Set flag to prevent ads when PiP is requested
            isPipRequested = true

            try {
                // Create PiP params with aspect ratio based on video dimensions
                val videoRatio = if (player.videoFormat != null) {
                    Rational(player.videoFormat!!.width, player.videoFormat!!.height)
                } else {
                    Rational(16, 9) // Default aspect ratio
                }

                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(videoRatio)
                    .build()

                super.enterPictureInPictureMode(params)

                // Hide controls when entering PiP
                binding.playerView.hideController()
                binding.lockButton.visibility = View.GONE
            } catch (e: Exception) {
                Log.e("PlayerActivity", "Failed to enter PiP mode: ${e.message}")
                isPipRequested = false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureControls() {
        binding.playerView.player = player

        // Setup YouTube style overlay


        // Handle touch events
        binding.playerView.setOnTouchListener { _, motionEvent ->
            // Don't process touch events when in PiP mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
                return@setOnTouchListener false
            }

            if (!isLocked) {
                gestureDetectorCompat.onTouchEvent(motionEvent)

                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    binding.brightnessIcon.visibility = View.GONE
                    binding.volumeIcon.visibility = View.GONE

                    // For immersive mode
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    WindowInsetsControllerCompat(window, binding.root).let { controller ->
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
            false
        }
    }

    override fun onScroll(
        e1: MotionEvent?,
        event: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (isLocked) return false

        minSwipeY += distanceY

        val sWidth = Resources.getSystem().displayMetrics.widthPixels
        val sHeight = Resources.getSystem().displayMetrics.heightPixels

        val border = 100 * Resources.getSystem().displayMetrics.density.toInt()
        if (event.x < border || event.y < border ||
            event.x > sWidth - border || event.y > sHeight - border)
            return false

        if (abs(distanceX) < abs(distanceY) && abs(minSwipeY) > 50) {
            if (event.x < sWidth / 2) {
                // Brightness control
                binding.brightnessIcon.visibility = View.VISIBLE
                binding.volumeIcon.visibility = View.GONE
                val increase = distanceY > 0
                val newValue = if (increase) brightness + 1 else brightness - 1
                if (newValue in 0..30) brightness = newValue
                binding.brightnessIcon.text = brightness.toString()
                setScreenBrightness(brightness)
            } else {
                // Volume control
                binding.brightnessIcon.visibility = View.GONE
                binding.volumeIcon.visibility = View.VISIBLE
                val maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val increase = distanceY > 0
                val newValue = if (increase) volume + 1 else volume - 1
                if (newValue in 0..maxVolume) volume = newValue
                binding.volumeIcon.text = volume.toString()
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            }
            minSwipeY = 0f
        }
        return true
    }

    private fun setScreenBrightness(value: Int) {
        val d = 1.0f / 30
        val lp = window.attributes
        lp.screenBrightness = d * value
        window.attributes = lp
    }

    // Add other required GestureDetector.OnGestureListener methods
    override fun onDown(e: MotionEvent) = false
    override fun onShowPress(e: MotionEvent) = Unit
    override fun onSingleTapUp(e: MotionEvent) = false
    override fun onLongPress(e: MotionEvent) = Unit
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float) = false

    private fun setupAudioBooster() {
        try {
            // Create new LoudnessEnhancer with player's audio session
            loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)

            // Restore saved settings
            val prefs = getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
            boostLevel = prefs.getInt("boost_level", 0)
            isBoostEnabled = prefs.getBoolean("boost_enabled", false)

            // Apply saved settings
            loudnessEnhancer.enabled = isBoostEnabled
            if (isBoostEnabled && boostLevel > 0) {
                loudnessEnhancer.setTargetGain(boostLevel * 100) // Convert to millibels
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing audio booster", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showAudioBoosterDialog() {
        val customDialogB = LayoutInflater.from(this)
            .inflate(R.layout.booster, binding.root, false)
        val bindingB = BoosterBinding.bind(customDialogB)

        // Set initial values
        bindingB.verticalBar.apply {
            progress = boostLevel
            // The max value should be set in XML via app:vsb_max_value="15"
        }

        val dialogB = MaterialAlertDialogBuilder(this)
            .setView(customDialogB)
            .setTitle("Audio Boost")
            .setOnCancelListener { playVideo() }
            .setPositiveButton("Apply") { self, _ ->
                try {
                    // Update boost level
                    boostLevel = bindingB.verticalBar.progress
                    isBoostEnabled = boostLevel > 0

                    // Apply settings
                    loudnessEnhancer.enabled = isBoostEnabled
                    loudnessEnhancer.setTargetGain(boostLevel * 100)

                    // Save settings
                    getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("boost_level", boostLevel)
                        .putBoolean("boost_enabled", isBoostEnabled)
                        .apply()

                    // Show feedback
                    val message = if (isBoostEnabled)
                        "Audio boost set to ${boostLevel * 10}%"
                    else
                        "Audio boost disabled"
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    Toast.makeText(this, "Error setting audio boost", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
                playVideo()
                self.dismiss()
            }
            .setNegativeButton("Reset") { _, _ ->
                try {
                    // Reset all settings
                    boostLevel = 0
                    isBoostEnabled = false
                    bindingB.verticalBar.progress = 0
                    loudnessEnhancer.enabled = false
                    loudnessEnhancer.setTargetGain(0)

                    // Save reset state
                    getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("boost_level", 0)
                        .putBoolean("boost_enabled", false)
                        .apply()

                    Snackbar.make(binding.root, "Audio boost reset", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error resetting audio boost", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
            .create()

        // Update progress text function
        fun updateProgressText(progress: Int) {
            val percentage = progress * 10
            bindingB.progressText.text = if (progress > 0) {
                "Audio Boost\n\n+${percentage}%"
            } else {
                "Audio Boost\n\nOff"
            }
        }

        updateProgressText(boostLevel)

        // Update progress text while sliding
        bindingB.verticalBar.setOnProgressChangeListener { progress ->
            updateProgressText(progress)
        }

        dialogB.show()
    }

    private fun loadRemoteMedia(position: Long = 0) {
        val castSession = castSession ?: return
        val remoteMediaClient = castSession.remoteMediaClient ?: return

        try {
            // Create media metadata
            val videoMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
            val title = intent.getStringExtra("CHANNEL_NAME") ?: getString(R.string.video_name)
            videoMetadata.putString(MediaMetadata.KEY_TITLE, title)

            // Get correct MIME type and stream type
            val mimeType = getMimeType(url)
            val streamType = when {
                // HLS streams
                url?.contains(".m3u8", ignoreCase = true) == true ||
                        url?.contains(".m3u", ignoreCase = true) == true ||
                        url?.contains("live", ignoreCase = true) == true ||
                        url?.contains("stream", ignoreCase = true) == true ->
                    MediaInfo.STREAM_TYPE_LIVE

                // DASH streams
                url?.contains("dash", ignoreCase = true) == true ||
                        mimeType == "application/dash+xml" ->
                    MediaInfo.STREAM_TYPE_BUFFERED

                // Progressive streams (MP4, WebM etc)
                mimeType.startsWith("video/") ->
                    MediaInfo.STREAM_TYPE_BUFFERED

                // Default to buffered
                else -> MediaInfo.STREAM_TYPE_BUFFERED
            }

            // Create media info with proper content type and stream type
            val mediaInfo = MediaInfo.Builder(url ?: return)
                .setStreamType(streamType)
                .setContentType(mimeType)
                .setMetadata(videoMetadata)
                .apply {
                    // Only set duration for buffered streams
                    if (streamType == MediaInfo.STREAM_TYPE_BUFFERED) {
                        setStreamDuration(player.duration)
                    }
                }
                .build()

            // Load media with options
            val loadRequestData = MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .apply {
                    // Only set position for buffered streams
                    if (streamType == MediaInfo.STREAM_TYPE_BUFFERED) {
                        setCurrentTime(position)
                    }
                }
                .build()

            // Add result listener with enhanced error handling
            remoteMediaClient.load(loadRequestData)
                .addStatusListener { result ->
                    when {
                        result.isSuccess -> {
                            Toast.makeText(this, "Casting started", Toast.LENGTH_SHORT).show()
                            getSharedPreferences("cast_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("is_casting", true)
                                .apply()
                        }
                        result.isInterrupted -> {
                            handleCastError("Casting interrupted")
                        }
                        else -> {
                            val errorMsg = when (result.statusCode) {
                                CastStatusCodes.FAILED -> "Format not supported"
                                CastStatusCodes.INVALID_REQUEST -> "Invalid stream URL"
                                CastStatusCodes.NETWORK_ERROR -> "Network error"
                                CastStatusCodes.APPLICATION_NOT_RUNNING -> "Cast app not running"
                                else -> "Cast error: ${result.statusCode}"
                            }
                            handleCastError(errorMsg)
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            handleCastError("Cast error: ${e.message}")
        }
    }

    private fun handleCastError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        // Fallback to local playback
        castSession?.remoteMediaClient?.stop()
        player.playWhenReady = true
        getSharedPreferences("cast_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_casting", false)
            .apply()
    }

    // Enhance getMimeType function for better format detection
    private fun getMimeType(url: String?): String {
        if (url == null) return "video/mp4"

        return try {
            // First check for streaming formats
            val lowercaseUrl = url.lowercase()
            when {
                // RTMP streams
                lowercaseUrl.startsWith("rtmp://") -> "video/rtmp"
                lowercaseUrl.startsWith("rtmps://") -> "video/rtmps"

                // HLS streams
                lowercaseUrl.endsWith(".m3u8") ||
                        lowercaseUrl.endsWith(".m3u") -> "application/x-mpegURL"

                // DASH streams
                lowercaseUrl.endsWith(".mpd") -> "application/dash+xml"

                // MSS streams
                lowercaseUrl.contains(".ism") || 
                        lowercaseUrl.contains("/Manifest") -> "application/vnd.ms-sstr+xml"

                // Then check file extension
                else -> {
                    val extension = url.substringAfterLast('.', "").lowercase()
                    supportedFormats[extension] ?: when {
                        // Pattern-based detection for URLs without clear extensions
                        url.contains("dash", ignoreCase = true) -> "application/dash+xml"
                        url.contains("hls", ignoreCase = true) -> "application/x-mpegURL"
                        url.contains("smooth", ignoreCase = true) -> "application/vnd.ms-sstr+xml"
                        url.contains("rtmp", ignoreCase = true) -> "video/rtmp"
                        url.contains("/manifest", ignoreCase = true) -> "application/dash+xml"
                        url.contains("/playlist", ignoreCase = true) -> "application/x-mpegURL"
                        url.contains("/master", ignoreCase = true) -> "application/x-mpegURL"
                        url.contains("m3u8", ignoreCase = true) -> "application/x-mpegURL"
                        url.contains(".ts", ignoreCase = true) -> "video/mp2t"
                        url.contains("mp4", ignoreCase = true) -> "video/mp4"
                        // Default to MP4 for unknown types
                        else -> "video/mp4"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error determining MIME type: ${e.message}")
            "video/mp4"  // Default fallback
        }
    }

    // Add this function to calculate optimal subtitle size
    private fun calculateSubtitleSize(): Float {
        // Get screen dimensions if not already set
        if (screenHeight == 0 || screenWidth == 0) {
            val metrics = resources.displayMetrics
            screenHeight = metrics.heightPixels
            screenWidth = metrics.widthPixels
        }

        // Base size calculation on screen width
        // For 1080p width, default size would be 20sp
        val baseSize = 20f
        val baseWidth = 1080f

        // Calculate scaled size based on screen width
        val scaledSize = (screenWidth / baseWidth) * baseSize

        // Clamp the size between min and max values
        return scaledSize.coerceIn(16f, 26f)
    }

    // Add this function to apply subtitle styling
    private fun applySubtitleStyle() {
        try {
            val subtitleSize = calculateSubtitleSize()

            // Create subtitle style
            val style = CaptionStyleCompat(
                Color.WHITE,                      // Text color
                Color.TRANSPARENT,                // Background color
                Color.TRANSPARENT,                // Window color
                CaptionStyleCompat.EDGE_TYPE_OUTLINE, // Edge type
                Color.BLACK,                      // Edge color
                null                             // Default typeface
            )


            // Apply style to player view
            playerView.subtitleView?.setStyle(style)

            // Set text size
            playerView.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize)

            // Center align subtitles and position them slightly above bottom
            playerView.subtitleView?.let { subtitleView ->
                subtitleView.setApplyEmbeddedStyles(true)
                subtitleView.setApplyEmbeddedFontSizes(false)

                // Position subtitles slightly above bottom (90% from top)
                val params = subtitleView.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                params.bottomMargin = (screenHeight * 0.1).toInt() // 10% from bottom
                subtitleView.layoutParams = params
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Update the shouldShowAd method to always return true except in special cases
    private fun shouldShowAd(): Boolean {
        // Don't show ads for premium content, when PiP is requested, or when in PiP mode
        return url?.contains("premium") != true &&
                !isPipRequested &&
                !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)
    }

    // Add this method to set up edge-to-edge display with notch support
    private fun setupEdgeToEdgeDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Enable layout in cutout area
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Make the content draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hide system bars
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Add this method to toggle notch mode
    private fun toggleNotchMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isNotchModeEnabled = !isNotchModeEnabled

            window.attributes.layoutInDisplayCutoutMode = if (isNotchModeEnabled) {
                // Use the entire screen including notch area
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                // Avoid notch area
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }

            // Apply changes
            window.attributes = window.attributes
        }
    }

    // Improve the configureLiveTimeBar method to make the time bar update in real-time
    private fun configureLiveTimeBar() {
        try {
            // Find the time bar from player view
            val timeBar = playerView.findViewById<DefaultTimeBar>(
                androidx.media3.ui.R.id.exo_progress
            )

            // Set scrubbing enabled for live streams with DVR support
            timeBar?.isEnabled = true

            // Set the live playback parameters
            player.setPlaybackParameters(PlaybackParameters(1.0f))

            // Initialize the live stream start time
            liveStreamStartTime = System.currentTimeMillis() - 30000 // Start 30 seconds in the past

            // Set initial duration for the progress bar (30 minutes buffer)
            liveStreamDuration = 30 * 60 * 1000

            // Make the time bar more responsive for live streams
            timeBar?.apply {
                // Set colors for live stream
                setPlayedColor(Color.RED)
                setScrubberColor(Color.RED)
                setBufferedColor(Color.parseColor("#4DFFFFFF")) // Semi-transparent white
            }

            // Start a timer to update the time bar in real-time
            startLiveTimeBarUpdater()

        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error configuring live time bar: ${e.message}")
        }
    }

    // Add this method to continuously update the time bar for live streams
    private fun startLiveTimeBarUpdater() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val updateInterval = 1000L // Update every second

        val runnable = object : Runnable {
            override fun run() {
                if (isLiveStream && isPlayerReady && !isInPictureInPictureMode) {
                    try {
                        // Calculate current position and duration
                        val currentPosition = player.currentPosition
                        val currentTime = System.currentTimeMillis()

                        // For live streams, we need to continuously update the duration
                        // This simulates the time bar moving forward in real-time
                        val elapsedSinceStart = currentTime - liveStreamStartTime
                        val updatedDuration = elapsedSinceStart + 30000 // Keep 30 seconds ahead

                        // Update the live stream duration
                        liveStreamDuration = updatedDuration

                        // Force the player to update its timeline to reflect the new duration
                        // This is a trick to make the time bar update in real-time
                        if (player is ExoPlayer) {
                            val timeline = player.currentTimeline
                            if (!timeline.isEmpty) {
                                val window = Timeline.Window()
                                timeline.getWindow(player.currentMediaItemIndex, window)

                                // Only update if we have a valid window
                                if (window.isDynamic) {
                                    // Update position text and duration text
                                    updateLivePositionAndDuration(currentPosition, updatedDuration)
                                }
                            }
                        }

                        // Update the time bar's position directly if needed
                        val timeBar = playerView.findViewById<DefaultTimeBar>(
                            androidx.media3.ui.R.id.exo_progress
                        )
                        timeBar?.setDuration(updatedDuration)
                        timeBar?.setPosition(currentPosition)

                    } catch (e: Exception) {
                        Log.e("PlayerActivity", "Error updating live time bar: ${e.message}")
                    }
                }

                // Continue updating if player is still active and in live mode
                if (isPlayerReady && isLiveStream && !isDestroyed) {
                    handler.postDelayed(this, updateInterval)
                }
            }
        }

        // Start the updater immediately
        handler.post(runnable)
    }

    // Add this method to update position and duration text views
    private fun updateLivePositionAndDuration(position: Long, duration: Long) {
        try {
            // Get references to text views
            val positionText = playerView.findViewById<TextView>(androidx.media3.ui.R.id.exo_position)
            val durationText = playerView.findViewById<TextView>(androidx.media3.ui.R.id.exo_duration)

            // Calculate how far behind live we are
            val timeBehindLive = duration - position

            // Check if we're at live edge
            val isAtLiveEdge = timeBehindLive < 5000 // 5 second threshold

            // Update position text
            positionText?.apply {
                if (isAtLiveEdge) {
                    // At live edge, show "LIVE"
                    text = "LIVE"
                    setTextColor(Color.RED)
                    setTypeface(typeface, Typeface.BOLD)
                } else {
                    // When behind live, show the actual position
                    text = formatDuration(position)
                    setTextColor(Color.WHITE)
                    setTypeface(typeface, Typeface.NORMAL)
                }
            }

            // Update duration text
            durationText?.apply {
                text = formatDuration(duration)
            }
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error updating live position and duration: ${e.message}")
        }
    }

    // Replace the showCustomToast method with this improved version
    private fun showCustomToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showMoreFeaturesDialog() {
        val customDialog = LayoutInflater.from(this)
            .inflate(R.layout.more_features, binding.root, false)
        val bindingMF = MoreFeaturesBinding.bind(customDialog)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(customDialog)
            .setOnCancelListener { playVideo() }
            .setBackground(ColorDrawable(0x803700B3.toInt()))
            .create()

        dialog.show()

        // Handle audio booster click
        bindingMF.audioBooster.setOnClickListener {
            dialog.dismiss()
            showAudioBoosterDialog()
        }

        // Add subtitle button click listener
        bindingMF.subtitlesBtn.setOnClickListener {
            dialog.dismiss()
            playVideo()
            val subtitles = ArrayList<String>()
            val subtitlesList = ArrayList<String>()
            var hasSubtitles = false

            // Get available subtitle tracks
            try {
                for (group in player.currentTracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT) {
                        hasSubtitles = true
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val language = format.language ?: "unknown"
                            val label = format.label ?: Locale(language).displayLanguage

                            subtitles.add(language)
                            subtitlesList.add(
                                "${subtitlesList.size + 1}. $label" +
                                        if (language != "unknown") " (${Locale(language).displayLanguage})" else ""
                            )
                        }
                    }
                }

                if (!hasSubtitles) {
                    Toast.makeText(this, "No subtitles available for this video", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val tempTracks = subtitlesList.toArray(arrayOfNulls<CharSequence>(subtitlesList.size))

                MaterialAlertDialogBuilder(this, R.style.SubtitleDialogStyle)
                    .setTitle("Select Subtitles")
                    .setOnCancelListener { playVideo() }
                    .setPositiveButton("Off Subtitles") { self, _ ->
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setRendererDisabled(C.TRACK_TYPE_TEXT, true)
                        )
                        self.dismiss()
                        playVideo()
                        Snackbar.make(playerView, "Subtitles disabled", 3000).show()
                    }
                    .setItems(tempTracks) { _, position ->
                        try {
                            trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                    .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                                    .setPreferredTextLanguage(subtitles[position])
                            )
                            Snackbar.make(
                                playerView,
                                "Selected: ${subtitlesList[position]}",
                                3000
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error selecting subtitles", Toast.LENGTH_SHORT).show()
                        }
                        playVideo()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                    .apply {
                        show()
                        getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.WHITE)
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading subtitles", Toast.LENGTH_SHORT).show()
            }
        }

        // Video Quality button in more features dialog
        bindingMF.videoQuality.setOnClickListener {
            dialog.dismiss()
            showQualityDialog()
        }

        // Add PiP button click handler
        bindingMF.pipModeBtn.setOnClickListener {
            dialog.dismiss()
            enterPictureInPictureMode()
        }

        // Add language button click handler
        bindingMF.languageBtn.setOnClickListener {
            dialog.dismiss()
            showLanguageDialog()
        }

        // In the showMoreFeaturesDialog method, add a new button handler for audio tracks
        bindingMF.audioTrackBtn.setOnClickListener {
            dialog.dismiss()
            showAudioTracksDialog()
        }
    }

    // Add this new method
    private fun showLanguageDialog() {
        val languages = LanguageManager.getSupportedLanguages()

        // Get current language code
        val currentLang = LanguageManager.getCurrentLanguage(this)

        // Find current selection index
        val currentIndex = languages.indexOfFirst { it.second == currentLang }.coerceAtLeast(0)

        // Create items array
        val items = languages.map { it.first }.toTypedArray()

        val dialog = MaterialAlertDialogBuilder(this, R.style.AlertDialogCustom)
            .setTitle(getString(R.string.select_language))
            .setSingleChoiceItems(items, currentIndex) { dialog, which ->
                val (_, langCode) = languages[which]

                // Use language manager to set language
                LanguageManager.setLanguage(this, langCode)

                dialog.dismiss()

                // Show confirmation
                Toast.makeText(this, getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                playVideo()
            }
            .setBackground(ColorDrawable(0x803700B3.toInt()))
            .create()

        // Apply styling
        dialog.setOnShowListener { dialogInterface ->
            val alertDialog = dialogInterface as AlertDialog

            // Set title color
            val titleId = resources.getIdentifier("alertTitle", "id", "android")
            alertDialog.findViewById<TextView>(titleId)?.setTextColor(Color.WHITE)

            // Set list item colors
            alertDialog.listView?.apply {
                setSelector(R.drawable.dialog_item_selector)
                divider = ColorDrawable(Color.WHITE)
                dividerHeight = 1
            }

            // Set button colors
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
        }

        dialog.show()
    }

    // Add this new method to handle audio track selection
    private fun showAudioTracksDialog() {
        val audioTracks = ArrayList<String>()
        val audioTracksList = ArrayList<String>()
        var hasAudioTracks = false

        try {
            // Get available audio tracks using Media3 API
            val trackGroups = player.currentTracks.groups

            for (group in trackGroups) {
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    hasAudioTracks = true
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val language = format.language ?: "unknown"
                        val label = format.label ?: Locale(language).displayLanguage
                        val channels = format.channelCount
                        val bitrate = format.bitrate / 1000 // Convert to kbps

                        audioTracks.add(language)
                        audioTracksList.add(
                            "${audioTracksList.size + 1}. $label" +
                                    if (language != "unknown") " (${Locale(language).displayLanguage})" else "" +
                                            if (channels > 0) " - ${channels}ch" else "" +
                                                    if (bitrate > 0) " - ${bitrate}kbps" else ""
                        )
                    }
                }
            }

            if (!hasAudioTracks) {
                Toast.makeText(this, "No audio tracks available for this video", Toast.LENGTH_SHORT).show()
                return
            }

            val tempTracks = audioTracksList.toArray(arrayOfNulls<CharSequence>(audioTracksList.size))

            MaterialAlertDialogBuilder(this, R.style.AlertDialogCustom)
                .setTitle(getString(R.string.audio_track))
                .setOnCancelListener { playVideo() }
                .setItems(tempTracks) { dialog, position ->
                    try {
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setPreferredAudioLanguage(audioTracks[position])
                        )
                        Snackbar.make(
                            playerView,
                            "Selected: ${audioTracksList[position]}",
                            3000
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error selecting audio track", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                    playVideo()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                    playVideo()
                }
                .setBackground(ColorDrawable(0x803700B3.toInt()))
                .create()
                .apply {
                    setOnShowListener { dialogInterface ->
                        val alertDialog = dialogInterface as AlertDialog

                        // Set title color
                        val titleId = resources.getIdentifier("alertTitle", "id", "android")
                        alertDialog.findViewById<TextView>(titleId)?.setTextColor(Color.WHITE)

                        // Set list item colors
                        alertDialog.listView?.apply {
                            setSelector(R.drawable.dialog_item_selector)
                            divider = ColorDrawable(Color.WHITE)
                            dividerHeight = 1
                        }

                        // Set button colors
                        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
                    }
                    show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading audio tracks", Toast.LENGTH_SHORT).show()
        }
    }



    // Replace enterPipMode() with this method that uses the standard Android API
    override fun enterPictureInPictureMode() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                android.os.Process.myUid(),
                packageName
            ) == AppOpsManager.MODE_ALLOWED
        } else {
            false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (status) {
                // Save current state before entering PiP
                prePipScreenMode = currentScreenMode
                prePipNotchEnabled = isNotchModeEnabled

                // Hide controls immediately before entering PiP
                binding.playerView.hideController()
                binding.lockButton.visibility = View.GONE
                binding.brightnessIcon.visibility = View.GONE
                binding.volumeIcon.visibility = View.GONE

                // Enter PiP mode
                try {
                    // Create PiP params with aspect ratio based on video dimensions
                    val videoRatio = if (player.videoFormat != null) {
                        Rational(player.videoFormat!!.width, player.videoFormat!!.height)
                    } else {
                        Rational(16, 9) // Default aspect ratio
                    }

                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(videoRatio)
                        .build()

                    super.enterPictureInPictureMode(params)

                    // Set flag to prevent ads when PiP is requested
                    isPipRequested = true

                    // Ensure video is playing
                    playVideo()
                } catch (e: Exception) {
                    Log.e("PlayerActivity", "Failed to enter PiP mode: ${e.message}")
                    Toast.makeText(this, "Failed to enter PiP mode", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Open PiP settings if not enabled
                val intent = Intent(
                    "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "Feature Not Supported!!", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper method to format duration in Hotstar cricket style (HH:MM:SS or MM:SS)
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    // Add this helper method to update the repeat button icon
    private fun updateRepeatButtonIcon(repeatMode: Int) {
        val iconResId = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_icon
            Player.REPEAT_MODE_ALL -> R.drawable.repeat_all_icon
            else -> R.drawable.repeat_off_icon
        }
        repeatButton.setImageResource(iconResId)
    }

    // Optimized position and duration text updates for live streaming
    private fun setupLiveTextUpdater() {
        if (!isLiveStream) return

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val updateInterval = 33L // 30fps updates for ultra-smooth text changes

        val runnable = object : Runnable {
            override fun run() {
                if (isLiveStream && isPlayerReady && !isInPictureInPictureMode) {
                    try {
                        // Get references to text views
                        val positionText = playerView.findViewById<TextView>(androidx.media3.ui.R.id.exo_position)
                        val durationText = playerView.findViewById<TextView>(androidx.media3.ui.R.id.exo_duration)

                        // Get current window and position info
                        val currentWindow = player.currentTimeline.getWindow(
                            player.currentMediaItemIndex, Timeline.Window()
                        )
                        val currentPosition = player.contentPosition
                        var duration = currentWindow.durationMs

                        // For live streams, we need to continuously update the duration
                        if (duration > 0) {
                            // Store the last known duration if it's larger than what we have
                            if (duration > lastKnownLiveDuration) {
                                lastKnownLiveDuration = duration
                            } else if (player.isPlaying) {
                                // If we're playing but duration didn't increase, simulate
                                // the duration increasing in real-time
                                val timeSinceLastUpdate = System.currentTimeMillis() - lastLiveUpdateTime
                                if (timeSinceLastUpdate > 0) {
                                    // Increase duration at real-time rate
                                    duration = lastKnownLiveDuration + timeSinceLastUpdate
                                    lastKnownLiveDuration = duration
                                }
                            }
                            lastLiveUpdateTime = System.currentTimeMillis()

                            // Calculate how far behind live we are
                            val timeBehindLive = duration - currentPosition

                            // Check if we're at live edge
                            val isAtLiveEdge = timeBehindLive < 1000 // 1 second threshold

                            // Update position text with zero delay
                            positionText?.apply {
                                if (isAtLiveEdge) {
                                    // At live edge, show "LIVE"
                                    text = "LIVE"
                                    setTextColor(Color.RED)
                                    setTypeface(typeface, Typeface.BOLD)
                                } else {
                                    // When behind live, show the actual position with real-time updates
                                    val adjustedPosition = if (player.isPlaying) {
                                        // Smoothly interpolate position for real-time updates
                                        val timeSincePositionUpdate = System.currentTimeMillis() - lastPositionUpdateTime
                                        currentPosition + (timeSincePositionUpdate * player.playbackParameters.speed).toLong()
                                    } else {
                                        currentPosition
                                    }
                                    text = formatDuration(adjustedPosition)
                                    setTextColor(Color.WHITE)
                                }
                            }

                            // Update duration text
                            durationText?.apply {
                                // For live streams, always show the current duration
                                text = formatDuration(duration)
                            }

                            // Update last position time for smooth interpolation
                            lastPositionUpdateTime = System.currentTimeMillis()
                        }
                    } catch (e: Exception) {
                        Log.e("PlayerActivity", "Error updating live text: ${e.message}")
                    }
                }

                // Schedule next update at display refresh rate
                if (isPlayerReady && !isDestroyed) {
                    handler.postDelayed(this, updateInterval)
                }
            }
        }

        // Start the updater immediately
        handler.post(runnable)
    }



    // Add this method to set up player listeners
    private fun setupPlayerListeners() {
        player.addListener(object : Player.Listener {
            // Copy your existing listener implementation here
        })
    }

    // Add this method to check if URL is an Akamaized stream
    private fun isAkamaizedStream(url: String?): Boolean {
        return url?.contains("akamaized", ignoreCase = true) == true &&
                (url.contains("hdntl=exp", ignoreCase = true) ||
                        url.contains("hmac=", ignoreCase = true))
    }

    // Add these methods to handle player errors
    private fun handlePlayerError(error: PlaybackException) {
        // Log detailed error information
        Log.e("PlayerActivity", "Player error: ${error.message}")
        Log.e("PlayerActivity", "Error code: ${error.errorCode}, cause: ${error.cause?.message}")
        Log.e("PlayerActivity", "Stack trace: ${error.stackTraceToString()}")

        // Determine error type for better handling
        val errorType = categorizeError(error)

        // Show appropriate error message
        showErrorMessage(errorType, error)

        // Attempt recovery based on error type
        when (errorType) {
            ErrorType.NETWORK -> attemptNetworkErrorRecovery()
            ErrorType.FORMAT -> attemptFormatErrorRecovery()
            ErrorType.RTMP -> attemptRtmpErrorRecovery()
            ErrorType.DRM -> showDrmErrorMessage()
            ErrorType.RENDERER -> attemptRendererErrorRecovery()
            ErrorType.UNEXPECTED -> attemptGenericErrorRecovery()
        }
    }

    // Categorize errors for better handling
    private enum class ErrorType {
        NETWORK, FORMAT, RTMP, DRM, RENDERER, UNEXPECTED
    }

    private fun categorizeError(error: PlaybackException): ErrorType {
        return when {
            // RTMP specific errors
            error.cause?.message?.contains("rtmp", ignoreCase = true) == true ||
                    url?.startsWith("rtmp://", ignoreCase = true) == true -> ErrorType.RTMP

            // Network errors
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                    error.cause is java.net.SocketTimeoutException ||
                    error.cause is java.net.UnknownHostException -> ErrorType.NETWORK

            // Format errors
            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> ErrorType.FORMAT

            // DRM errors
            error.errorCode == PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR ||
                    error.errorCode == PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR -> ErrorType.DRM

            // Renderer errors
            error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                    // Replace the missing error code with a check for video decoder errors
                    error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> ErrorType.RENDERER

            // Everything else
            else -> ErrorType.UNEXPECTED
        }
    }

    // Show appropriate error message based on error type
    private fun showErrorMessage(errorType: ErrorType, error: PlaybackException) {
        errorTextView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE

        val errorMessage = when (errorType) {
            ErrorType.RTMP -> "RTMP stream error. The stream may be offline or the URL is invalid."
            ErrorType.NETWORK -> "Network connection error. Please check your internet connection."
            ErrorType.FORMAT -> "This stream format is not supported or the URL is invalid."
            ErrorType.DRM -> "This content is protected and cannot be played."
            ErrorType.RENDERER -> "Your device doesn't support playing this format."
            ErrorType.UNEXPECTED -> "Playback error: ${error.message}"
        }

        errorTextView.text = errorMessage

        // Show retry button
        showRetryButton(errorType)
    }

    // Show retry button with appropriate action based on error type
    private fun showRetryButton(errorType: ErrorType) {
        if (errorTextView.parent is ViewGroup) {
            val container = errorTextView.parent as ViewGroup
            if (container.findViewById<Button>(R.id.retry_button) == null) {
                val retryButton = Button(this).apply {
                    id = R.id.retry_button
                    text = "Retry"
                    setOnClickListener {
                        // Hide error views
                        errorTextView.visibility = View.GONE
                        this.visibility = View.GONE
                        progressBar.visibility = View.VISIBLE

                        // Different retry strategies based on error type
                        when (errorType) {
                            ErrorType.RTMP -> attemptRtmpErrorRecovery()
                            ErrorType.NETWORK -> attemptNetworkErrorRecovery()
                            ErrorType.FORMAT -> attemptFormatErrorRecovery()
                            else -> setupPlayer() // Default fallback
                        }
                    }
                }
                container.addView(retryButton)
            } else {
                container.findViewById<Button>(R.id.retry_button).visibility = View.VISIBLE
            }
        }
    }

    private fun handleSetupError(error: Exception) {
        Log.e("PlayerActivity", "Setup error: ${error.message}")
        error.printStackTrace()

        // Show error UI
        errorTextView.apply {
            visibility = View.VISIBLE
            text = "Error initializing player: ${error.message}"
        }
        progressBar.visibility = View.GONE

        // Show toast with more details
        Toast.makeText(
            this,
            "Failed to initialize player: ${error.message}",
            Toast.LENGTH_LONG
        ).show()

        // Add retry button
        if (errorTextView.parent is ViewGroup) {
            val container = errorTextView.parent as ViewGroup
            if (container.findViewById<Button>(R.id.retry_button) == null) {
                val retryButton = Button(this).apply {
                    id = R.id.retry_button
                    text = "Retry"
                    setOnClickListener {
                        // Hide error views
                        errorTextView.visibility = View.GONE
                        this.visibility = View.GONE
                        progressBar.visibility = View.VISIBLE

                        // Try to initialize player again
                        setupPlayer()
                    }
                }
                container.addView(retryButton)
            }
        }
    }

    // Add helper method to detect Hotstar-style streams
    private fun isHotstarStyleStream(url: String?): Boolean {
        if (url == null) return false

        // Enhanced pattern matching for Hotstar-style URLs
        return url.matches(Regex(".*(m3u8|php)(\\?|&)id=\\d+.*")) ||  // Matches m3u8?id=123 or php?id=123
                url.matches(Regex(".*\\d+\\.(m3u8|php)(\\?|&)id=\\d+.*")) ||  // Matches number.m3u8?id=123 or number.php?id=123
                url.matches(Regex(".*(m3u8|php)(\\?|&)c(hannel)?=\\w+.*")) || // Channel parameter
                url.contains("akamaized", ignoreCase = true) ||    // Common in Hotstar streams
                url.contains("hotstar", ignoreCase = true) ||      // Direct Hotstar URLs
                url.contains("/hls/", ignoreCase = true) ||        // Common HLS path
                url.contains("/live/", ignoreCase = true) ||       // Live stream paths
                url.contains("master.m3u8", ignoreCase = true) ||  // Master playlist
                url.contains("manifest.m3u8", ignoreCase = true) || // Manifest playlist
                url.contains("playlist.m3u8", ignoreCase = true) || // Playlist file
                (url.contains(".m3u8") && url.contains("?")) ||    // Any m3u8 with query params
                (url.contains(".php") && url.contains("?"))        // Any PHP endpoint with query params
    }

    // Add method to configure live playback settings
    private fun configureLivePlayback() {
        // Set controller timeout using the correct method
        playerView.controllerShowTimeoutMs = 3500 // Show controls for 3.5 seconds

        // Set buffering display mode
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)

        // Configure time bar for live streams
        configureLiveTimeBar()

        // Initialize live text updates
//        initializeLiveTextUpdates()

        // Set live playback parameters
        player.setPlaybackParameters(PlaybackParameters(1.0f))

        // Configure player for low latency if possible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setMaxVideoBitrate(Int.MAX_VALUE) // Allow highest quality
                    .setForceHighestSupportedBitrate(true)
                    .build()
            } catch (e: Exception) {
                Log.e("PlayerActivity", "Error configuring live playback: ${e.message}")
            }
        }
    }

    // Add this method to check RTMP support
    private fun isRtmpSupported(): Boolean {
        try {
            // Try to load the Media3 RTMP data source class
            Class.forName("androidx.media3.datasource.rtmp.RtmpDataSource")
            return true
        } catch (e: ClassNotFoundException) {
            Log.e("PlayerActivity", "RTMP support not available: ${e.message}")
            return false
        }
    }

    // Add this method to configure player for Hotstar-style streams
    private fun configureHotstarPlayback() {
        try {
            // Set optimal buffer size for Hotstar streams
            player.setVideoSurfaceView(playerView.videoSurfaceView as SurfaceView?)

            // Set playback parameters optimized for Hotstar streams
            player.setPlaybackParameters(PlaybackParameters(1.0f))

            // Configure track selection for better quality on Hotstar
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setForceHighestSupportedBitrate(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setSelectUndeterminedTextLanguage(true)
                    .setTunnelingEnabled(true) // Enable tunneling for smoother playback
            )

            // Increase buffer size for smoother playback
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15000, // Min buffer (15 seconds)
                    60000, // Max buffer (60 seconds)
                    2000,  // Buffer for playback (2 seconds)
                    2000   // Buffer for playback after rebuffer (2 seconds)
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            // Apply the load control if possible
            if (player is ExoPlayer) {
                // We can't directly set the load control after player creation,
                // but we can remember these settings for next time
                getSharedPreferences("player_settings", Context.MODE_PRIVATE).edit().apply {
                    putInt("buffer_min_ms", 15000)
                    putInt("buffer_max_ms", 60000)
                    putInt("playback_buffer_ms", 2000)
                    putInt("rebuffer_ms", 2000)
                    apply()
                }
            }

            // Set longer timeout for Hotstar streams which might take longer to load
            playerView.controllerShowTimeoutMs = 4000

            Log.d("PlayerActivity", "Configured player for Hotstar-style stream")
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error configuring Hotstar playback: ${e.message}")
        }
    }

    // Set up enhanced player listeners with better error handling
    private fun setupEnhancedPlayerListeners() {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        playbackState = PlaybackState.BUFFERING
                        progressBar.visibility = View.VISIBLE

                        // Show buffering percentage if available
                        val bufferingPercentage = player.bufferedPercentage
                        if (bufferingPercentage > 0) {
                            progressBar.progress = bufferingPercentage
                        }
                    }
                    Player.STATE_READY -> {
                        progressBar.visibility = View.GONE
                        isPlayerReady = true

                        // Check if this is a live stream
                        val isCurrentlyLive = player.isCurrentMediaItemLive
                        if (isCurrentlyLive && !isLiveStream) {
                            isLiveStream = true
                            configureLivePlayback()

                            // Start the live time bar updater for real-time updates
                            startLiveTimeBarUpdater()
                        }

                        if (wasPlayingBeforePause) {
                            playbackState = PlaybackState.PLAYING
                            player.play()
                        } else {
                            playbackState = PlaybackState.PAUSED
                        }
                        updatePlayPauseButton(wasPlayingBeforePause)
                    }
                    Player.STATE_ENDED -> {
                        playbackState = PlaybackState.ENDED
                        updatePlayPauseButton(false)
                        handlePlaybackEnded()
                    }
                    Player.STATE_IDLE -> {
                        playbackState = PlaybackState.IDLE
                        updatePlayPauseButton(false)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@PlayerActivity.isPlaying = isPlaying
                updatePlayPauseButton(isPlaying)

                // Update UI based on playing state
                if (isPlaying) {
                    // Hide any error views when playback starts
                    errorTextView.visibility = View.GONE
                    if (errorTextView.parent is ViewGroup) {
                        val container = errorTextView.parent as ViewGroup
                        container.findViewById<Button>(R.id.retry_button)?.visibility = View.GONE
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                handlePlayerError(error)
            }

            // Add tracking for video format changes
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                Log.d("PlayerActivity", "Video size changed: ${videoSize.width}x${videoSize.height}")

                // Update quality display if needed
                if (isManualQualityControl) {
                    updateQualityInfo()
                }
            }

            // Add this to update the time bar when playback parameters change
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                if (isLiveStream) {
                    // Reconfigure live time bar when playback speed changes
                    configureLiveTimeBar()
                }
            }

            // Add this to update the time bar when timeline changes
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (isLiveStream && reason != Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                    // Timeline updated for reasons other than playlist change
                    // This is a good time to update our live time bar
                    val currentPosition = player.currentPosition
                    val currentTime = System.currentTimeMillis()
                    val elapsedSinceStart = currentTime - liveStreamStartTime
                    updateLivePositionAndDuration(currentPosition, elapsedSinceStart + 30000)
                }
            }
        })
    }

    // Improved error handling with automatic recovery attempts
    private fun attemptNetworkErrorRecovery() {
        // First check if we're connected to the internet
        if (!isNetworkAvailable()) {
            showCustomToast("No internet connection. Please check your network settings.")
            return
        }

        // Try with increased timeouts
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isDestroyed && !isFinishing) {
                try {
                    // Create a new player with increased timeouts
                    val dataSourceFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent(userAgent ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(30000) // Increased timeout
                        .setReadTimeoutMs(30000)    // Increased timeout
                        .setDefaultRequestProperties(createStreamingHeaders())

                    // Create media item
                    val mediaItem = createEnhancedMediaItem(url ?: return@postDelayed)

                    // Create media source
                    val mediaSource = createMediaSourceForUrl(url, mediaItem, dataSourceFactory)

                    // Reset player and try again
                    player.stop()
                    player.clearMediaItems()
                    player.setMediaSource(mediaSource)
                    player.prepare()
                    player.play()

                    // Hide error views
                    errorTextView.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE

                    showCustomToast("Retrying with increased timeout...")
                } catch (e: Exception) {
                    Log.e("PlayerActivity", "Network recovery failed: ${e.message}")
                    showCustomToast("Recovery failed. Please try again later.")
                }
            }
        }, 1000) // Wait 1 second before retry
    }

    // Format error recovery
    private fun attemptFormatErrorRecovery() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isDestroyed && !isFinishing) {
                try {
                    // Try to detect format from URL and create appropriate media source
                    val detectedMimeType = detectMimeTypeFromContent(url ?: return@postDelayed)

                    // Create media item with detected mime type
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(url))
                        .setMimeType(detectedMimeType)
                        .build()

                    // Reset player and try again
                    player.stop()
                    player.clearMediaItems()
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()

                    // Hide error views
                    errorTextView.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE

                    showCustomToast("Retrying with detected format...")
                } catch (e: Exception) {
                    Log.e("PlayerActivity", "Format recovery failed: ${e.message}")
                    setupPlayer() // Fall back to default setup
                }
            }
        }, 1000) // Wait 1 second before retry
    }

    // RTMP error recovery with alternative approach
    private fun attemptRtmpErrorRecovery() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isDestroyed && !isFinishing) {
                Log.d("PlayerActivity", "Attempting alternative RTMP playback approach")
                tryAlternativeRtmpPlayback()
            }
        }, 1000) // Wait 1 second before trying alternative approach
    }

    // Renderer error recovery
    private fun attemptRendererErrorRecovery() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isDestroyed && !isFinishing) {
                try {
                    // Try with lower quality settings
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(640, 360) // Try with lower resolution
                            .setMaxVideoBitrate(1_000_000) // Lower bitrate
                    )

                    // Reset player and try again
                    player.stop()
                    player.prepare()
                    player.play()

                    // Hide error views
                    errorTextView.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE

                    showCustomToast("Retrying with lower quality settings...")
                } catch (e: Exception) {
                    Log.e("PlayerActivity", "Renderer recovery failed: ${e.message}")
                    setupPlayer() // Fall back to default setup
                }
            }
        }, 1000) // Wait 1 second before retry
    }

    // Generic error recovery
    private fun attemptGenericErrorRecovery() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isDestroyed && !isFinishing) {
                setupPlayer() // Just try to set up the player again
            }
        }, 2000) // Wait 2 seconds before retry
    }

    // Helper method to check network availability
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

    // Enhance detectMimeTypeFromContent for better format detection
    private fun detectMimeTypeFromContent(url: String): String {
        // Default to a common format
        var mimeType = "video/mp4"

        try {
            // Try to make a HEAD request to get content type
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            // Add streaming-friendly headers
            connection.setRequestProperty("User-Agent", userAgent ?: "Mozilla/5.0")
            connection.setRequestProperty("Accept", "*/*")
            connection.setRequestProperty("Range", "bytes=0-1") // Just request 1 byte to check type
            
            connection.connect()

            val contentType = connection.contentType
            if (contentType != null) {
                // Parse content type, handling parameters
                val mainType = contentType.split(";")[0].trim()
                if (mainType.isNotEmpty()) {
                    mimeType = mainType
                    Log.d("PlayerActivity", "Detected MIME type from HTTP: $mimeType")
                }
            }

            // Check for specific headers that might indicate streaming formats
            val contentDisposition = connection.getHeaderField("Content-Disposition")
            if (contentDisposition != null && contentDisposition.contains(".m3u8")) {
                mimeType = "application/x-mpegURL"
            }

            connection.disconnect()
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error detecting MIME type: ${e.message}")
            // Fall back to detection from URL
            mimeType = detectFormatFromUrl(url)
        }

        return mimeType
    }

    private fun showDrmErrorMessage() {
        // Implement DRM error handling logic
        Log.d("PlayerActivity", "Showing DRM error message")
        // You might want to show a custom error message to the user
    }

    // Add this method to create enhanced streaming headers specifically for Hotstar-style streams
    private fun createHotstarStreamingHeaders(): Map<String, String> {
        val host = Uri.parse(url)?.host ?: ""

        return mapOf(
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Origin" to "https://$host",
            "Referer" to "https://$host",
            "Connection" to "keep-alive",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "cross-site",
            // Add headers that help with Hotstar streaming services
            "X-Requested-With" to "com.hotstar.android",
            "Pragma" to "no-cache",
            "Cache-Control" to "no-cache",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        )
    }

    // Add this new method to detect format from URL patterns
    private fun detectFormatFromUrl(url: String): String {
        return when {
            // HLS patterns
            url.contains(".m3u8", ignoreCase = true) ||
            url.contains("/hls/", ignoreCase = true) ||
            url.contains("/playlist", ignoreCase = true) ||
            url.contains("/master", ignoreCase = true) -> "application/x-mpegURL"
            
            // DASH patterns
            url.contains(".mpd", ignoreCase = true) ||
            url.contains("/dash/", ignoreCase = true) ||
            url.contains("/manifest", ignoreCase = true) -> "application/dash+xml"
            
            // Smooth Streaming patterns
            url.contains(".ism", ignoreCase = true) ||
            url.contains(".isml", ignoreCase = true) ||
            url.contains("/smooth/", ignoreCase = true) -> "application/vnd.ms-sstr+xml"
            
            // RTMP patterns
            url.startsWith("rtmp://", ignoreCase = true) ||
            url.startsWith("rtmps://", ignoreCase = true) -> "video/rtmp"
            
            // Transport Stream patterns
            url.contains(".ts", ignoreCase = true) ||
            url.contains(".mts", ignoreCase = true) -> "video/mp2t"
            
            // Progressive download patterns
            url.contains(".mp4", ignoreCase = true) -> "video/mp4"
            url.contains(".webm", ignoreCase = true) -> "video/webm"
            url.contains(".mkv", ignoreCase = true) -> "video/x-matroska"
            
            // Default to MP4 if no pattern matches
            else -> "video/mp4"
        }
    }

}