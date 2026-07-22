package dev.themobiledev.movie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.themobiledev.movie.navigation.MovieNavHost
import dev.themobiledev.movie.theme.MovieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MovieTheme {
                MovieNavHost()
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    MovieTheme {
        MovieNavHost()
    }
}