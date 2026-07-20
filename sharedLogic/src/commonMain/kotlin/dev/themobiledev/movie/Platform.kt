package dev.themobiledev.movie

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform