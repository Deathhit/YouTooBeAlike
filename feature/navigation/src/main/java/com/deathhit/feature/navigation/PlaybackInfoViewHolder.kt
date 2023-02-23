package com.deathhit.feature.navigation

import androidx.recyclerview.widget.RecyclerView
import com.deathhit.feature.media_item.model.MediaItemVO
import com.deathhit.feature.navigation.databinding.ItemPlaybackInfoBinding

class PlaybackInfoViewHolder(val binding: ItemPlaybackInfoBinding, var item: MediaItemVO? = null) :
    RecyclerView.ViewHolder(binding.root)