package com.shipthisinstead.sdk.models

/** Typed errors emitted by the SDK. */
sealed class STIError : Exception() {
    object NotConfigured : STIError()
    object Unauthorized : STIError()
    object FeatureNotAllowed : STIError()
    data class RateLimited(val retryAfter: Int? = null) : STIError()
    data class ServerError(val code: Int, override val message: String) : STIError()
    data class NetworkError(override val cause: Throwable?) : STIError()
    object FileUploadFailed : STIError()
    object FileTooLarge : STIError()
    object TooManyFiles : STIError()
    object UnsupportedFileType : STIError()
    data class Unknown(override val message: String? = null) : STIError()
}
