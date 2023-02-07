package com.deathhit.feature.video_list.fragment.video_list

import androidx.recyclerview.widget.RecyclerView
import com.deathhit.feature.video_list.databinding.ItemVideoBinding
import com.deathhit.feature.video_list.model.VideoVO

class VideoViewHolder(
    val binding: ItemVideoBinding,
    var item: VideoVO? = null
) : RecyclerView.ViewHolder(binding.root)