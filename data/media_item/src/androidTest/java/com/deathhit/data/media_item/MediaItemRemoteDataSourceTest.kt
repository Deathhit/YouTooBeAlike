package com.deathhit.data.media_item

import com.deathhit.core.media_api.MediaApiService
import com.deathhit.core.media_api.model.Media
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

    private val mediaApiService = object : MediaApiService {
        var exclusiveId: String? = null
        var page: Int? = null
        var pageSize: Int? = null
        var subtitle: String? = null

        override suspend fun getMediaList(
            exclusiveId: String?,
            page: Int,
            pageSize: Int,
            subtitle: String?
        ): List<Media> = emptyList<Media>().also {
            this.exclusiveId = exclusiveId
            this.page = page
            this.pageSize = pageSize
            this.subtitle = subtitle
        }

    }

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
        assert(mediaApiService.exclusiveId == exclusiveId)
        assert(mediaApiService.page == page)
        assert(mediaApiService.pageSize == pageSize)
        assert(mediaApiService.subtitle == subtitle)
    }
}