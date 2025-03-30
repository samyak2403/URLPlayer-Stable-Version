package com.samyak.urlplayer.models

data class PlaylistItem(
    val title: String,
    val url: String,
    val logoUrl: String? = null,
    val group: String? = null,
    val category: String? = null,
    val isActive: Boolean = true
)