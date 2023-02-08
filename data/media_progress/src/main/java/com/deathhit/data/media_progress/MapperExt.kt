package com.deathhit.data.media_progress

import com.deathhit.core.database.model.MediaProgressEntity

internal fun MediaProgressDO.toEntity() =
    MediaProgressEntity(position = position, sourceUrl = sourceUrl)

internal fun MediaProgressEntity.toDO() =
    MediaProgressDO(position = position, sourceUrl = sourceUrl)