package com.feedrail.sdk.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.feedrail.sdk.R
import com.feedrail.sdk.`public`.FeedRailSDK
import com.feedrail.sdk.auth.AuthState
import com.feedrail.sdk.ui.components.AvatarImage
import com.feedrail.sdk.ui.components.BrandingHeader
import com.feedrail.sdk.ui.components.PoweredByBadge
import com.feedrail.sdk.ui.components.FeedRailFloatingToolbar
import com.feedrail.sdk.ui.screens.*
import com.feedrail.sdk.ui.theme.FeedRailTheme
import com.feedrail.sdk.ui.viewmodels.*
import com.feedrail.sdk.ui.theme.Color as FeedRailColor

// ── Route constants ───────────────────────────────────────────────────────────

object FeedRailRoute {
    const val FEEDBACK_LIST = "feedback_list"
    const val FEEDBACK_DETAIL = "feedback_detail/{feedbackId}"
    const val SUBMIT_FEEDBACK = "submit_feedback"
    const val ROADMAP = "roadmap"
    const val CHANGELOG = "changelog"
    const val PROFILE = "profile/{userId}"

    fun feedbackDetail(id: String) = "feedback_detail/$id"
    fun profile(id: String) = "profile/$id"
}

// ── CompositionLocal ──────────────────────────────────────────────────────────

val LocalFeedRailSDK = staticCompositionLocalOf<FeedRailSDK> {
    error("No FeedRailSDK provided")
}

// ── Tab enum ──────────────────────────────────────────────────────────────────

enum class FeedRailTab(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    FEEDBACK(
        route = FeedRailRoute.FEEDBACK_LIST,
        titleResId = R.string.feedrail_general_feedback,
        selectedIcon = Icons.Filled.Campaign,
        unselectedIcon = Icons.Outlined.Campaign,
    ),
    ROADMAP(
        route = FeedRailRoute.ROADMAP,
        titleResId = R.string.feedrail_general_roadmap,
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map,
    ),
    CHANGELOG(
        route = FeedRailRoute.CHANGELOG,
        titleResId = R.string.feedrail_general_changelog,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
    ),
}

private val tabRoutes = FeedRailTab.entries.map { it.route }.toSet()

// ── Nav Graph ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedRailNavGraph(
    sdk: FeedRailSDK,
    onDismiss: () -> Unit,
    onFeedbackSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val isTabScreen = currentRoute in tabRoutes
    // Language selection infra is kept but not surfaced in the UI until
    // localization ships (see the hidden LanguageSelectorDialog below).
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Auth state
    val authState by sdk.authManager.authState.collectAsStateWithLifecycle()
    val currentUser = (authState as? AuthState.Authenticated)?.user

    // Shared FeedbackListViewModel for branding
    val feedbackListVm: FeedbackListViewModel = viewModel(
        factory = FeedbackListViewModel.factory(sdk),
    )
    val feedbackListState by feedbackListVm.uiState.collectAsStateWithLifecycle()

    // Accent color: host app's native theme color (paid plans only, server-verified)
    // takes priority, then branding's dashboard-configured color, then the
    // FeedRail brand default. Archived projects always use the default,
    // regardless of branding or native override.
    val accentColor = remember(
        feedbackListState.branding?.resolvedAccentColor,
        feedbackListState.branding?.allowsNativeTheme,
        feedbackListState.isProjectArchived,
    ) {
        val isArchived = feedbackListState.isProjectArchived == true
        val native = sdk.config.themeColor
        if (!isArchived && native != null && feedbackListState.branding?.allowsNativeTheme == true) {
            native
        } else {
            val hex = if (isArchived || feedbackListState.branding == null) {
                "#25A777" // FeedRail brand default
            } else {
                feedbackListState.branding?.resolvedAccentColor ?: "#25A777"
            }
            FeedRailColor(hex)
        }
    }

    // Powered By visibility
    val showPoweredBy = feedbackListState.branding?.shouldShowPoweredBy ?: true

    // Tablet / wide-screen: constrain width
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val contentWidthModifier = if (screenWidthDp >= 600) {
        Modifier.widthIn(max = 600.dp)
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalFeedRailSDK provides sdk) {
        FeedRailTheme(accentColor = accentColor) {
            Surface(
                modifier = modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    // Main Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.TopCenter)
                            .then(contentWidthModifier)
                    ) {
                        // Custom Header if it's a tab screen and NOT in a global error state
                        // Note: Screen-level errors are handled within the screen, but we check feedbackListState here as a heuristic.
                        val isError = feedbackListState.error != null && feedbackListState.items.isEmpty()
                        
                        if (isTabScreen && !isError) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BrandingHeader(
                                branding = feedbackListState.branding,
                                title = stringResource(R.string.feedrail_general_feedback),
                                modifier = Modifier.weight(1f)
                            )

                            // Tapping the avatar opens the current user's profile.
                            // Every user (including anonymous guests) has a hydrated
                            // identity, so this is always available. The language
                            // picker is intentionally hidden until localization ships.
                            IconButton(
                                onClick = {
                                    currentUser?.id?.let { navController.navigate(FeedRailRoute.profile(it)) }
                                },
                                enabled = currentUser != null,
                            ) {
                                AvatarImage(
                                    avatarUrl = currentUser?.avatarUrl,
                                    displayName = currentUser?.displayName
                                        ?: sdk.config.userName
                                        ?: "?",
                                    size = 36.dp,
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = FeedRailRoute.FEEDBACK_LIST,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        composable(FeedRailRoute.FEEDBACK_LIST) {
                            FeedbackListScreen(
                                viewModel = feedbackListVm,
                                onFeedbackClick = { id -> navController.navigate(FeedRailRoute.feedbackDetail(id)) },
                                onSubmitClick = { navController.navigate(FeedRailRoute.SUBMIT_FEEDBACK) },
                            )
                        }

                        composable(
                            route = FeedRailRoute.FEEDBACK_DETAIL,
                            arguments = listOf(navArgument("feedbackId") { type = NavType.StringType }),
                        ) { backStack ->
                            val feedbackId = backStack.arguments?.getString("feedbackId") ?: return@composable
                            val vm: FeedbackDetailViewModel = viewModel(factory = FeedbackDetailViewModel.factory(sdk))
                            FeedbackDetailScreen(
                                feedbackId = feedbackId,
                                viewModel = vm,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(FeedRailRoute.SUBMIT_FEEDBACK) {
                            val vm: SubmitFeedbackViewModel = viewModel(factory = SubmitFeedbackViewModel.factory(sdk))
                            SubmitFeedbackScreen(
                                viewModel = vm,
                                boards = feedbackListState.boards,
                                onBack = { navController.popBackStack() },
                                onSuccess = {
                                    onFeedbackSubmitted()
                                    navController.popBackStack()
                                },
                            )
                        }

                        composable(FeedRailRoute.ROADMAP) {
                            val vm: RoadmapViewModel = viewModel(factory = RoadmapViewModel.factory(sdk))
                            RoadmapScreen(
                                viewModel = vm,
                                onFeedbackClick = { id -> navController.navigate(FeedRailRoute.feedbackDetail(id)) },
                            )
                        }

                        composable(FeedRailRoute.CHANGELOG) {
                            val vm: ChangelogViewModel = viewModel(factory = ChangelogViewModel.factory(sdk))
                            ChangelogScreen(
                                viewModel = vm,
                                onFeedbackClick = { id -> navController.navigate(FeedRailRoute.feedbackDetail(id)) },
                            )
                        }

                        composable(
                            route = FeedRailRoute.PROFILE,
                            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
                        ) { backStack ->
                            val userId = backStack.arguments?.getString("userId") ?: return@composable
                            val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(sdk))
                            ProfileScreen(
                                userId = userId,
                                viewModel = vm,
                                onFeedbackClick = { id -> navController.navigate(FeedRailRoute.feedbackDetail(id)) },
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }

                    if (showPoweredBy) {
                        PoweredByBadge(
                            modifier = Modifier
                                .padding(bottom = if (isTabScreen && !isError) 80.dp else 16.dp)
                        )
                    }
                }

                // Overlay Toolbar
                val isError = feedbackListState.error != null && feedbackListState.items.isEmpty()
                if (isTabScreen && !isError) {
                    FeedRailFloatingToolbar(
                        tabs = FeedRailTab.entries,
                        currentRoute = currentRoute,
                        onTabClick = { tab ->
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(FeedRailRoute.FEEDBACK_LIST) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onAddClick = {
                            navController.navigate(FeedRailRoute.SUBMIT_FEEDBACK)
                        },
                        showAddButton = feedbackListState.canSubmit,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    )
                }
            }
            }
 
            if (showLanguageDialog) {
                LanguageSelectorDialog(
                    currentCode = sdk.languageManager.currentCode,
                    onSelectLanguage = { code -> sdk.languageManager.selectLanguage(code) },
                    onDismiss = { showLanguageDialog = false },
                )
            }
        }
    }
}
