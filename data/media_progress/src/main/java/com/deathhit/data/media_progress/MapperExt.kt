package com.deathhit.data.media_progress

import com.deathhit.core.database.model.MediaProgressEntity
import com.deathhit.data.media_progress.model.MediaProgressDO

internal fun MediaProgressDO.toEntity() =
    MediaProgressEntity(isEnded = isEnded, mediaItemId = mediaItemId, position = position)

internal fun MediaProgressEntity.toDO() =
    MediaProgressDO(isEnded = isEnded, mediaItemId = mediaItemId, position = position)