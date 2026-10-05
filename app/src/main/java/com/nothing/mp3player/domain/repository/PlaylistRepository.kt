package com.nothing.mp3player.domain.repository

import com.nothing.mp3player.model.Playlist
import com.nothing.mp3player.model.Song

interface PlaylistRepository {
    suspend fun loadPlaylists(): List<Playlist>
    suspend fun createPlaylist(name: String): List<Playlist>
    suspend fun deletePlaylist(name: String): List<Playlist>
    suspend fun addSongToPlaylist(playlistName: String, song: Song): List<Playlist>
    suspend fun removeSongFromPlaylist(playlistName: String, songId: Long): List<Playlist>
    suspend fun removeSongFromAllPlaylists(songId: Long): List<Playlist>
}
