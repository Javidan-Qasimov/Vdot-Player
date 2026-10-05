package com.nothing.mp3player.domain.repository

import com.nothing.mp3player.model.Song
import kotlinx.coroutines.flow.StateFlow

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentIndex: Int = -1,
    val isShuffleEnabled: Boolean = false,
    val currentPlaylist: List<Song> = emptyList(),
    val shuffleQueue: List<Song> = emptyList()
) {
    val activeSongList: List<Song>
        get() = if (isShuffleEnabled && shuffleQueue.isNotEmpty()) shuffleQueue else currentPlaylist

    val currentSong: Song?
        get() = activeSongList.getOrNull(currentIndex)
}

interface PlayerRepository {
    val playerState: StateFlow<PlayerState>

    fun play(index: Int, targetList: List<Song>)
    fun playInList(songList: List<Song>, index: Int)
    fun pauseResume()
    fun next()
    fun prev()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
}
