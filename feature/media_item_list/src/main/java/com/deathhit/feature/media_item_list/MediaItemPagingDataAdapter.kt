package com.deathhit.feature.media_item_list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.RequestManager
import com.deathhit.feature.media_item_list.databinding.ItemMediaItemBinding
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.google.android.exoplayer2.Player

abstract class MediaItemPagingDataAdapter(private val glideRequestManager: RequestManager) :
    PagingDataAdapter<MediaItemVO, MediaItemViewHolder>(COMPARATOR) {
    companion object {
        private const val TAG = "MediaItemAdapter"
        private const val PAYLOAD_PLAY_POSITION = "$TAG.PAYLOAD_PLAY_POSITION"

        private val COMPARATOR = object : DiffUtil.ItemCallback<MediaItemVO>() {
            override fun areItemsTheSame(oldItem: MediaItemVO, newItem: MediaItemVO): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MediaItemVO, newItem: MediaItemVO): Boolean =
                oldItem == newItem
        }
    }

    private var isFirstFrameRendered = false
    private var player: Player? = null
    private var playPosition: Int? = null   //This is an absoluteAdapterPosition.

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder =
        MediaItemViewHolder(
            ItemMediaItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        ).apply {
            binding.styledPlayerView?.apply {
                //Prevents StyledPlayerView from blocking item views from being clicked.
                isClickable = false
                isFocusable = false
            }

            itemView.setOnClickListener { item?.let { onClickItem(it) } }
        }

    override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
        holder.item = getItem(position)?.also { item ->
            holder.binding.imageViewThumbnail.apply {
                glideRequestManager.load(item.thumbUrl)
                    .placeholder(com.deathhit.core.ui.R.color.black)
                    .into(this)
            }

            holder.binding.textViewSubtitle.apply {
                text = item.subtitle
            }

            holder.binding.textViewTitle.apply {
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

            styledPlayerView?.player = null  //Releases the internal listeners from the player.
        }
    }

    fun notifyListStateChanged(isFirstFrameRendered: Boolean, player: Player?, playPosition: Int?) {
        if (isFirstFrameRendered == this.isFirstFrameRendered && player == this.player && playPosition == this.playPosition)
            return

        this.isFirstFrameRendered = isFirstFrameRendered

        this.player = player

        val oldPlayPosition = this.playPosition
        this.playPosition = playPosition

        oldPlayPosition?.let { notifyItemChanged(it, PAYLOAD_PLAY_POSITION) }
        this.playPosition?.let { notifyItemChanged(it, PAYLOAD_PLAY_POSITION) }
    }

    private fun bindPlayPosition(holder: MediaItemViewHolder, item: MediaItemVO) {
        val isAtPlayPosition = holder.absoluteAdapterPosition == playPosition && player != null
        val isFirstFrameRendered = isAtPlayPosition && isFirstFrameRendered

        holder.binding.imageViewThumbnail.apply {
            isInvisible = isFirstFrameRendered
        }

        holder.binding.styledPlayerView?.apply {
            //Set the player to the player view first to render the first frame.
            player = if (isAtPlayPosition) this@MediaItemPagingDataAdapter.player else null

            if (isFirstFrameRendered)
                showController()
            else
                hideController()

            if (isAtPlayPosition)
                onBindPlayPosition(item)
        }
    }

    abstract fun onBindPlayPosition(item: MediaItemVO)
    abstract fun onClickItem(item: MediaItemVO)
}