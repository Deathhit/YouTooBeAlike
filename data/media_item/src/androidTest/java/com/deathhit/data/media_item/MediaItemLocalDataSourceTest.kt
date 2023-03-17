package com.deathhit.data.media_item

import com.deathhit.core.app_database.AppDatabase
import com.deathhit.core.app_database.MediaItemDao
import com.deathhit.core.app_database.RemoteKeysDao
import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.core.app_database.entity.RemoteKeysEntity
import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
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
class MediaItemLocalDataSourceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    internal lateinit var appDatabase: AppDatabase

    @Inject
    internal lateinit var mediaItemLocalDataSource: MediaItemLocalDataSource

    private lateinit var mediaItemDao: MediaItemDao
    private lateinit var remoteKeysDao: RemoteKeysDao

    @Before
    fun before() {
        hiltRule.inject()

        mediaItemDao = appDatabase.mediaItemDao()
        remoteKeysDao = appDatabase.remoteKeysDao()
    }

    @After
    fun after() {
        appDatabase.close()
    }

    @Test
    fun clearByLabel_withDataInDatabase_clearsAllRelatedEntitiesWithGivenLabel() = runTest {
        //Given
        val mediaItemLabel0 = "mediaItemLabel0"
        val mediaItemLabel1 = "MediaItemLabel1"

        val mediaItemEntityList = listOf(
            MediaItemEntity(
                "description",
                mediaItemLabel0,
                "0",
                0,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel0,
                "1",
                1,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel0,
                "2",
                2,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel0,
                "3",
                3,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1,
                "4",
                4,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1,
                "5",
                5,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1,
                "6",
                6,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1,
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
        mediaItemLocalDataSource.clearByLabel(mediaItemLabel0)

        //Then
        mediaItemEntityList.filter { it.label == mediaItemLabel0 }.forEach {
            assert(mediaItemLocalDataSource.getMediaItemFlowById(it.mediaItemId).first() == null)
        }

        mediaItemEntityList.filter { it.label == mediaItemLabel1 }.forEach {
            assert(mediaItemLocalDataSource.getMediaItemFlowById(it.mediaItemId).first() != null)
        }
    }

    @Test
    fun getMediaItemFlowById_withoutDataInDatabase_returnNull() = runTest {
        //Given
        val mediaItemId = "mediaItemId"

        //When
        val result = mediaItemLocalDataSource.getMediaItemFlowById(mediaItemId).first()

        //Then
        assert(result == null)
    }

    @Test
    fun getMediaItemFlowById_withDataInDatabase_returnEntity() = runTest {
        //Given
        val mediaItemId = "mediaItemId"

        val mediaItemEntity = MediaItemEntity(
            "description",
            "label",
            mediaItemId,
            0,
            "sourceUrl",
            "subtitle",
            "thumbUrl",
            "title"
        )
        mediaItemDao.upsert(listOf(mediaItemEntity))

        //When
        val result = mediaItemLocalDataSource.getMediaItemFlowById(mediaItemId).first()

        //Then
        assert(result == mediaItemEntity)
    }

    @Test
    fun getRemoteKeysWithLabelAndMediaItemId_withDataInDatabase_returnEntity() = runTest {
        //Given
        val label = "label"
        val mediaItemId = "mediaItemId"

        val remoteKeysEntity =
            RemoteKeysEntity(label, mediaItemId, Random.nextInt(), Random.nextInt())

        remoteKeysDao.upsert(listOf(remoteKeysEntity))

        //When
        val result = mediaItemLocalDataSource.getRemoteKeysByLabelAndMediaItemId(label, mediaItemId)

        //Then
        assert(result == remoteKeysEntity)
    }

    @Test
    fun insertMediaItemPage_refresh_clearOldDataAndInsertNewData() = runTest {
        //Given
        val label = "label"

        val oldMediaItemEntityList = listOf(
            MediaItemEntity(
                "description",
                label,
                "0",
                -1,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                label,
                "1",
                -1,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                label,
                "2",
                -1,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                label,
                "3",
                -1,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            )
        )

        val newMediaItemEntityList = listOf(
            MediaItemEntity(
                "description",
                "",
                "4",
                -1,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                "",
                "5",
                -1,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                "",
                "6",
                -1,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                "",
                "7",
                -1,
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            )
        )

        mediaItemDao.upsert(oldMediaItemEntityList)

        //When
        mediaItemLocalDataSource.insertMediaItemPage(
            isFirstPage = true,
            isRefresh = true,
            label = label,
            mediaItems = newMediaItemEntityList,
            page = 0,
            pageSize = newMediaItemEntityList.size
        )

        //Then
        oldMediaItemEntityList.forEach {
            assert(mediaItemDao.getFlowById(it.mediaItemId).first() == null)
        }

        newMediaItemEntityList.forEach {
            val insertedMediaItem = mediaItemDao.getFlowById(it.mediaItemId).first()

            assert(
                insertedMediaItem != null && it.copy(
                    label = insertedMediaItem.label,
                    remoteOrder = insertedMediaItem.remoteOrder
                ) == insertedMediaItem
            )
        }
    }
}