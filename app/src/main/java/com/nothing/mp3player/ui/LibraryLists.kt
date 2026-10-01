package com.nothing.mp3player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nothing.mp3player.model.Playlist
import com.nothing.mp3player.model.Song
import com.nothing.mp3player.utils.NothingRed
import com.nothing.mp3player.utils.formatTime

/** Renders searchable tracks and forwards row actions to the screen state owner. */
@Composable
fun TrackLibraryList(
    songs: List<Song>,
    currentSongId: Long?,
    openMenuFor: Song?,
    onSelectSong: (Song) -> Unit,
    onOpenMenu: (Song?) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { song ->
            val isCurrent = currentSongId == song.id
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSong(song) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (isCurrent) NothingRed else Color.DarkGray, CircleShape)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    Text(song.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                }
                Text(
                    formatTime(song.durationMs ?: 0L),
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(16.dp))
                Box {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(20.dp).clickable { onOpenMenu(song) }
                    )
                    if (openMenuFor == song) {
                        MaterialTheme(
                            colorScheme = MaterialTheme.colorScheme.copy(
                                surface = Color(0xFF181818),
                                surfaceTint = Color.Transparent,
                                outlineVariant = Color.Transparent
                            ),
                            shapes = MaterialTheme.shapes.copy(extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { onOpenMenu(null) },
                                modifier = Modifier.background(Color(0xFF181818))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "+ ADD TO PLAYLIST",
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        onOpenMenu(null)
                                        onAddToPlaylist(song)
                                    }
                                )
                                HorizontalDivider(color = Color(0xFF2B2B2E))
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "DELETE FROM DEVICE",
                                            color = NothingRed,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        onOpenMenu(null)
                                        onDeleteSong(song)
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

/** Shows user playlists and artist groups, leaving playback and navigation decisions to the caller. */
@Composable
fun PlaylistLibraryList(
    playlists: List<Playlist>,
    localSongs: List<Song>,
    currentPlaylist: List<Song>,
    isPlaying: Boolean,
    openMenuFor: String?,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenMenu: (String?) -> Unit,
    onDeletePlaylist: (String) -> Unit
) {
    val artistGroups = remember(localSongs) { localSongs.groupBy { it.artist }.toSortedMap() }

    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "header_my_playlists") {
            Text(
                "MY PLAYLISTS",
                color = NothingRed,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }
        item(key = "playlist_favorites") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).background(Color.DarkGray, CircleShape))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "FAVORITES",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text("0 tracks", color = Color.Gray, fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = Color(0xFF1A1A1A))
        }
        items(playlists, key = { it.name }) { playlist ->
            val isPlayingPlaylist = isPlaying && currentPlaylist == playlist.songs && playlist.songs.isNotEmpty()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPlaylist(playlist) }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (isPlayingPlaylist) NothingRed else Color.DarkGray, CircleShape)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        playlist.name.uppercase(),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text("${playlist.songs.size} tracks", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(Modifier.width(16.dp))
                Box {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(20.dp).clickable { onOpenMenu(playlist.name) }
                    )
                    if (openMenuFor == playlist.name) {
                        MaterialTheme(
                            colorScheme = MaterialTheme.colorScheme.copy(
                                surface = Color(0xFF181818),
                                surfaceTint = Color.Transparent,
                                outlineVariant = Color.Transparent
                            ),
                            shapes = MaterialTheme.shapes.copy(extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { onOpenMenu(null) },
                                modifier = Modifier.background(Color(0xFF181818))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "DELETE PLAYLIST",
                                            color = NothingRed,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        onOpenMenu(null)
                                        onDeletePlaylist(playlist.name)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = Color(0xFF1A1A1A))
        }
        item(key = "header_artists") {
            Text(
                "ARTISTS",
                color = NothingRed,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }
        items(artistGroups.keys.toList(), key = { it }) { artistName ->
            val artistSongs = artistGroups[artistName] ?: emptyList()
            val isPlayingArtist = isPlaying && currentPlaylist == artistSongs && artistSongs.isNotEmpty()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPlaylist(Playlist(artistName, artistSongs)) }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (isPlayingArtist) NothingRed else Color.DarkGray, CircleShape)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        artistName.uppercase(),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text("${artistSongs.size} tracks", color = Color.Gray, fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = Color(0xFF1A1A1A))
        }
    }
}
