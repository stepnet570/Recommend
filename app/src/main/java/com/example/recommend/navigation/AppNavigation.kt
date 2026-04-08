package com.example.recommend.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.recommend.*
import com.example.recommend.data.model.*
import com.example.recommend.ui.add.AddScreen
import com.example.recommend.ui.explore.ExploreScreen
import com.example.recommend.ui.feed.FeedScreen
import com.example.recommend.ui.profile.ProfileScreen
import com.example.recommend.ui.theme.SoftPastelMint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private enum class BusinessAddDestination { Hub, Campaign, Post }

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    // Feed state
    posts: List<Post>,
    allUsers: List<UserProfile>,
    currentUserProfile: UserProfile?,
    feedRequests: List<PackRequest>,
    savedPostIds: Set<String>,
    activeOffers: List<AdOffer>,
    isLoading: Boolean,
    feedPostsForHome: List<Post>,
    feedOffersForHome: List<AdOffer>,
    // Profile state
    myPosts: List<Post>,
    userCollections: List<PostCollection>,
    myOffers: List<AdOffer>,
    followersCount: Int,
    participatingPromoCampaignsCount: Int,
    onLogout: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // --- overlay state ---
    var isAskModalOpen by remember { mutableStateOf(false) }
    var activeRequest by remember { mutableStateOf<PackRequest?>(null) }
    var postToSave by remember { mutableStateOf<String?>(null) }
    var activeCollection by remember { mutableStateOf<PostCollection?>(null) }
    var businessAddDestination by remember { mutableStateOf(BusinessAddDestination.Hub) }
    var viewUserProfileId by remember { mutableStateOf<String?>(null) }
    var openPostId by remember { mutableStateOf<String?>(null) }
    var createCampaignOverlay by remember { mutableStateOf(false) }
    var linkedRequestIdForAdd by remember { mutableStateOf<String?>(null) }
    var addPickForRequestId by remember { mutableStateOf<String?>(null) }
    var selectedOfferId by remember { mutableStateOf<String?>(null) }
    var profileOfferCache by remember { mutableStateOf<AdOffer?>(null) }
    var profileSurfaceOrdinal by rememberSaveable { mutableIntStateOf(0) }

    // Reset business sub-destination when leaving add tab
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(currentRoute) {
        if (currentRoute != "add") businessAddDestination = BusinessAddDestination.Hub
    }

    // Clear stale selectedOfferId if the offer is no longer known
    LaunchedEffect(myOffers, activeOffers, profileOfferCache, selectedOfferId) {
        val id = selectedOfferId ?: return@LaunchedEffect
        val known = myOffers.any { it.id == id } ||
            activeOffers.any { it.id == id } ||
            profileOfferCache?.id == id
        if (!known) selectedOfferId = null
    }

    LaunchedEffect(viewUserProfileId) {
        if (viewUserProfileId == null) profileOfferCache = null
    }

    fun ratePost(postId: String, stars: Int) {
        val uid = currentUser?.uid ?: return
        db.trustListDataRoot()
            .collection("posts").document(postId)
            .update("ratings.$uid", stars.coerceIn(1, 5))
    }

    fun toggleOfferPause(offer: AdOffer) {
        val uid = currentUser?.uid ?: return
        if (offer.businessId != uid) return
        val next = if (offer.status.equals("active", ignoreCase = true)) "paused" else "active"
        db.trustListDataRoot().collection("offers").document(offer.id).update("status", next)
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "feed") {

            composable("feed") {
                FeedScreen(
                    posts = feedPostsForHome,
                    requests = feedRequests,
                    users = allUsers,
                    followingUserIds = currentUserProfile?.following?.toSet() ?: emptySet(),
                    savedPostIds = savedPostIds,
                    activeOffers = feedOffersForHome,
                    trustCoins = currentUserProfile?.trustCoins ?: 0,
                    onOfferAccept = { offer -> selectedOfferId = offer.id },
                    onAskPackClick = { isAskModalOpen = true },
                    onRequestClick = { selectedRequest -> activeRequest = selectedRequest },
                    onSignalRequestOpen = { activeRequest = it },
                    onHelpRequest = { req -> addPickForRequestId = req.id },
                    onSaveClick = { postToSave = it },
                    onUserProfileClick = { viewUserProfileId = it },
                    onRequestAuthorProfileClick = { viewUserProfileId = it },
                    viewerUid = currentUser?.uid,
                    onAudienceRate = { postId, stars -> ratePost(postId, stars) },
                    onOpenPost = { openPostId = it },
                    onWalletClick = { navController.navigate("profile") }
                )
            }

            composable("explore") {
                ExploreScreen(onUserProfileClick = { viewUserProfileId = it })
            }

            composable("add") {
                if (currentUserProfile?.isBusiness == true) {
                    when (businessAddDestination) {
                        BusinessAddDestination.Hub -> AddHubScreen(
                            onCampaign = { businessAddDestination = BusinessAddDestination.Campaign },
                            onPost = { businessAddDestination = BusinessAddDestination.Post }
                        )
                        BusinessAddDestination.Campaign -> CreateOfferScreen(
                            userProfile = currentUserProfile,
                            onOfferCreated = {
                                businessAddDestination = BusinessAddDestination.Hub
                                navController.navigate("profile")
                            },
                            onBack = { businessAddDestination = BusinessAddDestination.Hub }
                        )
                        BusinessAddDestination.Post -> AddScreen(
                            onPostAdded = {
                                linkedRequestIdForAdd = null
                                businessAddDestination = BusinessAddDestination.Hub
                                navController.navigate("feed")
                            },
                            currentUserProfile = currentUserProfile,
                            onBack = { businessAddDestination = BusinessAddDestination.Hub },
                            requestId = linkedRequestIdForAdd
                        )
                    }
                } else {
                    AddScreen(
                        onPostAdded = {
                            linkedRequestIdForAdd = null
                            navController.navigate("feed")
                        },
                        currentUserProfile = currentUserProfile,
                        requestId = linkedRequestIdForAdd
                    )
                }
            }

            composable("profile") {
                if (currentUserProfile != null) {
                    ProfileScreen(
                        userProfile = currentUserProfile,
                        myPosts = myPosts,
                        collections = userCollections,
                        myOffers = myOffers,
                        followersCount = followersCount,
                        participatingPromoCampaignsCount = participatingPromoCampaignsCount,
                        profileSurfaceOrdinal = profileSurfaceOrdinal,
                        onProfileSurfaceChange = { profileSurfaceOrdinal = it.coerceIn(0, 1) },
                        onCollectionClick = { activeCollection = it },
                        onPostClick = { openPostId = it },
                        onCreateCampaign = { createCampaignOverlay = true },
                        onOfferClick = { selectedOfferId = it.id },
                        onOfferPauseToggle = { toggleOfferPause(it) },
                        onLogout = onLogout
                    )
                }
            }
        }

        // --- overlays (rendered above NavHost content) ---

        if (isAskModalOpen) {
            AskPackScreen(
                onDismiss = { isAskModalOpen = false },
                users = allUsers,
                currentUserProfile = currentUserProfile
            )
        }

        if (activeRequest != null) {
            val requestAuthor = allUsers.find { it.uid == activeRequest!!.userId }
                ?: UserProfile(uid = activeRequest!!.userId, name = "Member")
            Surface(modifier = Modifier.fillMaxSize().zIndex(5f), color = SoftPastelMint) {
                RequestDetailScreen(
                    request = activeRequest!!,
                    requestAuthor = requestAuthor,
                    users = allUsers,
                    savedPostIds = savedPostIds,
                    viewerUid = currentUser?.uid,
                    onBack = { activeRequest = null },
                    onUserProfileClick = { viewUserProfileId = it },
                    onSaveClick = { postToSave = it },
                    onAudienceRate = { postId, stars -> ratePost(postId, stars) },
                    onOpenPost = { openPostId = it },
                    onAddRecommendation = { addPickForRequestId = activeRequest!!.id }
                )
            }
        }

        if (addPickForRequestId != null) {
            Surface(modifier = Modifier.fillMaxSize().zIndex(6f), color = SoftPastelMint) {
                AddPickScreen(
                    requestId = addPickForRequestId!!,
                    currentUserProfile = currentUserProfile,
                    onDismiss = { addPickForRequestId = null }
                )
            }
        }

        if (postToSave != null) {
            SaveCollectionDialog(postId = postToSave!!, onDismiss = { postToSave = null })
        }

        if (activeCollection != null) {
            CollectionDetailScreen(
                collection = activeCollection!!,
                posts = posts.filter { activeCollection!!.postIds.contains(it.id) },
                savedPostIds = savedPostIds,
                users = allUsers,
                onBack = { activeCollection = null },
                onSaveClick = { postToSave = it },
                onUserProfileClick = { viewUserProfileId = it },
                viewerUid = currentUser?.uid,
                onAudienceRate = { postId, stars -> ratePost(postId, stars) },
                onOpenPost = { openPostId = it }
            )
        }

        if (viewUserProfileId != null && currentUser != null) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                PublicUserProfileScreen(
                    userId = viewUserProfileId!!,
                    viewerUid = currentUser.uid,
                    viewerProfile = currentUserProfile,
                    allUsers = allUsers,
                    userPosts = posts.filter { it.userId == viewUserProfileId },
                    onBack = { viewUserProfileId = null },
                    onPostClick = { openPostId = it },
                    onCollectionClick = { activeCollection = it },
                    onOfferClick = { offer ->
                        profileOfferCache = offer
                        selectedOfferId = offer.id
                    }
                )
            }
        }

        if (openPostId != null && currentUser != null) {
            val detailPost = posts.find { it.id == openPostId }
            if (detailPost != null) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    PostDetailScreen(
                        post = detailPost,
                        users = allUsers,
                        savedPostIds = savedPostIds,
                        viewerUid = currentUser.uid,
                        onBack = { openPostId = null },
                        onSaveClick = { postToSave = it },
                        onUserProfileClick = { viewUserProfileId = it },
                        onAudienceRate = { postId, stars -> ratePost(postId, stars) }
                    )
                }
            }
        }

        val offerForDetail = selectedOfferId?.let { id ->
            myOffers.find { it.id == id }
                ?: activeOffers.find { it.id == id }
                ?: profileOfferCache?.takeIf { it.id == id }
        }
        if (offerForDetail != null) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                BusinessOfferDetailScreen(
                    offer = offerForDetail,
                    onBack = {
                        selectedOfferId = null
                        profileOfferCache = null
                    },
                    onPauseToggle = { toggleOfferPause(it) },
                    canManageCampaign = offerForDetail.businessId == currentUser?.uid
                )
            }
        }

        val businessProfileForCampaign = currentUserProfile?.takeIf { it.isBusiness }
        if (createCampaignOverlay && businessProfileForCampaign != null) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                CreateOfferScreen(
                    userProfile = businessProfileForCampaign,
                    onOfferCreated = { createCampaignOverlay = false },
                    onBack = { createCampaignOverlay = false }
                )
            }
        }
    }
}
