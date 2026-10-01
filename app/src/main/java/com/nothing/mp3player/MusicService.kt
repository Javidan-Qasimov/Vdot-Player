package com.nothing.mp3player

import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

import androidx.media3.common.ForwardingPlayer

/** Owns Media3 playback, media-session commands, and audio focus. */
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                player.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (player.playWhenReady) player.play()
            }
        }
    }

    private val sessionCallback = object : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): com.google.common.util.concurrent.ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
            player.prepare()
            return future
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        setupAudioFocusRequest()
        player = createPlayer()
        setupPlayerListeners(player)

        val intent = android.content.Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE)

        val forwardingPlayer = ForwardingPlayer(player)

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setCallback(sessionCallback)
            .setSessionActivity(pendingIntent)
            .build()
    }

    private fun setupAudioFocusRequest() {
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(PlatformAudioAttributes.Builder().setUsage(PlatformAudioAttributes.USAGE_MEDIA).setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(audioFocusChangeListener).build()
    }

    private fun requestAudioFocus(): Boolean = focusRequest?.let { audioManager.requestAudioFocus(it) } == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    private fun abandonAudioFocus() { focusRequest?.let { audioManager.abandonAudioFocusRequest(it) } }

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(this)
            .setAudioAttributes(Media3AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), false)
            .setHandleAudioBecomingNoisy(false).setWakeMode(C.WAKE_MODE_LOCAL).build()
    }

    private fun setupPlayerListeners(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) {
                    if (!requestAudioFocus()) player.playWhenReady = false
                } else {
                    abandonAudioFocus()
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaMetadata?.let { player.playlistMetadata = it }
            }
            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                player.playlistMetadata = mediaMetadata
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
    override fun onDestroy() { serviceScope.cancel(); abandonAudioFocus(); mediaSession?.release(); player.release(); mediaSession = null; super.onDestroy() }
}
