package com.deathhit.feature.video_list.adapter.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.deathhit.feature.video_list.databinding.ItemVideoBinding
import com.deathhit.feature.video_list.model.VideoVO
import com.google.android.exoplayer2.Player

abstract class VideoAdapter : PagingDataAdapter<VideoVO, VideoViewHolder>(COMPARATOR) {
    companion object {
        private const val TAG = "VideoAdapter"
        private const val PAYLOAD_PLAY_POSITION = "$TAG.PAYLOAD_PLAY_POSITION"

        private val COMPARATOR = object : DiffUtil.ItemCallback<VideoVO>() {
            override fun areItemsTheSame(oldItem: VideoVO, newItem: VideoVO): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: VideoVO, newItem: VideoVO): Boolean =
                oldItem == newItem
        }
    }

    private var isFirstFrameRendered = false
    private var player: Player? = null
    private var playPosition: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder =
        VideoViewHolder(
            ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        ).apply {
            with(binding.styledPlayerView) {
                //Prevents StyledPlayerView from blocking item views from being clicked.
                isClickable = false
                isFocusable = false
            }

            itemView.setOnClickListener { item?.let { onClickItem(it) } }
        }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.item = getItem(position)?.also { item ->
            with(holder.binding.imageViewThumbnail) {
                Glide.with(this).load(item.thumbUrl).placeholder(com.deathhit.core.ui.R.color.black)
                    .into(this)
            }

            with(holder.binding.textViewSubtitle) {
                text = item.subtitle
            }

            with(holder.binding.textViewTitle) {
                text = item.title
            }

            bindPlayPosition(holder, item, position)
        }
    }

    override fun onBindViewHolder(
        holder: VideoViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty())
            super.onBindViewHolder(holder, position, payloads)
        else
            holder.item = getItem(position)?.also { item ->
                payloads.forEach { payload ->
                    when (payload) {
                        PAYLOAD_PLAY_POSITION -> bindPlayPosition(holder, item, position)
                    }
                }
            }
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        with(holder.binding.imageViewThumbnail) {
            Glide.with(this).clear(this)
        }
    }

    fun notifyIsFirstFrameRendered(isFirstFrameRendered: Boolean) {
        this.isFirstFrameRendered = isFirstFrameRendered

        notifyPlayPositionChanged(playPosition)
    }

    fun notifyPlayPositionChanged(playPosition: Int?) {
        val oldPlayPos = this.playPosition
        this.playPosition = playPosition

        oldPlayPos?.let { notifyItemChanged(it, PAYLOAD_PLAY_POSITION) }
        this.playPosition?.let { notifyItemChanged(it, PAYLOAD_PLAY_POSITION) }
    }

    fun setPlayer(player: Player?) {
        this.player = player

        notifyIsFirstFrameRendered(false)
    }

    private fun bindPlayPosition(holder: VideoViewHolder, item: VideoVO, position: Int) {
        val isAtPlayPosition = isAtPlayPosition(position)
        val isFirstFrameRendered = isAtPlayPosition && isFirstFrameRendered
        val player = if (isAtPlayPosition) player else null

        with(holder.binding.imageViewThumbnail) {
            visibility = if (isFirstFrameRendered)
                View.INVISIBLE
            else
                View.VISIBLE
        }

        with(holder.binding.styledPlayerView) {
            this.player = player

            if (isFirstFrameRendered)
                showController()
            else
                hideController()
        }

        if (isAtPlayPosition(position))
            onBindPlayPosition(item)
    }

    private fun isAtPlayPosition(position: Int) = position == playPosition

    abstract fun onBindPlayPosition(item: VideoVO)
    abstract fun onClickItem(item: VideoVO)
}