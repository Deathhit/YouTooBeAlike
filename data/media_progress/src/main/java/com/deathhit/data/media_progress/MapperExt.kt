package com.deathhit.data.media_progress

import com.deathhit.core.database.entity.MediaProgressEntity
import com.deathhit.data.media_progress.model.MediaProgressDO

internal fun MediaProgressDO.toMediaProgressEntity() =
    MediaProgressEntity(isEnded = isEnded, mediaItemId = mediaItemId, position = position)

internal fun MediaProgressEntity.toMediaProgressDO() =
    MediaProgressDO(isEnded = isEnded, mediaItemId = mediaItemId, position = position)