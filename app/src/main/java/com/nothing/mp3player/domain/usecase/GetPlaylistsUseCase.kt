package com.nothing.mp3player.domain.usecase

import com.nothing.mp3player.domain.repository.PlaylistRepository
import com.nothing.mp3player.model.Playlist

class GetPlaylistsUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(): List<Playlist> = repository.loadPlaylists()
}
