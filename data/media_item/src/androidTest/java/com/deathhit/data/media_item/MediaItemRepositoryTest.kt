package com.deathhit.data.media_item

import com.deathhit.core.app_database.AppDatabase
import com.deathhit.core.app_database.MediaItemDao
import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.data.media_item.model.MediaItemLabel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class MediaItemRepositoryTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    internal lateinit var appDatabase: AppDatabase

    @Inject
    internal lateinit var mediaItemRepository: MediaItemRepository

    private lateinit var mediaItemDao: MediaItemDao

    @Before
    fun before() {
        hiltRule.inject()

        mediaItemDao = appDatabase.mediaItemDao()
    }

    @After
    fun after() {
        appDatabase.close()
    }

    @Test
    fun clearByLabelShouldClearOnlyAllTheMediaItemsWithTheGivenLabel() = runTest {
        //Given
        val mediaItemLabelList0 = MediaItemLabel.values()
        val mediaItemLabel0 = mediaItemLabelList0[Random.nextInt(mediaItemLabelList0.size)]
        val mediaItemLabelList1 = mediaItemLabelList0.filter { it != mediaItemLabel0 }
        val mediaItemLabel1 = mediaItemLabelList1[Random.nextInt(mediaItemLabelList1.size)]

        val mediaItemEntityList = listOf(
            MediaItemEntity(
                "description",
                mediaItemLabel0.toLabelString(),
                "0",
                0,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel0.toLabelString(),
                "1",
                1,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel0.toLabelString(),
                "2",
                2,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel0.toLabelString(),
                "3",
                3,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1.toLabelString(),
                "4",
                4,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1.toLabelString(),
                "5",
                5,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1.toLabelString(),
                "6",
                6,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1.toLabelString(),
                "7",
                7,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
        )

        mediaItemDao.upsert(mediaItemEntityList)

        //When
        mediaItemRepository.clearByLabel(mediaItemLabel0)

        //Then
        mediaItemEntityList.filter { it.label == mediaItemLabel0.toLabelString() }.forEach {
            assert(mediaItemRepository.getMediaItemFlowById(it.mediaItemId).first() == null)
        }

        mediaItemEntityList.filter { it.label == mediaItemLabel1.toLabelString() }.forEach {
            assert(mediaItemRepository.getMediaItemFlowById(it.mediaItemId).first() != null)
        }
    }

    @Test
    fun getMediaItemFlowByIdShouldReturnExpectedResult() = runTest {
        //Given
        val mediaItemId0 = "0"
        val mediaItemId1 = "1"
        val mediaItemId2 = "2"

        val mediaItemEntity0 = MediaItemEntity(
            "description",
            "label",
            mediaItemId0,
            0,
            "sourceUrl",
            "subtitle",
            "thumbUrl",
            "title"
        )
        val mediaItemEntity1 = MediaItemEntity(
            "description",
            "label",
            mediaItemId1,
            1,
            "sourceUrl",
            "subtitle",
            "thumbUrl",
            "title"
        )

        mediaItemDao.upsert(listOf(mediaItemEntity0, mediaItemEntity1))

        //When
        val mediaItemDOFlow0 = mediaItemRepository.getMediaItemFlowById(mediaItemId0)
        val mediaItemDOFlow1 = mediaItemRepository.getMediaItemFlowById(mediaItemId1)
        val mediaItemDOFlow2 = mediaItemRepository.getMediaItemFlowById(mediaItemId2)

        //Then
        assert(mediaItemDOFlow0.first() == mediaItemEntity0.toMediaItemDO())
        assert(mediaItemDOFlow1.first() == mediaItemEntity1.toMediaItemDO())
        assert(mediaItemDOFlow2.first() == null)
    }
}