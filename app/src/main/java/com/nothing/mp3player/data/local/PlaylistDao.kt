package com.nothing.mp3player.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "name",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistName",
            entityColumn = "songId"
        )
    )
    val songs: List<SongEntity>
)

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlists WHERE name = :name")
    suspend fun deletePlaylist(name: String)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistName = :playlistName")
    suspend fun clearSongsForPlaylist(playlistName: String)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistName = :playlistName AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistName: String, songId: Long)

    @Query("DELETE FROM playlist_song_cross_ref WHERE songId = :songId")
    suspend fun removeSongFromAllPlaylists(songId: Long)

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    suspend fun getPlaylistsWithSongs(): List<PlaylistWithSongs>

    @Transaction
    suspend fun addSongToPlaylist(playlistName: String, song: SongEntity) {
        insertSong(song)
        val count = getSongCountForPlaylist(playlistName)
        insertCrossRef(PlaylistSongCrossRef(playlistName = playlistName, songId = song.id, position = count))
    }

    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistName = :playlistName")
    suspend fun getSongCountForPlaylist(playlistName: String): Int
}
