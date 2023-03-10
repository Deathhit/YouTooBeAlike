package com.deathhit.feature.playback_details.model

import com.deathhit.data.media_item.model.MediaItemDO

data class PlaybackDetailsVO(
    val description: String,
    val mediaItemId: String,
    val subtitle: String,
    val title: String
)

fun MediaItemDO.toPlaybackDetailsVO() = PlaybackDetailsVO(
    description = description,
    mediaItemId = mediaItemId,
    subtitle = subtitle,
    title = title
)
