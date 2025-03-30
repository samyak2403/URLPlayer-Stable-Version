package com.samyak.urlplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.samyak.urlplayer.R
import com.samyak.urlplayer.models.PlaylistItem

class PlaylistItemAdapter(
    private val onItemClick: (PlaylistItem) -> Unit
) : RecyclerView.Adapter<PlaylistItemAdapter.PlaylistViewHolder>() {

    private val items = mutableListOf<PlaylistItem>()

    fun updateItems(newItems: List<PlaylistItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.playlistItemTitle)
        private val groupTextView: TextView = itemView.findViewById(R.id.playlistItemGroup)
        private val logoImageView: ImageView = itemView.findViewById(R.id.playlistItemLogo)

        fun bind(item: PlaylistItem) {
            titleTextView.text = item.title
            
            if (item.group.isNullOrEmpty()) {
                groupTextView.visibility = View.GONE
            } else {
                groupTextView.text = item.group
                groupTextView.visibility = View.VISIBLE
            }

            // Load logo if available
            if (!item.logoUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(item.logoUrl)
                    .placeholder(R.drawable.ic_playlist)
                    .error(R.drawable.ic_playlist)
                    .into(logoImageView)
                logoImageView.visibility = View.VISIBLE
            } else {
                logoImageView.setImageResource(R.drawable.ic_playlist)
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
} 