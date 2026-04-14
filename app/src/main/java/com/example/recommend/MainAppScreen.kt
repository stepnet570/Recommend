package com.example.recommend

import androidx.compose.foundation.background
import com.example.recommend.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.recommend.navigation.AppNavigation
import com.example.recommend.ui.feed.FeedViewModel
import com.example.recommend.ui.profile.ProfileViewModel
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.AppLime
import com.example.recommend.ui.theme.AppTeal
import com.example.recommend.ui.theme.AppDark
import com.example.recommend.ui.theme.AppMuted
import com.example.recommend.ui.theme.PrimaryGradient
import com.example.recommend.ui.theme.PrimaryGradientLinear

@Composable
fun MainAppScreen(onLogout: () -> Unit) {
    val feedViewModel: FeedViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val navController = rememberNavController()

    val posts by feedViewModel.posts.collectAsStateWithLifecycle()
    val allUsers by feedViewModel.allUsers.collectAsStateWithLifecycle()
    val currentUserProfile by feedViewModel.currentUser.collectAsStateWithLifecycle()
    val feedRequests by feedViewModel.feedRequests.collectAsStateWithLifecycle()
    val savedPostIds by feedViewModel.savedPostIds.collectAsStateWithLifecycle()
    val activeOffers by feedViewModel.activeOffers.collectAsStateWithLifecycle()
    val isLoading by feedViewModel.isLoading.collectAsStateWithLifecycle()
    val feedPostsForHome by feedViewModel.feedPostsForHome.collectAsStateWithLifecycle()
    val feedOffersForHome by feedViewModel.feedOffersForHome.collectAsStateWithLifecycle()

    val myPosts by profileViewModel.myPosts.collectAsStateWithLifecycle()
    val userCollections by profileViewModel.userCollections.collectAsStateWithLifecycle()
    val myOffers by profileViewModel.myOffers.collectAsStateWithLifecycle()
    val followersCount by profileViewModel.followersCount.collectAsStateWithLifecycle()
    val participatingCount by profileViewModel.participatingPromoCampaignsCount.collectAsStateWithLifecycle()

    val currentRoute by navController.currentBackStackEntryAsState().let { entry ->
        remember { derivedStateOf { entry.value?.destination?.route } }
    }

    // Overlay + creation-flow state lifted from AppNavigation so MainAppScreen can react
    var isAskModalOpen by remember { mutableStateOf(false) }

    // Прямая функция сброса — регистрируется из AppNavigation синхронно через SideEffect
    var resetOverlaysFn by remember { mutableStateOf<() -> Unit>({}) }

    // Dialog state for navigating away from a creation flow
    var pendingTab by remember { mutableStateOf<String?>(null) }
    var showExitCreationDialog by remember { mutableStateOf(false) }

    // true только когда пользователь реально заполняет форму (не на экране выбора Hub)
    var isActuallyCreating by remember { mutableStateOf(false) }
    val isInCreationFlow = isAskModalOpen || isActuallyCreating

    fun navigateTo(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            popUpTo(navController.graph.startDestinationId) { saveState = false }
        }
    }

    fun onNavItemClick(targetRoute: String) {
        // Read state directly at call time — avoids stale capture from last composition
        val inFlow = isAskModalOpen || isActuallyCreating
        if (inFlow && currentRoute != targetRoute) {
            pendingTab = targetRoute
            showExitCreationDialog = true
            return
        }
        resetOverlaysFn()
        navigateTo(targetRoute)
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "Feed",
                            modifier = if (currentRoute == "feed") {
                                Modifier
                                    .size(24.dp)
                                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                    .drawWithCache {
                                        val gradient = PrimaryGradient
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(gradient, blendMode = BlendMode.SrcIn)
                                        }
                                    }
                            } else Modifier
                        )
                    },
                    selected = currentRoute == "feed",
                    onClick = { onNavItemClick("feed") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTeal,
                        indicatorColor = AppLime.copy(alpha = 0.15f),
                        unselectedIconColor = AppDark.copy(alpha = 0.4f)
                    )
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "People",
                            modifier = if (currentRoute == "explore") {
                                Modifier
                                    .size(24.dp)
                                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                    .drawWithCache {
                                        val gradient = PrimaryGradient
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(gradient, blendMode = BlendMode.SrcIn)
                                        }
                                    }
                            } else Modifier
                        )
                    },
                    selected = currentRoute == "explore",
                    onClick = { onNavItemClick("explore") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTeal,
                        indicatorColor = AppLime.copy(alpha = 0.15f),
                        unselectedIconColor = AppDark.copy(alpha = 0.4f)
                    )
                )
                NavigationBarItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = PrimaryGradient,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    },
                    selected = currentRoute == "add",
                    onClick = { onNavItemClick("add") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Profile",
                            modifier = if (currentRoute == "profile") {
                                Modifier
                                    .size(24.dp)
                                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                    .drawWithCache {
                                        val gradient = PrimaryGradient
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(gradient, blendMode = BlendMode.SrcIn)
                                        }
                                    }
                            } else Modifier
                        )
                    },
                    selected = currentRoute == "profile",
                    onClick = { onNavItemClick("profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTeal,
                        indicatorColor = AppLime.copy(alpha = 0.15f),
                        unselectedIconColor = AppDark.copy(alpha = 0.4f)
                    )
                )
            }
        }
    ) { paddingValues ->
        AppNavigation(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            posts = posts,
            allUsers = allUsers,
            currentUserProfile = currentUserProfile,
            feedRequests = feedRequests,
            savedPostIds = savedPostIds,
            activeOffers = activeOffers,
            isLoading = isLoading,
            feedPostsForHome = feedPostsForHome,
            feedOffersForHome = feedOffersForHome,
            myPosts = myPosts,
            userCollections = userCollections,
            myOffers = myOffers,
            followersCount = followersCount,
            participatingPromoCampaignsCount = participatingCount,
            isAskModalOpen = isAskModalOpen,
            onAskModalOpenChange = { isAskModalOpen = it },
            onRegisterReset = { resetOverlaysFn = it },
            onCreationFlowActive = { isActuallyCreating = it },
            onLogout = onLogout
        )
    }

    if (showExitCreationDialog && pendingTab != null) {
        AlertDialog(
            onDismissRequest = {
                showExitCreationDialog = false
                pendingTab = null
            },
            title = {
                Text("Leave this screen?", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Your progress will be lost.",
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryGradientLinear)
                        .clickable {
                            val target = pendingTab!!
                            pendingTab = null
                            showExitCreationDialog = false
                            resetOverlaysFn()
                            navigateTo(target)
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Leave", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitCreationDialog = false
                    pendingTab = null
                }) {
                    Text("Stay", color = AppMuted)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}
