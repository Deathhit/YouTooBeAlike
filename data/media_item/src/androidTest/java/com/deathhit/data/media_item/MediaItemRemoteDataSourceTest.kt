package com.deathhit.data.media_item

import com.deathhit.core.media_api.test.FakeMediaApiService
import com.deathhit.data.media_item.data_source.MediaItemRemoteDataSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class MediaItemRemoteDataSourceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    internal lateinit var fakeMediaApiService: FakeMediaApiService

    @Inject
    internal lateinit var mediaRemoteDataSource: MediaItemRemoteDataSource

    @Before
    fun before() {
        hiltRule.inject()
    }

    @Test
    fun getMediaList_givenParameters_serviceReceivesParameters() = runTest {
        //Given
        val exclusiveId = "exclusiveId"
        val page = Random.nextInt()
        val pageSize = Random.nextInt()
        val subtitle = "subtitle"

        //When
        mediaRemoteDataSource.getMediaList(exclusiveId, page, pageSize, subtitle)

        //Then
        with(fakeMediaApiService.stateFlow.value) {
            assert(
                actions == listOf(
                    FakeMediaApiService.State.Action.GetMediaList(
                        exclusiveId,
                        page,
                        pageSize,
                        subtitle
                    )
                )
            )
        }
    }
}