package com.deathhit.feature.media_item.model

import android.os.Parcelable
import com.deathhit.data.media_item.MediaItemDO
import kotlinx.parcelize.Parcelize

@Parcelize
data class ItemVO(
    val sourceUrl: String,
    val subtitle: String,
    val thumbUrl: String,
    val title: String
) : Parcelable

fun MediaItemDO.toItemVO() = ItemVO(sourceUrl, subtitle, thumbUrl, title)