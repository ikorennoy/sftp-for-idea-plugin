package com.github.ikorennoy.remotefileviewer.utils

/**
 * A type-safe version of stdlib [Result], however it requires additional allocations.
 */
sealed class Outcome<out T> {
    abstract fun <R> map(transform: (T) -> R): Outcome<R>
}

data class Ok<out T>(val value: T): Outcome<T>() {
    override fun <R> map(transform: (T) -> R): Outcome<R> {
        return try {
            Ok(transform(value))
        } catch (ex: Throwable) {
            Er(ex)
        }
    }
}

data class Er(val error: Throwable): Outcome<Nothing>() {
    override fun <R> map(transform: (Nothing) -> R): Outcome<R> {
        return this
    }
}
