package com.deathhit.feature.navigation

import androidx.recyclerview.widget.RecyclerView
import com.deathhit.feature.navigation.databinding.ItemPlaybackDetailsBinding
import com.deathhit.feature.navigation.model.PlaybackDetailsVO

class PlaybackDetailsViewHolder(
    val binding: ItemPlaybackDetailsBinding,
    var item: PlaybackDetailsVO? = null
) : RecyclerView.ViewHolder(binding.root)