package com.feedrail.sdk.models

import com.feedrail.sdk.R
import com.feedrail.sdk.`public`.FeedRailSDK

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
    /**
     * The team has banned this user from posting. Every write (post, comment, vote,
     * upload) fails with this; reading, reporting and blocking keep working.
     * [message] is localized; [serverMessage] is the API's own English text.
     */
    data class Banned(val serverMessage: String? = null) : FeedRailError() {
        override val message: String
            get() = runCatching {
                FeedRailSDK.languageManager.string(R.string.feedrail_error_banned)
            }.getOrNull() ?: serverMessage ?: "You can no longer post here."
    }
    data class Unknown(override val message: String? = null) : FeedRailError()

    companion object {
        /** Machine-readable `errorCode` the API sends on a banned user's writes. */
        const val END_USER_BANNED = "END_USER_BANNED"
    }
}
