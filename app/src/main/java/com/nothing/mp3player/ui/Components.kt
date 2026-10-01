package com.nothing.mp3player.ui

import android.content.ContentUris
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nothing.mp3player.R
import com.nothing.mp3player.model.Song
import kotlin.math.max

val NdotFont = FontFamily(Font(R.font.ndot))

/** Draws the rotating record, album artwork, grooves, and center label. */
@Composable
fun VinylDisc(accumulatedRotation: Float, albumId: Long?, modifier: Modifier = Modifier, isStatic: Boolean = false) {
    val artUri = albumId?.takeIf { it >= 0 }?.let { ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), it) }
    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { val center = this.center; val radius = size.minDimension / 2; drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFF4A4A4A), Color(0xFF222222)), center = center, radius = radius), radius = radius); drawCircle(color = Color.White.copy(alpha = 0.15f), radius = radius, style = Stroke(width = 1.dp.toPx())) }
        Box(Modifier.fillMaxSize().graphicsLayer(rotationZ = if (isStatic) 0f else accumulatedRotation)) {
            Box(modifier = Modifier.align(Alignment.Center).fillMaxSize(0.48f).aspectRatio(1f).clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.Black))
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(artUri).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(0.72f).clip(CircleShape), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.Black))
            }
            Canvas(Modifier.fillMaxSize()) { val radius = size.minDimension / 2; for (i in 0..4) { val r = radius * (0.54f + (i / 4f) * 0.42f); drawCircle(color = Color.Gray.copy(alpha = 0.35f), radius = r, style = Stroke(width = 1.5.dp.toPx())) }; drawArc(brush = Brush.sweepGradient(0f to Color.Transparent, 0.08f to Color.White.copy(alpha = 0.04f), 0.15f to Color.Transparent, 0.5f to Color.Transparent, 0.58f to Color.White.copy(alpha = 0.04f), 0.65f to Color.Transparent, 1f to Color.Transparent), startAngle = 0f, sweepAngle = 360f, useCenter = true, style = Stroke(width = radius * 0.5f)) }
            Canvas(Modifier.fillMaxSize()) { val center = this.center; val radius = size.minDimension / 2; val holeRadius = 15.dp.toPx(); val dist = radius * 0.82f; drawCircle(Color.Black, holeRadius, Offset(center.x, center.y - dist)); drawCircle(Color.Black, holeRadius, Offset(center.x, center.y + dist)); drawCircle(Color.Black, holeRadius, Offset(center.x + dist, center.y)); drawCircle(Color.Black, holeRadius, Offset(center.x - dist, center.y)); val dotDist = radius * 0.72f; drawCircle(color = Color(0xFF990000), radius = 8.dp.toPx(), center = Offset(center.x + dotDist * 0.707f, center.y - dotDist * 0.707f)) }
        }
        Canvas(Modifier.size(12.dp)) { drawCircle(color = Color.Black, radius = 6.dp.toPx()) }
    }
}

enum class DottedIconType { Play, Pause, SkipNext, SkipPrev }

/** Builds the player's transport icons from a grid of dots. */
@Composable
fun DottedIcon(type: DottedIconType, color: Color, modifier: Modifier = Modifier, dotRadius: Dp = 1.5.dp, spacing: Dp = 5.dp) {
    Canvas(modifier = modifier) { val s = spacing.toPx(); val r = dotRadius.toPx(); val cols = (size.width / s).toInt(); val rows = (size.height / s).toInt(); val offsetX = (size.width - (cols * s)) / 2; val offsetY = (size.height - (rows * s)) / 2
        for (i in 0..cols) { for (j in 0..rows) { val x = offsetX + i * s; val y = offsetY + j * s; val shouldDraw = when (type) { DottedIconType.Pause -> { val barWidth = cols / 3; (i < barWidth) || (i > cols - barWidth) }; DottedIconType.Play -> { val progress = (cols - i).toFloat() / cols; val halfHeight = rows / 2f; val limit = halfHeight * progress; j >= halfHeight - limit && j <= halfHeight + limit }; DottedIconType.SkipNext -> { val barWidth = max(1, cols / 6); val triangleCols = cols - barWidth - 2; if (i > cols - barWidth) true else if (i < triangleCols) { val progress = (triangleCols - i).toFloat() / triangleCols; val halfHeight = rows / 2f; val limit = halfHeight * progress; j >= halfHeight - limit && j <= halfHeight + limit } else false }; DottedIconType.SkipPrev -> { val barWidth = max(1, cols / 6); if (i < barWidth) true else if (i > barWidth + 1) { val progress = (i - (barWidth + 1)).toFloat() / (cols - (barWidth + 1)); val halfHeight = rows / 2f; val limit = halfHeight * progress; j >= halfHeight - limit && j <= halfHeight + limit } else false } }; if (shouldDraw) drawCircle(color, r, Offset(x, y)) } }
    }
}

/** Draws the dotted mix / shuffle icon with cleanly spaced, non-overlapping dots. */
@Composable
fun DottedShuffleIcon(
    color: Color,
    modifier: Modifier = Modifier,
    dotRadius: Dp = 1.1.dp
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cols = 10f
        val rows = 6f
        val stepX = w / cols
        val stepY = h / rows
        val r = dotRadius.toPx()

        // Path 1 (Top-Left to Bottom-Right)
        for (i in 0..3) drawCircle(color, r, Offset(i * stepX, 1f * stepY))
        drawCircle(color, r, Offset(4 * stepX, 1.8f * stepY))
        drawCircle(color, r, Offset(5 * stepX, 3.0f * stepY))
        drawCircle(color, r, Offset(6 * stepX, 4.2f * stepY))
        drawCircle(color, r, Offset(7 * stepX, 5f * stepY))
        drawCircle(color, r, Offset(8 * stepX, 5f * stepY))
        drawCircle(color, r, Offset(9 * stepX, 4f * stepY))
        drawCircle(color, r, Offset(9 * stepX, 5f * stepY))
        drawCircle(color, r, Offset(9 * stepX, 6f * stepY))
        drawCircle(color, r, Offset(10 * stepX, 5f * stepY))

        // Path 2 (Bottom-Left to Top-Right)
        for (i in 0..3) drawCircle(color, r, Offset(i * stepX, 5f * stepY))
        drawCircle(color, r, Offset(4 * stepX, 4.2f * stepY))
        drawCircle(color, r, Offset(5 * stepX, 3.0f * stepY))
        drawCircle(color, r, Offset(6 * stepX, 1.8f * stepY))
        drawCircle(color, r, Offset(7 * stepX, 1f * stepY))
        drawCircle(color, r, Offset(8 * stepX, 1f * stepY))
        drawCircle(color, r, Offset(9 * stepX, 0f * stepY))
        drawCircle(color, r, Offset(9 * stepX, 1f * stepY))
        drawCircle(color, r, Offset(9 * stepX, 2f * stepY))
        drawCircle(color, r, Offset(10 * stepX, 1f * stepY))
    }
}

/** Uses blurred album artwork as the ambient background for a song. */
@Composable
fun AmbientGlow(song: Song?, modifier: Modifier = Modifier) {
    val artUri = song?.albumId?.takeIf { it >= 0 }?.let { ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), it) }
    
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.6f)
                .graphicsLayer { alpha = 1f }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = 0f,
                            endY = size.height * 0.7f
                        )
                    )
                }
                .blur(80.dp),
            contentScale = ContentScale.Crop
        )
    }
}
