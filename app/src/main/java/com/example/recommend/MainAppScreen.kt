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
    var isLoading by remember { mutableStateOf(true) }

    var isAskModalOpen by remember { mutableStateOf(false) }
    var activeRequest by remember { mutableStateOf<PackRequest?>(null) }
    var postToSave by remember { mutableStateOf<String?>(null) }
    var activeCollection by remember { mutableStateOf<PostCollection?>(null) }
    var businessAddDestination by remember { mutableStateOf(BusinessAddDestination.Hub) }

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
                        Post(
                            id = doc.id, userId = doc.getString("userId") ?: "", title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "", category = doc.getString("category") ?: "Food",
                            location = doc.getString("location") ?: "", rating = doc.getLong("rating")?.toInt() ?: 5,
                            imageUrl = doc.getString("imageUrl"), authorName = doc.getString("authorName") ?: "User",
                            authorHandle = doc.getString("authorHandle") ?: "@user",
                            isSponsored = doc.getBoolean("sponsored") == true
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

        // Business ad offers (campaigns)
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
    }

    val feedRequests = allRequests.filter { it.userId == currentUser?.uid || it.selectedUsers.contains(currentUser?.uid) }

    val myPosts = realPosts.filter { it.userId == currentUser?.uid }

    val savedPostIds = remember(userCollections) {
        userCollections.flatMap { it.postIds }.toSet()
    }

    LaunchedEffect(currentTab) {
        if (currentTab != "add") businessAddDestination = BusinessAddDestination.Hub
    }

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
                            onSaveClick = { postToSave = it }
                        )
                    }
                }
                "explore" -> ExploreScreen()
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
                            onCollectionClick = { activeCollection = it },
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
            onBack = { activeRequest = null }
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
            onBack = { activeCollection = null },
            onSaveClick = { postToSave = it }
        )
    }
}