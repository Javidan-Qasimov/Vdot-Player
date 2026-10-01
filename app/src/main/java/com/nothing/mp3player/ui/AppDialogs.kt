package com.nothing.mp3player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.mp3player.model.Playlist
import com.nothing.mp3player.model.Song
import com.nothing.mp3player.utils.NothingRed

/** Collects a new playlist name and returns it to the caller for creation. */
@Composable
fun CreatePlaylistDialog(
    newPlaylistName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111111),
        shape = RectangleShape,
        title = {
            Text("NEW PLAYLIST", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        },
        text = {
            TextField(
                value = newPlaylistName,
                onValueChange = onNameChange,
                placeholder = { Text("Enter name...", color = Color.Gray, fontFamily = NdotFont) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Black,
                    unfocusedContainerColor = Color.Black,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = NothingRed,
                    focusedIndicatorColor = NothingRed,
                    unfocusedIndicatorColor = Color.DarkGray
                ),
                textStyle = TextStyle(fontFamily = NdotFont, fontSize = 16.sp),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (newPlaylistName.trim().isNotEmpty()) onCreate(newPlaylistName.trim()) }) {
                Text("CREATE", color = NothingRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

/** Displays the available playlists and reports the selected destination. */
@Composable
fun AddToPlaylistDialog(
    currentSong: Song?,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Int) -> Unit,
    onCreateNewClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111111),
        shape = RectangleShape,
        title = {
            Text("ADD TO PLAYLIST", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                if (currentSong != null) {
                    Text("Song: ${currentSong.title}", color = NothingRed, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                }
                if (playlists.isEmpty()) {
                    Text("No playlists created yet.", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(playlists.size) { idx ->
                            val pl = playlists[idx]
                            val isAlreadyIn = currentSong != null && pl.songs.any { it.id == currentSong.id }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (currentSong != null && !isAlreadyIn) onAddToPlaylist(idx) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(pl.name.uppercase(), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                                if (isAlreadyIn) {
                                    Text("ADDED", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                } else {
                                    Text("+ ADD", color = NothingRed, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(color = Color(0xFF222222))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onCreateNewClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ CREATE NEW PLAYLIST", color = NothingRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun DeleteSongConfirmationDialog(
    song: Song?,
    onDismiss: () -> Unit,
    onConfirmDelete: (Song) -> Unit
) {
    if (song == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111111),
        shape = RectangleShape,
        title = {
            Text("DELETE SONG FROM DEVICE", color = NothingRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text("Are you sure you want to permanently delete this song from your device storage?", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                Text("Song: ${song.title}", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("Artist: ${song.artist}", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmDelete(song) }) {
                Text("DELETE", color = NothingRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun DeletePlaylistConfirmationDialog(
    playlistName: String?,
    onDismiss: () -> Unit,
    onConfirmDelete: (String) -> Unit
) {
    if (playlistName == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111111),
        shape = RectangleShape,
        title = {
            Text("DELETE PLAYLIST", color = NothingRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text("Are you sure you want to delete this playlist?", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                Text("Playlist: $playlistName", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmDelete(playlistName) }) {
                Text("DELETE", color = NothingRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
