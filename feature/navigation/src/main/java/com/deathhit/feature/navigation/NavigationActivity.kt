package com.deathhit.feature.navigation

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.View.OnClickListener
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.isInvisible
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.deathhit.feature.media_item.MediaItemListFragment
import com.deathhit.feature.media_item.model.MediaItemSourceType
import com.deathhit.feature.navigation.databinding.ActivityNavigationBinding
import com.deathhit.feature.playback_details.PlaybackDetailsFragment
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView.FullscreenButtonClickListener
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NavigationActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "NavigationActivity"
        private const val KEY_MOTION_TRANSITION_STATE = "$TAG.KEY_MOTION_TRANSITION_STATE"
        private const val TAG_DASHBOARD = "$TAG.TAG_DASHBOARD"
        private const val TAG_HOME = "$TAG.TAG_HOME"
        private const val TAG_NOTIFICATIONS = "$TAG.TAG_NOTIFICATIONS"
        private const val TAG_PLAYBACK_DETAILS = "$TAG.TAG_PLAYBACK_DETAILS"
    }

    private lateinit var binding: ActivityNavigationBinding

    private val viewModel: NavigationActivityViewModel by viewModels()
    private val isPlayingInList get() = viewModel.stateFlow.value.isPlayingByTabPage

    private lateinit var glideRequestManager: RequestManager

    private var mediaSession: MediaSessionCompat? = null
    private var player: Player? = null
    private var thumbnailGlideTarget: Target<Drawable>? = null

    private val dashboardFragment
        get() = supportFragmentManager.findFragmentByTag(TAG_DASHBOARD) as MediaItemListFragment?
    private val homeFragment
        get() = supportFragmentManager.findFragmentByTag(TAG_HOME) as MediaItemListFragment?
    private val notificationsFragment
        get() = supportFragmentManager.findFragmentByTag(TAG_NOTIFICATIONS) as MediaItemListFragment?
    private val playbackDetailsFragment
        get() = supportFragmentManager.findFragmentByTag(
            TAG_PLAYBACK_DETAILS
        ) as PlaybackDetailsFragment?

    private val fullscreenButtonClickListener = FullscreenButtonClickListener {
        //todo implement
    }

    private val mediaPlayerServiceConnection: MediaPlayerService.ServiceConnection =
        object : MediaPlayerService.ServiceConnection() {
            override fun onServiceConnected(binder: MediaPlayerService.ServiceBinder) {
                with(binder.service) {
                    this@NavigationActivity.mediaSession = mediaSession

                    this@NavigationActivity.player = player.apply {
                        addListener(playerListener)
                    }
                }

                dashboardFragment?.player = player
                homeFragment?.player = player
                notificationsFragment?.player = player

                viewModel.setIsPlayerConnected(true)
            }
        }

    private val motionTransitionListener = object : MotionLayout.TransitionListener {
        override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {
            viewModel.setIsPlayerViewExpanded(false)
        }

        override fun onTransitionChange(
            motionLayout: MotionLayout?,
            startId: Int,
            endId: Int,
            progress: Float
        ) {

        }

        override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
            viewModel.setIsPlayerViewExpanded(currentId == R.id.playerView_expanded)
            //todo show controls if playback is ended.
        }

        override fun onTransitionTrigger(
            motionLayout: MotionLayout?,
            triggerId: Int,
            positive: Boolean,
            progress: Float
        ) {

        }
    }

    private val onCollapsePlayerViewCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.collapsePlayerView()
        }
    }

    private val onClearListener = OnClickListener {
        viewModel.clearItem()
    }

    private val onNavigationSelectedListener = OnItemSelectedListener {
        viewModel.setTab(
            when (it.itemId) {
                R.id.dashboard -> NavigationActivityViewModel.State.Tab.DASHBOARD
                R.id.notifications -> NavigationActivityViewModel.State.Tab.NOTIFICATIONS
                R.id.home -> NavigationActivityViewModel.State.Tab.HOME
                else -> throw java.lang.RuntimeException("Unexpected item id of ${it.itemId}!")
            }
        )

        true
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            mediaSession!!.isActive = isPlaying

            if (!isPlaying)
                player!!.run {
                    viewModel.savePlayItemPosition(
                        playbackState == Player.STATE_ENDED,
                        currentPosition
                    )
                }
        }

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            viewModel.setIsFirstFrameRendered(true)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_IDLE)
                viewModel.setIsFirstFrameRendered(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.addFragmentOnAttachListener { _, fragment ->
            when (fragment) {
                is MediaItemListFragment -> {
                    fragment.callback = object : MediaItemListFragment.Callback {
                        override fun onOpenItem(itemId: String) {
                            viewModel.openItem(itemId)
                        }

                        override fun onPrepareItem(itemId: String?) {
                            viewModel.prepareItem(itemId)
                        }
                    }

                    fragment.player = player
                }
                is PlaybackDetailsFragment -> {
                    fragment.callback = object : PlaybackDetailsFragment.Callback {
                        override fun onOpenItem(itemId: String) {
                            viewModel.openItem(itemId)
                        }
                    }
                }
            }
        }

        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(onCollapsePlayerViewCallback)

        binding = ActivityNavigationBinding.inflate(layoutInflater).also { setContentView(it.root) }

        glideRequestManager = Glide.with(this)

        MediaPlayerService.bindService(this, mediaPlayerServiceConnection)
        savedInstanceState
            ?: MediaPlayerService.startService(this) //Starts service to survive configuration changes.

        savedInstanceState ?: supportFragmentManager.commit {
            add(binding.containerPlaybackDetails.id, PlaybackDetailsFragment.create(), TAG_PLAYBACK_DETAILS)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stateFlow.map { it.actions }.distinctUntilChanged()
                        .collect { actions ->
                            actions.forEach { action ->
                                when (action) {
                                    NavigationActivityViewModel.State.Action.CollapsePlayerView -> with(
                                        binding.motionLayout
                                    ) {
                                        when (currentState) {
                                            R.id.playerView_expanded -> {
                                                setTransition(R.id.dragUp)
                                                transitionToEnd()
                                            }
                                        }
                                    }
                                    NavigationActivityViewModel.State.Action.ExpandPlayerView -> with(
                                        binding.motionLayout
                                    ) {
                                        when (currentState) {
                                            R.id.playerView_collapsed -> {
                                                setTransition(R.id.dragUp)
                                                transitionToStart()
                                            }
                                            R.id.playerView_hidden -> {
                                                setTransition(R.id.pop)
                                                transitionToEnd { setTransition(R.id.dragUp) }
                                            }
                                        }
                                    }
                                    NavigationActivityViewModel.State.Action.HidePlayerView -> with(
                                        binding.motionLayout
                                    ) {
                                        setTransition(R.id.hide)
                                        transitionToEnd()
                                    }
                                    NavigationActivityViewModel.State.Action.PauseMedia -> player?.pause()
                                    NavigationActivityViewModel.State.Action.PlayMedia -> player?.play()
                                    is NavigationActivityViewModel.State.Action.PrepareMedia -> player?.run {
                                        setMediaItem(
                                            MediaItem.fromUri(action.sourceUrl),
                                            if (action.isEnded) C.TIME_UNSET else action.position
                                        )
                                        prepare()
                                    }
                                    NavigationActivityViewModel.State.Action.StopMedia -> player?.stop()
                                }

                                viewModel.onAction(action)
                            }
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.isFirstFrameRendered }.distinctUntilChanged()
                        .collect {
                            binding.imageViewThumbnail.isInvisible = it
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.isPlayerViewExpanded }.distinctUntilChanged()
                        .collect {
                            binding.playerView.useController = it

                            onCollapsePlayerViewCallback.isEnabled = it
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.isPlayingByPlayerView }.distinctUntilChanged()
                        .collect {
                            val player = if (it) this@NavigationActivity.player else null

                            with(binding.playerControlViewPlayPause) {
                                this.player = player
                            }

                            with(binding.playerView) {
                                this.player = player
                            }
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.playItem }.distinctUntilChanged().collect {
                        it?.let {
                            //Since the size of the image view is dynamic, we need to load the image with a fixed size.
                            glideRequestManager.clear(thumbnailGlideTarget)
                            thumbnailGlideTarget = glideRequestManager
                                .load(it.thumbUrl)
                                .placeholder(com.deathhit.core.ui.R.color.black)
                                .override(
                                    Target.SIZE_ORIGINAL,
                                    resources.getDimensionPixelSize(com.deathhit.core.ui.R.dimen.min_height_player_view_list)
                                )
                                .into(object : CustomTarget<Drawable>() {
                                    override fun onResourceReady(
                                        resource: Drawable,
                                        transition: Transition<in Drawable>?
                                    ) {
                                        binding.imageViewThumbnail.setImageDrawable(resource)
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {
                                        binding.imageViewThumbnail.setImageDrawable(placeholder)
                                    }
                                })
                        }

                        with(binding.textViewPlaybackSubtitle) {
                            text = it?.subtitle
                        }

                        with(binding.textViewPlaybackTitle) {
                            text = it?.title
                        }

                        playbackDetailsFragment?.setPlayableItemId(it?.id)
                    }
                }

                launch {
                    viewModel.stateFlow.map { it.playingTab }.distinctUntilChanged().collect {
                        dashboardFragment?.setIsPlaying(it == NavigationActivityViewModel.State.Tab.DASHBOARD)
                        homeFragment?.setIsPlaying(it == NavigationActivityViewModel.State.Tab.HOME)
                        notificationsFragment?.setIsPlaying(it == NavigationActivityViewModel.State.Tab.NOTIFICATIONS)
                    }
                }

                launch {
                    viewModel.stateFlow.map { it.tab }.distinctUntilChanged().collect { tab ->
                        when (tab) {
                            NavigationActivityViewModel.State.Tab.DASHBOARD -> {
                                supportFragmentManager.commit {
                                    dashboardFragment?.let { show(it) } ?: add(
                                        binding.containerNavigationTabPage.id,
                                        MediaItemListFragment.create(
                                            isPlayingInList,
                                            MediaItemSourceType.DASHBOARD
                                        ),
                                        TAG_DASHBOARD
                                    )
                                    homeFragment?.let { hide(it) }
                                    notificationsFragment?.let { hide(it) }
                                }

                                binding.bottomNavigationView.selectedItemId = R.id.dashboard
                            }
                            NavigationActivityViewModel.State.Tab.HOME -> {
                                supportFragmentManager.commit {
                                    dashboardFragment?.let { hide(it) }
                                    homeFragment?.let { show(it) } ?: add(
                                        binding.containerNavigationTabPage.id,
                                        MediaItemListFragment.create(
                                            isPlayingInList,
                                            MediaItemSourceType.HOME
                                        ),
                                        TAG_HOME
                                    )
                                    notificationsFragment?.let { hide(it) }
                                }

                                binding.bottomNavigationView.selectedItemId = R.id.home
                            }
                            NavigationActivityViewModel.State.Tab.NOTIFICATIONS -> {
                                supportFragmentManager.commit {
                                    dashboardFragment?.let { hide(it) }
                                    homeFragment?.let { hide(it) }
                                    notificationsFragment?.let { show(it) } ?: add(
                                        binding.containerNavigationTabPage.id,
                                        MediaItemListFragment.create(
                                            isPlayingInList,
                                            MediaItemSourceType.NOTIFICATIONS
                                        ),
                                        TAG_NOTIFICATIONS
                                    )
                                }

                                binding.bottomNavigationView.selectedItemId = R.id.notifications
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        with(binding) {
            bottomNavigationView.setOnItemSelectedListener(onNavigationSelectedListener)
            buttonClear.setOnClickListener(onClearListener)
            motionLayout.addTransitionListener(motionTransitionListener)
            playerView.setFullscreenButtonClickListener(fullscreenButtonClickListener)
        }
    }

    override fun onPause() {
        super.onPause()
        with(binding) {
            bottomNavigationView.setOnItemSelectedListener(null)
            buttonClear.setOnClickListener(null)
            motionLayout.removeTransitionListener(motionTransitionListener)
            playerView.setFullscreenButtonClickListener(null)
        }

        if (!isChangingConfigurations)
            viewModel.pauseMedia()
    }

    override fun onDestroy() {
        super.onDestroy()
        //Releases the internal listeners from the player.
        binding.playerControlViewPlayPause.player = null
        binding.playerView.player = null

        player?.removeListener(playerListener)

        MediaPlayerService.unbindService(this, mediaPlayerServiceConnection)
        if (isFinishing)
            MediaPlayerService.stopService(this)

        viewModel.setIsPlayerConnected(false)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        with(binding.motionLayout) {
            transitionState = savedInstanceState.getBundle(KEY_MOTION_TRANSITION_STATE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        with(binding.motionLayout) {
            outState.putBundle(KEY_MOTION_TRANSITION_STATE, transitionState)
        }

        viewModel.saveState()
        super.onSaveInstanceState(outState)
    }
}