package dev.themobiledev.movie.network

sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Http(val code: Int, message: String) : ApiException("HTTP error $code: $message")
    class Serialization(cause: Throwable) : ApiException("Failed to parse response", cause)
    class Network(cause: Throwable) : ApiException("Network error", cause)
}
