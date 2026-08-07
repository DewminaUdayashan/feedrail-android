package com.feedrail.sdk.data

import android.content.Context
import com.feedrail.sdk.models.FeedRailBoardsResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Caches the last-known `/portal/boards` payload (branding + boards) on device.
 *
 * The feedback UI cannot render its own colours until branding arrives, so a cold
 * launch used to paint the FeedRail brand colour and then snap to the customer's.
 * Persisting the last response means every launch after the first paints correctly
 * from the first frame, and the network round-trip becomes a background refresh
 * rather than something the user waits on.
 *
 * Plain [android.content.SharedPreferences], not the [TokenStorage]-style
 * encrypted variety, on purpose: this is public presentation data (colours, logo
 * URL, board names) that the portal serves unauthenticated. Nothing
 * user-identifying is stored here.
 *
 * Keyed per organization+project so one host app embedding two projects cannot
 * paint one with the other's branding.
 */
internal class BrandingStore(context: Context, organizationSlug: String, projectSlug: String) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val key = "branding.$organizationSlug.$projectSlug"
    private val savedAtKey = "$key.savedAt"

    // Mirrors FeedRailApiClient's Moshi setup: codegen adapters win, reflection
    // is the fallback, so a payload round-trips through here exactly as it was
    // parsed off the wire.
    private val adapter =
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            .adapter(FeedRailBoardsResponse::class.java)

    /**
     * The cached payload, or `null` if absent, unreadable or past [MAX_AGE_MS].
     *
     * Parse failures clear the entry and return `null`: a payload written by an
     * older SDK whose model has since changed shape is not worth crashing or
     * repeatedly failing over.
     */
    fun load(): FeedRailBoardsResponse? {
        val json = prefs.getString(key, null) ?: return null
        val savedAt = prefs.getLong(savedAtKey, 0L)
        if (System.currentTimeMillis() - savedAt > MAX_AGE_MS) {
            clear()
            return null
        }
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            clear()
            null
        }
    }

    /**
     * Persists a freshly fetched payload. Failures are swallowed — a cache that
     * cannot be written is a missed optimisation, not an error worth surfacing.
     */
    fun save(payload: FeedRailBoardsResponse) {
        try {
            prefs.edit()
                .putString(key, adapter.toJson(payload))
                .putLong(savedAtKey, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            // Nothing to do — the next launch simply fetches as before.
        }
    }

    fun clear() {
        prefs.edit().remove(key).remove(savedAtKey).apply()
    }

    private companion object {
        const val PREFS_NAME = "com.feedrail.sdk.branding"

        /**
         * How long a cached payload may be used before it is treated as absent.
         *
         * Cached branding is only ever a *first paint* — a refresh is always
         * issued alongside it — so this is deliberately generous. It exists so an
         * app left unopened for months does not flash branding from a rebrand ago.
         */
        const val MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }
}
