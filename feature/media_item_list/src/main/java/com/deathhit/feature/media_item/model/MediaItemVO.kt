package com.deathhit.feature.media_item.model

import android.os.Parcelable
import com.deathhit.data.media_item.model.MediaItemDO
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItemVO(
    val id: String,
    val subtitle: String,
    val thumbUrl: String,
    val title: String
) : Parcelable

fun MediaItemDO.toMediaItemVO() = MediaItemVO(
    id = mediaItemId,
    subtitle = subtitle,
    thumbUrl = thumbUrl,
    title = title
)