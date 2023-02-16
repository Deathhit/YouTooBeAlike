package com.deathhit.feature.media_item.adapter.media_item

import androidx.recyclerview.widget.RecyclerView
import com.deathhit.feature.media_item.model.ItemVO
import com.deathhit.feature.media_item_list.databinding.ItemMediaItemBinding

class MediaItemViewHolder(
    val binding: ItemMediaItemBinding,
    var item: ItemVO? = null
) : RecyclerView.ViewHolder(binding.root)