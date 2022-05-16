package com.deathhit.video_list_example.extensions

import com.deathhit.domain.model.VideoDO
import com.deathhit.video_list_example.model.VideoVO

fun VideoDO.toVO() = VideoVO(description, sourceUrl, subtitle, thumbUrl, title)