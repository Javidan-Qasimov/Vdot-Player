package com.nothing.mp3player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.mp3player.model.Song
import com.nothing.mp3player.utils.NothingRed
import com.nothing.mp3player.utils.formatTime

/** Renders a playlist header, playback actions, search controls, and its song list. */
@Composable
fun PlaylistDetailView(
    playlistName: String,
    songs: List<Song>,
    currentPlayingList: List<Song>,
    currentIndex: Int,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    isSearchActive: Boolean,
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    onSearchToggle: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onPlayPlaylist: () -> Unit,
    onPauseResume: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSelectSong: (List<Song>, Int) -> Unit,
    onRemoveSong: (Song) -> Unit,
    onAddToOtherPlaylist: (Song) -> Unit,
    currentSongId: Long?
) {
    var menuSong by remember { mutableStateOf<Song?>(null) }
    val totalDurationMs = songs.sumOf { it.durationMs ?: 0L }
    val isThisPlaylistActive = currentPlayingList == songs
    val isThisPlaylistPlaying = isThisPlaylistActive && isPlaying

    Column(Modifier.fillMaxSize()) {
        // 1. Top Sub-Header Toolbar (Back Button on Left, Plus & Search Icons on Right AT THE TOP)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSearchActive) {
                var textState by remember { mutableStateOf(searchQuery) }
                LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
                TextField(
                    value = textState,
                    onValueChange = {
                        textState = it
                        onSearchQueryChange(it)
                    },
                    placeholder = { Text("Search in list...", color = Color.Gray, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.weight(1f).focusRequester(searchFocusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NothingRed,
                        focusedIndicatorColor = NothingRed,
                        unfocusedIndicatorColor = Color.DarkGray
                    ),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                    singleLine = true,
                    leadingIcon = {
                        IconButton(onClick = { onSearchToggle(false) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    }
                )
            } else {
                IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Plus Button (Top Right)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onAddClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(26.dp)) {
                            val color = Color.White; val radius = 1.3.dp.toPx(); val step = 3.5.dp.toPx()
                            val dotIndices = listOf(1.2f, 2.5f, 3.8f, 5.1f, 6.4f); val center = 3.8f
                            dotIndices.forEach { i -> drawCircle(color, radius, center = Offset(i * step, center * step)) }
                            dotIndices.forEach { i -> if (i != center) drawCircle(color, radius, center = Offset(center * step, i * step)) }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))

                    // Search Button (Top Right)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onSearchToggle(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(26.dp)) {
                            val color = Color.White; val radius = 1.3.dp.toPx(); val step = 3.5.dp.toPx()
                            drawCircle(color, radius, center = Offset(1 * step, 3 * step))
                            drawCircle(color, radius, center = Offset(1.5f * step, 1.5f * step))
                            drawCircle(color, radius, center = Offset(3 * step, 1 * step))
                            drawCircle(color, radius, center = Offset(4.5f * step, 1.5f * step))
                            drawCircle(color, radius, center = Offset(5 * step, 3 * step))
                            drawCircle(color, radius, center = Offset(4.5f * step, 4.5f * step))
                            drawCircle(color, radius, center = Offset(3 * step, 5 * step))
                            drawCircle(color, radius, center = Offset(1.5f * step, 4.5f * step))
                            drawCircle(color, radius, center = Offset(4.5f * step, 4.5f * step))
                            drawCircle(color, radius, center = Offset(5.5f * step, 5.5f * step))
                            drawCircle(color, radius, center = Offset(6.5f * step, 6.5f * step))
                        }
                    }
                }
            }
        }

        // 2. Playlist Name & Info Section
        Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp)) {
            Text("PLAYLIST", color = NothingRed, fontFamily = NdotFont, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                playlistName.uppercase(),
                color = Color.White,
                fontFamily = NdotFont,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.5.sp,
                lineHeight = 36.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${songs.size} TRACKS  •  ${formatTime(totalDurationMs)}",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }

        // 3. Row UNDERNEATH Title: Black Space on Left, Play & Mix Buttons on RIGHT (Spotify Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play / Pause Button with smooth animated icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (songs.isNotEmpty()) {
                                if (!isThisPlaylistActive || !isPlaying) {
                                    onPlayPlaylist()
                                } else {
                                    onPauseResume()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(64.dp)) {
                        val dotRadius = 1.4.dp.toPx()
                        for (angle in 0 until 360 step 15) {
                            val rad = Math.toRadians(angle.toDouble())
                            drawCircle(NothingRed, dotRadius, Offset(size.width/2 + (size.width/2.1f) * Math.cos(rad).toFloat(), size.height/2 + (size.height/2.1f) * Math.sin(rad).toFloat()))
                        }
                    }
                    AnimatedContent(
                        targetState = isThisPlaylistPlaying,
                        transitionSpec = {
                            (fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) + scaleIn(initialScale = 0.7f)) togetherWith
                            (fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) + scaleOut(targetScale = 0.7f))
                        },
                        label = "PlayPausePlaylistAnim"
                    ) { playing ->
                        DottedIcon(
                            type = if (playing) DottedIconType.Pause else DottedIconType.Play,
                            color = NothingRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Mix (Shuffle) Button - SIDE-BY-SIDE on Right
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isShuffleEnabled && isThisPlaylistActive) NothingRed.copy(alpha = 0.25f) else Color(0xFF161616))
                        .clickable { onToggleShuffle() },
                    contentAlignment = Alignment.Center
                ) {
                    val dotColor = if (isShuffleEnabled && isThisPlaylistActive) NothingRed else Color.White
                    DottedShuffleIcon(
                        color = dotColor,
                        modifier = Modifier.size(width = 28.dp, height = 18.dp)
                    )
                }
            }
        }

        // 3. Song List
        val filtered = remember(searchQuery, songs) {
            if (searchQuery.isEmpty()) songs else songs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
        }
        val currentPlayingSongId = currentSongId
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(filtered, key = { _, s -> s.id }) { i, s ->
                val isCurrent = currentPlayingSongId == s.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSong(filtered, i) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).background(if (isCurrent) NothingRed else Color.DarkGray, shape = CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(text = "%02d".format(i + 1), color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(text = s.title, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                        Text(s.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                    }
                    Text(formatTime(s.durationMs ?: 0L), color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Spacer(Modifier.width(16.dp))
                    Box {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.DarkGray,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { menuSong = s }
                        )
                        if (menuSong == s) {
                            MaterialTheme(
                                colorScheme = MaterialTheme.colorScheme.copy(
                                    surface = Color(0xFF181818),
                                    surfaceTint = Color.Transparent,
                                    outlineVariant = Color.Transparent
                                ),
                                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenu(
                                    expanded = true,
                                    onDismissRequest = { menuSong = null },
                                    modifier = Modifier.background(Color(0xFF181818))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("+ ADD TO OTHER PLAYLIST", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                        onClick = {
                                            menuSong = null
                                            onAddToOtherPlaylist(s)
                                        }
                                    )
                                    HorizontalDivider(color = Color(0xFF2B2B2E))
                                    DropdownMenuItem(
                                        text = { Text("REMOVE FROM PLAYLIST", color = NothingRed, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            menuSong = null
                                            onRemoveSong(s)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF1A1A1A))
            }
        }
    }
}
