package com.feedrail.sdk

import com.feedrail.sdk.models.SubmitFeedbackBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The exact JSON the SDK sends to `POST /portal/feedback`.
 *
 * The API names the board by `boardSlug` and, because its validation forbids unknown
 * fields, answers a `boardId` with a 400 — which is how every submission from the SDK
 * failed. The field name is pinned here so it cannot drift from the API again.
 */
class SubmitFeedbackBodyTest {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(SubmitFeedbackBody::class.java)

    @Test
    fun `sends the board as boardSlug, never boardId`() {
        val json = adapter.toJson(
            SubmitFeedbackBody(title = "Dark mode", content = null, boardSlug = "feature-requests"),
        )

        val fields = adapter.fromJson(json)!!
        assertEquals("feature-requests", fields.boardSlug)
        assertTrue(json, json.contains("\"boardSlug\":\"feature-requests\""))
        assertFalse(json, json.contains("boardId"))
    }
}
