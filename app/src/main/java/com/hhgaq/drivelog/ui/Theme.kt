package com.hhgaq.drivelog.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object DriveColors {
    val Brake = Color(0xFFE5484D)
    val Turn = Color(0xFFF5A524)
    val Accel = Color(0xFF30A46C)
    val Harsh = Color(0xFFFF6B3D)
    val Idle = Color(0xFF2A2F3A)
    val Panel = Color(0xFF161A22)
    val Muted = Color(0xFF8B93A5)
}

@Composable
fun DriveLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4C8DFF),
            background = Color(0xFF0E1117),
            surface = DriveColors.Panel,
            onBackground = Color(0xFFE8EAF0),
            onSurface = Color(0xFFE8EAF0),
        ),
        content = content,
    )
}
