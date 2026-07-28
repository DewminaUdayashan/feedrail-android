package com.feedrail.sdk.api

import com.feedrail.sdk.models.FeedRailError
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class FeedRailApiClient(
    private val service: FeedRailApiService,
    private val rateLimiter: RateLimiter,
) {

    val api: FeedRailApiService get() = service

    /**
     * Executes a suspend API call, applying rate limiting and mapping exceptions
     * to typed [FeedRailError]s.
     */
    suspend fun <T> call(block: suspend FeedRailApiService.() -> T): T {
        if (!rateLimiter.canProceed()) throw FeedRailError.RateLimited()
        return try {
            block(service)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> throw FeedRailError.Unauthorized
                403 -> throw FeedRailError.FeatureNotAllowed
                429 -> throw FeedRailError.RateLimited(
                    e.response()?.headers()?.get("Retry-After")?.toIntOrNull()
                )
                else -> throw FeedRailError.ServerError(e.code(), e.message())
            }
        } catch (e: IOException) {
            throw FeedRailError.NetworkError(e)
        }
    }

    companion object {
        fun create(
            baseUrl: String,
            authInterceptor: AuthInterceptor,
        ): FeedRailApiClient {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttp = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl.trimEnd('/') + "/")
                .client(okHttp)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val service = retrofit.create(FeedRailApiService::class.java)
            return FeedRailApiClient(service, RateLimiter())
        }
    }
}
