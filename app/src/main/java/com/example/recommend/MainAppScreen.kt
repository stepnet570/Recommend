package com.example.recommend

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.SurfaceMuted
import com.example.recommend.ui.theme.SurfacePastel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

private enum class BusinessAddDestination { Hub, Campaign, Post }

@Composable
fun MainAppScreen(onLogout: () -> Unit) {
    var currentTab by remember { mutableStateOf("feed") }

    var realPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var allRequests by remember { mutableStateOf<List<PackRequest>>(emptyList()) }
    var allAnswers by remember { mutableStateOf<List<Answer>>(emptyList()) }
    var allUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userCollections by remember { mutableStateOf<List<PostCollection>>(emptyList()) }
    var myOffers by remember { mutableStateOf<List<AdOffer>>(emptyList()) }
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
    var selectedOfferId by remember { mutableStateOf<String?>(null) }

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    LaunchedEffect(currentUser) {
        if (currentUser == null) return@LaunchedEffect

        // Email/password sign-up can miss the first Firestore write; Google users may get a doc from elsewhere.
        // This creates users/{uid} if missing so both flows match.
        db.ensureUserProfileForAuthUser(currentUser) { }

        // Posts
        db.trustListDataRoot()
            .collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    realPosts = snapshot.documents.mapNotNull { doc ->
                        val ratingsRaw = doc.get("ratings")
                        val ratingsByUser: Map<String, Int> = when (ratingsRaw) {
                            is Map<*, *> -> ratingsRaw.mapNotNull { (k, v) ->
                                val key = k as? String ?: return@mapNotNull null
                                val intVal = when (v) {
                                    is Long -> v.toInt()
                                    is Int -> v
                                    else -> null
                                } ?: return@mapNotNull null
                                key to intVal.coerceIn(1, 5)
                            }.toMap()
                            else -> emptyMap()
                        }
                        val likesRaw = doc.get("likes")
                        val likesByUser: Set<String> = when (likesRaw) {
                            is Map<*, *> -> likesRaw.mapNotNull { (k, v) ->
                                val key = k as? String ?: return@mapNotNull null
                                val on = when (v) {
                                    is Boolean -> v
                                    is Number -> v.toLong() != 0L
                                    else -> v != null
                                }
                                if (on) key else null
                            }.toSet()
                            else -> emptySet()
                        }
                        Post(
                            id = doc.id, userId = doc.getString("userId") ?: "", title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "", category = doc.getString("category") ?: "Food",
                            location = doc.getString("location") ?: "", rating = doc.getLong("rating")?.toInt() ?: 5,
                            imageUrl = doc.getString("imageUrl"), authorName = doc.getString("authorName") ?: "User",
                            authorHandle = doc.getString("authorHandle") ?: "@user",
                            isSponsored = doc.getBoolean("sponsored") == true,
                            ratingsByUser = ratingsByUser,
                            likesByUser = likesByUser
                        )
                    }
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

        // Answers
        db.trustListDataRoot()
            .collection("answers")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allAnswers = snapshot.documents.mapNotNull { doc ->
                        Answer(
                            id = doc.id, requestId = doc.getString("requestId") ?: "", userId = doc.getString("userId") ?: "",
                            text = doc.getString("text") ?: "", createdAt = doc.getLong("createdAt") ?: 0L
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

    val feedRequests = allRequests.filter { it.userId == currentUser?.uid || it.selectedUsers.contains(currentUser?.uid) }

    val myPosts = realPosts.filter { it.userId == currentUser?.uid }

    val savedPostIds = remember(userCollections) {
        userCollections.flatMap { it.postIds }.toSet()
    }

    LaunchedEffect(currentTab) {
        if (currentTab != "add") businessAddDestination = BusinessAddDestination.Hub
    }

    LaunchedEffect(myOffers, selectedOfferId) {
        val id = selectedOfferId ?: return@LaunchedEffect
        if (myOffers.none { it.id == id }) selectedOfferId = null
    }

    fun ratePost(postId: String, stars: Int) {
        val uid = currentUser?.uid ?: return
        db.trustListDataRoot()
            .collection("posts")
            .document(postId)
            .update("ratings.$uid", stars.coerceIn(1, 5))
    }

    fun toggleOfferPause(offer: AdOffer) {
        val next = if (offer.status.equals("active", ignoreCase = true)) "paused" else "active"
        db.trustListDataRoot()
            .collection("offers")
            .document(offer.id)
            .update("status", next)
    }

    fun toggleLike(postId: String) {
        val uid = currentUser?.uid ?: return
        val post = realPosts.find { it.id == postId } ?: return
        val ref = db.trustListDataRoot().collection("posts").document(postId)
        if (post.likesByUser.contains(uid)) {
            ref.update("likes.$uid", FieldValue.delete())
        } else {
            ref.update("likes.$uid", true)
        }
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
                    onClick = { currentTab = "add" },
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
                            posts = realPosts, requests = feedRequests, users = allUsers,
                            savedPostIds = savedPostIds,
                            onAskPackClick = { isAskModalOpen = true },
                            onRequestClick = { selectedRequest -> activeRequest = selectedRequest },
                            onSaveClick = { postToSave = it },
                            onUserProfileClick = { viewUserProfileId = it },
                            onRequestAuthorProfileClick = { viewUserProfileId = it },
                            viewerUid = currentUser?.uid,
                            onAudienceRate = { postId, stars -> ratePost(postId, stars) },
                            onOpenPost = { openPostId = it },
                            onLikeToggle = { toggleLike(it) }
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
                                    businessAddDestination = BusinessAddDestination.Hub
                                    currentTab = "feed"
                                },
                                currentUserProfile = currentUserProfile,
                                onBack = { businessAddDestination = BusinessAddDestination.Hub }
                            )
                        }
                    } else {
                        AddScreen(
                            onPostAdded = { currentTab = "feed" },
                            currentUserProfile = currentUserProfile
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
        val requestAnswers = allAnswers.filter { it.requestId == activeRequest!!.id }
        RequestDetailScreen(
            request = activeRequest!!,
            answers = requestAnswers,
            users = allUsers,
            onBack = { activeRequest = null },
            onUserProfileClick = { viewUserProfileId = it }
        )
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
            onOpenPost = { openPostId = it },
            onLikeToggle = { toggleLike(it) }
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
                onPostClick = { openPostId = it }
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
                    onAudienceRate = { postId, stars -> ratePost(postId, stars) },
                    onLikeToggle = { toggleLike(it) }
                )
            }
        }
    }

    val offerForDetail = selectedOfferId?.let { id -> myOffers.find { it.id == id } }
    if (offerForDetail != null) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            BusinessOfferDetailScreen(
                offer = offerForDetail,
                onBack = { selectedOfferId = null },
                onPauseToggle = { toggleOfferPause(it) }
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