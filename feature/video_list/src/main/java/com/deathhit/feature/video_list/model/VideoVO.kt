package com.deathhit.feature.video_list.model

import android.os.Parcelable
import com.deathhit.data.media_item.MediaItemDO
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoVO(
    val sourceUrl: String,
    val subtitle: String,
    val thumbUrl: String,
    val title: String
) : Parcelable

fun MediaItemDO.toVideoVO() = VideoVO(sourceUrl, subtitle, thumbUrl, title)