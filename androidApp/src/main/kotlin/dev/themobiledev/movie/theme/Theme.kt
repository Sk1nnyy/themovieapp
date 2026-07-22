package dev.themobiledev.movie.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeepBlueBackground = Color(0xFF0B1D3A)
private val CardBlueGray = Color(0xFF2C3B54)
private val OnColor = Color.White

private val MovieColorScheme = darkColorScheme(
    background = DeepBlueBackground,
    onBackground = OnColor,
    surface = CardBlueGray,
    onSurface = OnColor,
    surfaceVariant = CardBlueGray,
    onSurfaceVariant = OnColor,
)

@Composable
fun MovieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MovieColorScheme,
        content = content,
    )
}
