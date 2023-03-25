package com.deathhit.feature.media_item_list

import androidx.recyclerview.widget.RecyclerView
import com.deathhit.feature.media_item_list.databinding.ItemMediaItemBinding
import com.deathhit.feature.media_item_list.model.MediaItemVO

class MediaItemViewHolder(
    val binding: ItemMediaItemBinding,
    var item: MediaItemVO? = null
) : RecyclerView.ViewHolder(binding.root)