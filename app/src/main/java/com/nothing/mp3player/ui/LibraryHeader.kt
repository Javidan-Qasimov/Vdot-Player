package com.nothing.mp3player.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.mp3player.utils.NothingRed

/** Displays either the library search field or the title and top-level library actions. */
@Composable
fun LibraryHeader(
    isSearchActive: Boolean,
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onExitSearch: () -> Unit,
    onAddPlaylistAction: () -> Unit,
    onStartSearch: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isSearchActive) {
            val focusManager = LocalFocusManager.current
            LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }

            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search...", color = Color.Gray, fontFamily = FontFamily.Monospace) },
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
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                leadingIcon = {
                    IconButton(onClick = onExitSearch) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { focusManager.clearFocus() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                }
            )
        } else {
            Row(
                Modifier.fillMaxWidth().padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "VDOT PLAYER",
                    color = NothingRed,
                    fontFamily = NdotFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 28.sp,
                    letterSpacing = 2.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clickable(onClick = onAddPlaylistAction),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.size(26.dp)) {
                            val dotColor = Color.White
                            val dotRadius = 1.3.dp.toPx()
                            val step = 3.5.dp.toPx()
                            val positions = listOf(1.2f, 2.5f, 3.8f, 5.1f, 6.4f)
                            val center = 3.8f
                            positions.forEach { x -> drawCircle(dotColor, dotRadius, Offset(x * step, center * step)) }
                            positions.forEach { y -> drawCircle(dotColor, dotRadius, Offset(center * step, y * step)) }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.size(40.dp).clickable(onClick = onStartSearch),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.size(26.dp)) {
                            val dotColor = Color.White
                            val dotRadius = 1.3.dp.toPx()
                            val step = 3.5.dp.toPx()
                            listOf(
                                1f to 3f, 1.5f to 1.5f, 3f to 1f, 4.5f to 1.5f,
                                5f to 3f, 4.5f to 4.5f, 3f to 5f, 1.5f to 4.5f,
                                4.5f to 4.5f, 5.5f to 5.5f, 6.5f to 6.5f
                            ).forEach { (x, y) ->
                                drawCircle(dotColor, dotRadius, center = Offset(x * step, y * step))
                            }
                        }
                    }
                }
            }
        }
    }
}
