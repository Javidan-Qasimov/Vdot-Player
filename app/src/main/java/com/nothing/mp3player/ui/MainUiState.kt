package com.nothing.mp3player.ui

import com.nothing.mp3player.model.Playlist
import com.nothing.mp3player.model.Song

data class MainUiState(
    val granted: Boolean = false,
    val localSongs: List<Song> = emptyList(),
    val searchResults: List<Song> = emptyList(),
    val currentPlaylist: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val shuffleQueue: List<Song> = emptyList(),
    val playedShuffleIndices: Set<Int> = emptySet(),
    val selectedTab: Int = 0,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val playlists: List<Playlist> = emptyList(),
    val activePlaylistForAdding: Int? = null,
    val activePlaylistDetail: Playlist? = null,
    val showCreatePlaylistDialog: Boolean = false,
    val showAddToPlaylistDialog: Boolean = false,
    val newPlaylistName: String = "",
    val playlistSearchQuery: String = "",
    val isPlaylistSearchActive: Boolean = false,
    val addSongSearchQuery: String = "",
    val isAddSongSearchActive: Boolean = false,
    val isBigPlayerVisible: Boolean = false,
    val isDragging: Boolean = false,
    val wasPlayingBeforeDrag: Boolean = false,
    val playlistTrackMenuSong: Song? = null,
    val trackMenuSong: Song? = null,
    val songToDelete: Song? = null,
    val showDeleteConfirmationDialog: Boolean = false,
    val playlistMenuName: String? = null,
    val playlistToDelete: String? = null,
    val showDeletePlaylistConfirmationDialog: Boolean = false
) {
    val activeSongList: List<Song>
        get() = if (isShuffleEnabled && shuffleQueue.isNotEmpty()) shuffleQueue else currentPlaylist

    val currentSong: Song?
        get() = activeSongList.getOrNull(currentIndex)
}
