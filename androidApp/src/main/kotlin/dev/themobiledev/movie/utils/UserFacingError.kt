package dev.themobiledev.movie.utils

import androidx.annotation.StringRes
import dev.themobiledev.movie.R
import dev.themobiledev.movie.network.ApiException

/**
 * Maps a repository failure to a canned, localized string resource instead of surfacing the
 * raw exception message (e.g. "HTTP error 404: Not Found") to the user. Mirrors iOS's
 * `EquatableError.userFacingMessage` mapping so the same failure reads the same on both platforms.
 */
@StringRes
fun Throwable.userFacingMessageRes(): Int = when (this) {
    is ApiException.Network -> R.string.error_network
    is ApiException.Serialization -> R.string.error_serialization
    is ApiException.Http -> R.string.error_server
    else -> R.string.error_generic
}
