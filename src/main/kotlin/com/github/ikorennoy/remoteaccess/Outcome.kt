package com.github.ikorennoy.remoteaccess

sealed class Outcome<out T>

data class Ok<out T>(val value: T) : Outcome<T>()

data class Er(val error: Throwable) : Outcome<Nothing>()
