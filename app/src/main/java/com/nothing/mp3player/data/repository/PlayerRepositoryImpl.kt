package com.nothing.mp3player.data.repository

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.nothing.mp3player.domain.repository.PlayerRepository
import com.nothing.mp3player.domain.repository.PlayerState
import com.nothing.mp3player.model.Song
import com.nothing.mp3player.model.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class PlayerRepositoryImpl : PlayerRepository {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var mediaController: MediaController? = null
    private var tickerJob: Job? = null

    fun attachController(controller: MediaController) {
        this.mediaController = controller

        _playerState.update { state ->
            state.copy(
                isShuffleEnabled = controller.shuffleModeEnabled,
                isPlaying = controller.isPlaying,
                duration = controller.duration.coerceAtLeast(0L),
                currentPosition = controller.currentPosition.coerceAtLeast(0L)
            )
        }

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startTicker() else stopTicker()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = controller.currentMediaItemIndex
                _playerState.update { state ->
                    val newIdx = if (idx in 0 until controller.mediaItemCount) idx else state.currentIndex
                    state.copy(currentIndex = newIdx, duration = controller.duration.coerceAtLeast(0L))
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                _playerState.update { it.copy(duration = controller.duration.coerceAtLeast(0L)) }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                _playerState.update { it.copy(currentPosition = newPosition.positionMs) }
            }
        }

        controller.addListener(listener)
        if (controller.isPlaying) startTicker()
    }

    fun detachController() {
        stopTicker()
        mediaController = null
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (_playerState.value.isPlaying && mediaController != null) {
                val controller = mediaController ?: break
                val pos = controller.currentPosition.coerceAtLeast(0L)
                val dur = controller.duration.coerceAtLeast(0L)
                _playerState.update { it.copy(currentPosition = pos, duration = dur) }
                delay(500.milliseconds)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun generateShuffleQueue(playlist: List<Song>, currentSong: Song?): List<Song> {
        if (playlist.isEmpty()) return emptyList()
        val remaining = if (currentSong != null) playlist.filter { it.id != currentSong.id }.shuffled() else playlist.shuffled()
        return if (currentSong != null) listOf(currentSong) + remaining else remaining
    }

    override fun play(index: Int, targetList: List<Song>) {
        val song = targetList.getOrNull(index) ?: return
        _playerState.update { it.copy(currentIndex = index, isPlaying = true) }

        val mc = mediaController ?: return
        val currentMediaIdAtIndex = if (index in 0 until mc.mediaItemCount) mc.getMediaItemAt(index).mediaId else null
        if (mc.mediaItemCount == targetList.size && currentMediaIdAtIndex == song.id.toString()) {
            mc.seekTo(index, 0L)
            mc.play()
        } else {
            mc.setMediaItems(targetList.map { it.toMediaItem() }, index, 0L)
            mc.prepare()
            mc.play()
        }
    }

    override fun playInList(songList: List<Song>, index: Int) {
        val selectedSong = songList.getOrNull(index) ?: return
        val isShuffle = _playerState.value.isShuffleEnabled
        val targetList = if (isShuffle) {
            val newQueue = generateShuffleQueue(songList, selectedSong)
            _playerState.update { it.copy(shuffleQueue = newQueue, currentPlaylist = songList) }
            newQueue
        } else {
            _playerState.update { it.copy(shuffleQueue = emptyList(), currentPlaylist = songList) }
            songList
        }
        val targetIdx = if (isShuffle) 0 else index
        play(targetIdx, targetList)
    }

    override fun pauseResume() {
        val mc = mediaController ?: return
        if (mc.isPlaying) {
            mc.pause()
            _playerState.update { it.copy(isPlaying = false) }
        } else {
            mc.play()
            _playerState.update { it.copy(isPlaying = true) }
        }
    }

    override fun next() {
        val state = _playerState.value
        val activeList = state.activeSongList
        if (activeList.isNotEmpty()) {
            if (state.currentIndex < activeList.size - 1) {
                play(state.currentIndex + 1, activeList)
            } else if (state.isShuffleEnabled) {
                val currentSong = activeList.getOrNull(state.currentIndex)
                val newQueue = generateShuffleQueue(state.currentPlaylist, currentSong)
                _playerState.update { it.copy(shuffleQueue = newQueue) }
                play(0, newQueue)
            } else if (mediaController?.hasNextMediaItem() == true) {
                mediaController?.seekToNext()
            }
        }
    }

    override fun prev() {
        val state = _playerState.value
        val activeList = state.activeSongList
        if (activeList.isNotEmpty()) {
            if (state.currentIndex > 0) {
                play(state.currentIndex - 1, activeList)
            } else if (mediaController?.hasPreviousMediaItem() == true) {
                mediaController?.seekToPrevious()
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playerState.update { it.copy(currentPosition = positionMs) }
    }

    override fun toggleShuffle() {
        _playerState.update { state ->
            val newShuffleState = !state.isShuffleEnabled
            mediaController?.shuffleModeEnabled = newShuffleState
            if (newShuffleState) {
                val newQueue = generateShuffleQueue(state.currentPlaylist, state.currentSong)
                state.copy(isShuffleEnabled = true, shuffleQueue = newQueue)
            } else {
                state.copy(isShuffleEnabled = false, shuffleQueue = emptyList())
            }
        }
    }

    fun syncSongRemoval(songId: Long) {
        _playerState.update { state ->
            val newCurrentPlaylist = state.currentPlaylist.filterNot { it.id == songId }
            val newShuffleQueue = state.shuffleQueue.filterNot { it.id == songId }
            state.copy(currentPlaylist = newCurrentPlaylist, shuffleQueue = newShuffleQueue)
        }
    }
}
