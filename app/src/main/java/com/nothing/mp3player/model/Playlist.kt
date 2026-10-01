package com.nothing.mp3player.model

/** A named collection of songs shown in the playlist screens. */
data class Playlist(
    val name: String,
    val songs: List<Song>
)
