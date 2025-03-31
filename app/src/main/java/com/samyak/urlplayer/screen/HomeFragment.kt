package com.samyak.urlplayer.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samyak.urlplayer.AdManage.loadBannerAd
import com.samyak.urlplayer.AdManage.showInterstitialAd
import com.samyak.urlplayer.R
import com.samyak.urlplayer.adapters.ChannelAdapter
import com.samyak.urlplayer.databinding.FragmentHomeBinding
import com.samyak.urlplayer.models.Videos
import com.samyak.urlplayer.utils.ChannelItemDecoration
import com.samyak2403.custom_toast.TastyToast

class HomeFragment : Fragment() {
    private lateinit var adapter: ChannelAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var channelList: MutableList<Videos>
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "HomeFragment"
        private const val UPDATE_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Enable options menu in fragment
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.bannerAdContainer.loadBannerAd()
        // Initialize components
        setupRecyclerView()
    }


    private fun setupRecyclerView() {
        recyclerView = binding.recyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(ChannelItemDecoration(resources.getDimensionPixelSize(R.dimen.item_spacing)))
            setHasFixedSize(true)
            // Enable recycling of views
            recycledViewPool.setMaxRecycledViews(0, 10)
        }

        channelList = mutableListOf()
        initializeAdapter()


    }

    private fun initializeAdapter() {
        adapter = ChannelAdapter(
            onPlayClick = { video ->
                launchPlayerActivity(video)
            },
            onEditClick = { video ->
                launchUpdateActivity(video)
            },
            onError = { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = adapter
    }

    private fun launchPlayerActivity(video: Videos) {
        if (!video.pin.isNullOrEmpty()) {
            // Show PIN verification dialog
            showPinVerificationDialog(video)
        } else {
            // No PIN, proceed directly
            proceedToPlayVideo(video)
        }
    }

    private fun showPinVerificationDialog(video: Videos) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pin_verification, null)
        val pinEditTexts = listOf<EditText>(
            dialogView.findViewById(R.id.etPinVerify1),
            dialogView.findViewById(R.id.etPinVerify2),
            dialogView.findViewById(R.id.etPinVerify3),
            dialogView.findViewById(R.id.etPinVerify4)
        )

        // Create the dialog first so we can reference it in the TextWatcher
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Enter PIN")
            .setView(dialogView)
            .setPositiveButton("Verify") { _, _ ->
                // Collect entered PIN
                val enteredPin = StringBuilder()
                for (pinEditText in pinEditTexts) {
                    enteredPin.append(pinEditText.text.toString())
                }

                // Verify PIN
                if (enteredPin.toString() == video.pin) {
                    proceedToPlayVideo(video)
                } else {
                    TastyToast.show(requireContext(), "Incorrect PIN",TastyToast.Type.WARNING)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Setup PIN input behavior
        for (i in 0 until pinEditTexts.size - 1) {
            val currentEditText = pinEditTexts[i]
            val nextEditText = pinEditTexts[i + 1]

            currentEditText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

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
                    // Auto-submit when all digits are entered
                    val enteredPin = StringBuilder()
                    for (pinEditText in pinEditTexts) {
                        enteredPin.append(pinEditText.text.toString())
                    }
                    
                    if (enteredPin.length == 4) {
                        // Verify PIN
                        if (enteredPin.toString() == video.pin) {
                            dialog.dismiss()
                            proceedToPlayVideo(video)
                        } else {
                            TastyToast.show(requireContext(), "Incorrect PIN",TastyToast.Type.WARNING)
                        }
                    }
                }
            }
        })

        dialog.show()
    }

    private fun proceedToPlayVideo(video: Videos) {
        (context as Activity).showInterstitialAd(customCode = {
            val url = video.url?.lowercase()
            // Check if this is a playlist file using more comprehensive pattern matching
            if (url != null) {
                if (url.endsWith(".m3u") ||
                    url.endsWith(".m3u8") && url.contains("playlist") ||
                    url.contains("playlist") ||
                    url.contains("/list/") ||
                    url.contains("channel")
                ) {
                    startPlaylistActivity(video)
                } else {
                    startPlayerActivity(video)
                }
            }
        })
    }

    @OptIn(UnstableApi::class)
    private fun startPlayerActivity(video: Videos) {
        Intent(requireContext(), PlayerActivity::class.java).also { intent ->
            intent.putExtra("URL", video.url)
            intent.putExtra("USER_AGENT", video.userAgent)
            intent.putExtra("TITLE", video.name)
            startActivity(intent)
        }
    }

    private fun startPlaylistActivity(video: Videos) {
        Intent(requireContext(), PlaylistActivity::class.java).also { intent ->
            intent.putExtra("URL", video.url)
            intent.putExtra("USER_AGENT", video.userAgent)
            intent.putExtra("TITLE", video.name)
            startActivity(intent)
        }
    }

    private fun launchUpdateActivity(video: Videos) {
        startUpdateActivity(video)
    }

    private fun startUpdateActivity(video: Videos) {
        Intent(requireContext(), UpdateActivity::class.java).also { intent ->
            intent.putExtra("TITLE", video.name)
            intent.putExtra("URL", video.url)
            intent.putExtra("USER_AGENT", video.userAgent)
            startActivityForResult(intent, UPDATE_REQUEST_CODE)
        }
    }

    private fun loadSavedChannels() {
        try {
            val sharedPreferences = requireActivity().getSharedPreferences("M3U8Links", Context.MODE_PRIVATE)
            val links = sharedPreferences.getStringSet("links", mutableSetOf()) ?: mutableSetOf()

            // Process channels in background
            Thread {
                val newChannelList = mutableListOf<Videos>()

                links.forEach { link ->
                    link.split("###").let { parts ->
                        when {
                            parts.size >= 6 -> {
                                newChannelList.add(
                                    Videos(
                                        name = parts[0],
                                        url = parts[1],
                                        userAgent = if (parts[3].isNotEmpty()) parts[3] else null,
                                        pin = parts[5]
                                    )
                                )
                            }

                            parts.size == 5 -> {
                                newChannelList.add(
                                    Videos(
                                        name = parts[0],
                                        url = parts[1],
                                        userAgent = if (parts[3].isNotEmpty()) parts[3] else null,
                                        pin = ""
                                    )
                                )
                            }

                            parts.size == 4 -> {
                                newChannelList.add(
                                    Videos(
                                        name = parts[0],
                                        url = parts[1],
                                        userAgent = parts[3],
                                        pin = ""
                                    )
                                )
                            }

                            parts.size == 3 -> {
                                newChannelList.add(
                                    Videos(
                                        name = parts[0],
                                        url = parts[1],
                                        userAgent = null,
                                        pin = ""
                                    )
                                )
                            }

                            parts.size == 2 -> {
                                newChannelList.add(
                                    Videos(
                                        name = parts[0],
                                        url = parts[1],
                                        userAgent = null,
                                        pin = ""
                                    )
                                )
                            }
                        }
                    }
                }

                // Sort channels by name
                newChannelList.sortBy { it.name }

                // Update UI on main thread
                requireActivity().runOnUiThread {
                    channelList.clear()
                    channelList.addAll(newChannelList)
                    adapter.updateItems(channelList)
                    updateEmptyState()
                }
            }.start()

        } catch (e: Exception) {
            TastyToast.show(requireContext(), getString(R.string.error_loading_channels), TastyToast.Type.ERROR)
        }
    }

    private fun updateEmptyState() {
        if (channelList.isEmpty()) {
            // Show empty state view
            binding.root.findViewById<View>(R.id.empty_state_view).visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            // Show recycler view
            binding.root.findViewById<View>(R.id.empty_state_view).visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadSavedChannels()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadSavedChannels()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_pin_management -> {
                launchPinManagement()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun launchPinManagement() {
        startActivity(Intent(requireContext(), PinManagementActivity::class.java))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 