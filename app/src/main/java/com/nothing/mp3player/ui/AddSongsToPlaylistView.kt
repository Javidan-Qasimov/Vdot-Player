package com.nothing.mp3player.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** Lets the user search the device library and select songs for a playlist. */
@Composable
fun AddSongsToPlaylistView(
    playlistName: String,
    currentSongs: List<Song>,
    allLocalSongs: List<Song>,
    isSearchActive: Boolean,
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    onSearchToggle: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onGoBack: () -> Unit,
    onDone: (List<Song>) -> Unit
) {
    var tempSongs by remember(playlistName) { mutableStateOf(currentSongs) }
    val isModified = tempSongs != currentSongs

    Column(Modifier.fillMaxSize()) {
        // Top Bar: GO BACK & DONE
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onGoBack() }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("GO BACK", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "DONE",
                color = if (isModified) NothingRed else Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = isModified) { onDone(tempSongs) }
            )
        }

        Spacer(Modifier.height(4.dp))

        // Search Bar / Row Below DONE
        if (isSearchActive) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var textState by remember { mutableStateOf(searchQuery) }
                LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
                TextField(
                    value = textState,
                    onValueChange = {
                        textState = it
                        onSearchQueryChange(it)
                    },
                    placeholder = { Text("Search songs to add...", color = Color.Gray, fontFamily = FontFamily.Monospace) },
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
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
                    singleLine = true,
                    leadingIcon = {
                        IconButton(onClick = { onSearchToggle(false) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Search", tint = Color.White)
                        }
                    }
                )
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SELECT TRACKS", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 1.sp)
                
                // Search Button Icon (Ndot Style)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onSearchToggle(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val color = Color.White
                        val radius = 1.2.dp.toPx()
                        val step = 3.5.dp.toPx()
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

        Spacer(Modifier.height(6.dp))

        // Filtered Song List
        val songsToDisplay = remember(searchQuery, allLocalSongs) {
            if (searchQuery.trim().isEmpty()) {
                allLocalSongs
            } else {
                allLocalSongs.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
                }
            }
        }
        val tempSongIds = remember(tempSongs) { tempSongs.mapTo(HashSet()) { it.id } }

        LazyColumn(Modifier.weight(1f)) {
            items(songsToDisplay, key = { it.id }) { s ->
                val isAlreadyIn = tempSongIds.contains(s.id)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newList = tempSongs.toMutableList()
                            if (isAlreadyIn) newList.removeAll { it.id == s.id } else newList.add(s)
                            tempSongs = newList
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).background(if (isAlreadyIn) NothingRed else Color.DarkGray, shape = CircleShape))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(text = s.title, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                        Text(s.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                    }
                    if (isAlreadyIn) Icon(Icons.Default.Check, contentDescription = "Added", tint = NothingRed, modifier = Modifier.size(18.dp))
                }
                HorizontalDivider(color = Color(0xFF1A1A1A))
            }
        }
    }
}
