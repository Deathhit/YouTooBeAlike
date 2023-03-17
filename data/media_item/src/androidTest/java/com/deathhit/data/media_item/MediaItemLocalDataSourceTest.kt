package com.deathhit.data.media_item

import androidx.paging.ExperimentalPagingApi
import com.deathhit.core.app_database.AppDatabase
import com.deathhit.core.app_database.MediaItemDao
import com.deathhit.core.app_database.entity.MediaItemEntity
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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPagingApi::class)
@HiltAndroidTest
class MediaItemLocalDataSourceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    internal lateinit var appDatabase: AppDatabase

    @Inject
    internal lateinit var mediaItemLocalDataSource: MediaItemLocalDataSource

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
    fun clearByLabel_withDataInDatabase_clearsAllRelatedEntitiesWithGivenLabel() = runTest {
        //Given
        val mediaItemLabel0 = "mediaItemLabel0"
        val mediaItemLabel1 = "MediaItemLabel1"

        val mediaItemEntityList = listOf(
            MediaItemEntity(
                "description",
                mediaItemLabel0,
                "0",
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel0,
                "1",
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel0,
                "2",
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel0,
                "3",
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1,
                "4",
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1,
                "5",
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1,
                "6",
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            ),
            MediaItemEntity(
                "description",
                mediaItemLabel1,
                "7",
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

    //todo test loadPage()
}