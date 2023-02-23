package com.deathhit.data.media_item

enum class MediaItemSourceType(internal val columnValue: String, internal val remoteKeyLabel: String) {
    DASHBOARD("dashboard", "5c45c0e673d545328060adf114bf81ff"),
    HOME("home", "73c61cde3d515e24bad2f3239c30099f"),
    NOTIFICATIONS("notifications", "7e94ad54ede1488ca0546ae829ceb418"),
    RECOMMENDED("recommended", "84d72def4e626f35cbe3g4350d41210g")
}