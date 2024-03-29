package com.deathhit.data.media_item

import androidx.paging.*
import com.deathhit.core.app_database.AppDatabase
import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.core.media_api.model.Media
import com.deathhit.core.media_api.test.FakeMediaApiService
import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
import com.deathhit.data.media_item.data_source.MediaItemRemoteDataSource
import com.deathhit.domain.enum_type.MediaItemLabel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import javax.inject.Inject
import kotlin.random.Random

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
            MediaItemLabel.values().run { get(Random.nextInt(0, size)) },
            null
        )
    }

    @After
    fun after() {
        appDatabase.close()
    }

    @Test
    fun refreshLoad_errorOccurs_returnsErrorResult() = runTest {
        //Given
        fakeMediaApiService.funcGetMediaList = { _, _, _, _ -> throw RuntimeException("Test") }

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
    fun refreshLoad_moreDataIsPresent_returnsSuccessResult() = runTest {
        //Given
        val mediaList = listOf(
            Media(
                "0",
                "description",
                "sourceUrl",
                "subtitle",
                "thumbUrl",
                "title"
            )
        )

        fakeMediaApiService.funcGetMediaList = { _, _, _, _ -> mediaList }

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
    fun refreshLoad_noMoreData_returnSuccessAndEndOfPagination() = runTest {
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