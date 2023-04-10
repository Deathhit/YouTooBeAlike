package com.deathhit.feature.navigation

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.OrientationEventListener
import android.view.View.OnClickListener
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
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
import com.deathhit.feature.media_item_list.MediaItemListFragment
import com.deathhit.feature.media_item_list.enum_type.MediaItemLabel
import com.deathhit.feature.navigation.databinding.ActivityNavigationBinding
import com.deathhit.feature.playback_details.PlaybackDetailsFragment
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
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
    private var textViewPlayerViewSubtitle: TextView? = null
    private var textViewPlayerViewTitle: TextView? = null

    private val viewModel: NavigationActivityViewModel by viewModels()

    private lateinit var glideRequestManager: RequestManager

    private lateinit var orientationEventListener: OrientationEventListener

    private lateinit var windowInsetsController: WindowInsetsControllerCompat

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
        ) as PlaybackDetailsFragment

    private val mediaPlayerServiceConnection: MediaPlayerService.ServiceConnection =
        object : MediaPlayerService.ServiceConnection() {
            override fun onServiceConnected(binder: MediaPlayerService.ServiceBinder) {
                with(binder.service) {
                    this@NavigationActivity.mediaSession = mediaSession

                    this@NavigationActivity.player = player.apply {
                        addListener(playerListener)
                    }
                }

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

    private val onFullscreenListener = OnClickListener {
        viewModel.toggleScreenOrientation()
    }

    private val onNavigationSelectedListener = OnItemSelectedListener {
        viewModel.setCurrentPage(
            when (it.itemId) {
                R.id.dashboard -> NavigationActivityViewModel.State.Page.DASHBOARD
                R.id.notifications -> NavigationActivityViewModel.State.Page.NOTIFICATIONS
                R.id.home -> NavigationActivityViewModel.State.Page.HOME
                else -> throw java.lang.RuntimeException("Unexpected item id of ${it.itemId}!")
            }
        )

        true
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (!isPlaying)
                with(player!!) {
                    viewModel.saveMediaProgress(
                        playbackState == Player.STATE_ENDED,
                        currentMediaItem!!.mediaId,
                        currentPosition
                    )
                }
        }

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            viewModel.notifyFirstFrameRendered(player!!.currentMediaItem!!.mediaId)
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
                TAG_DASHBOARD -> NavigationActivityViewModel.State.Page.DASHBOARD
                TAG_HOME -> NavigationActivityViewModel.State.Page.HOME
                TAG_NOTIFICATIONS -> NavigationActivityViewModel.State.Page.NOTIFICATIONS
                else -> null
            }?.let { viewModel.notifyPageAttached(it) }
        }

        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityNavigationBinding.inflate(layoutInflater).also {
            setContentView(it.root)

            buttonFullscreen =
                it.playerView.findViewById(com.deathhit.core.ui.R.id.button_fullscreen)

            textViewPlayerViewSubtitle =
                it.playerView.findViewById(com.deathhit.core.ui.R.id.textView_subtitle)

            textViewPlayerViewTitle =
                it.playerView.findViewById(com.deathhit.core.ui.R.id.textView_title)
        }

        glideRequestManager = Glide.with(this)

        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                viewModel.unlockScreenOrientation(orientation)
            }
        }

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
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.stateFlow.map { it.isMediaSessionActive }.distinctUntilChanged()
                        .collect {
                            //Skip in case of isChangingConfigurations to prevent toggling media sessions on rotation.
                            if (isChangingConfigurations)
                                return@collect

                            it?.let { mediaSession!!.isActive = it }
                        }
                }
            }
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
                                    NavigationActivityViewModel.State.Action.PausePlayback -> player?.pause()
                                    NavigationActivityViewModel.State.Action.PlayPlayback -> player?.play()
                                    is NavigationActivityViewModel.State.Action.PreparePlayback -> player?.run {
                                        setMediaItem(
                                            MediaItem.Builder().setMediaId(action.mediaItemId)
                                                .setUri(action.sourceUrl).build(),
                                            if (action.isEnded) C.TIME_UNSET else action.position
                                                ?: C.TIME_UNSET
                                        )
                                        prepare()
                                    }
                                    NavigationActivityViewModel.State.Action.ShowPlayerViewControls -> binding.playerView.showController()
                                    NavigationActivityViewModel.State.Action.StopPlayback -> player?.stop()
                                }

                                viewModel.onAction(action)
                            }
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.currentPage }.distinctUntilChanged()
                        .collect { tab ->
                            binding.bottomNavigationView.selectedItemId = when (tab) {
                                NavigationActivityViewModel.State.Page.DASHBOARD -> R.id.dashboard
                                NavigationActivityViewModel.State.Page.HOME -> R.id.home
                                NavigationActivityViewModel.State.Page.NOTIFICATIONS -> R.id.notifications
                            }

                            supportFragmentManager.commit {
                                if (tab == NavigationActivityViewModel.State.Page.DASHBOARD)
                                    dashboardFragment?.let { show(it) } ?: add(
                                        binding.containerNavigationTabPage.id,
                                        MediaItemListFragment.create(MediaItemLabel.DASHBOARD),
                                        TAG_DASHBOARD
                                    )
                                else
                                    dashboardFragment?.let { hide(it) }

                                if (tab == NavigationActivityViewModel.State.Page.HOME)
                                    homeFragment?.let { show(it) } ?: add(
                                        binding.containerNavigationTabPage.id,
                                        MediaItemListFragment.create(MediaItemLabel.HOME),
                                        TAG_HOME
                                    )
                                else
                                    homeFragment?.let { hide(it) }

                                if (tab == NavigationActivityViewModel.State.Page.NOTIFICATIONS)
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

                        binding.motionLayout.getTransition(R.id.dragUp).isEnabled = !it

                        binding.playerView.resizeMode =
                            if (it) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                        with(windowInsetsController) {
                            val type = WindowInsetsCompat.Type.systemBars()

                            if (it)
                                hide(type)
                            else
                                show(type)
                        }
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

                        binding.textViewPlaybackSubtitle.text = it?.subtitle

                        binding.textViewPlaybackTitle.text = it?.title

                        textViewPlayerViewSubtitle?.text = it?.subtitle

                        textViewPlayerViewTitle?.text = it?.title

                        playbackDetailsFragment.loadPlaybackDetails(it?.id)
                    }
                }

                launch {
                    viewModel.stateFlow.map { it.playPage }.distinctUntilChanged()
                        .collect { playPage ->
                            dashboardFragment?.player =
                                if (playPage == NavigationActivityViewModel.State.Page.DASHBOARD)
                                    player
                                else
                                    null

                            homeFragment?.player =
                                if (playPage == NavigationActivityViewModel.State.Page.HOME)
                                    player
                                else
                                    null

                            notificationsFragment?.player =
                                if (playPage == NavigationActivityViewModel.State.Page.NOTIFICATIONS)
                                    player
                                else
                                    null
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.requestedScreenOrientation }.distinctUntilChanged()
                        .collect { screenOrientation ->
                            requestedOrientation = when (screenOrientation) {
                                NavigationActivityViewModel.State.ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                                NavigationActivityViewModel.State.ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                                NavigationActivityViewModel.State.ScreenOrientation.UNSPECIFIED -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        }
                }
            }
        }

        viewModel.setIsViewInLandscape(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    override fun onStart() {
        super.onStart()
        viewModel.setIsViewInForeground(true)
    }

    override fun onResume() {
        super.onResume()
        with(binding) {
            bottomNavigationView.setOnItemSelectedListener(onNavigationSelectedListener)
            buttonClear.setOnClickListener(onClearListener)
            motionLayout.addTransitionListener(motionTransitionListener)
        }

        buttonFullscreen.setOnClickListener(onFullscreenListener)

        orientationEventListener.enable()
    }

    override fun onPause() {
        super.onPause()
        with(binding) {
            bottomNavigationView.setOnItemSelectedListener(null)
            buttonClear.setOnClickListener(null)
            motionLayout.removeTransitionListener(motionTransitionListener)
        }

        buttonFullscreen.setOnClickListener(null)

        orientationEventListener.disable()

        if (!isChangingConfigurations)
            viewModel.pausePlayerViewPlayback()
    }

    override fun onStop() {
        super.onStop()
        viewModel.setIsViewInForeground(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        //Releases the internal listeners from the player.
        binding.playerControlViewPlayPause.player = null
        binding.playerView.player = null

        mediaSession = null

        player?.removeListener(playerListener)
        player = null

        MediaPlayerService.unbindService(this, mediaPlayerServiceConnection)
        if (isFinishing)
            MediaPlayerService.stopService(this)

        viewModel.setIsPlayerConnected(false)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.motionLayout.transitionState =
            savedInstanceState.getBundle(KEY_MOTION_TRANSITION_STATE)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle(KEY_MOTION_TRANSITION_STATE, binding.motionLayout.transitionState)

        viewModel.saveState()
        super.onSaveInstanceState(outState)
    }
}