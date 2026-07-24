package dev.themobiledev.movie

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// kotlinx.coroutines doesn't expose a public Dispatchers.IO on Kotlin/Native in this version
// (it's `internal`), so blocking SQLDelight calls fall back to Default here.
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
