package com.run.peakflow

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform