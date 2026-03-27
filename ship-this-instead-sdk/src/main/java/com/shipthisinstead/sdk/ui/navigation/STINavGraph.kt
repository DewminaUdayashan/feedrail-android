package com.shipthisinstead.sdk.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shipthisinstead.sdk.R
import com.shipthisinstead.sdk.`public`.ShipThisInsteadSDK
import com.shipthisinstead.sdk.auth.AuthState
import com.shipthisinstead.sdk.ui.components.AvatarImage
import com.shipthisinstead.sdk.ui.components.BrandingHeader
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipthisinstead.sdk.ui.screens.ChangelogScreen
import com.shipthisinstead.sdk.ui.screens.FeedbackDetailScreen
import com.shipthisinstead.sdk.ui.screens.FeedbackListScreen
import com.shipthisinstead.sdk.ui.screens.ProfileScreen
import com.shipthisinstead.sdk.ui.screens.RoadmapScreen
import com.shipthisinstead.sdk.ui.screens.SubmitFeedbackScreen
import com.shipthisinstead.sdk.ui.theme.STITheme
import com.shipthisinstead.sdk.ui.viewmodels.ChangelogViewModel
import com.shipthisinstead.sdk.ui.viewmodels.FeedbackDetailViewModel
import com.shipthisinstead.sdk.ui.viewmodels.FeedbackListViewModel
import com.shipthisinstead.sdk.ui.viewmodels.ProfileViewModel
import com.shipthisinstead.sdk.ui.viewmodels.RoadmapViewModel
import com.shipthisinstead.sdk.ui.viewmodels.SubmitFeedbackViewModel
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

    val showBottomBar = currentRoute in tabRoutes
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }

    // ── Reactive auth state → correctly updates avatar when auth completes ────
    val authState by sdk.authManager.authState.collectAsStateWithLifecycle()
    val currentUser = (authState as? AuthState.Authenticated)?.user

    // ── Shared FeedbackListViewModel — also provides branding for the theme ───
    val feedbackListVm: FeedbackListViewModel = viewModel(
        factory = FeedbackListViewModel.factory(sdk),
    )
    val feedbackListState by feedbackListVm.uiState.collectAsStateWithLifecycle()

    // ── Accent color: live-updates from branding once boards load ─────────────
    val accentColor = remember(feedbackListState.branding?.resolvedAccentColor) {
        STIColor(feedbackListState.branding?.resolvedAccentColor ?: "#6366F1")
    }

    // ── Tablet / wide-screen: constrain sheet width so it doesn't stretch ─────
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val contentWidthModifier = if (screenWidthDp >= 600) {
        Modifier.widthIn(max = 560.dp)
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalSTISDK provides sdk) {
        // STITheme is applied HERE (inside the sheet) so it reacts to branding
        STITheme(accentColor = accentColor) {
            Scaffold(
                modifier = modifier.then(contentWidthModifier),
                // Zero out Scaffold's own insets — the ModalBottomSheet already
                // sits above the navigation bar; Scaffold must not add a second
                // nav-bar-sized gap below the NavigationBar.
                contentWindowInsets = WindowInsets(0),
                topBar = {
                    if (showBottomBar) {
                        TopAppBar(
                            title = {
                                BrandingHeader(
                                    branding = feedbackListState.branding,
                                    title = stringResource(R.string.sti_general_feedback),
                                )
                            },
                            actions = {
                                Box {
                                    IconButton(onClick = { showProfileMenu = true }) {
                                        AvatarImage(
                                            avatarUrl = currentUser?.avatarUrl,
                                            displayName = currentUser?.displayName
                                                ?: sdk.config.userName
                                                ?: "?",
                                            size = 32.dp,
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
                            },
                        )
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        // windowInsets = 0: the ModalBottomSheet already positions
                        // the sheet above the system nav bar, so NavigationBar must
                        // not add its own nav-bar bottom padding on top of that.
                        NavigationBar(windowInsets = WindowInsets(0)) {
                            STITab.entries.forEach { tab ->
                                val isSelected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (!isSelected) {
                                            navController.navigate(tab.route) {
                                                popUpTo(STIRoute.FEEDBACK_LIST) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.titleResId),
                                        )
                                    },
                                    label = { Text(stringResource(tab.titleResId)) },
                                )
                            }
                        }
                    }
                },
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = STIRoute.FEEDBACK_LIST,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    // ── Feedback List ──────────────────────────────────────────
                    composable(STIRoute.FEEDBACK_LIST) {
                        FeedbackListScreen(
                            viewModel = feedbackListVm,
                            onFeedbackClick = { id ->
                                navController.navigate(STIRoute.feedbackDetail(id))
                            },
                            onSubmitClick = {
                                navController.navigate(STIRoute.SUBMIT_FEEDBACK)
                            },
                            onProfileClick = { id ->
                                navController.navigate(STIRoute.profile(id))
                            },
                        )
                    }

                    // ── Feedback Detail ────────────────────────────────────────
                    composable(
                        route = STIRoute.FEEDBACK_DETAIL,
                        arguments = listOf(navArgument("feedbackId") { type = NavType.StringType }),
                    ) { backStack ->
                        val feedbackId = backStack.arguments?.getString("feedbackId") ?: return@composable
                        val vm: FeedbackDetailViewModel = viewModel(
                            factory = FeedbackDetailViewModel.factory(sdk),
                        )
                        FeedbackDetailScreen(
                            feedbackId = feedbackId,
                            viewModel = vm,
                            onBack = { navController.popBackStack() },
                            onUserClick = { userId ->
                                navController.navigate(STIRoute.profile(userId))
                            },
                        )
                    }

                    // ── Submit Feedback ────────────────────────────────────────
                    composable(STIRoute.SUBMIT_FEEDBACK) {
                        val vm: SubmitFeedbackViewModel = viewModel(
                            factory = SubmitFeedbackViewModel.factory(sdk),
                        )
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

                    // ── Roadmap ────────────────────────────────────────────────
                    composable(STIRoute.ROADMAP) {
                        val vm: RoadmapViewModel = viewModel(
                            factory = RoadmapViewModel.factory(sdk),
                        )
                        RoadmapScreen(
                            viewModel = vm,
                            onFeedbackClick = { id ->
                                navController.navigate(STIRoute.feedbackDetail(id))
                            },
                        )
                    }

                    // ── Changelog ─────────────────────────────────────────────
                    composable(STIRoute.CHANGELOG) {
                        val vm: ChangelogViewModel = viewModel(
                            factory = ChangelogViewModel.factory(sdk),
                        )
                        ChangelogScreen(
                            viewModel = vm,
                            onFeedbackClick = { id ->
                                navController.navigate(STIRoute.feedbackDetail(id))
                            },
                        )
                    }

                    // ── Profile ────────────────────────────────────────────────
                    composable(
                        route = STIRoute.PROFILE,
                        arguments = listOf(navArgument("userId") { type = NavType.StringType }),
                    ) { backStack ->
                        val userId = backStack.arguments?.getString("userId") ?: return@composable
                        val vm: ProfileViewModel = viewModel(
                            factory = ProfileViewModel.factory(sdk),
                        )
                        ProfileScreen(
                            userId = userId,
                            viewModel = vm,
                            onFeedbackClick = { id ->
                                navController.navigate(STIRoute.feedbackDetail(id))
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }

            // Language selector dialog
            if (showLanguageDialog) {
                LanguageSelectorDialog(
                    currentCode = sdk.languageManager.currentCode,
                    onSelectLanguage = { code ->
                        sdk.languageManager.selectLanguage(code)
                    },
                    onDismiss = { showLanguageDialog = false },
                )
            }
        }
    }
}
