package com.nothing.mp3player.utils

import androidx.compose.ui.graphics.Color

val NothingRed = Color(0xFFD01010)

fun formatTime(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 1000) / 60
    return "%02d:%02d".format(min, sec)
}
