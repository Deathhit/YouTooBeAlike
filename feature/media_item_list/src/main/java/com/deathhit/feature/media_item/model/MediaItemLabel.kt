package com.deathhit.feature.media_item.model

enum class MediaItemLabel {
    DASHBOARD,
    HOME,
    NOTIFICATIONS,
    RECOMMENDED
}

fun MediaItemLabel.toMediaItemLabelDO() = when(this) {
    MediaItemLabel.DASHBOARD -> com.deathhit.data.media_item.model.MediaItemLabel.DASHBOARD
    MediaItemLabel.HOME -> com.deathhit.data.media_item.model.MediaItemLabel.HOME
    MediaItemLabel.NOTIFICATIONS -> com.deathhit.data.media_item.model.MediaItemLabel.NOTIFICATIONS
    MediaItemLabel.RECOMMENDED -> com.deathhit.data.media_item.model.MediaItemLabel.RECOMMENDED
}