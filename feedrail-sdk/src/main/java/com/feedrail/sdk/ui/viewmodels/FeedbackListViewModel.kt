package com.feedrail.sdk.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.auth.AuthManager
import com.feedrail.sdk.auth.AuthState
import com.feedrail.sdk.models.FeedRailBoard
import com.feedrail.sdk.models.FeedRailBranding
import com.feedrail.sdk.models.FeedRailFeedbackItem
import com.feedrail.sdk.models.FeedRailModerationStatus
import com.feedrail.sdk.moderation.Moderation
import com.feedrail.sdk.moderation.ModerationEvent
import com.feedrail.sdk.moderation.isByEndUser
import com.feedrail.sdk.`public`.FeedRailSDK
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class FeedbackListUiState(
    val items: List<FeedRailFeedbackItem> = emptyList(),
    val boards: List<FeedRailBoard> = emptyList(),
    val branding: FeedRailBranding? = null,
    val isProjectArchived: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val selectedBoardSlug: String? = null,
    val sortOption: String = "upvotes",
    val isAuthenticated: Boolean = false,
    val isAuthenticating: Boolean = false,
    val isGuest: Boolean = true,
    /** False until the first feedback load finishes — lets the UI show the
     * skeleton from the first frame instead of flashing the empty state. */
    val hasLoadedOnce: Boolean = false,
    /** The post the reader has just submitted, and how it was received. */
    val submissionNotice: SubmissionNotice? = null,
    /** That post's id, tinted in the list so it can be found at a glance. */
    val highlightedItemId: String? = null,
) {
    val hasMore: Boolean get() = currentPage < totalPages

    /** Whether the current user may submit feedback. Identified users always
     * can; guests/anonymous only when the plan allows guest posting. */
    val canSubmit: Boolean
        get() = isAuthenticated && (!isGuest || (branding?.guestPostingAllowed ?: false))
}

/** A just-submitted post and its review state, for the confirmation banner. */
data class SubmissionNotice(
    val id: String,
    val status: FeedRailModerationStatus?,
)

class FeedbackListViewModel(
    private val apiClient: FeedRailApiClient,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackListUiState())
    val uiState: StateFlow<FeedbackListUiState> = _uiState.asStateFlow()

    /** Kept so the post survives a reload that would otherwise drop it. */
    private var justSubmitted: FeedRailFeedbackItem? = null
    private var noticeJob: Job? = null

    init {
        // Branding is deliberately NOT waited on auth. `/portal/boards` needs no
        // token — the old code held it behind the auth state, so the UI painted in
        // the fallback brand colour for the whole handshake before it could even
        // ask what colour it should be.
        loadBoards()

        viewModelScope.launch {
            Moderation.events.collect(::onModerationEvent)
        }

        viewModelScope.launch {
            authManager.authState.collectLatest { state ->
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = state is AuthState.Authenticated,
                    isAuthenticating = state is AuthState.Authenticating,
                    isGuest = (state as? AuthState.Authenticated)?.user?.isGuestUser ?: true,
                )
                if (state is AuthState.Authenticated || state is AuthState.Guest) {
                    // The feedback list *does* need identity — it carries per-user
                    // state such as hasVoted — so it still waits.
                    if (_uiState.value.items.isEmpty() && !_uiState.value.isLoading) {
                        loadInitial()
                    }
                }
            }
        }
    }

    /**
     * Drops what the reader has just reported or blocked. The server already
     * excludes it from later pages; this covers the rows already on screen.
     */
    private fun onModerationEvent(event: ModerationEvent) {
        val items = _uiState.value.items
        val kept = when (event) {
            is ModerationEvent.FeedbackReported -> items.filter { it.id != event.feedbackId }
            is ModerationEvent.UserBlocked -> items.filter { !it.isByEndUser(event.userId) }
            // List rows carry no comments.
            is ModerationEvent.CommentReported -> return
            // Their posts are visible again, wherever they sort.
            is ModerationEvent.UserUnblocked -> {
                refresh()
                return
            }
        }
        if (justSubmitted?.let { s -> kept.none { it.id == s.id } } == true) justSubmitted = null
        _uiState.value = _uiState.value.copy(items = kept)
    }

    /**
     * Resolves branding in three escalating steps, so the UI paints in the
     * customer's colours as early as it can:
     *
     * 1. the on-device cache, applied immediately — correct colours on the first
     *    frame for every launch after the first;
     * 2. the prefetch started by `FeedRailSDK.configure()`, awaited rather than
     *    duplicated — usually already finished by the time the sheet opens;
     * 3. a fresh request, if there was no prefetch or it failed.
     *
     * Branding stays non-critical throughout: every step may fail quietly and the
     * UI falls back to defaults, as it did before any of this existed.
     */
    private fun loadBoards() {
        val store = FeedRailSDK.brandingStore

        // 1. Paint from cache first, so no frame renders in the wrong colour.
        if (_uiState.value.branding == null) {
            store?.load()?.let(::applyBoards)
        }

        viewModelScope.launch {
            // 2. Prefer the launch-time prefetch over a duplicate request.
            FeedRailSDK.consumeBrandingPrefetch()?.let { prefetched ->
                applyBoards(prefetched)
                store?.save(prefetched)
                return@launch
            }

            // 3. No usable prefetch — fetch it now.
            try {
                val response = apiClient.call { getBoards() }
                applyBoards(response)
                store?.save(response)
            } catch (e: Exception) {
                Log.e("FeedRail", "loadBoards failed: ${e.message}", e)
            }
        }
    }

    private fun applyBoards(response: com.feedrail.sdk.models.FeedRailBoardsResponse) {
        _uiState.value = _uiState.value.copy(
            boards = response.boards,
            branding = response.branding,
            isProjectArchived = response.isProjectArchived ?: false,
        )
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val s = _uiState.value
                val response = apiClient.call {
                    getFeedback(
                        page = 1,
                        limit = 20,
                        boardSlugs = s.selectedBoardSlug,
                        sort = s.sortOption,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    items = withJustSubmitted(response.items),
                    currentPage = response.meta.page,
                    totalPages = response.meta.totalPages,
                    isLoading = false,
                    hasLoadedOnce = true,
                )
            } catch (e: Exception) {
                Log.e("FeedRail", "loadInitial failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasLoadedOnce = true,
                    error = e.message ?: "Unknown error",
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                val s = _uiState.value
                val response = apiClient.call {
                    getFeedback(page = 1, limit = 20, boardSlugs = s.selectedBoardSlug, sort = s.sortOption)
                }
                _uiState.value = _uiState.value.copy(
                    items = withJustSubmitted(response.items),
                    currentPage = response.meta.page,
                    totalPages = response.meta.totalPages,
                    isRefreshing = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false, error = e.message)
            }
        }
    }

    fun loadMore() {
        val s = _uiState.value
        if (!s.hasMore || s.isLoadingMore) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            try {
                val response = apiClient.call {
                    getFeedback(
                        page = s.currentPage + 1,
                        limit = 20,
                        boardSlugs = s.selectedBoardSlug,
                        sort = s.sortOption,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    // Deduplicated: a just-submitted post pinned to the top also
                    // arrives in whichever page its own sort position falls on,
                    // and two rows with the same key crash a LazyColumn.
                    items = (s.items + response.items).distinctBy { it.id },
                    currentPage = response.meta.page,
                    totalPages = response.meta.totalPages,
                    isLoadingMore = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun selectBoard(slug: String?) {
        _uiState.value = _uiState.value.copy(selectedBoardSlug = slug, items = emptyList(), currentPage = 1)
        loadInitial()
    }

    fun selectSort(sort: String) {
        _uiState.value = _uiState.value.copy(sortOption = sort, items = emptyList(), currentPage = 1)
        loadInitial()
    }

    /**
     * Puts a post the reader has just written at the top of the list.
     *
     * Submitting used to leave the feed exactly as it was — the screen popped back
     * to an unchanged list and nothing said the post had been accepted. Even a
     * refresh would not have helped: the default order is Top Voted, so a new post
     * with one vote sorts wherever its votes put it — usually pages down — and a
     * post awaiting review is not shown to anyone but its author at all.
     */
    fun onFeedbackSubmitted(item: FeedRailFeedbackItem) {
        justSubmitted = item
        // The notice goes in first: the merge below is a no-op while it is absent,
        // which is how pinning stops once the reader dismisses the banner.
        _uiState.value = _uiState.value.copy(
            highlightedItemId = item.id,
            submissionNotice = SubmissionNotice(item.id, item.moderationState),
        )
        _uiState.value = _uiState.value.copy(items = withJustSubmitted(_uiState.value.items))

        noticeJob?.cancel()
        noticeJob = viewModelScope.launch {
            delay(NOTICE_LIFETIME_MS)
            if (_uiState.value.submissionNotice?.id == item.id) dismissSubmissionNotice()
        }
    }

    fun dismissSubmissionNotice() {
        noticeJob?.cancel()
        justSubmitted = null
        _uiState.value = _uiState.value.copy(submissionNotice = null, highlightedItemId = null)
    }

    /**
     * Keeps the just-submitted post at the top of a freshly fetched page.
     *
     * A pull-to-refresh or a sort change re-asks the server, which sorts the new
     * post like everything else and may not return it on the first page at all.
     * While the confirmation is still on screen the banner promises the reader it
     * is at the top, so it stays there — but the server's copy wins whenever it is
     * present, so the row never goes stale.
     */
    private fun withJustSubmitted(fetched: List<FeedRailFeedbackItem>): List<FeedRailFeedbackItem> {
        val submitted = justSubmitted ?: return fetched
        if (_uiState.value.submissionNotice == null) return fetched
        // A board filter the post does not belong to is a deliberate "show me
        // something else" — pinning it there would be noise, not reassurance.
        val slug = _uiState.value.selectedBoardSlug
        if (slug != null && submitted.board?.slug != slug) return fetched
        val server = fetched.firstOrNull { it.id == submitted.id }
        return listOf(server ?: submitted) + fetched.filter { it.id != submitted.id }
    }

    fun updateItemVote(itemId: String, voted: Boolean, voteCount: Int) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { item ->
                if (item.id == itemId) item.copy(
                    hasVoted = voted,
                    count = item.count?.copy(votes = voteCount) ?: com.feedrail.sdk.models.FeedRailFeedbackCount(votes = voteCount)
                ) else item
            }
        )
    }

    companion object {
        /** Long enough to read the confirmation, short enough not to become furniture. */
        private const val NOTICE_LIFETIME_MS = 12_000L

        fun factory(sdk: FeedRailSDK): ViewModelProvider.Factory = viewModelFactory {
            initializer { FeedbackListViewModel(sdk.apiClient, sdk.authManager) }
        }
    }
}
