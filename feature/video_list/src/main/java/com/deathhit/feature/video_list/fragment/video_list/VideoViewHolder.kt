package com.deathhit.feature.video_list.fragment.video_list

import androidx.recyclerview.widget.RecyclerView
import com.deathhit.feature.video_list.databinding.ItemVideoBinding
import com.deathhit.feature.video_list.model.MediaItemVO

class VideoViewHolder(
    val binding: ItemVideoBinding,
    var item: MediaItemVO? = null
) : RecyclerView.ViewHolder(binding.root)