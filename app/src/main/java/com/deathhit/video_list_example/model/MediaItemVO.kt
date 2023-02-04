package com.deathhit.video_list_example.model

import android.os.Parcelable
import com.deathhit.data.media_item.MediaItemDO
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItemVO(
    val description: String,
    val sourceUrl: String,
    val subtitle: String,
    val thumbUrl: String,
    val title: String
) : Parcelable

fun MediaItemDO.toVO() = MediaItemVO(description, sourceUrl, subtitle, thumbUrl, title)