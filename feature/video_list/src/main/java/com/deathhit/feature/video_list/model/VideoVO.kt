package com.deathhit.feature.video_list.model

import com.deathhit.data.media_item.MediaItemDO

data class VideoVO(
    val sourceUrl: String,
    val subtitle: String,
    val thumbUrl: String,
    val title: String
)

fun MediaItemDO.toVideoVO() = VideoVO(sourceUrl, subtitle, thumbUrl, title)