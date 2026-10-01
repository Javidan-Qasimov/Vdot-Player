package com.nothing.mp3player

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nothing.mp3player.model.Song
import com.nothing.mp3player.model.Playlist
import com.nothing.mp3player.data.AudioLibraryRepository
import com.nothing.mp3player.data.PlaylistRepository

import com.nothing.mp3player.model.toMediaItem
import com.nothing.mp3player.ui.*
import com.nothing.mp3player.utils.*
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController by mutableStateOf<MediaController?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(mediaController) }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }

    @Composable
    fun App(controller: MediaController?) {
        var granted by remember { mutableStateOf(hasPermissions()) }
        var localSongs by remember { mutableStateOf(listOf<Song>()) }
        var searchResults by remember { mutableStateOf(listOf<Song>()) }
        var currentPlaylist by remember { mutableStateOf(listOf<Song>()) }

        var currentIndex by remember { mutableIntStateOf(-1) }
        var isPlaying by remember { mutableStateOf(false) }
        var currentPosition by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var isBigPlayerVisible by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }
        var wasPlayingBeforeDrag by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val focusManager = LocalFocusManager.current
        val playlistRepository = remember(context) { PlaylistRepository(context.applicationContext) }
        var isShuffleEnabled by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableIntStateOf(0) }
        var isSearchActive by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        // Screen state is grouped here; playback and persistence work are delegated to focused helpers.
        var playlists by remember { mutableStateOf(emptyList<Playlist>()) }
        var activePlaylistForAdding by remember { mutableStateOf<Int?>(null) }
        var activePlaylistDetail by remember { mutableStateOf<Playlist?>(null) }
        var showCreatePlaylistDialog by remember { mutableStateOf(false) }
        var showAddToPlaylistDialog by remember { mutableStateOf(false) }
        var newPlaylistName by remember { mutableStateOf("") }
        var playlistSearchQuery by remember { mutableStateOf("") }
        var isPlaylistSearchActive by remember { mutableStateOf(false) }
        var addSongSearchQuery by remember { mutableStateOf("") }
        var isAddSongSearchActive by remember { mutableStateOf(false) }
        var isFadingOut by remember { mutableStateOf(false) }

        var playlistTrackMenuSong by remember { mutableStateOf<Song?>(null) }
        var trackMenuSong by remember { mutableStateOf<Song?>(null) }
        var songToDelete by remember { mutableStateOf<Song?>(null) }
        var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
        var playlistMenuName by remember { mutableStateOf<String?>(null) }
        var playlistToDelete by remember { mutableStateOf<String?>(null) }
        var showDeletePlaylistConfirmationDialog by remember { mutableStateOf(false) }

        // Removes a deleted track from every in-memory collection that can display or play it.
        fun removeSongFromLists(song: Song) {
            localSongs = localSongs.filter { it.id != song.id }
            searchResults = searchResults.filter { it.id != song.id }
            currentPlaylist = currentPlaylist.filter { it.id != song.id }
            playlists = playlists.map { playlist ->
                playlist.copy(songs = playlist.songs.filterNot { it.id == song.id })
            }
            if (activePlaylistDetail != null) {
                activePlaylistDetail = activePlaylistDetail!!.copy(
                    songs = activePlaylistDetail!!.songs.filterNot { it.id == song.id }
                )
            }
        }

        // Handles Android's confirmation flow when deleting media requires user approval.
        val deleteLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                songToDelete?.let { song ->
                    removeSongFromLists(song)
                    songToDelete = null
                }
            }
        }

        fun deleteSongFromDevice(song: Song) {
            songToDelete = song
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(song.uri))
                    deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        context.contentResolver.delete(song.uri, null, null)
                        removeSongFromLists(song)
                        songToDelete = null
                    } catch (e: RecoverableSecurityException) {
                        deleteLauncher.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                    }
                } else {
                    val rows = context.contentResolver.delete(song.uri, null, null)
                    if (rows > 0) {
                        removeSongFromLists(song)
                        songToDelete = null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Focus requesters and shuffle history support transient interactions in the screens.
        val searchFocusRequester = remember { FocusRequester() }
        val playlistSearchFocusRequester = remember { FocusRequester() }
        val addSongSearchFocusRequester = remember { FocusRequester() }
        var playedShuffleIndices by remember { mutableStateOf(setOf<Int>()) }
        var shuffleQueue by remember { mutableStateOf<List<Song>>(emptyList()) }

        fun generateShuffleQueue(playlist: List<Song>, currentSong: Song?): List<Song> {
            if (playlist.isEmpty()) return emptyList()
            val remaining = if (currentSong != null) playlist.filter { it.id != currentSong.id }.shuffled() else playlist.shuffled()
            return if (currentSong != null) listOf(currentSong) + remaining else remaining
        }

        val activeSongList = if (isShuffleEnabled && shuffleQueue.isNotEmpty()) shuffleQueue else currentPlaylist

        // Playback commands coordinate the Media3 service.
        fun play(index: Int) {
            val song = activeSongList.getOrNull(index) ?: return
            currentIndex = index
            playedShuffleIndices = playedShuffleIndices + index
            val mc = controller ?: return
            mc.setMediaItems(activeSongList.map { it.toMediaItem() }, index, 0L)
            mc.prepare()
            mc.play()
            isPlaying = true
        }

        fun playInList(songList: List<Song>, index: Int) {
            val selectedSong = songList.getOrNull(index) ?: return
            currentPlaylist = songList
            if (isShuffleEnabled) {
                shuffleQueue = generateShuffleQueue(songList, selectedSong)
                play(0)
            } else {
                shuffleQueue = emptyList()
                play(index)
            }
        }

        fun pauseResume() {
            val mc = controller ?: return
            if (mc.isPlaying) mc.pause() else mc.play()
        }

        fun next() {
            if (activeSongList.isNotEmpty()) {
                if (currentIndex < activeSongList.size - 1) {
                    play(currentIndex + 1)
                } else if (isShuffleEnabled) {
                    val currentSong = activeSongList.getOrNull(currentIndex)
                    shuffleQueue = generateShuffleQueue(currentPlaylist, currentSong)
                    play(0)
                } else if (controller?.hasNextMediaItem() == true) {
                    controller.seekToNext()
                }
            }
        }

        fun prev() {
            if (activeSongList.isNotEmpty()) {
                if (currentIndex > 0) {
                    play(currentIndex - 1)
                } else if (controller?.hasPreviousMediaItem() == true) {
                    controller.seekToPrevious()
                }
            }
        }

        // Requests the platform permissions needed to read music and show playback notifications.
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
            val audioGranted = if (Build.VERSION.SDK_INT >= 33) {
                map[Manifest.permission.READ_MEDIA_AUDIO] == true
            } else {
                map[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            }
            if (audioGranted || hasPermissions()) {
                granted = true
            }
        }
        // Rechecks media access when the user returns from Android's Settings screen.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (hasPermissions()) granted = true
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(Unit) { if (!granted) launcher.launch(requiredPermissions()) }

        // Restore playlists once when the app screen is first composed.
        LaunchedEffect(Unit) {
            playlists = playlistRepository.loadPlaylists()
        }

        // Persist playlist edits through the repository instead of building JSON in the UI.
        LaunchedEffect(playlists) {
            playlistRepository.savePlaylists(playlists)
        }

        // Request the device library off the main thread after media permission is granted.
        LaunchedEffect(granted) {
            if (granted) {
                withContext(Dispatchers.IO) {
                    val libraryRepository = AudioLibraryRepository(this@MainActivity)
                    var songs = libraryRepository.loadSongs()
                    var retries = 0
                    while (songs.isEmpty() && retries < 3) {
                        delay(400)
                        songs = libraryRepository.loadSongs()
                        retries++
                    }
                    withContext(Dispatchers.Main) {
                        localSongs = songs
                        if (currentPlaylist.isEmpty()) currentPlaylist = songs
                    }
                }
            }
        }


        // Mirrors Media3 player state into Compose.
        if (controller != null) {
            isShuffleEnabled = controller.shuffleModeEnabled
            if (shuffleQueue.isEmpty() && controller.shuffleModeEnabled && localSongs.isNotEmpty()) {
                val controllerSongs = mutableListOf<Song>()
                for (i in 0 until controller.mediaItemCount) {
                    val item = controller.getMediaItemAt(i)
                    val id = item.mediaId.toLongOrNull() ?: 0L
                    val found = localSongs.find { it.id == id }
                        ?: playlists.asSequence().flatMap { it.songs }.find { it.id == id }
                        ?: Song(
                            id = id,
                            title = item.mediaMetadata.title?.toString() ?: "Unknown",
                            artist = item.mediaMetadata.artist?.toString() ?: "Unknown",
                            uri = item.localConfiguration?.uri ?: Uri.EMPTY,
                            albumId = -1
                        )
                    controllerSongs.add(found)
                }
                shuffleQueue = controllerSongs
            }
            if (currentPlaylist.isEmpty() && !controller.shuffleModeEnabled && localSongs.isNotEmpty()) {
                val controllerSongs = mutableListOf<Song>()
                for (i in 0 until controller.mediaItemCount) {
                    val item = controller.getMediaItemAt(i)
                    val id = item.mediaId.toLongOrNull() ?: 0L
                    val found = localSongs.find { it.id == id }
                        ?: playlists.asSequence().flatMap { it.songs }.find { it.id == id }
                        ?: Song(
                            id = id,
                            title = item.mediaMetadata.title?.toString() ?: "Unknown",
                            artist = item.mediaMetadata.artist?.toString() ?: "Unknown",
                            uri = item.localConfiguration?.uri ?: Uri.EMPTY,
                            albumId = -1
                        )
                    controllerSongs.add(found)
                }
                currentPlaylist = controllerSongs
            }

            isPlaying = controller.isPlaying
            duration = controller.duration.coerceAtLeast(0L)
            currentPosition = controller.currentPosition.coerceAtLeast(0L)

            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
                override fun onMediaItemTransition(m: MediaItem?, r: Int) {
                    val mediaId = m?.mediaId ?: controller.currentMediaItem?.mediaId
                    if (mediaId != null) {
                        val mappedIdx = activeSongList.indexOfFirst { it.id.toString() == mediaId }
                        if (mappedIdx != -1) currentIndex = mappedIdx
                    }
                    duration = controller.duration.coerceAtLeast(0L)
                }
                override fun onPlaybackStateChanged(s: Int) { duration = controller.duration.coerceAtLeast(0L) }
                override fun onPositionDiscontinuity(o: Player.PositionInfo, n: Player.PositionInfo, r: Int) { currentPosition = n.positionMs }
            })
        }

        // Keeps elapsed time synchronized with the active track.
        LaunchedEffect(isPlaying, controller, isDragging, currentIndex, duration, currentPosition, isBigPlayerVisible) {
            if (isPlaying && !isDragging && isBigPlayerVisible && controller != null) {
                while (isPlaying && !isDragging && isBigPlayerVisible) {
                    currentPosition = controller.currentPosition.coerceAtLeast(0L)
                    duration = controller.duration.coerceAtLeast(0L)
                    delay(500.milliseconds)
                }
            }
        }

        // Fades tracks out near the end and fades the next track in after playback starts.
        LaunchedEffect(isPlaying, isDragging, currentIndex, duration, currentPosition) {
            if (isPlaying && !isDragging && duration > 10_000L) {
                val remainingMs = duration - currentPosition
                
                if (remainingMs in 1L..10_000L) {
                    val fadeOutVolume = (remainingMs / 10_000f).coerceIn(0.05f, 1f)
                    controller?.volume = fadeOutVolume
                    
                    if (remainingMs <= 1000L && !isFadingOut) {
                        isFadingOut = true
                        next()
                    }
                } else if (remainingMs > 10_000L) {
                    isFadingOut = false
                    if (currentPosition in 1L..2500L) {
                        val fadeInVolume = (currentPosition / 2500f).coerceIn(0.05f, 1f)
                        controller?.volume = fadeInVolume
                    } else if (currentPosition > 2500L) {
                        controller?.volume = 1f
                    }
                }
            }
        }

        val isSubScreenOpen = isBigPlayerVisible || activePlaylistForAdding != null || activePlaylistDetail != null || isPlaylistSearchActive || isAddSongSearchActive || isSearchActive || showDeleteConfirmationDialog

        // Closes the most recently opened screen or search mode when Back is pressed.
        BackHandler(enabled = isSubScreenOpen) {
            when {
                isBigPlayerVisible -> {
                    isBigPlayerVisible = false
                }
                activePlaylistForAdding != null -> {
                    activePlaylistForAdding = null
                    isAddSongSearchActive = false
                    addSongSearchQuery = ""
                }
                activePlaylistDetail != null -> {
                    activePlaylistDetail = null
                    isPlaylistSearchActive = false
                    playlistSearchQuery = ""
                }
                isPlaylistSearchActive -> {
                    isPlaylistSearchActive = false
                    playlistSearchQuery = ""
                }
                isAddSongSearchActive -> {
                    isAddSongSearchActive = false
                    addSongSearchQuery = ""
                }
                isSearchActive -> {
                    isSearchActive = false
                    searchQuery = ""
                }
            }
        }

        // Main app surface: background player host, library screens, navigation and overlays.
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            // Header and library content switch between tracks, playlists, search, and details.
            Column(Modifier.fillMaxSize()) {
                if (activePlaylistDetail == null && activePlaylistForAdding == null) {
                    LibraryHeader(
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        searchFocusRequester = searchFocusRequester,
                        onSearchQueryChange = { query ->
                            searchQuery = query
                            val trimmedQuery = query.trim()
                            searchResults = if (trimmedQuery.isEmpty()) {
                                localSongs
                            } else {
                                localSongs.filter { song ->
                                    song.title.contains(trimmedQuery, ignoreCase = true) ||
                                        song.artist.contains(trimmedQuery, ignoreCase = true)
                                }
                            }
                        },
                        onExitSearch = {
                            isSearchActive = false
                            searchResults = emptyList()
                            searchQuery = ""
                        },
                        onAddPlaylistAction = {
                            if (currentIndex != -1 && currentPlaylist.isNotEmpty()) {
                                showAddToPlaylistDialog = true
                            } else {
                                showCreatePlaylistDialog = true
                            }
                        },
                        onStartSearch = {
                            isSearchActive = true
                            searchResults = localSongs
                        }
                    )
                }

                Box(Modifier.weight(1f)) {
                    if (!granted) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clickable {
                                    if (hasPermissions()) {
                                        granted = true
                                    } else {
                                        launcher.launch(requiredPermissions())
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Music permission required (Tap to grant)", color = Color.White, fontFamily = NdotFont, letterSpacing = 1.sp)
                        }
                    }
                    else {
                        val currentSongId = remember(currentIndex, currentPlaylist, isShuffleEnabled, shuffleQueue) { 
                            activeSongList.getOrNull(currentIndex)?.id ?: controller?.currentMediaItem?.mediaId?.toLongOrNull() 
                        }
                        if (activePlaylistForAdding != null) {
                            val targetIndex = activePlaylistForAdding!!
                            val currentPl = playlists.getOrNull(targetIndex)
                            if (currentPl != null) {
                                AddSongsToPlaylistView(
                                    playlistName = currentPl.name,
                                    currentSongs = currentPl.songs,
                                    allLocalSongs = localSongs,
                                    isSearchActive = isAddSongSearchActive,
                                    searchQuery = addSongSearchQuery,
                                    searchFocusRequester = addSongSearchFocusRequester,
                                    onSearchToggle = { isAddSongSearchActive = it },
                                    onSearchQueryChange = { addSongSearchQuery = it },
                                    onGoBack = {
                                        activePlaylistForAdding = null
                                        isAddSongSearchActive = false
                                        addSongSearchQuery = ""
                                    },
                                    onDone = { tempSongs ->
                                        val updatedList = playlists.toMutableList()
                                        val updatedPlaylist = currentPl.copy(songs = tempSongs)
                                        updatedList[targetIndex] = updatedPlaylist
                                        playlists = updatedList
                                        if (activePlaylistDetail?.name == currentPl.name) {
                                            activePlaylistDetail = updatedPlaylist
                                        }
                                        activePlaylistForAdding = null
                                        isAddSongSearchActive = false
                                        addSongSearchQuery = ""
                                    }
                                )
                            }
                        } else if (activePlaylistDetail != null) {
                            val pl = activePlaylistDetail!!
                            val plIdx = playlists.indexOfFirst { it.name == pl.name }
                            PlaylistDetailView(
                                playlistName = pl.name,
                                songs = pl.songs,
                                currentPlayingList = currentPlaylist,
                                currentIndex = currentIndex,
                                isPlaying = isPlaying,
                                isShuffleEnabled = isShuffleEnabled,
                                isSearchActive = isPlaylistSearchActive,
                                searchQuery = playlistSearchQuery,
                                searchFocusRequester = playlistSearchFocusRequester,
                                onSearchToggle = { isPlaylistSearchActive = it },
                                onSearchQueryChange = { playlistSearchQuery = it },
                                onBackClick = { activePlaylistDetail = null },
                                onAddClick = {
                                    if (plIdx != -1) {
                                        activePlaylistForAdding = plIdx
                                    } else {
                                        val existingIdx = playlists.indexOfFirst { it.name.equals(pl.name, ignoreCase = true) }
                                        if (existingIdx != -1) {
                                            activePlaylistForAdding = existingIdx
                                        } else {
                                            playlists = playlists + Playlist(pl.name, pl.songs)
                                            activePlaylistForAdding = playlists.size - 1
                                        }
                                    }
                                },
                                onPlayPlaylist = {
                                    if (pl.songs.isNotEmpty()) {
                                        playInList(pl.songs, 0)
                                    }
                                },
                                onPauseResume = { pauseResume() },
                                onToggleShuffle = {
                                    if (pl.songs.isNotEmpty()) {
                                        currentPlaylist = pl.songs
                                        val currentSong = activeSongList.getOrNull(currentIndex)
                                        if (!isShuffleEnabled) {
                                            isShuffleEnabled = true
                                            controller?.shuffleModeEnabled = true
                                            val newQueue = generateShuffleQueue(pl.songs, currentSong)
                                            shuffleQueue = newQueue
                                            if (currentSong != null) {
                                                currentIndex = newQueue.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
                                            } else {
                                                currentIndex = 0
                                                if (!isPlaying) play(0)
                                            }
                                        } else {
                                            isShuffleEnabled = false
                                            controller?.shuffleModeEnabled = false
                                            shuffleQueue = emptyList()
                                            if (currentSong != null) {
                                                currentIndex = pl.songs.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
                                            }
                                        }
                                    }
                                },
                                onSelectSong = { songList, index ->
                                    focusManager.clearFocus()
                                    playInList(songList, index)
                                    isBigPlayerVisible = true
                                },
                                onRemoveSong = { s ->
                                    val updatedSongs = pl.songs.filter { it.id != s.id }
                                    val updatedPlaylist = pl.copy(songs = updatedSongs)
                                    if (plIdx != -1) {
                                        val updatedList = playlists.toMutableList()
                                        updatedList[plIdx] = updatedPlaylist
                                        playlists = updatedList
                                    }
                                    activePlaylistDetail = updatedPlaylist
                                },
                                onAddToOtherPlaylist = { s ->
                                    currentIndex = activeSongList.indexOfFirst { it.id == s.id }
                                    showAddToPlaylistDialog = true
                                },
                                currentSongId = currentSongId
                            )
                        } else if (selectedTab == 0) {
                            val displaySongs = if (isSearchActive) searchResults else localSongs
                            val currentSongId = remember(currentIndex, currentPlaylist, isShuffleEnabled, shuffleQueue) { 
                                activeSongList.getOrNull(currentIndex)?.id ?: controller?.currentMediaItem?.mediaId?.toLongOrNull() 
                            }
                            TrackLibraryList(
                                songs = displaySongs,
                                currentSongId = currentSongId,
                                openMenuFor = trackMenuSong,
                                onSelectSong = { song ->
                                    focusManager.clearFocus()
                                    val targetIdx = displaySongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                    playInList(displaySongs, targetIdx)
                                    isBigPlayerVisible = true
                                },
                                onOpenMenu = { trackMenuSong = it },
                                onAddToPlaylist = { song ->
                                    currentIndex = displaySongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                    showAddToPlaylistDialog = true
                                },
                                onDeleteSong = { song ->
                                    songToDelete = song
                                    showDeleteConfirmationDialog = true
                                }
                            )
                        } else {
                            PlaylistLibraryList(
                                playlists = playlists,
                                localSongs = localSongs,
                                currentPlaylist = currentPlaylist,
                                isPlaying = isPlaying,
                                openMenuFor = playlistMenuName,
                                onOpenPlaylist = { activePlaylistDetail = it },
                                onOpenMenu = { playlistMenuName = it },
                                onDeletePlaylist = {
                                    playlistToDelete = it
                                    showDeletePlaylistConfirmationDialog = true
                                }
                            )
                        }
                    }
                }

                // Mini-player and bottom tabs stay anchored below the scrollable library.
                Box(Modifier.fillMaxWidth()) {
                    Column {
                        AnimatedVisibility(visible = currentIndex != -1) {
                            val song = activeSongList.getOrNull(currentIndex)
                            Row(Modifier.fillMaxWidth().background(Color(0xFF111111)).clickable { isBigPlayerVisible = true }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(text = song?.title ?: "", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp, maxLines = 1); Text(song?.artist ?: "", color = Color.Gray, fontSize = 11.sp, maxLines = 1) }
                                IconButton(onClick = { prev() }, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(26.dp)) }
                                IconButton(onClick = { pauseResume() }, modifier = Modifier.size(48.dp)) { Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause", tint = NothingRed, modifier = Modifier.size(26.dp)) }
                                IconButton(onClick = { next() }, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(26.dp)) }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF080808)).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable { selectedTab = 0; activePlaylistDetail = null; activePlaylistForAdding = null }) { Text(text = "TRACKS", color = if (selectedTab == 0 && activePlaylistDetail == null) NothingRed else Color.Gray, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = 3.sp); Spacer(Modifier.height(4.dp)); Box(Modifier.size(4.dp).background(if (selectedTab == 0 && activePlaylistDetail == null) NothingRed else Color.Transparent, CircleShape)) }
                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFF222222)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable { selectedTab = 1; activePlaylistDetail = null; activePlaylistForAdding = null }) { Text(text = "PLAYLISTS", color = if (selectedTab == 1 || activePlaylistDetail != null) NothingRed else Color.Gray, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = 3.sp); Spacer(Modifier.height(4.dp)); Box(Modifier.size(4.dp).background(if (selectedTab == 1 || activePlaylistDetail != null) NothingRed else Color.Transparent, CircleShape)) }
                        }
                    }
                }
            }
            // Modal dialogs are rendered above the library and report actions through callbacks.
            if (showCreatePlaylistDialog) {
                CreatePlaylistDialog(
                    newPlaylistName = newPlaylistName,
                    onNameChange = { newPlaylistName = it },
                    onDismiss = { showCreatePlaylistDialog = false; newPlaylistName = "" },
                    onCreate = { name ->
                        playlists = playlists + Playlist(name, emptyList())
                        newPlaylistName = ""
                        showCreatePlaylistDialog = false
                    }
                )
            }
            if (showAddToPlaylistDialog) {
                val currentSong = activeSongList.getOrNull(currentIndex)
                AddToPlaylistDialog(
                    currentSong = currentSong,
                    playlists = playlists,
                    onDismiss = { showAddToPlaylistDialog = false },
                    onAddToPlaylist = { idx ->
                        if (currentSong != null) {
                            val targetPl = playlists[idx]
                            val isAlreadyIn = targetPl.songs.any { it.id == currentSong.id }
                            if (!isAlreadyIn) {
                                val updated = playlists.toMutableList()
                                updated[idx] = targetPl.copy(songs = targetPl.songs + currentSong)
                                playlists = updated
                            }
                        }
                        showAddToPlaylistDialog = false
                    },
                    onCreateNewClick = {
                        showAddToPlaylistDialog = false
                        showCreatePlaylistDialog = true
                    }
                )
            }
            if (showDeleteConfirmationDialog && songToDelete != null) {
                DeleteSongConfirmationDialog(
                    song = songToDelete,
                    onDismiss = {
                        showDeleteConfirmationDialog = false
                        songToDelete = null
                    },
                    onConfirmDelete = { song ->
                        showDeleteConfirmationDialog = false
                        deleteSongFromDevice(song)
                    }
                )
            }
            if (showDeletePlaylistConfirmationDialog && playlistToDelete != null) {
                DeletePlaylistConfirmationDialog(
                    playlistName = playlistToDelete,
                    onDismiss = {
                        showDeletePlaylistConfirmationDialog = false
                        playlistToDelete = null
                    },
                    onConfirmDelete = { name ->
                        showDeletePlaylistConfirmationDialog = false
                        playlists = playlists.filter { it.name != name }
                        if (activePlaylistDetail?.name == name) {
                            activePlaylistDetail = null
                        }
                        playlistToDelete = null
                    }
                )
            }
            // Full-screen player overlay receives state and sends user actions to the activity.
            AnimatedVisibility(visible = isBigPlayerVisible, enter = slideInVertically(initialOffsetY = { it }), exit = slideOutVertically(targetOffsetY = { it })) {
                BigPlayer(
                    songs = activeSongList,
                    currentIndex = currentIndex,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onClose = { isBigPlayerVisible = false },
                    onPlayPause = { pauseResume() },
                    onNext = { next() },
                    onPrev = { prev() },
                    onSeek = { pos -> currentPosition = pos; controller?.seekTo(pos) },
                    onDragging = { dragging, moved -> if (dragging) { if (!isDragging) wasPlayingBeforeDrag = isPlaying; isDragging = true; if (isPlaying) controller?.pause() } else { isDragging = false; if (moved || wasPlayingBeforeDrag) controller?.play() } },
                    onSeekToSong = { index -> if (index != currentIndex) play(index) },
                    isShuffleEnabled = isShuffleEnabled,
                    onToggleShuffle = { enable ->
                        val currentSong = activeSongList.getOrNull(currentIndex)
                        isShuffleEnabled = enable
                        controller?.shuffleModeEnabled = enable
                        if (enable) {
                            val newQueue = generateShuffleQueue(currentPlaylist, currentSong)
                            shuffleQueue = newQueue
                            if (currentSong != null) {
                                currentIndex = newQueue.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
                            } else {
                                currentIndex = 0
                            }
                        } else {
                            shuffleQueue = emptyList()
                            if (currentSong != null) {
                                currentIndex = currentPlaylist.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
                            }
                        }
                    }
                )
            }
        }
    }

    // Checks the Android-version-specific permission required to read audio files.
    private fun hasPermissions(): Boolean = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    // Returns only the runtime permissions relevant to the current Android version.
    private fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
