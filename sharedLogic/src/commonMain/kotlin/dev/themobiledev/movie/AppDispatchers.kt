package dev.themobiledev.movie

import kotlinx.coroutines.CoroutineDispatcher

/**
 * A dispatcher suitable for blocking I/O (SQLDelight's synchronous driver calls). `Dispatchers.IO`
 * is public on the JVM/Android target but internal on Kotlin/Native in this coroutines version, so
 * this is expect/actual per platform rather than a single shared `Dispatchers.IO` reference.
 */
expect val ioDispatcher: CoroutineDispatcher
