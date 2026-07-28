package com.feedrail.sdk.api

import com.feedrail.sdk.models.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface FeedRailApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @GET("api/v1/auth/portal/me")
    suspend fun getPortalMe(): FeedRailUser

    @GET("api/v1/auth/portal/guest")
    suspend fun getPortalGuest(): FeedRailAuthResponse

    @POST("api/v1/auth/portal/anonymous")
    suspend fun anonymousAuth(@Body body: AnonymousAuthBody): FeedRailAuthResponse

    @POST("api/v1/auth/portal/sdk-auth")
    suspend fun sdkAuth(@Body body: SdkAuthBody): FeedRailAuthResponse

    // ── Boards & Branding ─────────────────────────────────────────────────────

    @GET("api/v1/portal/boards")
    suspend fun getBoards(): FeedRailBoardsResponse

    // ── Feedback ──────────────────────────────────────────────────────────────

    @GET("api/v1/portal/feedback")
    suspend fun getFeedback(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("boardSlugs") boardSlugs: String? = null,
        @Query("sort") sort: String? = null,
    ): FeedRailPaginatedResponse<FeedRailFeedbackItem>

    @GET("api/v1/portal/feedback/{id}")
    suspend fun getFeedbackDetail(@Path("id") id: String): FeedRailFeedbackItem

    // TODO: Update to use paginated comments endpoint api/v1/portal/feedback/{id}/comments
    @GET("api/v1/portal/feedback/by-number/{number}")
    suspend fun getFeedbackByNumber(@Path("number") number: Int): FeedRailFeedbackItem

    @POST("api/v1/portal/feedback")
    suspend fun submitFeedback(@Body body: SubmitFeedbackBody): FeedRailFeedbackItem

    @POST("api/v1/portal/feedback/{id}/vote")
    suspend fun voteFeedback(@Path("id") id: String): FeedRailVoteResponse

    @POST("api/v1/portal/feedback/{id}/comments")
    suspend fun addComment(
        @Path("id") feedbackId: String,
        @Body body: AddCommentBody,
    ): FeedRailComment

    @Multipart
    @POST("api/v1/portal/feedback/upload-attachment")
    suspend fun uploadAttachment(@Part file: MultipartBody.Part): FeedRailUploadResponse

    // ── Roadmap & Changelog ───────────────────────────────────────────────────

    @GET("api/v1/portal/roadmap")
    suspend fun getRoadmap(): List<FeedRailStatus>

    @GET("api/v1/portal/changelog")
    suspend fun getChangelog(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): FeedRailPaginatedResponse<FeedRailReleaseNote>

    // ── Profile ───────────────────────────────────────────────────────────────

    @GET("api/v1/portal/profile/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): FeedRailUserProfile

    @GET("api/v1/portal/profile/{userId}/activity")
    suspend fun getUserActivity(@Path("userId") userId: String): FeedRailUserActivity
}
