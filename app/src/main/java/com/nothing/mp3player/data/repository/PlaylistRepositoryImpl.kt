package com.nothing.mp3player.data.repository

import android.content.Context
import android.net.Uri
import com.nothing.mp3player.data.local.AppDatabase
import com.nothing.mp3player.data.local.PlaylistEntity
import com.nothing.mp3player.data.local.SongEntity
import com.nothing.mp3player.domain.repository.PlaylistRepository
import com.nothing.mp3player.model.Playlist
import com.nothing.mp3player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class PlaylistRepositoryImpl(context: Context) : PlaylistRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val dao = AppDatabase.getInstance(context).playlistDao()

    override suspend fun loadPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        migrateFromPreferencesIfNeeded()
        val playlistsWithSongs = dao.getPlaylistsWithSongs()
        playlistsWithSongs.map { item ->
            Playlist(
                name = item.playlist.name,
                songs = item.songs.map { it.toDomainSong() }
            )
        }
    }

    private suspend fun migrateFromPreferencesIfNeeded() {
        val json = preferences.getString(PLAYLISTS_KEY, null)
        if (!json.isNullOrEmpty()) {
            try {
                val playlistArray = JSONArray(json)
                for (i in 0 until playlistArray.length()) {
                    val playlistObject = playlistArray.getJSONObject(i)
                    val playlistName = playlistObject.getString("name")
                    dao.insertPlaylist(PlaylistEntity(name = playlistName))

                    val songArray = playlistObject.getJSONArray("songs")
                    for (j in 0 until songArray.length()) {
                        val songObject = songArray.getJSONObject(j)
                        val song = Song(
                            id = songObject.getLong("id"),
                            title = songObject.getString("title"),
                            artist = songObject.getString("artist"),
                            uri = Uri.parse(songObject.getString("uri")),
                            albumId = songObject.getLong("albumId"),
                            durationMs = if (songObject.has("durationMs")) songObject.getLong("durationMs") else 0L
                        )
                        dao.addSongToPlaylist(playlistName, SongEntity.fromDomainSong(song))
                    }
                }
                preferences.edit().remove(PLAYLISTS_KEY).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun createPlaylist(name: String): List<Playlist> = withContext(Dispatchers.IO) {
        if (name.isNotBlank()) {
            dao.insertPlaylist(PlaylistEntity(name = name.trim()))
        }
        loadPlaylists()
    }

    override suspend fun deletePlaylist(name: String): List<Playlist> = withContext(Dispatchers.IO) {
        dao.clearSongsForPlaylist(name)
        dao.deletePlaylist(name)
        loadPlaylists()
    }

    override suspend fun addSongToPlaylist(playlistName: String, song: Song): List<Playlist> = withContext(Dispatchers.IO) {
        dao.addSongToPlaylist(playlistName, SongEntity.fromDomainSong(song))
        loadPlaylists()
    }

    override suspend fun removeSongFromPlaylist(playlistName: String, songId: Long): List<Playlist> = withContext(Dispatchers.IO) {
        dao.removeSongFromPlaylist(playlistName, songId)
        loadPlaylists()
    }

    override suspend fun removeSongFromAllPlaylists(songId: Long): List<Playlist> = withContext(Dispatchers.IO) {
        dao.removeSongFromAllPlaylists(songId)
        loadPlaylists()
    }

    private companion object {
        const val PREFERENCES_NAME = "nothing_player_prefs"
        const val PLAYLISTS_KEY = "playlists_json"
    }
}
