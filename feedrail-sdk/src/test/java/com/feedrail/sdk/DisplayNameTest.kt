package com.feedrail.sdk

import com.feedrail.sdk.models.FeedRailUser
import com.feedrail.sdk.models.FeedRailUserCount
import com.feedrail.sdk.models.FeedRailUserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A nameless end user has to be labelled the same everywhere they appear.
 *
 * The API computes "Anonymous · 9f3a" for feedback and comments; the SDK used to
 * fall back to a bare "Anonymous", so a profile disagreed with the post the reader
 * had just tapped through from, and a thread with several nameless participants
 * read as one person talking to themselves.
 */
class DisplayNameTest {

    private val count = FeedRailUserCount(feedbackItems = 0, comments = 0, votes = 0)

    @Test
    fun namelessUserGetsIdSuffix() {
        val user = FeedRailUser(id = "cmseedaaaabbbbcccc9f3a")
        assertEquals("Anonymous · 9f3a", user.displayName)
    }

    @Test
    fun realNameWins() {
        val user = FeedRailUser(id = "cmseedaaaabbbbcccc9f3a", name = "Ada Lovelace")
        assertEquals("Ada Lovelace", user.displayName)
    }

    @Test
    fun blankNameFallsBackToFirstAndLast() {
        val user = FeedRailUser(id = "u1", name = "  ", firstName = "Ada", lastName = "Lovelace")
        assertEquals("Ada Lovelace", user.displayName)
    }

    @Test
    fun profileMatchesTheFeed() {
        val profile = FeedRailUserProfile(id = "cmseedaaaabbbbcccc9f3a", count = count)
        assertEquals("Anonymous · 9f3a", profile.displayName)
        // The activity rows borrow the subject as their author, so they must agree.
        assertEquals(profile.displayName, profile.asAuthor().displayName)
    }
}
