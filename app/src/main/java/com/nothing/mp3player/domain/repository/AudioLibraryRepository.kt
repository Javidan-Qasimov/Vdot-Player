package com.nothing.mp3player.domain.repository

import com.nothing.mp3player.model.Song

interface AudioLibraryRepository {
    suspend fun loadSongs(): List<Song>
}
