package com.deathhit.feature.playback_details

import com.deathhit.data.media_item.model.MediaItemDO
import com.deathhit.feature.playback_details.model.toPlaybackDetailsVO
import org.junit.Test

class PlaybackDetailsVOTest {
    @Test
    fun mediaItemDOToPlaybackDetailsVOShouldReturnExpectedResult() {
        //Given
        val mediaItemDO = MediaItemDO(
            "description",
            "mediaItemId",
            "sourceUrl",
            "subtitle",
            "thumbUrl",
            "title"
        )

        //When
        val playbackDetailsVO = mediaItemDO.toPlaybackDetailsVO()

        //Then
        assert(mediaItemDO.description == playbackDetailsVO.description)
        assert(mediaItemDO.mediaItemId == playbackDetailsVO.mediaItemId)
        assert(mediaItemDO.subtitle == playbackDetailsVO.subtitle)
        assert(mediaItemDO.title == playbackDetailsVO.title)
    }
}