package com.nothing.mp3player.data.local

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nothing.mp3player.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val uriString: String,
    val albumId: Long,
    val durationMs: Long
) {
    fun toDomainSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            uri = Uri.parse(uriString),
            albumId = albumId,
            durationMs = durationMs
        )
    }

    companion object {
        fun fromDomainSong(song: Song): SongEntity {
            return SongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                uriString = song.uri.toString(),
                albumId = song.albumId,
                durationMs = song.durationMs ?: 0L
            )
        }
    }
}
