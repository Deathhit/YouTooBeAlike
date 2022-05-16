package com.deathhit.video_list_example.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoVO(
    val description: String,
    val sourceUrl: String,
    val subtitle: String,
    val thumbUrl: String,
    val title: String
) : Parcelable