package dev.themobiledev.movie.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FavoriteRed = Color(0xFFFF3B30)
val RatingYellow = Color(0xFFFFCC00)

@Composable
fun MovieTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
