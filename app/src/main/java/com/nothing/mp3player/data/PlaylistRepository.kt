package com.nothing.mp3player.data

import android.content.Context
import android.net.Uri
import com.nothing.mp3player.model.Playlist
import com.nothing.mp3player.model.Song
import org.json.JSONArray
import org.json.JSONObject

/** Reads and writes user-created playlists in the app's local preferences. */
class PlaylistRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Restores saved playlists, returning an empty list when none are stored. */
    fun loadPlaylists(): List<Playlist> {
        val json = preferences.getString(PLAYLISTS_KEY, null)
        if (json.isNullOrEmpty()) return emptyList()

        return try {
            val playlistArray = JSONArray(json)
            buildList {
                for (playlistIndex in 0 until playlistArray.length()) {
                    val playlistObject = playlistArray.getJSONObject(playlistIndex)
                    val songArray = playlistObject.getJSONArray("songs")
                    val songs = buildList {
                        for (songIndex in 0 until songArray.length()) {
                            val songObject = songArray.getJSONObject(songIndex)
                            add(
                                Song(
                                    id = songObject.getLong("id"),
                                    title = songObject.getString("title"),
                                    artist = songObject.getString("artist"),
                                    uri = Uri.parse(songObject.getString("uri")),
                                    albumId = songObject.getLong("albumId"),
                                    durationMs = if (songObject.has("durationMs")) {
                                        songObject.getLong("durationMs")
                                    } else {
                                        0L
                                    }
                                )
                            )
                        }
                    }
                    add(Playlist(playlistObject.getString("name"), songs))
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            emptyList()
        }
    }

    /** Persists playlists without creating a preference entry before the first save. */
    fun savePlaylists(playlists: List<Playlist>) {
        if (playlists.isEmpty() && !preferences.contains(PLAYLISTS_KEY)) return

        val playlistArray = JSONArray()
        playlists.forEach { playlist ->
            val playlistObject = JSONObject().put("name", playlist.name)
            val songArray = JSONArray()
            playlist.songs.forEach { song ->
                val songObject = JSONObject()
                    .put("id", song.id)
                    .put("title", song.title)
                    .put("artist", song.artist)
                    .put("uri", song.uri.toString())
                    .put("albumId", song.albumId)
                song.durationMs?.let { songObject.put("durationMs", it) }
                songArray.put(songObject)
            }
            playlistObject.put("songs", songArray)
            playlistArray.put(playlistObject)
        }

        preferences.edit().putString(PLAYLISTS_KEY, playlistArray.toString()).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "nothing_player_prefs"
        const val PLAYLISTS_KEY = "playlists_json"
    }
}
