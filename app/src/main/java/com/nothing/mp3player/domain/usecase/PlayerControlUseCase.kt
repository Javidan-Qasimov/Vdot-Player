package com.nothing.mp3player.domain.usecase

import com.nothing.mp3player.domain.repository.PlayerRepository
import com.nothing.mp3player.domain.repository.PlayerState
import com.nothing.mp3player.model.Song
import kotlinx.coroutines.flow.StateFlow

class PlayerControlUseCase(private val playerRepository: PlayerRepository) {
    val playerState: StateFlow<PlayerState> get() = playerRepository.playerState

    fun play(index: Int, targetList: List<Song>) = playerRepository.play(index, targetList)
    fun playInList(songList: List<Song>, index: Int) = playerRepository.playInList(songList, index)
    fun pauseResume() = playerRepository.pauseResume()
    fun next() = playerRepository.next()
    fun prev() = playerRepository.prev()
    fun seekTo(positionMs: Long) = playerRepository.seekTo(positionMs)
    fun toggleShuffle() = playerRepository.toggleShuffle()
}
