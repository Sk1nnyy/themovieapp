package dev.themobiledev.movie.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

private const val IMAGE_CACHE_MAX_AGE_SECONDS = 24 * 60 * 60L
private const val IMAGE_DISK_CACHE_MAX_BYTES = 50L * 1024 * 1024
private const val IMAGE_DISK_CACHE_DIR_NAME = "poster_image_cache"

class MovieImageLoaderFactory : SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { imageOkHttpClient(context) }))
            }
            .build()
    }

    private fun imageOkHttpClient(context: PlatformContext): OkHttpClient =
        OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, IMAGE_DISK_CACHE_DIR_NAME), IMAGE_DISK_CACHE_MAX_BYTES))
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=$IMAGE_CACHE_MAX_AGE_SECONDS")
                    .removeHeader("Pragma")
                    .build()
            }
            .build()
}
