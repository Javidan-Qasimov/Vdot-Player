package com.nothing.mp3player.domain.usecase

import com.nothing.mp3player.domain.repository.PlaylistRepository
import com.nothing.mp3player.model.Playlist
import com.nothing.mp3player.model.Song

class ManagePlaylistUseCase(private val repository: PlaylistRepository) {
    suspend fun createPlaylist(name: String): List<Playlist> = repository.createPlaylist(name)
    suspend fun deletePlaylist(name: String): List<Playlist> = repository.deletePlaylist(name)
    suspend fun addSong(playlistName: String, song: Song): List<Playlist> = repository.addSongToPlaylist(playlistName, song)
    suspend fun removeSong(playlistName: String, songId: Long): List<Playlist> = repository.removeSongFromPlaylist(playlistName, songId)
    suspend fun removeSongFromAll(songId: Long): List<Playlist> = repository.removeSongFromAllPlaylists(songId)
}
