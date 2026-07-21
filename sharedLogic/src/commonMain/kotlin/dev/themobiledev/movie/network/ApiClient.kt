package dev.themobiledev.movie.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Base class for TMDB API clients. Subclasses call [get] with an endpoint
 * path relative to [BASE_URL] to fetch and deserialize a response body.
 * Failures are surfaced as a typed [ApiException] wrapped in [Result],
 * never as raw Ktor/serialization exceptions.
 */
abstract class ApiClient(@PublishedApi internal val httpClient: HttpClient) {

    protected suspend inline fun <reified T> get(
        path: String,
        parameters: Map<String, Any?> = emptyMap(),
    ): Result<T> =
        try {
            val body = httpClient.get(BASE_URL + path) {
                parameters.forEach { (key, value) -> parameter(key, value) }
            }.body<T>()
            Result.success(body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseException) {
            Result.failure(ApiException.Http(e.response.status.value, e.message ?: "HTTP error"))
        } catch (e: SerializationException) {
            Result.failure(ApiException.Serialization(e))
        } catch (e: Throwable) {
            Result.failure(ApiException.Network(e))
        }

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
    }
}
