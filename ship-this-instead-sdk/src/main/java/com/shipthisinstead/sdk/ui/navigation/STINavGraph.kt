package com.shipthisinstead.sdk.ui.navigation

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
import com.shipthisinstead.sdk.R
import com.shipthisinstead.sdk.`public`.ShipThisInsteadSDK
import com.shipthisinstead.sdk.auth.AuthState
import com.shipthisinstead.sdk.ui.components.AvatarImage
import com.shipthisinstead.sdk.ui.components.BrandingHeader
import com.shipthisinstead.sdk.ui.components.PoweredByBadge
import com.shipthisinstead.sdk.ui.components.STIFloatingToolbar
import com.shipthisinstead.sdk.ui.screens.*
import com.shipthisinstead.sdk.ui.theme.STITheme
import com.shipthisinstead.sdk.ui.viewmodels.*
import com.shipthisinstead.sdk.ui.theme.Color as STIColor

// ── Route constants ───────────────────────────────────────────────────────────

object STIRoute {
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

val LocalSTISDK = staticCompositionLocalOf<ShipThisInsteadSDK> {
    error("No ShipThisInsteadSDK provided")
}

// ── Tab enum ──────────────────────────────────────────────────────────────────

enum class STITab(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    FEEDBACK(
        route = STIRoute.FEEDBACK_LIST,
        titleResId = R.string.sti_general_feedback,
        selectedIcon = Icons.Filled.Campaign,
        unselectedIcon = Icons.Outlined.Campaign,
    ),
    ROADMAP(
        route = STIRoute.ROADMAP,
        titleResId = R.string.sti_general_roadmap,
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map,
    ),
    CHANGELOG(
        route = STIRoute.CHANGELOG,
        titleResId = R.string.sti_general_changelog,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
    ),
}

private val tabRoutes = STITab.entries.map { it.route }.toSet()

// ── Nav Graph ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun STINavGraph(
    sdk: ShipThisInsteadSDK,
    onDismiss: () -> Unit,
    onFeedbackSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val isTabScreen = currentRoute in tabRoutes
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }

    // Auth state
    val authState by sdk.authManager.authState.collectAsStateWithLifecycle()
    val currentUser = (authState as? AuthState.Authenticated)?.user

    // Shared FeedbackListViewModel for branding
    val feedbackListVm: FeedbackListViewModel = viewModel(
        factory = FeedbackListViewModel.factory(sdk),
    )
    val feedbackListState by feedbackListVm.uiState.collectAsStateWithLifecycle()

    // Accent color from branding - Fallback to default if archived OR no branding
    val accentColor = remember(feedbackListState.branding?.resolvedAccentColor, feedbackListState.isProjectArchived) {
        val hex = if (feedbackListState.isProjectArchived == true || feedbackListState.branding == null) {
            "#6366F1" // Default Indigo
        } else {
            feedbackListState.branding?.resolvedAccentColor ?: "#6366F1"
        }
        STIColor(hex)
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

    CompositionLocalProvider(LocalSTISDK provides sdk) {
        STITheme(accentColor = accentColor) {
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
                                title = stringResource(R.string.sti_general_feedback),
                                modifier = Modifier.weight(1f)
                            )

                            Box {
                                IconButton(onClick = { showProfileMenu = true }) {
                                    AvatarImage(
                                        avatarUrl = currentUser?.avatarUrl,
                                        displayName = currentUser?.displayName
                                            ?: sdk.config.userName
                                            ?: "?",
                                        size = 36.dp,
                                    )
                                }
                                DropdownMenu(
                                    expanded = showProfileMenu,
                                    onDismissRequest = { showProfileMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sti_settings_language)) },
                                        onClick = {
                                            showProfileMenu = false
                                            showLanguageDialog = true
                                        },
                                    )
                                    if (currentUser != null) {
                                        DropdownMenuItem(
                                            text = { Text(currentUser.displayName) },
                                            onClick = {
                                                showProfileMenu = false
                                                navController.navigate(STIRoute.profile(currentUser.id))
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = STIRoute.FEEDBACK_LIST,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        composable(STIRoute.FEEDBACK_LIST) {
                            FeedbackListScreen(
                                viewModel = feedbackListVm,
                                onFeedbackClick = { id -> navController.navigate(STIRoute.feedbackDetail(id)) },
                                onSubmitClick = { navController.navigate(STIRoute.SUBMIT_FEEDBACK) },
                                onProfileClick = { id -> navController.navigate(STIRoute.profile(id)) },
                            )
                        }

                        composable(
                            route = STIRoute.FEEDBACK_DETAIL,
                            arguments = listOf(navArgument("feedbackId") { type = NavType.StringType }),
                        ) { backStack ->
                            val feedbackId = backStack.arguments?.getString("feedbackId") ?: return@composable
                            val vm: FeedbackDetailViewModel = viewModel(factory = FeedbackDetailViewModel.factory(sdk))
                            FeedbackDetailScreen(
                                feedbackId = feedbackId,
                                viewModel = vm,
                                onBack = { navController.popBackStack() },
                                onUserClick = { userId -> navController.navigate(STIRoute.profile(userId)) },
                            )
                        }

                        composable(STIRoute.SUBMIT_FEEDBACK) {
                            val vm: SubmitFeedbackViewModel = viewModel(factory = SubmitFeedbackViewModel.factory(sdk))
                            SubmitFeedbackScreen(
                                viewModel = vm,
                                boards = feedbackListVm.uiState.value.boards,
                                onBack = { navController.popBackStack() },
                                onSuccess = {
                                    onFeedbackSubmitted()
                                    navController.popBackStack()
                                },
                            )
                        }

                        composable(STIRoute.ROADMAP) {
                            val vm: RoadmapViewModel = viewModel(factory = RoadmapViewModel.factory(sdk))
                            RoadmapScreen(
                                viewModel = vm,
                                onFeedbackClick = { id -> navController.navigate(STIRoute.feedbackDetail(id)) },
                            )
                        }

                        composable(STIRoute.CHANGELOG) {
                            val vm: ChangelogViewModel = viewModel(factory = ChangelogViewModel.factory(sdk))
                            ChangelogScreen(
                                viewModel = vm,
                                onFeedbackClick = { id -> navController.navigate(STIRoute.feedbackDetail(id)) },
                            )
                        }

                        composable(
                            route = STIRoute.PROFILE,
                            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
                        ) { backStack ->
                            val userId = backStack.arguments?.getString("userId") ?: return@composable
                            val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(sdk))
                            ProfileScreen(
                                userId = userId,
                                viewModel = vm,
                                onFeedbackClick = { id -> navController.navigate(STIRoute.feedbackDetail(id)) },
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
                    STIFloatingToolbar(
                        tabs = STITab.entries,
                        currentRoute = currentRoute,
                        onTabClick = { tab ->
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(STIRoute.FEEDBACK_LIST) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onAddClick = {
                            navController.navigate(STIRoute.SUBMIT_FEEDBACK)
                        },
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
