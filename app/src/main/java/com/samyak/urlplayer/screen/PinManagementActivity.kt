package com.samyak.urlplayer.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samyak.urlplayer.R
import com.samyak.urlplayer.databinding.ActivityPinManagementBinding
import com.samyak.urlplayer.databinding.DialogChangePinBinding
import com.samyak.urlplayer.databinding.DialogForgotPinBinding
import com.samyak.urlplayer.databinding.DialogViewPinBinding
import com.samyak.urlplayer.databinding.ItemPinChannelBinding
import com.samyak.urlplayer.models.Videos
import com.samyak2403.custom_toast.TastyToast
import android.content.ActivityNotFoundException

class PinManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPinManagementBinding
    private lateinit var adapter: PinChannelAdapter
    private val pinProtectedChannels = mutableListOf<Videos>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        initializeViews()
        loadPinProtectedChannels()

    }


    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "PIN Management"
        }
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
    }

    private fun initializeViews() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PinChannelAdapter(
            onViewPin = { channel -> showViewPinDialog(channel) },
            onChangePin = { channel -> showChangePinDialog(channel) },
            onForgotPin = { channel -> showForgotPinDialog(channel) }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun loadPinProtectedChannels() {
        try {
            val sharedPreferences = getSharedPreferences("M3U8Links", MODE_PRIVATE)
            val links = sharedPreferences.getStringSet("links", mutableSetOf()) ?: mutableSetOf()

            pinProtectedChannels.clear()

            links.forEach { link ->
                val parts = link.split("###")
                if (parts.size >= 6 && parts[5].isNotEmpty()) {
                    pinProtectedChannels.add(
                        Videos(
                            name = parts[0],
                            url = parts[1],
                            userAgent = if (parts[3].isNotEmpty()) parts[3] else null,
                            pin = parts[5]
                        )
                    )
                }
            }

            // Sort channels by name
            pinProtectedChannels.sortBy { it.name }

            // Update UI
            adapter.updateItems(pinProtectedChannels)
            updateEmptyState()
        } catch (e: Exception) {
            TastyToast.show(this, "Error loading PIN protected channels", TastyToast.Type.ERROR)
        }
    }

    private fun updateEmptyState() {
        if (pinProtectedChannels.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyStateContainer.visibility = View.GONE
        }
    }

    private fun showViewPinDialog(channel: Videos) {
        val dialogBinding = DialogViewPinBinding.inflate(layoutInflater)
        
        // Generate and show encrypted representation
        val encryptedPin = encryptPin(channel.pin ?: "")
        dialogBinding.pinEncryptedView.text = encryptedPin
        
        // Set up copy button
        dialogBinding.pinCopyButton.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("PIN", encryptedPin)
            clipboardManager.setPrimaryClip(clipData)
            TastyToast.show(this, "Encrypted PIN copied to clipboard", TastyToast.Type.SUCCESS)
        }
        
        // Set up PIN Pro card click listener - find it in the dialog's view hierarchy
        val pinProRoot = dialogBinding.root.findViewById<CardView>(R.id.pinProRoot)
        val installButton = dialogBinding.root.findViewById<Button>(R.id.install_button)
        
        val clickListener = View.OnClickListener {
            openPinProInPlayStore()
        }
        
        // Set click listeners for both the card and the button
        pinProRoot?.setOnClickListener(clickListener)
        installButton?.setOnClickListener(clickListener)

        AlertDialog.Builder(this)
            .setTitle("Encrypted PIN for ${channel.name}")
            .setView(dialogBinding.root)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun installPinProApp() {
        // This method is no longer needed as we're handling the PIN Pro installation
        // directly in the showViewPinDialog method
    }

    // Helper method to open PIN Pro in Play Store
    private fun openPinProInPlayStore() {
        try {
            // Open Play Store to PIN Pro app
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=com.samyak.pinpro")
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // If Play Store app is not available, open in browser
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=com.samyak.pinpro")
            }
            startActivity(intent)
        }
    }

    // Function to encrypt PIN into alphanumeric representation
    private fun encryptPin(pin: String): String {
        if (pin.isEmpty()) return ""

        // Simple encryption: convert each digit to a letter/number combination
        val sb = StringBuilder()
        val random =
            java.util.Random(pin.hashCode().toLong()) // Use PIN as seed for consistent results

        for (digit in pin) {
            if (digit.isDigit()) {
                val num = digit.toString().toInt()
                // Generate a random letter (A-Z) based on the digit
                val letter = ('A' + ((num * 3 + random.nextInt(5)) % 26)).toChar()
                // Generate a different number based on the digit
                val newNum = (num + 7) % 10
                // Combine them in a pattern
                sb.append("$letter$newNum")
            }
        }

        return sb.toString()
    }

    private fun showChangePinDialog(channel: Videos) {
        val dialogBinding = DialogChangePinBinding.inflate(layoutInflater)

        val currentPinEditTexts = listOf(
            dialogBinding.etCurrentPin1,
            dialogBinding.etCurrentPin2,
            dialogBinding.etCurrentPin3,
            dialogBinding.etCurrentPin4
        )

        val newPinEditTexts = listOf(
            dialogBinding.etNewPin1,
            dialogBinding.etNewPin2,
            dialogBinding.etNewPin3,
            dialogBinding.etNewPin4
        )

        // Setup PIN input behavior for current PIN
        setupPinInputBehavior(currentPinEditTexts)

        // Setup PIN input behavior for new PIN
        setupPinInputBehavior(newPinEditTexts)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Change PIN for ${channel.name}")
            .setView(dialogBinding.root)
            .setPositiveButton("Change", null) // We'll set the listener later
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val currentPin = getPinFromInputs(currentPinEditTexts)
                val newPin = getPinFromInputs(newPinEditTexts)

                when {
                    currentPin.length != 4 -> {
                        TastyToast.show(
                            this,
                            "Please enter your current 4-digit PIN",
                            TastyToast.Type.WARNING
                        )
                    }

                    currentPin != channel.pin -> {
                        TastyToast.show(this, "Current PIN is incorrect", TastyToast.Type.ERROR)
                    }

                    newPin.length != 4 -> {
                        TastyToast.show(
                            this,
                            "Please enter a new 4-digit PIN",
                            TastyToast.Type.WARNING
                        )
                    }

                    else -> {
                        updateChannelPin(channel, newPin)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showForgotPinDialog(channel: Videos) {
        val dialogBinding = DialogForgotPinBinding.inflate(layoutInflater)
        val newPinEditTexts = listOf(
            dialogBinding.etNewPin1,
            dialogBinding.etNewPin2,
            dialogBinding.etNewPin3,
            dialogBinding.etNewPin4
        )

        // Setup PIN input behavior
        setupPinInputBehavior(newPinEditTexts)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Reset PIN for ${channel.name}")
            .setMessage("Warning: This will reset the PIN without verification.")
            .setView(dialogBinding.root)
            .setPositiveButton("Reset", null) // We'll set the listener later
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val newPin = getPinFromInputs(newPinEditTexts)

                if (newPin.length != 4) {
                    TastyToast.show(this, "Please enter a new 4-digit PIN", TastyToast.Type.WARNING)
                } else {
                    updateChannelPin(channel, newPin)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun setupPinInputBehavior(pinEditTexts: List<EditText>) {
        // Auto-advance to next PIN field when a digit is entered
        for (i in 0 until pinEditTexts.size - 1) {
            val currentEditText = pinEditTexts[i]
            val nextEditText = pinEditTexts[i + 1]

            currentEditText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

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

    private fun getPinFromInputs(pinEditTexts: List<EditText>): String {
        val pinBuilder = StringBuilder()
        for (pinEditText in pinEditTexts) {
            pinBuilder.append(pinEditText.text.toString())
        }
        return pinBuilder.toString()
    }

    private fun updateChannelPin(channel: Videos, newPin: String) {
        try {
            val sharedPreferences = getSharedPreferences("M3U8Links", MODE_PRIVATE)
            val currentLinks =
                sharedPreferences.getStringSet("links", mutableSetOf()) ?: mutableSetOf()
            val newLinks = mutableSetOf<String>()

            // Update the channel with the new PIN
            currentLinks.forEach { link ->
                val parts = link.split("###")
                if (parts.size >= 6 && parts[0] == channel.name) {
                    // This is the channel to update
                    val updatedLink = buildString {
                        append("${parts[0]}###${parts[1]}###${parts[2]}")
                        if (parts.size > 3) {
                            append("###${parts[3]}")
                        } else {
                            append("###")
                        }
                        if (parts.size > 4) {
                            append("###${parts[4]}")
                        } else {
                            append("###")
                        }
                        append("###$newPin")
                    }
                    newLinks.add(updatedLink)
                } else {
                    // Keep other channels unchanged
                    newLinks.add(link)
                }
            }

            // Save updated links
            sharedPreferences.edit().apply {
                putStringSet("links", newLinks)
                apply()
            }

            // Update local data and UI
            // Instead of modifying the existing object, create a new one and replace it
            val index = pinProtectedChannels.indexOfFirst { it.name == channel.name }
            if (index != -1) {
                val updatedChannel = Videos(
                    name = channel.name,
                    url = channel.url,
                    userAgent = channel.userAgent,
                    pin = newPin
                )
                pinProtectedChannels[index] = updatedChannel
                adapter.notifyItemChanged(index)
            }

            TastyToast.show(this, "PIN updated successfully", TastyToast.Type.SUCCESS)
        } catch (e: Exception) {
            TastyToast.show(this, "Error updating PIN: ${e.message}", TastyToast.Type.ERROR)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Adapter for PIN protected channels
    inner class PinChannelAdapter(
        private val onViewPin: (Videos) -> Unit,
        private val onChangePin: (Videos) -> Unit,
        private val onForgotPin: (Videos) -> Unit
    ) : RecyclerView.Adapter<PinChannelAdapter.ViewHolder>() {

        private val items = mutableListOf<Videos>()

        inner class ViewHolder(private val binding: ItemPinChannelBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(channel: Videos) {
                binding.channelName.text = channel.name

                binding.viewPinButton.setOnClickListener { onViewPin(channel) }
                binding.changePinButton.setOnClickListener { onChangePin(channel) }
                binding.forgotPinButton.setOnClickListener { onForgotPin(channel) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPinChannelBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        fun updateItems(newItems: List<Videos>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }
    }
} 