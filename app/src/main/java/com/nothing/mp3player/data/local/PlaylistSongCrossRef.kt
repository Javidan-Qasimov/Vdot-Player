package com.nothing.mp3player.data.local

import androidx.room.Entity

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistName", "songId"]
)
data class PlaylistSongCrossRef(
    val playlistName: String,
    val songId: Long,
    val position: Int = 0
)
