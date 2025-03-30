package com.samyak.urlplayer.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Videos(
    val name: String? = null,
    val url: String? = null,
    val userAgent: String? = null,
    var pin: String? = null
) : Parcelable