package com.deathhit.feature.media_item

import com.deathhit.feature.media_item.model.MediaItemLabel
import com.deathhit.feature.media_item.model.toMediaItemLabelDO
import org.junit.Test

class MediaItemLabelTest {
    @Test
    fun mediaItemLabelToMediaItemLabelDOShouldReturnExpectedResult() {
        //Given
        val dashboard = MediaItemLabel.DASHBOARD
        val home = MediaItemLabel.HOME
        val notifications = MediaItemLabel.NOTIFICATIONS
        val recommended = MediaItemLabel.RECOMMENDED

        //When
        val dashboardDO = dashboard.toMediaItemLabelDO()
        val homeDO = home.toMediaItemLabelDO()
        val notificationsDO = notifications.toMediaItemLabelDO()
        val recommendedDO = recommended.toMediaItemLabelDO()

        //Then
        assert(com.deathhit.data.media_item.model.MediaItemLabel.DASHBOARD == dashboardDO)
        assert(com.deathhit.data.media_item.model.MediaItemLabel.HOME == homeDO)
        assert(com.deathhit.data.media_item.model.MediaItemLabel.NOTIFICATIONS == notificationsDO)
        assert(com.deathhit.data.media_item.model.MediaItemLabel.RECOMMENDED == recommendedDO)
    }
}