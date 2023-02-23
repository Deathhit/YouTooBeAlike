package com.deathhit.data.media_item

import com.deathhit.core.database.model.MediaItemEntity
import com.deathhit.core.media_api.model.Media

internal fun Media.toEntity() =
    MediaItemEntity(
        id = id,
        description = description,
        sourceUrl = sourceUrl,
        subtitle = subtitle,
        thumbUrl = thumbUrl,
        title = title
    )

internal fun MediaItemEntity.toDO() =
    MediaItemDO(
        id = id,
        description = description,
        sourceUrl = sourceUrl,
        subtitle = subtitle,
        thumbUrl = thumbUrl,
        title = title
    )