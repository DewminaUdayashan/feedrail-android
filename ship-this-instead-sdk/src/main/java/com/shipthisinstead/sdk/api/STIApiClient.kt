package com.shipthisinstead.sdk.api

import com.shipthisinstead.sdk.models.STIError
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class STIApiClient(
    private val service: STIApiService,
    private val rateLimiter: RateLimiter,
) {

    val api: STIApiService get() = service

    /**
     * Executes a suspend API call, applying rate limiting and mapping exceptions
     * to typed [STIError]s.
     */
    suspend fun <T> call(block: suspend STIApiService.() -> T): T {
        if (!rateLimiter.canProceed()) throw STIError.RateLimited()
        return try {
            block(service)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> throw STIError.Unauthorized
                403 -> throw STIError.FeatureNotAllowed
                429 -> throw STIError.RateLimited(
                    e.response()?.headers()?.get("Retry-After")?.toIntOrNull()
                )
                else -> throw STIError.ServerError(e.code(), e.message())
            }
        } catch (e: IOException) {
            throw STIError.NetworkError(e)
        }
    }

    companion object {
        fun create(
            baseUrl: String,
            authInterceptor: AuthInterceptor,
        ): STIApiClient {
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

            val service = retrofit.create(STIApiService::class.java)
            return STIApiClient(service, RateLimiter())
        }
    }
}
