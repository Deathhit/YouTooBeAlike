package com.deathhit.data.media_progress

import com.deathhit.core.app_database.entity.MediaProgressEntity
import com.deathhit.domain.model.MediaProgressDO
import org.junit.Test
import kotlin.random.Random

class MapperExtTest {
    @Test
    fun mediaProgressDOToMediaProgressEntityShouldReturnExpectedResult() {
        //Given
        val mediaProgressDO = MediaProgressDO(
            isEnded = Random.nextBoolean(),
            mediaItemId = "mediaItemId",
            position = Random.nextLong()
        )

        //When
        val mediaProgressEntity = mediaProgressDO.toMediaProgressEntity()

        //Then
        assert(mediaProgressDO.isEnded == mediaProgressEntity.isEnded)
        assert(mediaProgressDO.mediaItemId == mediaProgressEntity.mediaItemId)
        assert(mediaProgressDO.position == mediaProgressEntity.position)
    }

    @Test
    fun mediaProgressEntityToMediaProgressDOShouldReturnExpectedResult() {
        //Given
        val mediaProgressEntity = MediaProgressEntity(
            isEnded = Random.nextBoolean(),
            mediaItemId = "mediaItemId",
            position = Random.nextLong()
        )

        //When
        val mediaProgressDO = mediaProgressEntity.toMediaProgressDO()

        //Then
        assert(mediaProgressEntity.isEnded == mediaProgressDO.isEnded)
        assert(mediaProgressEntity.mediaItemId == mediaProgressDO.mediaItemId)
        assert(mediaProgressEntity.position == mediaProgressDO.position)
    }
}