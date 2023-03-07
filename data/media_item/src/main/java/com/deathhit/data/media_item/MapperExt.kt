package com.deathhit.data.media_item

import com.deathhit.core.database.entity.MediaItemEntity
import com.deathhit.core.media_api.model.Media
import com.deathhit.data.media_item.model.MediaItemDO
import com.deathhit.data.media_item.model.MediaItemLabel

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

internal fun MediaItemLabel.toLabel() = when(this) {
    MediaItemLabel.DASHBOARD -> "dashboard_5c45c0e673d545328060adf114bf81ff"
    MediaItemLabel.HOME -> "home_73c61cde3d515e24bad2f3239c30099f"
    MediaItemLabel.NOTIFICATIONS -> "notifications_7e94ad54ede1488ca0546ae829ceb418"
    MediaItemLabel.RECOMMENDED -> "recommended_84d72def4e626f35cbe3g4350d41210g"
}