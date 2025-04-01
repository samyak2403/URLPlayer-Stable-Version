package com.samyak.urlplayer.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.samyak.urlplayer.R
import com.samyak.urlplayer.databinding.ItemChannelsBinding
import com.samyak.urlplayer.models.Videos
import com.samyak.urlplayer.utils.ChannelState
import android.graphics.Color
import com.samyak.urlplayer.utils.QRCodeGenerator
import android.app.AlertDialog
import android.content.Context

class ChannelAdapter(
    private val onPlayClick: (Videos) -> Unit,
    private val onEditClick: (Videos) -> Unit,
    private val onError: (String) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val items = mutableListOf<Videos>()
    private var currentState: ChannelState = ChannelState.Loading

    inner class ChannelViewHolder(private val binding: ItemChannelsBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Videos) {
            try {
                with(binding) {
                    // Set text views with null checks
                    tvChannelName.text = item.name.orEmpty()
                    tvChannelLink.text = item.url.orEmpty()

                    // Handle user agent visibility and text
                    if (!item.userAgent.isNullOrEmpty()) {
                        userAgentText.apply {
                            visibility = View.VISIBLE
                            text = "User Agent: ${item.userAgent}"
                        }
                    } else {
                        userAgentText.visibility = View.GONE
                    }
                    
                    // Show PIN indicator if channel has PIN protection
                    if (!item.pin.isNullOrEmpty()) {
                        pinIndicator.visibility = View.VISIBLE
                    } else {
                        pinIndicator.visibility = View.GONE
                    }

                    // Edit button click with position validation
                    editButton.setOnClickListener {
                        handleClick(bindingAdapterPosition) { position ->
                            onEditClick(items[position])
                        }
                    }

                    // Play click (root item click) with position validation
                    root.setOnClickListener {
                        handleClick(bindingAdapterPosition) { position ->
                            onPlayClick(items[position])
                        }
                    }

                    // Generate QR code
                    generateQRCode(item)
                }
            } catch (e: Exception) {
                handleError("Error binding channel: ${e.message}")
            }
        }

        private fun generateQRCode(video: Videos) {
            try {
                val bitmap = QRCodeGenerator.generateQRCode(video)
                binding.qrCodeImage.setImageBitmap(bitmap)
                
                // Set click listener to show larger QR code in dialog
                binding.qrCodeImage.setOnClickListener {
                    showQRCodeDialog(video, it.context)
                }
            } catch (e: Exception) {
                onError("Failed to generate QR code: ${e.message}")
                binding.qrCodeImage.visibility = View.GONE
            }
        }

        private fun showQRCodeDialog(video: Videos, context: Context) {
            try {
                // Create dialog with custom layout
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_qr_code, null)
                val qrImageView = dialogView.findViewById<ImageView>(R.id.dialogQrCodeImage)
                val titleTextView = dialogView.findViewById<TextView>(R.id.dialogQrCodeTitle)
                
                // Generate larger QR code for dialog
                val largeBitmap = QRCodeGenerator.generateQRCode(video, 300, 300)
                qrImageView.setImageBitmap(largeBitmap)
                titleTextView.text = video.name
                
                // Show dialog
                AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton("Close", null)
                    .show()
            } catch (e: Exception) {
                onError("Failed to show QR code dialog: ${e.message}")
            }
        }
    }

    private fun handleClick(position: Int, action: (Int) -> Unit) {
        if (position != RecyclerView.NO_POSITION && position < items.size) {
            action(position)
        } else {
            handleError("Invalid channel position")
        }
    }

    private fun handleError(message: String) {
        currentState = ChannelState.Error(message)
        onError(message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        return try {
            val binding = ItemChannelsBinding.inflate(
                LayoutInflater.from(parent.context), 
                parent, 
                false
            )
            ChannelViewHolder(binding)
        } catch (e: Exception) {
            handleError("Error creating view holder: ${e.message}")
            throw e
        }
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        try {
            if (position in items.indices) {
                holder.bind(items[position])
            } else {
                handleError("Invalid position: $position")
            }
        } catch (e: Exception) {
            handleError("Error binding view holder: ${e.message}")
        }
    }

    override fun getItemCount() = items.size

    fun addItem(newItem: Videos) {
        try {
            items.add(newItem)
            notifyItemInserted(items.lastIndex)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateItems(newItems: List<Videos>) {
        try {
            if (newItems.isEmpty()) {
                items.clear()
                notifyDataSetChanged()
                return
            }

            val diffCallback = ChannelDiffCallback(items, newItems)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            
            items.clear()
            items.addAll(newItems)
            
            diffResult.dispatchUpdatesTo(this)
            currentState = ChannelState.Success("Channel list updated")
        } catch (e: Exception) {
            handleError("Error updating channels: ${e.message}")
        }
    }

    fun removeItem(position: Int) {
        try {
            if (position in 0 until items.size) {
                items.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, items.size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private class ChannelDiffCallback(
        private val oldList: List<Videos>,
        private val newList: List<Videos>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        
        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
            return oldList[oldPos].name == newList[newPos].name
        }
        
        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
            return oldList[oldPos] == newList[newPos]
        }
    }
} 
