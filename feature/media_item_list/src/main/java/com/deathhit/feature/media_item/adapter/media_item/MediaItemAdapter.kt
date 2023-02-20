package com.deathhit.feature.media_item.adapter.media_item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.RequestManager
import com.deathhit.feature.media_item.model.MediaItemVO
import com.deathhit.feature.media_item_list.databinding.ItemMediaItemBinding
import com.google.android.exoplayer2.Player

abstract class MediaItemAdapter(private val glideRequestManager: RequestManager) :
    PagingDataAdapter<MediaItemVO, MediaItemViewHolder>(COMPARATOR) {
    companion object {
        private const val TAG = "MediaItemAdapter"
        private const val PAYLOAD_PLAY_POSITION = "$TAG.PAYLOAD_PLAY_POSITION"

        private val COMPARATOR = object : DiffUtil.ItemCallback<MediaItemVO>() {
            override fun areItemsTheSame(oldItem: MediaItemVO, newItem: MediaItemVO): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: MediaItemVO, newItem: MediaItemVO): Boolean =
                oldItem == newItem
        }
    }

    private var isFirstFrameRendered = false
    private var isPlayingInList = true
    private var player: Player? = null
    private var playPosition: Int? = null   //This is an absoluteAdapterPosition.

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder =
        MediaItemViewHolder(
            ItemMediaItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        ).apply {
            with(binding.styledPlayerView) {
                //Prevents StyledPlayerView from blocking item views from being clicked.
                isClickable = false
                isFocusable = false
            }

            itemView.setOnClickListener { item?.let { onClickItem(it) } }
        }

    override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
        holder.item = getItem(position)?.also { item ->
            with(holder.binding.imageViewThumbnail) {
                glideRequestManager.load(item.thumbUrl)
                    .placeholder(com.deathhit.core.ui.R.color.black)
                    .into(this)
            }

            with(holder.binding.textViewSubtitle) {
                text = item.subtitle
            }

            with(holder.binding.textViewTitle) {
                text = item.title
            }

            bindPlayPosition(holder, item)
        }
    }

    override fun onBindViewHolder(
        holder: MediaItemViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty())
            super.onBindViewHolder(holder, position, payloads)
        else
            holder.item = getItem(position)?.also { item ->
                payloads.forEach { payload ->
                    when (payload) {
                        PAYLOAD_PLAY_POSITION -> bindPlayPosition(holder, item)
                    }
                }
            }
    }

    override fun onViewRecycled(holder: MediaItemViewHolder) {
        super.onViewRecycled(holder)
        with(holder.binding) {
            glideRequestManager.clear(imageViewThumbnail)

            styledPlayerView.player = null  //Releases the internal listeners from the player.
        }
    }

    fun notifyIsFirstFrameRendered(isFirstFrameRendered: Boolean) {
        this.isFirstFrameRendered = isFirstFrameRendered

        notifyPlayPositionChanged(playPosition)
    }

    fun notifyIsPlayingInList(isPlayingInList: Boolean) {
        this.isPlayingInList = isPlayingInList

        notifyPlayPositionChanged(playPosition)
    }

    fun notifyPlayPositionChanged(playPosition: Int?) {
        val oldPlayPosition = this.playPosition
        this.playPosition = playPosition

        oldPlayPosition?.let { notifyItemChanged(it, PAYLOAD_PLAY_POSITION) }
        this.playPosition?.let { notifyItemChanged(it, PAYLOAD_PLAY_POSITION) }
    }

    fun setPlayer(player: Player?) {
        this.player = player

        notifyIsFirstFrameRendered(false)
    }

    private fun bindPlayPosition(holder: MediaItemViewHolder, item: MediaItemVO) {
        val isAtPlayPosition = holder.absoluteAdapterPosition == playPosition && isPlayingInList
        val isFirstFrameRendered = isAtPlayPosition && isFirstFrameRendered

        with(holder.binding.imageViewThumbnail) {
            visibility = if (isFirstFrameRendered)
                View.INVISIBLE
            else
                View.VISIBLE
        }

        with(holder.binding.styledPlayerView) {
            player = if (isAtPlayPosition) this@MediaItemAdapter.player else null

            if (isFirstFrameRendered)
                showController()
            else
                hideController()
        }

        if (isAtPlayPosition)
            onBindPlayPosition(item)
    }

    abstract fun onBindPlayPosition(item: MediaItemVO)
    abstract fun onClickItem(item: MediaItemVO)
}