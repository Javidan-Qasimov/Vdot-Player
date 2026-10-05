package com.nothing.mp3player

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.nothing.mp3player.model.Song
import com.nothing.mp3player.ui.AddSongsToPlaylistView
import com.nothing.mp3player.ui.AddToPlaylistDialog
import com.nothing.mp3player.ui.BigPlayer
import com.nothing.mp3player.ui.CreatePlaylistDialog
import com.nothing.mp3player.ui.DeletePlaylistConfirmationDialog
import com.nothing.mp3player.ui.DeleteSongConfirmationDialog
import com.nothing.mp3player.ui.LibraryHeader
import com.nothing.mp3player.ui.MainViewModel
import com.nothing.mp3player.ui.NdotFont
import com.nothing.mp3player.ui.PlaylistDetailView
import com.nothing.mp3player.ui.PlaylistLibraryList
import com.nothing.mp3player.ui.TrackLibraryList
import com.nothing.mp3player.utils.NothingRed

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(viewModel, mediaController) }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                mediaController = controller
                if (controller != null) {
                    viewModel.attachPlayerController(controller)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.detachPlayerController()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }

    @Composable
    fun App(viewModel: MainViewModel, controller: MediaController?) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val focusManager = LocalFocusManager.current

        val searchFocusRequester = remember { FocusRequester() }
        val playlistSearchFocusRequester = remember { FocusRequester() }
        val addSongSearchFocusRequester = remember { FocusRequester() }

        // Handles Android's confirmation flow when deleting media requires user approval.
        val deleteLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                state.songToDelete?.let { song ->
                    viewModel.removeSongFromLists(song)
                }
            }
        }

        fun deleteSongFromDevice(song: Song) {
            viewModel.setSongToDelete(song, showDialog = false)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(song.uri))
                    deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        context.contentResolver.delete(song.uri, null, null)
                        viewModel.removeSongFromLists(song)
                    } catch (e: RecoverableSecurityException) {
                        deleteLauncher.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                    }
                } else {
                    val rows = context.contentResolver.delete(song.uri, null, null)
                    if (rows > 0) {
                        viewModel.removeSongFromLists(song)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Requests platform permissions.
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
            val audioGranted = if (Build.VERSION.SDK_INT >= 33) {
                map[Manifest.permission.READ_MEDIA_AUDIO] == true
            } else {
                map[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            }
            if (audioGranted || hasPermissions()) {
                viewModel.setPermissionGranted(true)
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (hasPermissions()) viewModel.setPermissionGranted(true)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(Unit) {
            if (!state.granted) {
                if (hasPermissions()) viewModel.setPermissionGranted(true)
                else launcher.launch(requiredPermissions())
            }
        }

        LaunchedEffect(Unit) {
            viewModel.loadPlaylists()
        }

        LaunchedEffect(state.granted) {
            if (state.granted) {
                viewModel.loadLibrary()
            }
        }

        val isSubScreenOpen = state.isBigPlayerVisible || state.activePlaylistForAdding != null ||
            state.activePlaylistDetail != null || state.isPlaylistSearchActive ||
            state.isAddSongSearchActive || state.isSearchActive || state.showDeleteConfirmationDialog

        BackHandler(enabled = isSubScreenOpen) {
            when {
                state.isBigPlayerVisible -> viewModel.setBigPlayerVisible(false)
                state.activePlaylistForAdding != null -> {
                    viewModel.showAddToPlaylistDialog(false)
                    viewModel.setIsAddSongSearchActive(false)
                    viewModel.setAddSongSearchQuery("")
                }
                state.activePlaylistDetail != null -> {
                    viewModel.setActivePlaylistDetail(null)
                    viewModel.setIsPlaylistSearchActive(false)
                    viewModel.setPlaylistSearchQuery("")
                }
                state.isPlaylistSearchActive -> {
                    viewModel.setIsPlaylistSearchActive(false)
                    viewModel.setPlaylistSearchQuery("")
                }
                state.isAddSongSearchActive -> {
                    viewModel.setIsAddSongSearchActive(false)
                    viewModel.setAddSongSearchQuery("")
                }
                state.isSearchActive -> {
                    viewModel.setIsSearchActive(false)
                    viewModel.setSearchQuery("")
                }
            }
        }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Column(Modifier.fillMaxSize()) {
                if (state.activePlaylistDetail == null && state.activePlaylistForAdding == null) {
                    LibraryHeader(
                        isSearchActive = state.isSearchActive,
                        searchQuery = state.searchQuery,
                        searchFocusRequester = searchFocusRequester,
                        onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                        onExitSearch = {
                            viewModel.setIsSearchActive(false)
                            viewModel.setSearchQuery("")
                        },
                        onAddPlaylistAction = {
                            if (state.currentIndex != -1 && state.currentPlaylist.isNotEmpty()) {
                                viewModel.showAddToPlaylistDialog(true)
                            } else {
                                viewModel.showCreatePlaylistDialog(true)
                            }
                        },
                        onStartSearch = {
                            viewModel.setIsSearchActive(true)
                            viewModel.setSearchQuery("")
                        }
                    )
                }

                Box(Modifier.weight(1f)) {
                    if (!state.granted) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clickable {
                                    if (hasPermissions()) viewModel.setPermissionGranted(true)
                                    else launcher.launch(requiredPermissions())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Music permission required (Tap to grant)", color = Color.White, fontFamily = NdotFont, letterSpacing = 1.sp)
                        }
                    } else {
                        val currentSongId = remember(state.currentIndex, state.currentPlaylist, state.isShuffleEnabled, state.shuffleQueue) {
                            state.activeSongList.getOrNull(state.currentIndex)?.id ?: controller?.currentMediaItem?.mediaId?.toLongOrNull()
                        }

                        if (state.activePlaylistForAdding != null) {
                            val targetIndex = state.activePlaylistForAdding!!
                            val currentPl = state.playlists.getOrNull(targetIndex)
                            if (currentPl != null) {
                                AddSongsToPlaylistView(
                                    playlistName = currentPl.name,
                                    currentSongs = currentPl.songs,
                                    allLocalSongs = state.localSongs,
                                    isSearchActive = state.isAddSongSearchActive,
                                    searchQuery = state.addSongSearchQuery,
                                    searchFocusRequester = addSongSearchFocusRequester,
                                    onSearchToggle = { viewModel.setIsAddSongSearchActive(it) },
                                    onSearchQueryChange = { viewModel.setAddSongSearchQuery(it) },
                                    onGoBack = {
                                        viewModel.showAddToPlaylistDialog(false)
                                        viewModel.setIsAddSongSearchActive(false)
                                        viewModel.setAddSongSearchQuery("")
                                    },
                                    onDone = { tempSongs ->
                                        tempSongs.forEach { song ->
                                            viewModel.addSongToPlaylist(currentPl.name, song)
                                        }
                                        viewModel.showAddToPlaylistDialog(false)
                                        viewModel.setIsAddSongSearchActive(false)
                                        viewModel.setAddSongSearchQuery("")
                                    }
                                )
                            }
                        } else if (state.activePlaylistDetail != null) {
                            val pl = state.activePlaylistDetail!!
                            PlaylistDetailView(
                                playlistName = pl.name,
                                songs = pl.songs,
                                currentPlayingList = state.currentPlaylist,
                                currentIndex = state.currentIndex,
                                isPlaying = state.isPlaying,
                                isShuffleEnabled = state.isShuffleEnabled,
                                isSearchActive = state.isPlaylistSearchActive,
                                searchQuery = state.playlistSearchQuery,
                                searchFocusRequester = playlistSearchFocusRequester,
                                onSearchToggle = { viewModel.setIsPlaylistSearchActive(it) },
                                onSearchQueryChange = { viewModel.setPlaylistSearchQuery(it) },
                                onBackClick = { viewModel.setActivePlaylistDetail(null) },
                                onAddClick = {
                                    val existingIdx = state.playlists.indexOfFirst { it.name.equals(pl.name, ignoreCase = true) }
                                    if (existingIdx != -1) {
                                        viewModel.showAddToPlaylistDialog(true, pl.name)
                                    } else {
                                        viewModel.createPlaylist(pl.name)
                                        viewModel.showAddToPlaylistDialog(true, pl.name)
                                    }
                                },
                                onPlayPlaylist = {
                                    if (pl.songs.isNotEmpty()) viewModel.playInList(pl.songs, 0)
                                },
                                onPauseResume = { viewModel.pauseResume() },
                                onToggleShuffle = {
                                    viewModel.toggleShuffle()
                                },
                                onSelectSong = { songList, index ->
                                    focusManager.clearFocus()
                                    viewModel.playInList(songList, index)
                                    viewModel.setBigPlayerVisible(true)
                                },
                                onRemoveSong = { s ->
                                    viewModel.removeSongFromPlaylist(pl.name, s.id)
                                },
                                onAddToOtherPlaylist = { s ->
                                    viewModel.setTrackMenuSong(s)
                                    viewModel.showAddToPlaylistDialog(true)
                                },
                                currentSongId = currentSongId
                            )
                        } else if (state.selectedTab == 0) {
                            val displaySongs = if (state.isSearchActive) state.searchResults else state.localSongs
                            TrackLibraryList(
                                songs = displaySongs,
                                currentSongId = currentSongId,
                                openMenuFor = state.trackMenuSong,
                                onSelectSong = { song ->
                                    focusManager.clearFocus()
                                    val targetIdx = displaySongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                    viewModel.playInList(displaySongs, targetIdx)
                                    viewModel.setBigPlayerVisible(true)
                                },
                                onOpenMenu = { viewModel.setTrackMenuSong(it) },
                                onAddToPlaylist = { song ->
                                    viewModel.setTrackMenuSong(song)
                                    viewModel.showAddToPlaylistDialog(true)
                                },
                                onDeleteSong = { song ->
                                    viewModel.setSongToDelete(song, showDialog = true)
                                }
                            )
                        } else {
                            PlaylistLibraryList(
                                playlists = state.playlists,
                                localSongs = state.localSongs,
                                currentPlaylist = state.currentPlaylist,
                                isPlaying = state.isPlaying,
                                openMenuFor = state.playlistMenuName,
                                onOpenPlaylist = { viewModel.setActivePlaylistDetail(it) },
                                onOpenMenu = { _ -> },
                                onDeletePlaylist = { name ->
                                    viewModel.setPlaylistToDelete(name, showDialog = true)
                                }
                            )
                        }
                    }
                }

                Box(Modifier.fillMaxWidth()) {
                    Column {
                        AnimatedVisibility(visible = state.currentIndex != -1) {
                            val song = state.activeSongList.getOrNull(state.currentIndex)
                            Row(
                                Modifier.fillMaxWidth().background(Color(0xFF111111)).clickable { viewModel.setBigPlayerVisible(true) }.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(text = song?.title ?: "", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp, maxLines = 1)
                                    Text(song?.artist ?: "", color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                                }
                                IconButton(onClick = { viewModel.prev() }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                IconButton(onClick = { viewModel.pauseResume() }, modifier = Modifier.size(48.dp)) {
                                    Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause", tint = NothingRed, modifier = Modifier.size(26.dp))
                                }
                                IconButton(onClick = { viewModel.next() }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF080808)).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable { viewModel.setSelectedTab(0); viewModel.setActivePlaylistDetail(null); viewModel.showAddToPlaylistDialog(false) }) {
                                Text(text = "TRACKS", color = if (state.selectedTab == 0 && state.activePlaylistDetail == null) NothingRed else Color.Gray, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = 3.sp)
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.size(4.dp).background(if (state.selectedTab == 0 && state.activePlaylistDetail == null) NothingRed else Color.Transparent, CircleShape))
                            }
                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFF222222)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable { viewModel.setSelectedTab(1); viewModel.setActivePlaylistDetail(null); viewModel.showAddToPlaylistDialog(false) }) {
                                Text(text = "PLAYLISTS", color = if (state.selectedTab == 1 || state.activePlaylistDetail != null) NothingRed else Color.Gray, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = 3.sp)
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.size(4.dp).background(if (state.selectedTab == 1 || state.activePlaylistDetail != null) NothingRed else Color.Transparent, CircleShape))
                            }
                        }
                    }
                }
            }

            if (state.showCreatePlaylistDialog) {
                CreatePlaylistDialog(
                    newPlaylistName = state.newPlaylistName,
                    onNameChange = { viewModel.setNewPlaylistName(it) },
                    onDismiss = { viewModel.showCreatePlaylistDialog(false); viewModel.setNewPlaylistName("") },
                    onCreate = { name -> viewModel.createPlaylist(name) }
                )
            }
            if (state.showAddToPlaylistDialog) {
                val currentSong = state.activeSongList.getOrNull(state.currentIndex) ?: state.trackMenuSong
                AddToPlaylistDialog(
                    currentSong = currentSong,
                    playlists = state.playlists,
                    onDismiss = { viewModel.showAddToPlaylistDialog(false) },
                    onAddToPlaylist = { idx ->
                        if (currentSong != null) {
                            val targetPl = state.playlists.getOrNull(idx)
                            if (targetPl != null) {
                                viewModel.addSongToPlaylist(targetPl.name, currentSong)
                            }
                        }
                        viewModel.showAddToPlaylistDialog(false)
                    },
                    onCreateNewClick = {
                        viewModel.showAddToPlaylistDialog(false)
                        viewModel.showCreatePlaylistDialog(true)
                    }
                )
            }
            if (state.showDeleteConfirmationDialog && state.songToDelete != null) {
                DeleteSongConfirmationDialog(
                    song = state.songToDelete!!,
                    onDismiss = { viewModel.setSongToDelete(null, showDialog = false) },
                    onConfirmDelete = { song -> deleteSongFromDevice(song) }
                )
            }
            if (state.showDeletePlaylistConfirmationDialog && state.playlistToDelete != null) {
                DeletePlaylistConfirmationDialog(
                    playlistName = state.playlistToDelete!!,
                    onDismiss = { viewModel.setPlaylistToDelete(null, showDialog = false) },
                    onConfirmDelete = { name -> viewModel.deletePlaylist(name) }
                )
            }

            AnimatedVisibility(visible = state.isBigPlayerVisible, enter = slideInVertically(initialOffsetY = { it }), exit = slideOutVertically(targetOffsetY = { it })) {
                BigPlayer(
                    songs = state.activeSongList,
                    currentIndex = state.currentIndex,
                    isPlaying = state.isPlaying,
                    currentPosition = state.currentPosition,
                    duration = state.duration,
                    onClose = { viewModel.setBigPlayerVisible(false) },
                    onPlayPause = { viewModel.pauseResume() },
                    onNext = { viewModel.next() },
                    onPrev = { viewModel.prev() },
                    onSeek = { pos -> viewModel.seekTo(pos) },
                    onDragging = { dragging, moved ->
                        if (dragging) {
                            if (!state.isDragging) viewModel.setDragging(true, wasPlaying = state.isPlaying)
                            if (state.isPlaying) controller?.pause()
                        } else {
                            viewModel.setDragging(false)
                            if (moved || state.wasPlayingBeforeDrag) controller?.play()
                        }
                    },
                    onSeekToSong = { index -> if (index != state.currentIndex) viewModel.play(index) },
                    isShuffleEnabled = state.isShuffleEnabled,
                    onToggleShuffle = { _ -> viewModel.toggleShuffle() }
                )
            }
        }
    }

    private fun hasPermissions(): Boolean = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
