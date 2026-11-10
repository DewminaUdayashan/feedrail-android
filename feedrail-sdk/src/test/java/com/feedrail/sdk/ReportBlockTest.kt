package com.feedrail.sdk

import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.api.FeedRailApiService
import com.feedrail.sdk.api.RateLimiter
import com.feedrail.sdk.auth.AuthManager
import com.feedrail.sdk.auth.AuthState
import com.feedrail.sdk.models.FeedRailBoardsResponse
import com.feedrail.sdk.models.FeedRailComment
import com.feedrail.sdk.models.FeedRailError
import com.feedrail.sdk.models.FeedRailFeedbackItem
import com.feedrail.sdk.models.FeedRailPaginatedResponse
import com.feedrail.sdk.models.FeedRailPaginationMeta
import com.feedrail.sdk.models.FeedRailReportReason
import com.feedrail.sdk.models.FeedRailReportResponse
import com.feedrail.sdk.models.FeedRailUser
import com.feedrail.sdk.moderation.Moderation
import com.feedrail.sdk.ui.viewmodels.FeedbackDetailViewModel
import com.feedrail.sdk.ui.viewmodels.FeedbackListViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Report and block take content off the reader's screen at once — the API hides it
 * on later fetches, but rows already on screen have to go too.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportBlockTest {

    private val me = FeedRailUser(id = "me", name = "Me")
    private val troll = FeedRailUser(id = "troll", name = "Troll")
    private val other = FeedRailUser(id = "other", name = "Other")
    private val teamMember = FeedRailUser(id = "troll", name = "Team")

    private lateinit var service: FeedRailApiService
    private lateinit var api: FeedRailApiClient
    private lateinit var auth: AuthManager

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        service = mockk(relaxed = true)
        api = FeedRailApiClient(service, RateLimiter())
        auth = mockk(relaxed = true)
        every { auth.authState } returns MutableStateFlow(AuthState.Authenticated(me, "token"))
        coEvery { service.reportFeedback(any(), any()) } returns FeedRailReportResponse("r1", "OPEN")
        coEvery { service.reportComment(any(), any(), any()) } returns FeedRailReportResponse("r2", "OPEN")
        coEvery { service.blockUser(any()) } returns Response.success(Unit)
        coEvery { service.getBoards() } returns FeedRailBoardsResponse(boards = emptyList())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun post(id: String, user: FeedRailUser?, team: Boolean = false, comments: List<FeedRailComment>? = null) =
        FeedRailFeedbackItem(
            id = id,
            number = 1,
            title = id,
            isTeamPost = team,
            user = if (team) null else user,
            orgUser = if (team) user else null,
            comments = comments,
            createdAt = "2026-11-10T10:00:00.000Z",
            updatedAt = "2026-11-10T10:00:00.000Z",
        )

    private fun comment(id: String, user: FeedRailUser, replies: List<FeedRailComment>? = null) =
        FeedRailComment(id = id, content = id, user = user, replies = replies, createdAt = "2026-11-10T10:00:00.000Z")

    private fun listWith(vararg items: FeedRailFeedbackItem): FeedbackListViewModel {
        coEvery { service.getFeedback(any(), any(), any(), any()) } returns FeedRailPaginatedResponse(
            items = items.toList(),
            meta = FeedRailPaginationMeta(total = items.size, page = 1, limit = 20, totalPages = 1),
        )
        return FeedbackListViewModel(api, auth)
    }

    @Test
    fun reportingAPostDropsItFromTheList() = runTest {
        val vm = listWith(post("a", troll), post("b", other))
        Moderation.reportFeedback(api, "a", FeedRailReportReason.SPAM, null)
        assertEquals(listOf("b"), vm.uiState.value.items.map { it.id })
    }

    @Test
    fun blockingAnAuthorDropsAllTheirPostsButNotTeamPosts() = runTest {
        // A team post whose org user happens to share the id must survive: blocking
        // is about end users only.
        val vm = listWith(post("a", troll), post("b", other), post("c", troll), post("t", teamMember, team = true))
        Moderation.blockUser(api, "troll")
        assertEquals(listOf("b", "t"), vm.uiState.value.items.map { it.id })
    }

    @Test
    fun detailDropsReportedCommentsAndRepliesAndBlockedCommenters() = runTest {
        coEvery { service.getFeedbackDetail("p") } returns post(
            "p", other,
            comments = listOf(
                comment("c1", other, replies = listOf(comment("r1", troll), comment("r2", other))),
                comment("c2", troll),
                comment("c3", other),
            ),
        )
        val vm = FeedbackDetailViewModel(api, auth)
        vm.load("p")

        Moderation.reportComment(api, "p", "c3", FeedRailReportReason.HARASSMENT, "note")
        Moderation.blockUser(api, "troll")

        val comments = vm.uiState.value.feedback?.comments.orEmpty()
        assertEquals(listOf("c1"), comments.map { it.id })
        assertEquals(listOf("r2"), comments.single().replies?.map { it.id })
        assertFalse(vm.uiState.value.isRemoved)
    }

    @Test
    fun blockingThePostAuthorLeavesTheDetailScreen() = runTest {
        coEvery { service.getFeedbackDetail("p") } returns post("p", troll)
        val vm = FeedbackDetailViewModel(api, auth)
        vm.load("p")
        Moderation.blockUser(api, "troll")
        assertTrue(vm.uiState.value.isRemoved)
    }

    @Test
    fun bannedWriteMapsToBanned() = runTest {
        coEvery { service.voteFeedback(any()) } throws HttpException(
            Response.error<Any>(
                403,
                """{"message":"You can no longer post here.","errorCode":"END_USER_BANNED"}"""
                    .toResponseBody(),
            ),
        )
        try {
            api.call { voteFeedback("p") }
            fail("expected an error")
        } catch (e: FeedRailError.Banned) {
            assertEquals("You can no longer post here.", e.message)
        }
    }

    @Test
    fun plainForbiddenStaysAPlanGate() = runTest {
        coEvery { service.voteFeedback(any()) } throws HttpException(
            Response.error<Any>(403, """{"message":"Upgrade"}""".toResponseBody()),
        )
        try {
            api.call { voteFeedback("p") }
            fail("expected an error")
        } catch (e: FeedRailError) {
            assertEquals(FeedRailError.FeatureNotAllowed, e)
        }
    }
}
