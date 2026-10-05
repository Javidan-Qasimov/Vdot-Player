package com.nothing.mp3player.domain.usecase

import com.nothing.mp3player.domain.repository.AudioLibraryRepository
import com.nothing.mp3player.model.Song

class GetAudioSongsUseCase(private val repository: AudioLibraryRepository) {
    suspend operator fun invoke(): List<Song> = repository.loadSongs()
}
