package com.deathhit.feature.media_item_list.model

import android.os.Parcelable
import com.deathhit.domain.model.MediaItemDO
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItemVO(
    val id: String,
    val sourceUrl: String,
    val subtitle: String,
    val thumbUrl: String,
    val title: String
) : Parcelable

fun MediaItemDO.toMediaItemVO() = MediaItemVO(
    id = mediaItemId,
    sourceUrl = sourceUrl,
    subtitle = subtitle,
    thumbUrl = thumbUrl,
    title = title
)