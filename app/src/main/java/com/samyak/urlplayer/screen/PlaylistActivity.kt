package com.samyak.urlplayer.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.samyak.urlplayer.R
import com.samyak.urlplayer.adapters.PlaylistItemAdapter
import com.samyak.urlplayer.databinding.ActivityPlaylistBinding
import com.samyak.urlplayer.models.PlaylistItem
import com.samyak.urlplayer.utils.GridSpacingItemDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.Locale
import java.util.LinkedHashSet

class PlaylistActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlaylistBinding
    private lateinit var adapter: PlaylistItemAdapter
    private lateinit var recyclerView: RecyclerView
    private var playlistUrl: String? = null
    private var playlistName: String? = null
    private var userAgent: String? = null
    private var allPlaylistItems = listOf<PlaylistItem>()
    private var currentGroup: String? = null

    companion object {
        private const val GRID_SPAN_COUNT = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        playlistUrl = intent.getStringExtra("URL")
        playlistName = intent.getStringExtra("TITLE")
        userAgent = intent.getStringExtra("USER_AGENT")

        if (playlistUrl == null) {
            Toast.makeText(this, getString(R.string.error_invalid_playlist), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        loadPlaylist()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = playlistName ?: getString(R.string.playlist)
        }
    }

    private fun setupRecyclerView() {
        val spanCount = GRID_SPAN_COUNT
        // Get spacing dimension
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        recyclerView = binding.playlistRecyclerView
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@PlaylistActivity, spanCount)
            addItemDecoration(GridSpacingItemDecoration(spanCount, spacing, true))
            setHasFixedSize(true)
        }

        adapter = PlaylistItemAdapter { playlistItem ->
            launchPlayerForItem(playlistItem)
        }
        recyclerView.adapter = adapter
    }

    private fun loadPlaylist() {
        binding.progressBar.visibility = View.VISIBLE
        binding.playlistRecyclerView.visibility = View.GONE
        binding.emptyStateView.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val playlistItems = parseM3uPlaylist(playlistUrl!!)
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    
                    if (playlistItems.isEmpty()) {
                        binding.emptyStateView.visibility = View.VISIBLE
                    } else {
                        allPlaylistItems = playlistItems
                        setupGroupTabs(playlistItems)
                        updatePlaylistForCurrentGroup()
                        binding.playlistRecyclerView.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.emptyStateView.visibility = View.VISIBLE
                    Toast.makeText(
                        this@PlaylistActivity,
                        getString(R.string.error_loading_playlist) + ": ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupGroupTabs(items: List<PlaylistItem>) {
        // Extract unique groups while preserving order
        val groups = LinkedHashSet<String>()
        
        // Add "ALL" as the first tab
        groups.add("ALL")
        
        // Add all other groups
        items.forEach { item ->
            item.group?.let { 
                if (it.isNotEmpty()) {
                    groups.add(it)
                }
            }
        }
        
        // Clear existing tabs
        binding.groupTabLayout.removeAllTabs()
        
        // Add a tab for each group
        groups.forEach { group ->
            val tab = binding.groupTabLayout.newTab()
            tab.text = if (group == "ALL") "ALL(${items.size})" else "$group(${countItemsInGroup(items, group)})"
            binding.groupTabLayout.addTab(tab)
        }
        
        // Set tab selection listener
        binding.groupTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selectedGroup = tab.text.toString().split("(")[0]
                currentGroup = if (selectedGroup == "ALL") null else selectedGroup
                updatePlaylistForCurrentGroup()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun countItemsInGroup(items: List<PlaylistItem>, group: String): Int {
        return if (group == "ALL") {
            items.size
        } else {
            items.count { it.group == group }
        }
    }
    
    private fun updatePlaylistForCurrentGroup() {
        val filteredItems = if (currentGroup == null) {
            allPlaylistItems
        } else {
            allPlaylistItems.filter { it.group == currentGroup && it.isActive }
        }
        
        adapter.updateItems(filteredItems)
        
        // Show empty state if no items in the selected group
        if (filteredItems.isEmpty()) {
            binding.emptyStateView.visibility = View.VISIBLE
            binding.playlistRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateView.visibility = View.GONE
            binding.playlistRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun parseM3uPlaylist(playlistUrl: String): List<PlaylistItem> {
        val playlistItems = mutableListOf<PlaylistItem>()
        
        try {
            // Check if the URL is a PHP-based stream URL or M3U8 with query parameters
            if ((playlistUrl.contains(".php") && playlistUrl.contains("?")) ||
                (playlistUrl.contains(".m3u8") && playlistUrl.contains("?"))) {
                // Handle PHP-based stream or M3U8 with query parameters directly
                return handlePhpBasedStream(playlistUrl)
            }
            
            val url = URL(playlistUrl)
            val connection = url.openConnection().apply {
                userAgent?.let { setRequestProperty("User-Agent", it) }
                connectTimeout = 15000  // Increased timeout for slow connections
                readTimeout = 15000
            }
            
            BufferedReader(InputStreamReader(connection.getInputStream())).use { reader ->
                var line: String?
                var currentTitle: String? = null
                var currentLogo: String? = null
                var currentGroup: String? = null
                var currentUrl: String? = null
                
                // Check if playlist starts with #EXTM3U
                val firstLine = reader.readLine()
                var isExtendedM3u = firstLine?.trim()?.startsWith("#EXTM3U") == true
                
                // If not an extended M3U, treat each line as a URL
                if (!isExtendedM3u && firstLine != null) {
                    if (!firstLine.startsWith("#") && isValidUrl(firstLine.trim())) {
                        playlistItems.add(
                            PlaylistItem(
                                title = "Stream 1",
                                url = firstLine.trim(),
                                logoUrl = null,
                                group = null
                            )
                        )
                    }
                }
                
                // Process the rest of the file
                while (reader.readLine().also { line = it } != null) {
                    val trimmedLine = line?.trim() ?: continue
                    
                    when {
                        // Handle extended info line
                        trimmedLine.startsWith("#EXTINF:") -> {
                            // Parse title from EXTINF line
                            val titleMatch = Regex("tvg-name=\"([^\"]+)\"").find(trimmedLine)
                            currentTitle = titleMatch?.groupValues?.get(1) ?: extractTitleFromExtInf(trimmedLine)
                            
                            // Parse logo if available
                            val logoMatch = Regex("tvg-logo=\"([^\"]+)\"").find(trimmedLine)
                            currentLogo = logoMatch?.groupValues?.get(1)
                            
                            // Parse group if available
                            val groupMatch = Regex("group-title=\"([^\"]+)\"").find(trimmedLine)
                            currentGroup = groupMatch?.groupValues?.get(1)
                        }
                        
                        // Handle simple M3U format (just URLs)
                        !trimmedLine.startsWith("#") && isValidUrl(trimmedLine) -> {
                            currentUrl = trimmedLine
                            
                            // For simple M3U format without EXTINF
                            if (currentTitle == null && !isExtendedM3u) {
                                // Generate a title from the URL
                                val urlTitle = extractTitleFromUrl(trimmedLine)
                                playlistItems.add(
                                    PlaylistItem(
                                        title = urlTitle,
                                        url = trimmedLine,
                                        logoUrl = null,
                                        group = null
                                    )
                                )
                            } 
                            // For extended M3U format with EXTINF
                            else if (currentTitle != null) {
                                playlistItems.add(
                                    PlaylistItem(
                                        title = currentTitle ?: "Unknown",
                                        url = trimmedLine,
                                        logoUrl = currentLogo,
                                        group = currentGroup
                                    )
                                )
                                // Reset for next item
                                currentTitle = null
                                currentLogo = null
                                currentGroup = null
                            }
                        }
                        
                        // Handle other playlist directives
                        trimmedLine.startsWith("#EXT-X-STREAM-INF:") -> {
                            // Parse bandwidth/resolution info if needed
                            val bandwidthMatch = Regex("BANDWIDTH=(\\d+)").find(trimmedLine)
                            val bandwidth = bandwidthMatch?.groupValues?.get(1)
                            
                            val resolutionMatch = Regex("RESOLUTION=(\\d+x\\d+)").find(trimmedLine)
                            val resolution = resolutionMatch?.groupValues?.get(1)
                            
                            // Create title from resolution/bandwidth
                            currentTitle = if (resolution != null) {
                                "Stream ($resolution)"
                            } else if (bandwidth != null) {
                                "Stream (${bandwidth.toInt() / 1000} kbps)"
                            } else {
                                "Stream ${playlistItems.size + 1}"
                            }
                        }
                    }
                }
            }
            
            // Before sorting and returning, enhance items with format information
            val enhancedItems = playlistItems.map { enhancePlaylistItem(it) }

            // Sort items by group then title
            return enhancedItems.sortedWith(
                compareBy({ it.group ?: "zzz" }, { it.title })
            )
            
        } catch (e: Exception) {
            Log.e("PlaylistActivity", "Error parsing playlist: ${e.message}")
            e.printStackTrace()
            // Return any items we managed to parse before the error
            return playlistItems
        }
    }

    // Enhanced isValidUrl function with more protocol support
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || 
               url.startsWith("https://") || 
               url.startsWith("rtmp://") || 
               url.startsWith("rtsp://") ||
               url.startsWith("udp://") ||
               url.startsWith("rtp://") ||
               url.startsWith("mms://") ||
               url.startsWith("mmsh://") ||
               url.startsWith("mmst://") ||
               url.startsWith("srt://") ||
               url.startsWith("srtp://") ||
               url.startsWith("rist://")
    }

    // Add this method to detect stream type from URL
    private fun detectStreamType(url: String): String {
        val lowercaseUrl = url.lowercase()
        return when {
            // HLS streams
            lowercaseUrl.endsWith(".m3u8") -> "HLS"
            
            // DASH streams
            lowercaseUrl.endsWith(".mpd") -> "DASH"
            
            // Transport streams
            lowercaseUrl.endsWith(".ts") -> "TS"
            
            // Common video formats
            lowercaseUrl.endsWith(".mp4") -> "MP4"
            lowercaseUrl.endsWith(".mkv") -> "MKV"
            lowercaseUrl.endsWith(".avi") -> "AVI"
            lowercaseUrl.endsWith(".mov") -> "MOV"
            lowercaseUrl.endsWith(".webm") -> "WebM"
            
            // Streaming protocols
            lowercaseUrl.startsWith("rtmp://") -> "RTMP"
            lowercaseUrl.startsWith("rtsp://") -> "RTSP"
            lowercaseUrl.startsWith("udp://") -> "UDP"
            lowercaseUrl.startsWith("rtp://") -> "RTP"
            
            // Default
            else -> "HTTP"
        }
    }

    // Add this method to enhance the playlist item with format information
    private fun enhancePlaylistItem(item: PlaylistItem): PlaylistItem {
        val streamType = detectStreamType(item.url)
        
        // Add stream type to title if not already present
        val enhancedTitle = if (!item.title.contains(streamType, ignoreCase = true)) {
            "${item.title} [$streamType]"
        } else {
            item.title
        }
        
        return item.copy(title = enhancedTitle)
    }

    // Helper function to extract title from URL
    private fun extractTitleFromUrl(url: String): String {
        val path = try {
            URL(url).path
        } catch (e: Exception) {
            url
        }
        
        return path.substringAfterLast('/')
            .substringBeforeLast('.')
            .replace("_", " ")
            .replace("-", " ")
            .capitalize(Locale.getDefault())
            .ifEmpty { "Stream ${System.currentTimeMillis() % 1000}" }
    }

    // Enhanced function to extract title from EXTINF line
    private fun extractTitleFromExtInf(extInfLine: String): String {
        // First try to extract title after the duration and comma
        val commaIndex = extInfLine.indexOf(',')
        if (commaIndex != -1 && commaIndex < extInfLine.length - 1) {
            val afterComma = extInfLine.substring(commaIndex + 1).trim()
            
            // If there are no more attributes after the comma, use the whole string
            if (!afterComma.contains("tvg-") && !afterComma.contains("group-")) {
                return afterComma
            }
        }
        
        // If we couldn't extract a clean title, try to find it in tvg-name
        val nameMatch = Regex("tvg-name=\"([^\"]+)\"").find(extInfLine)
        if (nameMatch != null) {
            return nameMatch.groupValues[1]
        }
        
        // If all else fails, extract what's after the comma and before any attributes
        if (commaIndex != -1) {
            val afterComma = extInfLine.substring(commaIndex + 1).trim()
            val firstSpace = afterComma.indexOf(' ')
            if (firstSpace != -1) {
                return afterComma.substring(0, firstSpace)
            }
            return afterComma
        }
        
        return "Unknown Channel"
    }

    private fun launchPlayerForItem(item: PlaylistItem) {
        Intent(this, PlayerActivity::class.java).also { intent ->
            intent.putExtra("URL", item.url)
            intent.putExtra("USER_AGENT", userAgent)
            intent.putExtra("TITLE", item.title)
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Update the handlePhpBasedStream method to better handle M3U8 streams with query parameters
    private fun handlePhpBasedStream(url: String): List<PlaylistItem> {
        val playlistItems = mutableListOf<PlaylistItem>()
        
        try {
            // Special handling for M3U8 streams with query parameters
            if (url.contains(".m3u8") && url.contains("?")) {
                // Extract channel ID from URL parameters
                val channelId = extractChannelFromUrl(url)
                
                // Create a title from the channel ID
                val title = if (channelId.isNotEmpty()) {
                    channelId.replace("_", " ")
                        .replace("-", " ")
                        .capitalize(Locale.getDefault())
                } else {
                    "Live Stream"
                }
                
                // Add the stream directly
                playlistItems.add(
                    PlaylistItem(
                        title = "$title [HLS]",
                        url = url,
                        logoUrl = null,
                        group = "Live Streams"
                    )
                )
                
                return playlistItems
            }
            
            val connection = URL(url).openConnection().apply {
                userAgent?.let { setRequestProperty("User-Agent", it) }
                // Add common headers that might be required by PHP streams
                setRequestProperty("Referer", url)
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Origin", "https://${URL(url).host}")
                connectTimeout = 15000
                readTimeout = 15000
            }
            
            // Read the response to determine what type of content we received
            val contentType = connection.contentType ?: ""
            val inputStream = connection.getInputStream()
            
            when {
                // If it's a direct video stream
                contentType.contains("video/") || 
                contentType.contains("application/octet-stream") -> {
                    // Extract channel name from URL parameters
                    val channelParam = extractChannelFromUrl(url)
                    
                    val title = channelParam.uppercase()
                        .replace("_", " ")
                        .capitalize(Locale.getDefault())
                    
                    playlistItems.add(
                        PlaylistItem(
                            title = title,
                            url = url,
                            logoUrl = null,
                            group = "PHP Streams"
                        )
                    )
                }
                
                // If it's a text response that might contain a playlist or redirect URL
                contentType.contains("text/") -> {
                    val responseText = inputStream.bufferedReader().use { it.readText() }
                    
                    // Check if it's an M3U playlist
                    if (responseText.trim().startsWith("#EXTM3U")) {
                        // Create a temporary file with the content and parse it
                        val tempUrl = "data:application/x-mpegURL;base64," + 
                            android.util.Base64.encodeToString(responseText.toByteArray(), android.util.Base64.NO_WRAP)
                        return parseM3uPlaylist(tempUrl)
                    }
                    
                    // Check for embedded URLs in the response
                    val urlPattern = Regex("(https?://[^\\s\"'<>()]+)")
                    val foundUrls = urlPattern.findAll(responseText).map { it.value }.toList()
                    
                    if (foundUrls.isNotEmpty()) {
                        // Extract channel name from URL parameters
                        val channelParam = extractChannelFromUrl(url)
                        
                        val baseTitle = channelParam.uppercase()
                            .replace("_", " ")
                            .capitalize(Locale.getDefault())
                        
                        // Add each found URL as a separate stream
                        foundUrls.forEachIndexed { index, streamUrl ->
                            val streamType = detectStreamType(streamUrl)
                            playlistItems.add(
                                PlaylistItem(
                                    title = "$baseTitle Stream ${index + 1} [$streamType]",
                                    url = streamUrl,
                                    logoUrl = null,
                                    group = "PHP Streams"
                                )
                            )
                        }
                    } else {
                        // If no URLs found, use the original URL as a direct stream
                        val channelParam = extractChannelFromUrl(url)
                        
                        val title = channelParam.uppercase()
                            .replace("_", " ")
                            .capitalize(Locale.getDefault())
                        
                        playlistItems.add(
                            PlaylistItem(
                                title = title,
                                url = url,
                                logoUrl = null,
                                group = "PHP Streams"
                            )
                        )
                    }
                }
                
                // For any other content type, treat as direct stream
                else -> {
                    val channelParam = extractChannelFromUrl(url)
                    
                    val title = channelParam.uppercase()
                        .replace("_", " ")
                        .capitalize(Locale.getDefault())
                    
                    playlistItems.add(
                        PlaylistItem(
                            title = title,
                            url = url,
                            logoUrl = null,
                            group = "PHP Streams"
                        )
                    )
                }
            }
            
            return playlistItems.map { enhancePlaylistItem(it) }
            
        } catch (e: Exception) {
            Log.e("PlaylistActivity", "Error handling PHP stream: ${e.message}")
            e.printStackTrace()
            
            // If all else fails, add the URL as a direct stream
            val channelParam = extractChannelFromUrl(url)
            
            val title = channelParam.uppercase()
                .replace("_", " ")
                .capitalize(Locale.getDefault())
            
            playlistItems.add(
                PlaylistItem(
                    title = title,
                    url = url,
                    logoUrl = null,
                    group = "PHP Streams"
                )
            )
            
            return playlistItems.map { enhancePlaylistItem(it) }
        }
    }

    // Enhance the extractChannelFromUrl method to handle more parameter formats
    private fun extractChannelFromUrl(url: String): String {
        val queryParams = url.substringAfter("?", "").split("&")
        
        // Look for common channel parameter names
        for (param in queryParams) {
            when {
                param.startsWith("id=") -> return param.substringAfter("id=")
                param.startsWith("c=") -> return param.substringAfter("c=")
                param.startsWith("channel=") -> return param.substringAfter("channel=")
                param.startsWith("ch=") -> return param.substringAfter("ch=")
                param.startsWith("name=") -> return param.substringAfter("name=")
                param.startsWith("stream=") -> return param.substringAfter("stream=")
            }
        }
        
        // If no channel parameter found, extract the filename
        val path = try {
            URL(url).path.substringAfterLast('/')
        } catch (e: Exception) {
            "channel"
        }
        
        return path.substringBeforeLast('.').ifEmpty { "channel" }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlist_menu, menu)
        
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                filterPlaylist(newText)
                return true
            }
        })
        
        return true
    }
    
    private fun filterPlaylist(query: String?) {
        if (query.isNullOrBlank()) {
            adapter.updateItems(allPlaylistItems)
            return
        }
        
        val filteredList = allPlaylistItems.filter { item ->
            item.title.contains(query, ignoreCase = true) || 
            (item.group?.contains(query, ignoreCase = true) ?: false)
        }
        
        adapter.updateItems(filteredList)
        
        // Show empty state if no results found
        if (filteredList.isEmpty()) {
            binding.emptyStateView.visibility = View.VISIBLE
        } else {
            binding.emptyStateView.visibility = View.GONE
        }
    }
} 