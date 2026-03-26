package com.shipthisinstead.sdk.api

import com.shipthisinstead.sdk.models.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface STIApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @GET("api/v1/auth/portal/me")
    suspend fun getPortalMe(): STIUser

    @GET("api/v1/auth/portal/guest")
    suspend fun getPortalGuest(): STIAuthResponse

    @POST("api/v1/auth/portal/sdk-auth")
    suspend fun sdkAuth(@Body body: SdkAuthBody): STIAuthResponse

    // ── Boards & Branding ─────────────────────────────────────────────────────

    @GET("api/v1/portal/boards")
    suspend fun getBoards(): STIBoardsResponse

    // ── Feedback ──────────────────────────────────────────────────────────────

    @GET("api/v1/portal/feedback")
    suspend fun getFeedback(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("boardSlugs") boardSlugs: String? = null,
        @Query("sort") sort: String? = null,
    ): STIPaginatedResponse<STIFeedbackItem>

    @GET("api/v1/portal/feedback/{id}")
    suspend fun getFeedbackDetail(@Path("id") id: String): STIFeedbackItem

    @GET("api/v1/portal/feedback/by-number/{number}")
    suspend fun getFeedbackByNumber(@Path("number") number: Int): STIFeedbackItem

    @POST("api/v1/portal/feedback")
    suspend fun submitFeedback(@Body body: SubmitFeedbackBody): STIFeedbackItem

    @POST("api/v1/portal/feedback/{id}/vote")
    suspend fun voteFeedback(@Path("id") id: String): STIVoteResponse

    @POST("api/v1/portal/feedback/{id}/comments")
    suspend fun addComment(
        @Path("id") feedbackId: String,
        @Body body: AddCommentBody,
    ): STIComment

    @Multipart
    @POST("api/v1/portal/feedback/upload-attachment")
    suspend fun uploadAttachment(@Part file: MultipartBody.Part): STIUploadResponse

    // ── Roadmap & Changelog ───────────────────────────────────────────────────

    @GET("api/v1/portal/roadmap")
    suspend fun getRoadmap(): List<STIStatus>

    @GET("api/v1/portal/changelog")
    suspend fun getChangelog(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): STIPaginatedResponse<STIReleaseNote>

    // ── Profile ───────────────────────────────────────────────────────────────

    @GET("api/v1/portal/profile/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): STIUserProfile

    @GET("api/v1/portal/profile/{userId}/activity")
    suspend fun getUserActivity(@Path("userId") userId: String): STIUserActivity
}
