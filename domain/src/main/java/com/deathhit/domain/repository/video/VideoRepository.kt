package com.deathhit.domain.repository.video

import com.deathhit.domain.model.VideoDO

interface VideoRepository {
    suspend fun getVideoList(): List<VideoDO>
}