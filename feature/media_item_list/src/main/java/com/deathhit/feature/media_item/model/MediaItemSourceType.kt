package com.deathhit.feature.media_item.model

enum class MediaItemSourceType {
    DASHBOARD,
    HOME,
    NOTIFICATIONS,
    RECOMMENDED
}

fun MediaItemSourceType.toDO() = when(this) {
    MediaItemSourceType.DASHBOARD -> com.deathhit.data.media_item.MediaItemSourceType.DASHBOARD
    MediaItemSourceType.HOME -> com.deathhit.data.media_item.MediaItemSourceType.HOME
    MediaItemSourceType.NOTIFICATIONS -> com.deathhit.data.media_item.MediaItemSourceType.NOTIFICATIONS
    MediaItemSourceType.RECOMMENDED -> com.deathhit.data.media_item.MediaItemSourceType.RECOMMENDED
}