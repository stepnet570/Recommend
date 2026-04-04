package com.example.recommend

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.SurfaceMuted
import com.example.recommend.ui.theme.SoftPastelMint
import com.example.recommend.ui.theme.SurfacePastel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

private enum class BusinessAddDestination { Hub, Campaign, Post }

@Composable
fun MainAppScreen(onLogout: () -> Unit) {
    var currentTab by remember { mutableStateOf("feed") }

    var realPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var allRequests by remember { mutableStateOf<List<PackRequest>>(emptyList()) }
    var allUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userCollections by remember { mutableStateOf<List<PostCollection>>(emptyList()) }
    var myOffers by remember { mutableStateOf<List<AdOffer>>(emptyList()) }
    /** Active B2B offers for the feed carousel (all businesses). */
    var feedActiveOffers by remember { mutableStateOf<List<AdOffer>>(emptyList()) }
    var followersCount by remember { mutableStateOf(0) }
    var participatingPromoCampaignsCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    var isAskModalOpen by remember { mutableStateOf(false) }
    var activeRequest by remember { mutableStateOf<PackRequest?>(null) }
    var postToSave by remember { mutableStateOf<String?>(null) }
    var activeCollection by remember { mutableStateOf<PostCollection?>(null) }
    var businessAddDestination by remember { mutableStateOf(BusinessAddDestination.Hub) }
    var viewUserProfileId by remember { mutableStateOf<String?>(null) }
    var openPostId by remember { mutableStateOf<String?>(null) }
    var createCampaignOverlay by remember { mutableStateOf(false) }
    /** When helping from PackSignals: bind new post to this request (main Add tab only). */
    var linkedRequestIdForAdd by remember { mutableStateOf<String?>(null) }
    /** Full-screen [AddPickScreen] for pack signal replies (not the main Add tab). */
    var addPickForRequestId by remember { mutableStateOf<String?>(null) }
    var selectedOfferId by remember { mutableStateOf<String?>(null) }
    /** Offer opened from another user's profile (not in myOffers / feedActiveOffers). */
    var profileOfferCache by remember { mutableStateOf<AdOffer?>(null) }
    /** 0 = personal cabinet, 1 = ads campaigns; lives in MainApp so it survives tab switches */
    var profileSurfaceOrdinal by rememberSaveable { mutableIntStateOf(0) }

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val appContext = LocalContext.current.applicationContext

    LaunchedEffect(currentUser) {
        if (currentUser == null) return@LaunchedEffect

        // Email/password sign-up can miss the first Firestore write; Google users may get a doc from elsewhere.
        // This creates users/{uid} if missing so both flows match.
        db.ensureUserProfileForAuthUser(currentUser) { }
        // One-time merge for legacy user docs missing fields (trustRatings, following shape, etc.).
        db.migrateAllUserProfilesIfNeeded(appContext) { }

        // Posts
        db.trustListDataRoot()
            .collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    realPosts = snapshot.documents.map { it.toPostFromDoc() }
                    isLoading = false
                }
            }

        // Pack requests
        db.trustListDataRoot()
            .collection("requests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allRequests = snapshot.documents.mapNotNull { doc ->
                        PackRequest(
                            id = doc.id, userId = doc.getString("userId") ?: "", text = doc.getString("text") ?: "",
                            tags = (doc.get("tags") as? List<String>) ?: emptyList(), location = doc.getString("location") ?: "",
                            selectedUsers = (doc.get("selectedUsers") as? List<String>) ?: emptyList(),
                            status = doc.getString("status") ?: "active", createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                }
            }

        // Users
        db.trustListDataRoot()
            .collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allUsers = snapshot.documents.mapNotNull { it.toUserProfileOrNull() }
                }
            }

        // Current user profile
        db.trustListDataRoot()
            .collection("users").document(currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    currentUserProfile = snapshot.toUserProfileOrNull()
                }
            }

        // Collections
        db.trustListDataRoot()
            .collection("collections")
            .whereEqualTo("userId", currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    userCollections = snapshot.documents.mapNotNull { doc ->
                        PostCollection(
                            id = doc.id, userId = doc.getString("userId") ?: "", name = doc.getString("name") ?: "",
                            postIds = (doc.get("postIds") as? List<String>) ?: emptyList(), createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                }
            }

        // Business ad offers (campaigns you launched)
        db.trustListDataRoot()
            .collection("offers")
            .whereEqualTo("businessId", currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    myOffers = snapshot.documents
                        .mapNotNull { it.toAdOfferOrNull() }
                        .sortedByDescending { it.createdAt }
                }
            }

        // Public feed: all active offers (B2B strip on Feed)
        db.trustListDataRoot()
            .collection("offers")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    feedActiveOffers = snapshot.documents
                        .mapNotNull { it.toAdOfferOrNull() }
                        .filter { it.status.equals("active", ignoreCase = true) }
                        .sortedByDescending { it.createdAt }
                }
            }

        // Users who follow this account (their user doc has "following" containing our uid)
        db.trustListDataRoot()
            .collection("users")
            .whereArrayContains("following", currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                followersCount = snapshot?.size() ?: 0
            }

        // Offers where we participate as promoter (advertiser role for another brand)
        db.trustListDataRoot()
            .collection("offers")
            .whereArrayContains("promoterUserIds", currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                participatingPromoCampaignsCount = snapshot?.size() ?: 0
            }
    }

    val feedRequests = remember(allRequests, realPosts, currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid.isNullOrBlank()) emptyList()
        else allRequests
            .filter { it.userId == uid || it.selectedUsers.contains(uid) }
            .map { req ->
                req.copy(answersCount = realPosts.count { it.replyToRequestId == req.id })
            }
    }

    /** Pack-signal replies (picks) live on the request detail / Add pick flow, not in the main feed. */
    val feedPostsForHome = remember(realPosts) {
        realPosts.filter { it.replyToRequestId.isNullOrBlank() }
    }

    val myPosts = realPosts.filter { it.userId == currentUser?.uid }

    val savedPostIds = remember(userCollections) {
        userCollections.flatMap { it.postIds }.toSet()
    }

    /** Home feed: show other businesses' active offers only, not your own campaigns. */
    val feedOffersForHome = remember(feedActiveOffers, currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid.isNullOrBlank()) feedActiveOffers
        else feedActiveOffers.filter { it.businessId != uid }
    }

    LaunchedEffect(currentTab) {
        if (currentTab != "add") businessAddDestination = BusinessAddDestination.Hub
    }

    LaunchedEffect(myOffers, feedActiveOffers, profileOfferCache, selectedOfferId) {
        val id = selectedOfferId ?: return@LaunchedEffect
        val known = myOffers.any { it.id == id } ||
            feedActiveOffers.any { it.id == id } ||
            profileOfferCache?.id == id
        if (!known) selectedOfferId = null
    }

    LaunchedEffect(viewUserProfileId) {
        if (viewUserProfileId == null) profileOfferCache = null
    }

    fun ratePost(postId: String, stars: Int) {
        val uid = currentUser?.uid ?: return
        db.trustListDataRoot()
            .collection("posts")
            .document(postId)
            .update("ratings.$uid", stars.coerceIn(1, 5))
    }

    fun toggleOfferPause(offer: AdOffer) {
        val uid = currentUser?.uid ?: return
        if (offer.businessId != uid) return
        val next = if (offer.status.equals("active", ignoreCase = true)) "paused" else "active"
        db.trustListDataRoot()
            .collection("offers")
            .document(offer.id)
            .update("status", next)
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = SurfacePastel, tonalElevation = 14.dp) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, "Feed") }, selected = currentTab == "feed", onClick = { currentTab = "feed" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RichPastelCoral,
                        unselectedIconColor = MutedPastelTeal,
                        indicatorColor = SurfaceMuted
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Search, "People") }, selected = currentTab == "explore", onClick = { currentTab = "explore" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RichPastelCoral,
                        unselectedIconColor = MutedPastelTeal,
                        indicatorColor = SurfaceMuted
                    )
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Filled.AddCircle,
                            "Add",
                            tint = if (currentTab == "add") RichPastelCoral else MutedPastelTeal,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    selected = currentTab == "add",
                    onClick = {
                        linkedRequestIdForAdd = null
                        currentTab = "add"
                    },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, "Profile") }, selected = currentTab == "profile", onClick = { currentTab = "profile" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RichPastelCoral,
                        unselectedIconColor = MutedPastelTeal,
                        indicatorColor = SurfaceMuted
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                "feed" -> {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RichPastelCoral) }
                    } else {
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
                            onWalletClick = { currentTab = "profile" }
                        )
                    }
                }
                "explore" -> ExploreScreen(onUserProfileClick = { viewUserProfileId = it })
                "add" -> {
                    if (currentUserProfile?.isBusiness == true) {
                        when (businessAddDestination) {
                            BusinessAddDestination.Hub -> AddHubScreen(
                                onCampaign = { businessAddDestination = BusinessAddDestination.Campaign },
                                onPost = { businessAddDestination = BusinessAddDestination.Post }
                            )
                            BusinessAddDestination.Campaign -> CreateOfferScreen(
                                userProfile = currentUserProfile!!,
                                onOfferCreated = {
                                    businessAddDestination = BusinessAddDestination.Hub
                                    currentTab = "profile"
                                },
                                onBack = { businessAddDestination = BusinessAddDestination.Hub }
                            )
                            BusinessAddDestination.Post -> AddScreen(
                                onPostAdded = {
                                    linkedRequestIdForAdd = null
                                    businessAddDestination = BusinessAddDestination.Hub
                                    currentTab = "feed"
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
                                currentTab = "feed"
                            },
                            currentUserProfile = currentUserProfile,
                            requestId = linkedRequestIdForAdd
                        )
                    }
                }
                "profile" -> {
                    if (currentUserProfile != null) {
                        ProfileScreen(
                            userProfile = currentUserProfile!!,
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
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = RichPastelCoral)
                        }
                    }
                }
            }
        }
    }

    if (isAskModalOpen) {
        AskPackScreen(onDismiss = { isAskModalOpen = false }, users = allUsers, currentUserProfile = currentUserProfile)
    }

    if (activeRequest != null) {
        val requestAuthor = allUsers.find { it.uid == activeRequest!!.userId }
            ?: UserProfile(uid = activeRequest!!.userId, name = "Member")
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(5f),
            color = SoftPastelMint
        ) {
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(6f),
            color = SoftPastelMint
        ) {
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
        val collectionPosts = realPosts.filter { activeCollection!!.postIds.contains(it.id) }
        CollectionDetailScreen(
            collection = activeCollection!!,
            posts = collectionPosts,
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
                userPosts = realPosts.filter { it.userId == viewUserProfileId },
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
        val detailPost = realPosts.find { it.id == openPostId }
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
            ?: feedActiveOffers.find { it.id == id }
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