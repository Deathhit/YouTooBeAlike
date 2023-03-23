package com.deathhit.feature.media_item_list.enum_type

enum class MediaItemLabel {
    DASHBOARD,
    HOME,
    NOTIFICATIONS,
    RECOMMENDED
}

fun MediaItemLabel.toMediaItemLabelDO() = when(this) {
    MediaItemLabel.DASHBOARD -> com.deathhit.domain.enum_type.MediaItemLabel.DASHBOARD
    MediaItemLabel.HOME -> com.deathhit.domain.enum_type.MediaItemLabel.HOME
    MediaItemLabel.NOTIFICATIONS -> com.deathhit.domain.enum_type.MediaItemLabel.NOTIFICATIONS
    MediaItemLabel.RECOMMENDED -> com.deathhit.domain.enum_type.MediaItemLabel.RECOMMENDED
}