package com.deathhit.feature.media_item.model

import android.os.Parcelable
import com.deathhit.data.media_item.MediaItemDO
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItemVO(
    val id: String,
    val description: String,
    val sourceUrl: String,
    val subtitle: String,
    val thumbUrl: String,
    val title: String
) : Parcelable

fun MediaItemDO.toMediaItemVO() = MediaItemVO(id, description, sourceUrl, subtitle, thumbUrl, title)