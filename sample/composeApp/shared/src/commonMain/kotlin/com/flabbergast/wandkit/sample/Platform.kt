package com.flabbergast.wandkit.sample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform