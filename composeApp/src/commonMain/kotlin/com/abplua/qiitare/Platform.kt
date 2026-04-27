package com.abplua.qiitare

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform