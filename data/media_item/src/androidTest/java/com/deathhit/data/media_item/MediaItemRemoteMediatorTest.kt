package com.deathhit.data.media_item

import androidx.paging.*
import com.deathhit.core.database.AppDatabase
import com.deathhit.core.database.entity.MediaItemEntity
import com.deathhit.core.media_api.model.Media
import com.deathhit.data.media_item.config.FakeMediaApiService
import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
import com.deathhit.data.media_item.data_source.MediaItemRemoteDataSource
import com.deathhit.data.media_item.model.MediaItemLabel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPagingApi::class)
@HiltAndroidTest
class MediaItemRemoteMediatorTest {
    companion object {
        private const val PAGE_SIZE = 25
    }

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    internal lateinit var appDatabase: AppDatabase

    @Inject
    internal lateinit var fakeMediaApiService: FakeMediaApiService

    @Inject
    internal lateinit var mediaItemLocalDataSource: MediaItemLocalDataSource

    @Inject
    internal lateinit var mediaItemRemoteDataSource: MediaItemRemoteDataSource

    private lateinit var remoteMediator: MediaItemRemoteMediator

    @Before
    fun before() {
        hiltRule.inject()

        remoteMediator = MediaItemRemoteMediator(
            null,
            mediaItemLocalDataSource,
            mediaItemRemoteDataSource,
            MediaItemLabel.DASHBOARD,
            null
        )
    }

    @After
    fun after() {
        appDatabase.close()
    }

    @Test
    fun refreshLoadReturnsErrorResultWhenErrorOccurs() = runTest {
        //Given
        fakeMediaApiService.isThrowingError = true

        //When
        val pagingState = PagingState<Int, MediaItemEntity>(
            listOf(),
            null,
            PagingConfig(PAGE_SIZE),
            0
        )

        val result = remoteMediator.load(LoadType.REFRESH, pagingState)

        //Then
        Assert.assertTrue(result is RemoteMediator.MediatorResult.Error)
    }

    @Test
    fun refreshLoadReturnsSuccessResultWhenMoreDataIsPresent() = runTest {
        //Given
        fakeMediaApiService.mutableMediaList.addAll(
            listOf(
                Media(
                    "0",
                    "description",
                    "sourceUrl",
                    "subtitle",
                    "thumbUrl",
                    "title"
                )
            )
        )

        //When
        val pagingState = PagingState<Int, MediaItemEntity>(
            listOf(),
            null,
            PagingConfig(PAGE_SIZE),
            0
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)

        //Then
        Assert.assertTrue(result is RemoteMediator.MediatorResult.Success)
        Assert.assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun refreshLoadSuccessAndEndOfPaginationWhenNoMoreData() = runTest {
        //Given

        //When
        val pagingState = PagingState<Int, MediaItemEntity>(
            listOf(),
            null,
            PagingConfig(PAGE_SIZE),
            0
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)

        //Then
        Assert.assertTrue(result is RemoteMediator.MediatorResult.Success)
        Assert.assertTrue((result is RemoteMediator.MediatorResult.Success) && result.endOfPaginationReached)
    }
}