package com.deathhit.feature.navigation

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes

class MediaPlayerService : Service() {
    companion object {
        fun bindService(context: Context, serviceConnection: ServiceConnection) =
            context.bindService(
                Intent(context, MediaPlayerService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE
            )

        fun startService(context: Context) =
            getBaseIntent(context).also { context.startService(it) }

        fun stopService(context: Context) = getBaseIntent(context).also { context.stopService(it) }

        fun unbindService(context: Context, serviceConnection: ServiceConnection) =
            context.unbindService(serviceConnection)

        private fun getBaseIntent(context: Context) =
            Intent(context, MediaPlayerService::class.java)
    }

    class ServiceBinder(val service: MediaPlayerService) : Binder()

    abstract class ServiceConnection : android.content.ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            onServiceConnected(service as ServiceBinder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }

        abstract fun onServiceConnected(binder: ServiceBinder)
    }

    val player get() = _player
    private lateinit var _player: Player

    override fun onCreate() {
        super.onCreate()
        _player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder().build(), true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder = ServiceBinder(this)

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}