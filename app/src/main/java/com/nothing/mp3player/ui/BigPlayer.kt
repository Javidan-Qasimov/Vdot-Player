package com.nothing.mp3player.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.mp3player.model.Song
import com.nothing.mp3player.utils.NothingRed
import com.nothing.mp3player.utils.formatTime
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/** Full-screen playback controls, track paging, and vinyl display. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BigPlayer(
    songs: List<Song>, 
    currentIndex: Int, 
    isPlaying: Boolean, 
    currentPosition: Long, 
    duration: Long, 
    onClose: () -> Unit, 
    onPlayPause: () -> Unit, 
    onNext: () -> Unit, 
    onPrev: () -> Unit, 
    onSeek: (Long) -> Unit, 
    onDragging: (Boolean, Boolean) -> Unit, 
    onSeekToSong: (Int) -> Unit,
    isShuffleEnabled: Boolean,
    onToggleShuffle: (Boolean) -> Unit
) {
    val songCount = songs.size
    val safeIndex = if (songCount > 0) currentIndex.coerceIn(0, songCount - 1) else 0

    val pagerState = rememberPagerState(
        initialPage = safeIndex,
        pageCount = { songCount }
    )

    val currentOnSeekToSong by rememberUpdatedState(onSeekToSong)

    var localDragPosition by remember { mutableStateOf<Long?>(null) }
    var accumulatedRotation by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val speed = 360f / 20f
            var last = System.currentTimeMillis()
            while (isPlaying) {
                delay(16.milliseconds)
                val now = System.currentTimeMillis()
                val diff = now - last
                accumulatedRotation += speed * (diff / 1000f)
                last = now
            }
        }
    }
    val displayPosition = localDragPosition ?: currentPosition
    val currentSong = songs.getOrNull(if (songCount > 0) pagerState.currentPage.coerceIn(0, songCount - 1) else 0)

    // 1. Sync external track index changes (e.g., Next/Prev button or playlist tap) -> Pager
    LaunchedEffect(safeIndex, songCount) {
        if (songCount > 0 && pagerState.currentPage != safeIndex && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(safeIndex)
        }
    }

    // 2. Sync user swipe gestures on Pager -> Player state (triggers when pager settles on a page)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                if (songCount > 0 && page in 0 until songCount && page != safeIndex) {
                    currentOnSeekToSong(page)
                }
            }
    }
    
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(Unit) {
            detectVerticalDragGestures { _, dragAmount ->
                if (dragAmount > 30f) onClose()
            }
        }
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) { 
            val spacing = 32.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxDist = sqrt(center.x.pow(2) + center.y.pow(2))
            val cols = (size.width / spacing).toInt()
            val rows = (size.height / spacing).toInt()
            for (i in 0..cols) { 
                for (j in 0..rows) { 
                    val x = i * spacing
                    val y = j * spacing
                    val dist = sqrt((x - center.x).pow(2) + (y - center.y).pow(2))
                    val alphaFactor = (1f - (dist / maxDist) * 0.5f).coerceIn(0f, 1f)
                    drawCircle(color = Color.White.copy(alpha = 0.25f * alphaFactor), radius = 1.0.dp.toPx(), center = Offset(x, y)) 
                } 
            } 
        }

        Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 16.dp).graphicsLayer(clip = false), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 32.dp)
                .graphicsLayer(clip = false)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 30f) onClose()
                    }
                }, 
                contentAlignment = Alignment.TopCenter
            ) {
                Column(modifier = Modifier.graphicsLayer(clip = false), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = currentSong?.title?.uppercase() ?: "UNKNOWN", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, delayMillis = 2000))
                    Spacer(modifier = Modifier.height(4.dp)); Text(text = currentSong?.artist ?: "Unknown Artist", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 16.sp, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, delayMillis = 2000))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Hosts the swipeable vinyl carousel.
            Box(modifier = Modifier.fillMaxWidth().height(410.dp).graphicsLayer(clip = false), contentAlignment = Alignment.Center) {
                HorizontalPager(
                    state = pagerState,
                    beyondBoundsPageCount = 1,
                    modifier = Modifier.fillMaxSize().graphicsLayer(clip = false),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 16.dp
                ) { page ->
                    val song = songs.getOrNull(page)
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    val scale = (1.04f - (abs(pageOffset) * 0.4f)).coerceIn(0.6f, 1.04f)
                    Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }, contentAlignment = Alignment.Center) {
                        VinylDisc(accumulatedRotation = if (page == currentIndex) accumulatedRotation else 0f, albumId = song?.albumId, isStatic = page != currentIndex)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Provides seek progress, track timing, shuffle, and transport controls.
            Column(modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatTime(displayPosition),
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.graphicsLayer { translationY = 20.dp.toPx() }
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatTime(duration),
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.graphicsLayer { translationY = 20.dp.toPx() }
                        )
                    }
                    ShuffleButton(isShuffleEnabled, onToggleShuffle, Modifier.align(Alignment.TopStart).graphicsLayer { translationX = (-12).dp.toPx(); translationY = (-18).dp.toPx() })
                }

                Slider(value = if (duration > 0) displayPosition.toFloat() else 0f, onValueChange = { if (localDragPosition == null) onDragging(true, false); localDragPosition = it.toLong() }, onValueChangeFinished = { localDragPosition?.let { onSeek(it) }; localDragPosition = null; onDragging(false, false) }, valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f), colors = SliderDefaults.colors(activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent),
                    track = { sliderState ->
                        val fraction = if (sliderState.valueRange.endInclusive > sliderState.valueRange.start) (sliderState.value - sliderState.valueRange.start) / (sliderState.valueRange.endInclusive - sliderState.valueRange.start) else 0f
                        Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) { val dotSpacing = 10.dp.toPx(); val dotRadius = 2.8.dp.toPx(); val numDots = (size.width / dotSpacing).toInt(); for (i in 0..numDots) { val x = i * dotSpacing; val isPlayed = x <= size.width * fraction; drawCircle(color = if (isPlayed) Color(0xFF990000) else Color.DarkGray.copy(alpha = 0.4f), radius = dotRadius, center = Offset(x, size.height / 2)) } }
                    },
                    thumb = { Box(modifier = Modifier.width(12.dp).height(48.dp), contentAlignment = Alignment.Center) { Box(Modifier.size(12.dp).background(Color(0xFF990000), CircleShape)) } },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Box(Modifier.size(48.dp).clickable { onPrev() }, contentAlignment = Alignment.Center) { DottedIcon(type = DottedIconType.SkipPrev, color = Color.LightGray, modifier = Modifier.size(32.dp)) }
                    Spacer(Modifier.width(64.dp))
                    Box(Modifier.size(80.dp).clip(CircleShape).clickable { onPlayPause() }, contentAlignment = Alignment.Center) { DottedIcon(type = if (isPlaying) DottedIconType.Pause else DottedIconType.Play, color = NothingRed, modifier = Modifier.size(32.dp)) }
                    Spacer(Modifier.width(64.dp))
                    Box(Modifier.size(48.dp).clickable { onNext() }, contentAlignment = Alignment.Center) { DottedIcon(type = DottedIconType.SkipNext, color = Color.LightGray, modifier = Modifier.size(32.dp)) }
                }
            }
            Spacer(modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 30f) onClose()
                    }
                }
            )
         }
    }
}

@Composable
private fun ShuffleButton(isShuffleEnabled: Boolean, onToggleShuffle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(44.dp).clickable { onToggleShuffle(!isShuffleEnabled) },
        contentAlignment = Alignment.Center
    ) {
        val dotColor = if (isShuffleEnabled) Color(0xFFB30000) else Color.White.copy(alpha = 0.8f)
        DottedShuffleIcon(
            color = dotColor,
            modifier = Modifier.size(width = 32.dp, height = 20.dp)
        )
    }
}
