package com.deathhit.feature.navigation

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.OrientationEventListener
import android.view.View.OnClickListener
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.deathhit.feature.media_item.model.MediaItemLabel
import com.deathhit.feature.navigation.databinding.ActivityNavigationBinding
import com.deathhit.feature.playback_details.PlaybackDetailsFragment
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
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
    private lateinit var buttonFullscreen: ImageButton

    private val viewModel: NavigationActivityViewModel by viewModels()

    private lateinit var glideRequestManager: RequestManager

    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    private val isInLandscapeConfig get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private val mediaSession get() = _mediaSession!!
    private var _mediaSession: MediaSessionCompat? = null

    private val player get() = _player!!
    private var _player: Player? = null

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

    private val mediaPlayerServiceConnection: MediaPlayerService.ServiceConnection =
        object : MediaPlayerService.ServiceConnection() {
            override fun onServiceConnected(binder: MediaPlayerService.ServiceBinder) {
                with(binder.service) {
                    this@NavigationActivity._mediaSession = mediaSession

                    this@NavigationActivity._player = player.apply {
                        addListener(playerListener)
                    }
                }

                viewModel.setIsPlayerConnected(true)

                if (player.playbackState == Player.STATE_IDLE)
                    viewModel.resumePlayerViewPlayback()
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
            with(viewModel) {
                setIsPlayerViewExpanded(currentId == R.id.playerView_expanded)
                toggleSystemBars(isInLandscapeConfig)
            }
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
        viewModel.clearPlayerViewPlayback()
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

    private val onToggleFullscreenListener = OnClickListener {
        viewModel.toggleScreenOrientation(isInLandscapeConfig)
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            mediaSession.isActive = isPlaying

            if (!isPlaying)
                with(player) {
                    viewModel.savePlayItemPosition(
                        playbackState == Player.STATE_ENDED,
                        currentPosition
                    )
                }
        }

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            viewModel.notifyFirstFrameRendered()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when (playbackState) {
                Player.STATE_ENDED -> viewModel.showPlayerViewControls()
                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        onBackPressedDispatcher.addCallback(onCollapsePlayerViewCallback)

        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                viewModel.unlockScreenOrientation(isInLandscapeConfig, orientation)
            }
        }.apply { enable() }

        supportFragmentManager.addFragmentOnAttachListener { _, fragment ->
            when (fragment) {
                is MediaItemListFragment -> {
                    fragment.callback = object : MediaItemListFragment.Callback {
                        override fun onOpenItem(itemId: String) {
                            viewModel.openItem(itemId)
                        }
                    }
                }
                is PlaybackDetailsFragment -> {
                    fragment.callback = object : PlaybackDetailsFragment.Callback {
                        override fun onOpenItem(itemId: String) {
                            viewModel.openItem(itemId)
                        }
                    }
                }
            }

            when (fragment.tag) {
                TAG_DASHBOARD -> NavigationActivityViewModel.State.Tab.DASHBOARD
                TAG_HOME -> NavigationActivityViewModel.State.Tab.HOME
                TAG_NOTIFICATIONS -> NavigationActivityViewModel.State.Tab.NOTIFICATIONS
                else -> null
            }?.let { viewModel.addAttachedTab(it) }
        }

        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater).also {
            setContentView(it.root)

            buttonFullscreen =
                it.playerView.findViewById(com.deathhit.core.ui.R.id.button_fullscreen)
        }

        glideRequestManager = Glide.with(this)

        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        MediaPlayerService.bindService(this, mediaPlayerServiceConnection)
        savedInstanceState
            ?: MediaPlayerService.startService(this) //Starts service to survive configuration changes.

        savedInstanceState ?: supportFragmentManager.commit {
            add(
                binding.containerPlaybackDetails.id,
                PlaybackDetailsFragment.create(),
                TAG_PLAYBACK_DETAILS
            )
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
                                    NavigationActivityViewModel.State.Action.HideSystemBars -> windowInsetsController.hide(
                                        WindowInsetsCompat.Type.systemBars()
                                    )
                                    NavigationActivityViewModel.State.Action.PausePlayback -> player.pause()
                                    NavigationActivityViewModel.State.Action.PlayPlayback -> player.play()
                                    is NavigationActivityViewModel.State.Action.PreparePlayback -> with(
                                        player
                                    ) {
                                        setMediaItem(
                                            MediaItem.fromUri(action.sourceUrl),
                                            if (action.isEnded) C.TIME_UNSET else action.position
                                        )
                                        prepare()
                                    }
                                    is NavigationActivityViewModel.State.Action.SetScreenOrientation -> requestedOrientation =
                                        if (action.isToLandscape)
                                            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                                        else
                                            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                                    NavigationActivityViewModel.State.Action.ShowPlayerViewControls -> binding.playerView.showController()
                                    NavigationActivityViewModel.State.Action.ShowSystemBars -> windowInsetsController.show(
                                        WindowInsetsCompat.Type.systemBars()
                                    )
                                    NavigationActivityViewModel.State.Action.StopPlayback -> player.stop()
                                    NavigationActivityViewModel.State.Action.UnlockScreenOrientation -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
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
                    viewModel.stateFlow.map { it.isFullscreen }.distinctUntilChanged().collect {
                        buttonFullscreen.setImageResource(
                            if (it)
                                com.google.android.exoplayer2.R.drawable.exo_controls_fullscreen_exit
                            else
                                com.google.android.exoplayer2.R.drawable.exo_controls_fullscreen_enter
                        )
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
                                    resources.getDimensionPixelSize(com.deathhit.core.ui.R.dimen.min_height_player_view)
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

                        playbackDetailsFragment?.setPlayItemId(it?.id)
                    }
                }

                launch {
                    viewModel.stateFlow.map { it.playTab }.distinctUntilChanged()
                        .collect { playTab ->
                            dashboardFragment?.setPlayer(
                                if (playTab == NavigationActivityViewModel.State.Tab.DASHBOARD)
                                    player
                                else
                                    null
                            )
                            homeFragment?.setPlayer(
                                if (playTab == NavigationActivityViewModel.State.Tab.HOME)
                                    player
                                else
                                    null
                            )
                            notificationsFragment?.setPlayer(
                                if (playTab == NavigationActivityViewModel.State.Tab.NOTIFICATIONS)
                                    player
                                else
                                    null
                            )
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.tab }.distinctUntilChanged().collect { tab ->
                        binding.bottomNavigationView.selectedItemId = when (tab) {
                            NavigationActivityViewModel.State.Tab.DASHBOARD -> R.id.dashboard
                            NavigationActivityViewModel.State.Tab.HOME -> R.id.home
                            NavigationActivityViewModel.State.Tab.NOTIFICATIONS -> R.id.notifications
                        }

                        supportFragmentManager.commit {
                            if (tab == NavigationActivityViewModel.State.Tab.DASHBOARD)
                                dashboardFragment?.let { show(it) } ?: add(
                                    binding.containerNavigationTabPage.id,
                                    MediaItemListFragment.create(MediaItemLabel.DASHBOARD),
                                    TAG_DASHBOARD
                                )
                            else
                                dashboardFragment?.let { hide(it) }

                            if (tab == NavigationActivityViewModel.State.Tab.HOME)
                                homeFragment?.let { show(it) } ?: add(
                                    binding.containerNavigationTabPage.id,
                                    MediaItemListFragment.create(MediaItemLabel.HOME),
                                    TAG_HOME
                                )
                            else
                                homeFragment?.let { hide(it) }

                            if (tab == NavigationActivityViewModel.State.Tab.NOTIFICATIONS)
                                notificationsFragment?.let { show(it) } ?: add(
                                    binding.containerNavigationTabPage.id,
                                    MediaItemListFragment.create(MediaItemLabel.NOTIFICATIONS),
                                    TAG_NOTIFICATIONS
                                )
                            else
                                notificationsFragment?.let { hide(it) }
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
        }

        buttonFullscreen.setOnClickListener(onToggleFullscreenListener)
    }

    override fun onPause() {
        super.onPause()
        with(binding) {
            bottomNavigationView.setOnItemSelectedListener(null)
            buttonClear.setOnClickListener(null)
            motionLayout.removeTransitionListener(motionTransitionListener)
        }

        buttonFullscreen.setOnClickListener(null)

        if (!isChangingConfigurations)
            viewModel.pausePlayerViewPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        //Releases the internal listeners from the player.
        binding.playerControlViewPlayPause.player = null
        binding.playerView.player = null

        _mediaSession = null

        _player?.removeListener(playerListener)
        _player = null

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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        viewModel.toggleSystemBars(isInLandscapeConfig)
    }
}