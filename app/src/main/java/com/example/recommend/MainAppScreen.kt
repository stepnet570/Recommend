package com.example.recommend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.recommend.navigation.AppNavigation
import com.example.recommend.ui.feed.FeedViewModel
import com.example.recommend.ui.profile.ProfileViewModel
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.SurfaceMuted

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
                                        val gradient = Brush.horizontalGradient(listOf(Color(0xFF7AE23A), Color(0xFF3BD4C0)))
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(gradient, blendMode = BlendMode.SrcIn)
                                        }
                                    }
                            } else Modifier
                        )
                    },
                    selected = currentRoute == "feed",
                    onClick = { navController.navigate("feed") { launchSingleTop = true } },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF3BD4C0), indicatorColor = Color(0xFF7AE23A).copy(alpha = 0.15f), unselectedIconColor = Color(0xFF2D3A36).copy(alpha = 0.4f))
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
                                        val gradient = Brush.horizontalGradient(listOf(Color(0xFF7AE23A), Color(0xFF3BD4C0)))
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(gradient, blendMode = BlendMode.SrcIn)
                                        }
                                    }
                            } else Modifier
                        )
                    },
                    selected = currentRoute == "explore",
                    onClick = { navController.navigate("explore") { launchSingleTop = true } },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF3BD4C0), indicatorColor = Color(0xFF7AE23A).copy(alpha = 0.15f), unselectedIconColor = Color(0xFF2D3A36).copy(alpha = 0.4f))
                )
                NavigationBarItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFF7AE23A), Color(0xFF3BD4C0))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    },
                    selected = currentRoute == "add",
                    onClick = { navController.navigate("add") { launchSingleTop = true } },
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
                                        val gradient = Brush.horizontalGradient(listOf(Color(0xFF7AE23A), Color(0xFF3BD4C0)))
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(gradient, blendMode = BlendMode.SrcIn)
                                        }
                                    }
                            } else Modifier
                        )
                    },
                    selected = currentRoute == "profile",
                    onClick = { navController.navigate("profile") { launchSingleTop = true } },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF3BD4C0), indicatorColor = Color(0xFF7AE23A).copy(alpha = 0.15f), unselectedIconColor = Color(0xFF2D3A36).copy(alpha = 0.4f))
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
            onLogout = onLogout
        )
    }
}
