package com.deathhit.feature.media_item_list

import com.deathhit.data.media_item.model.MediaItemDO
import com.deathhit.feature.media_item_list.model.toMediaItemVO
import org.junit.Test

class MediaItemVOTest {
    @Test
    fun mediaItemDOToMediaItemVOShouldReturnExpectedResult() {
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
        val mediaItemVO = mediaItemDO.toMediaItemVO()

        //Then
        assert(mediaItemDO.mediaItemId == mediaItemVO.id)
        assert(mediaItemDO.sourceUrl == mediaItemVO.sourceUrl)
        assert(mediaItemDO.subtitle == mediaItemVO.subtitle)
        assert(mediaItemDO.thumbUrl == mediaItemVO.thumbUrl)
        assert(mediaItemDO.title == mediaItemVO.title)
    }
}