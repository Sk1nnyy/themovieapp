package dev.themobiledev.movie.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient

private const val IMAGE_CACHE_MAX_AGE_SECONDS = 24 * 60 * 60L

class MovieImageLoaderFactory : SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { imageOkHttpClient }))
            }
            .build()
    }

    private val imageOkHttpClient by lazy {
        OkHttpClient.Builder()
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=$IMAGE_CACHE_MAX_AGE_SECONDS")
                    .removeHeader("Pragma")
                    .build()
            }
            .build()
    }
}
