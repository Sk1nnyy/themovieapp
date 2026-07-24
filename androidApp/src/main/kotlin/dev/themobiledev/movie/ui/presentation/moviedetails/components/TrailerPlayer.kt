package dev.themobiledev.movie.ui.presentation.moviedetails.components

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import dev.themobiledev.movie.R

private const val PLAYER_ORIGIN = "https://example.com"

@SuppressLint("InflateParams")
@Composable
fun TrailerPlayer(videoKey: String, modifier: Modifier = Modifier) {
    val trailerContentDescription = stringResource(R.string.content_description_trailer_player)
    val lifecycleOwner = LocalLifecycleOwner.current
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    var cuedVideoKey by remember { mutableStateOf<String?>(null) }

    AndroidView(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .semantics { contentDescription = trailerContentDescription },
        factory = { context ->
            val playerView = LayoutInflater.from(context)
                .inflate(R.layout.trailer_player, null) as YouTubePlayerView
            lifecycleOwner.lifecycle.addObserver(playerView)
            playerView.initialize(
                object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        player = youTubePlayer
                        youTubePlayer.cueVideo(videoKey, 0f)
                        cuedVideoKey = videoKey
                    }
                },
                true,
                IFramePlayerOptions.Builder().origin(PLAYER_ORIGIN).build(),
            )
            playerView
        },
        update = {
            val readyPlayer = player
            if (readyPlayer != null && cuedVideoKey != videoKey) {
                readyPlayer.cueVideo(videoKey, 0f)
                cuedVideoKey = videoKey
            }
        },
        onRelease = { view ->
            lifecycleOwner.lifecycle.removeObserver(view)
            view.release()
        },
    )
}
