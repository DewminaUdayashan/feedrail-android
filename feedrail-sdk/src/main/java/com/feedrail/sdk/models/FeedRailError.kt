package com.feedrail.sdk.models

/** Typed errors emitted by the SDK. */
sealed class FeedRailError : Exception() {
    object NotConfigured : FeedRailError()
    object Unauthorized : FeedRailError()
    object FeatureNotAllowed : FeedRailError()
    data class RateLimited(val retryAfter: Int? = null) : FeedRailError()
    data class ServerError(val code: Int, override val message: String) : FeedRailError()
    data class NetworkError(override val cause: Throwable?) : FeedRailError()
    object FileUploadFailed : FeedRailError()
    object FileTooLarge : FeedRailError()
    object TooManyFiles : FeedRailError()
    object UnsupportedFileType : FeedRailError()
    data class Unknown(override val message: String? = null) : FeedRailError()
}
