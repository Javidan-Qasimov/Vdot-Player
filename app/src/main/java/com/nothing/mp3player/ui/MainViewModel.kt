package com.nothing.mp3player.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import com.nothing.mp3player.data.repository.AudioLibraryRepositoryImpl
import com.nothing.mp3player.data.repository.PlayerRepositoryImpl
import com.nothing.mp3player.data.repository.PlaylistRepositoryImpl
import com.nothing.mp3player.domain.usecase.GetAudioSongsUseCase
import com.nothing.mp3player.domain.usecase.GetPlaylistsUseCase
import com.nothing.mp3player.domain.usecase.ManagePlaylistUseCase
import com.nothing.mp3player.domain.usecase.PlayerControlUseCase
import com.nothing.mp3player.model.Playlist
import com.nothing.mp3player.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioLibraryRepository = AudioLibraryRepositoryImpl(application)
    private val playlistRepository = PlaylistRepositoryImpl(application)
    private val playerRepository = PlayerRepositoryImpl()

    private val getAudioSongsUseCase = GetAudioSongsUseCase(audioLibraryRepository)
    private val getPlaylistsUseCase = GetPlaylistsUseCase(playlistRepository)
    private val managePlaylistUseCase = ManagePlaylistUseCase(playlistRepository)
    val playerControlUseCase = PlayerControlUseCase(playerRepository)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.playerState.collect { pState ->
                _uiState.update { current ->
                    current.copy(
                        isPlaying = pState.isPlaying,
                        currentPosition = pState.currentPosition,
                        duration = pState.duration,
                        currentIndex = pState.currentIndex,
                        isShuffleEnabled = pState.isShuffleEnabled,
                        currentPlaylist = pState.currentPlaylist,
                        shuffleQueue = pState.shuffleQueue
                    )
                }
            }
        }
    }

    fun attachPlayerController(controller: MediaController) {
        playerRepository.attachController(controller)
    }

    fun detachPlayerController() {
        playerRepository.detachController()
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(granted = granted) }
    }

    // Loads the music library from MediaStore asynchronously without passing Context.
    fun loadLibrary() {
        viewModelScope.launch {
            val songs = getAudioSongsUseCase()
            _uiState.update { state ->
                val newCurrentPlaylist = if (state.currentPlaylist.isEmpty()) songs else state.currentPlaylist
                state.copy(localSongs = songs, currentPlaylist = newCurrentPlaylist)
            }
        }
    }

    // Loads saved playlists.
    fun loadPlaylists() {
        viewModelScope.launch {
            val loadedPlaylists = getPlaylistsUseCase()
            _uiState.update { it.copy(playlists = loadedPlaylists) }
        }
    }

    // Creates a new playlist.
    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val updated = managePlaylistUseCase.createPlaylist(name)
            _uiState.update { state ->
                state.copy(playlists = updated, newPlaylistName = "", showCreatePlaylistDialog = false)
            }
        }
    }

    // Deletes a playlist.
    fun deletePlaylist(playlistName: String) {
        viewModelScope.launch {
            val updated = managePlaylistUseCase.deletePlaylist(playlistName)
            _uiState.update { state ->
                val updatedDetail = if (state.activePlaylistDetail?.name == playlistName) null else state.activePlaylistDetail
                state.copy(
                    playlists = updated,
                    activePlaylistDetail = updatedDetail,
                    playlistToDelete = null,
                    showDeletePlaylistConfirmationDialog = false
                )
            }
        }
    }

    // Adds a song to a specific playlist.
    fun addSongToPlaylist(playlistName: String, song: Song) {
        viewModelScope.launch {
            val updatedPlaylists = managePlaylistUseCase.addSong(playlistName, song)
            _uiState.update { state ->
                val updatedDetail = if (state.activePlaylistDetail?.name == playlistName) {
                    updatedPlaylists.find { it.name == playlistName }
                } else {
                    state.activePlaylistDetail
                }
                state.copy(playlists = updatedPlaylists, activePlaylistDetail = updatedDetail)
            }
        }
    }

    // Removes a song from a specific playlist.
    fun removeSongFromPlaylist(playlistName: String, songId: Long) {
        viewModelScope.launch {
            val updatedPlaylists = managePlaylistUseCase.removeSong(playlistName, songId)
            _uiState.update { state ->
                val updatedDetail = if (state.activePlaylistDetail?.name == playlistName) {
                    updatedPlaylists.find { it.name == playlistName }
                } else {
                    state.activePlaylistDetail
                }
                state.copy(playlists = updatedPlaylists, activePlaylistDetail = updatedDetail)
            }
        }
    }

    // Removes a song from all local in-memory lists (after deletion from device).
    fun removeSongFromLists(song: Song) {
        viewModelScope.launch {
            val updatedPlaylists = managePlaylistUseCase.removeSongFromAll(song.id)
            playerRepository.syncSongRemoval(song.id)
            _uiState.update { state ->
                val newLocalSongs = state.localSongs.filterNot { it.id == song.id }
                val newSearchResults = state.searchResults.filterNot { it.id == song.id }
                val newActiveDetail = state.activePlaylistDetail?.copy(
                    songs = state.activePlaylistDetail.songs.filterNot { it.id == song.id }
                )
                state.copy(
                    localSongs = newLocalSongs,
                    searchResults = newSearchResults,
                    playlists = updatedPlaylists,
                    activePlaylistDetail = newActiveDetail,
                    songToDelete = null,
                    showDeleteConfirmationDialog = false
                )
            }
        }
    }

    // Playback Commands delegated to PlayerControlUseCase
    fun play(index: Int, targetList: List<Song> = uiState.value.activeSongList) {
        playerControlUseCase.play(index, targetList)
    }

    fun playInList(songList: List<Song>, index: Int) {
        playerControlUseCase.playInList(songList, index)
    }

    fun pauseResume() {
        playerControlUseCase.pauseResume()
    }

    fun next() {
        playerControlUseCase.next()
    }

    fun prev() {
        playerControlUseCase.prev()
    }

    fun toggleShuffle() {
        playerControlUseCase.toggleShuffle()
    }

    fun seekTo(positionMs: Long) {
        playerControlUseCase.seekTo(positionMs)
    }

    // UI State mutation helpers
    fun setSelectedTab(tab: Int) { _uiState.update { it.copy(selectedTab = tab) } }
    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val results = if (query.isBlank()) emptyList() else state.localSongs.filter {
                it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
            }
            state.copy(searchQuery = query, searchResults = results)
        }
    }
    fun setIsSearchActive(active: Boolean) { _uiState.update { it.copy(isSearchActive = active) } }
    fun setPlaylistSearchQuery(query: String) { _uiState.update { it.copy(playlistSearchQuery = query) } }
    fun setIsPlaylistSearchActive(active: Boolean) { _uiState.update { it.copy(isPlaylistSearchActive = active) } }
    fun setAddSongSearchQuery(query: String) { _uiState.update { it.copy(addSongSearchQuery = query) } }
    fun setIsAddSongSearchActive(active: Boolean) { _uiState.update { it.copy(isAddSongSearchActive = active) } }
    fun setBigPlayerVisible(visible: Boolean) { _uiState.update { it.copy(isBigPlayerVisible = visible) } }
    fun setDragging(dragging: Boolean, wasPlaying: Boolean = false) {
        _uiState.update { it.copy(isDragging = dragging, wasPlayingBeforeDrag = wasPlaying) }
    }
    fun setNewPlaylistName(name: String) { _uiState.update { it.copy(newPlaylistName = name) } }
    fun showCreatePlaylistDialog(show: Boolean) { _uiState.update { it.copy(showCreatePlaylistDialog = show) } }
    fun showAddToPlaylistDialog(show: Boolean, playlistName: String? = null) {
        _uiState.update { it.copy(showAddToPlaylistDialog = show, activePlaylistForAdding = if (playlistName == null) null else stateToPlaylistIndex(playlistName)) }
    }
    private fun stateToPlaylistIndex(name: String): Int? {
        val idx = uiState.value.playlists.indexOfFirst { it.name == name }
        return if (idx >= 0) idx else null
    }
    fun setActivePlaylistDetail(playlist: Playlist?) { _uiState.update { it.copy(activePlaylistDetail = playlist) } }
    fun setTrackMenuSong(song: Song?) { _uiState.update { it.copy(trackMenuSong = song) } }
    fun setPlaylistTrackMenuSong(song: Song?) { _uiState.update { it.copy(playlistTrackMenuSong = song) } }
    fun setSongToDelete(song: Song?, showDialog: Boolean) {
        _uiState.update { it.copy(songToDelete = song, showDeleteConfirmationDialog = showDialog) }
    }
    fun setPlaylistToDelete(playlistName: String?, showDialog: Boolean) {
        _uiState.update { it.copy(playlistToDelete = playlistName, showDeletePlaylistConfirmationDialog = showDialog) }
    }
}
