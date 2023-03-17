package com.deathhit.data.media_item

import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.core.media_api.model.Media
import com.deathhit.data.media_item.model.MediaItemLabel
import org.junit.Test

class MapperExtTest {
    @Test
    fun mediaToMediaItemEntityShouldReturnExpectedResult() {
        //Given
        val label = "label"
        val media = Media("id", "description", "sourceUrl", "subtitle", "thumbUrl", "title")

        //When
        val mediaItemEntity = media.toMediaItemEntity(label)

        //Then
        assert(label == mediaItemEntity.label)
        assert(media.id == mediaItemEntity.mediaItemId)
        assert(media.description == mediaItemEntity.description)
        assert(media.sourceUrl == mediaItemEntity.sourceUrl)
        assert(media.subtitle == mediaItemEntity.subtitle)
        assert(media.thumbUrl == mediaItemEntity.thumbUrl)
        assert(media.title == mediaItemEntity.title)
    }

    @Test
    fun mediaItemEntityToMediaItemDOShouldReturnExpectedResult() {
        //Given
        val mediaItemEntity = MediaItemEntity(
            "description",
            "label",
            "mediaItemId",
            "sourceUrl",
            "subtitle",
            "thumbUrl",
            "title"
        )

        //When
        val mediaItemDO = mediaItemEntity.toMediaItemDO()

        //Then
        assert(mediaItemEntity.description == mediaItemDO.description)
        assert(mediaItemEntity.mediaItemId == mediaItemDO.mediaItemId)
        assert(mediaItemEntity.sourceUrl == mediaItemDO.sourceUrl)
        assert(mediaItemEntity.subtitle == mediaItemDO.subtitle)
        assert(mediaItemEntity.thumbUrl == mediaItemDO.thumbUrl)
        assert(mediaItemEntity.title == mediaItemDO.title)
    }

    @Test
    fun mediaItemLabelToLabelStringShouldReturnUniqueResult() {
        //Given
        val dashboard = MediaItemLabel.DASHBOARD
        val home = MediaItemLabel.HOME
        val notifications = MediaItemLabel.NOTIFICATIONS
        val recommended = MediaItemLabel.RECOMMENDED

        //When
        val dashboardString = dashboard.toLabelString()
        val homeString = home.toLabelString()
        val notificationsString = notifications.toLabelString()
        val recommendedString = recommended.toLabelString()

        //Then
        assert(
            setOf(
                dashboardString,
                homeString,
                notificationsString,
                recommendedString
            ).size == MediaItemLabel.values().size
        )
    }
}