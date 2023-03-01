package com.deathhit.data.media_item

import com.deathhit.core.database.model.MediaItemEntity
import com.deathhit.core.media_api.model.Media
import com.deathhit.data.media_item.model.MediaItemDO

internal fun Media.toEntity(mediaItemSource: String) =
    MediaItemEntity(
        description = description,
        mediaItemId = id,
        mediaItemSourceType = mediaItemSource,
        sourceUrl = sourceUrl,
        subtitle = subtitle,
        thumbUrl = thumbUrl,
        title = title
    )

internal fun MediaItemEntity.toDO() =
    MediaItemDO(
        description = description,
        mediaItemId = mediaItemId,
        sourceUrl = sourceUrl,
        subtitle = subtitle,
        thumbUrl = thumbUrl,
        title = title
    )