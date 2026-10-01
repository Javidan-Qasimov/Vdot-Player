package com.nothing.mp3player.model

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import androidx.annotation.Keep
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

@Keep
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val albumId: Long,
    val durationMs: Long? = null
)

fun Song.toMediaItem(): MediaItem {
    val art = if (albumId >= 0) ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), albumId) else null
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setDisplayTitle(title)
        .setArtist(artist)
        .setSubtitle(artist)
        .setAlbumArtist(artist)
        .setAlbumTitle("Nothing MP3")
        .setArtworkUri(art)
        .setIsPlayable(true)
        .setExtras(Bundle().apply { durationMs?.let { putLong("durationMs", it) } })
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}
