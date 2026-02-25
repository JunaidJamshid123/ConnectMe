package com.junaidjamshid.i211203.util

/**
 * A generic class that holds a value with its loading status.
 * Used to wrap responses from repositories to handle Success, Error, and Loading states.
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
