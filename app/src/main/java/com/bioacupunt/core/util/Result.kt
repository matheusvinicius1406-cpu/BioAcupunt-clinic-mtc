package com.bioacupunt.core.util

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val kind: AppError, val cause: Throwable? = null) : Result<Nothing>
    data object Loading : Result<Nothing>
}

inline fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> {
    if (this is Result.Success) block(data)
    return this
}

inline fun <T> Result<T>.onError(block: (Result.Error) -> Unit): Result<T> {
    if (this is Result.Error) block(this)
    return this
}
