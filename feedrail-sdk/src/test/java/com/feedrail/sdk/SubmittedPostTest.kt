package com.feedrail.sdk

import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.auth.AuthManager
import com.feedrail.sdk.auth.AuthState
import com.feedrail.sdk.models.FeedRailBoard
import com.feedrail.sdk.models.FeedRailFeedbackCount
import com.feedrail.sdk.models.FeedRailFeedbackItem
import com.feedrail.sdk.models.FeedRailModerationStatus
import com.feedrail.sdk.ui.viewmodels.FeedbackListViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * What happens to a post immediately after the reader writes it.
 *
 * Submitting used to leave the feed untouched: the screen popped back to a list that
 * did not contain the new post, and nothing on screen said it had been accepted.
 * Under the default Top Voted order it would not have been near the top even after a
 * refresh, and a post awaiting review is not returned to anybody but its author.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubmittedPostTest {

    private lateinit var viewModel: FeedbackListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val auth = mockk<AuthManager>(relaxed = true)
        every { auth.authState } returns MutableStateFlow(AuthState.Idle)
        viewModel = FeedbackListViewModel(mockk<FeedRailApiClient>(relaxed = true), auth)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun item(
        id: String,
        boardSlug: String = "features",
        moderation: FeedRailModerationStatus? = null,
        comments: Int = 0,
    ) = FeedRailFeedbackItem(
        id = id,
        number = 1,
        title = "A post",
        board = FeedRailBoard(id = "b1", name = "Features", slug = boardSlug),
        moderationStatus = moderation?.name,
        count = FeedRailFeedbackCount(votes = 1, comments = comments),
        createdAt = "2026-08-10T10:00:00.000Z",
        updatedAt = "2026-08-10T10:00:00.000Z",
    )

    @Test
    fun submittedPostGoesToTheTopWithANotice() {
        viewModel.onFeedbackSubmitted(item("new", moderation = FeedRailModerationStatus.PENDING))

        val state = viewModel.uiState.value
        assertEquals("new", state.items.firstOrNull()?.id)
        assertEquals("new", state.highlightedItemId)
        assertEquals("new", state.submissionNotice?.id)
        assertEquals(FeedRailModerationStatus.PENDING, state.submissionNotice?.status)
    }

    @Test
    fun dismissingTheNoticeClearsTheHighlight() {
        viewModel.onFeedbackSubmitted(item("new"))
        viewModel.dismissSubmissionNotice()

        val state = viewModel.uiState.value
        assertNull(state.submissionNotice)
        assertNull(state.highlightedItemId)
    }
}
