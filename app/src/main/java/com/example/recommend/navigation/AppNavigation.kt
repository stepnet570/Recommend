package com.example.recommend.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.*
import androidx.compose.runtime.SideEffect
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
import com.example.recommend.data.repository.CollectionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

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
    isAskModalOpen: Boolean = false,
    onAskModalOpenChange: (Boolean) -> Unit = {},
    onRegisterReset: (() -> Unit) -> Unit = {},
    onCreationFlowActive: (Boolean) -> Unit = {},
    onLogout: () -> Unit,
    isLoadingMore: Boolean = false,
    canLoadMore: Boolean = true,
    onLoadMore: () -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val collectionRepo = remember { CollectionRepository(db) }
    val coroutineScope = rememberCoroutineScope()

    // --- overlay state ---
    var activeRequest by remember { mutableStateOf<PackRequest?>(null) }
    var postToSave by remember { mutableStateOf<String?>(null) }
    // Стек открытых коллекций — позволяет заходить в подколлекцию и возвращаться к родителю по back.
    var collectionStack by remember { mutableStateOf<List<PostCollection>>(emptyList()) }
    val activeCollection: PostCollection? = collectionStack.lastOrNull()
    // Диалог создания коллекции/подколлекции.
    // Если parentIdForNewCollection != null → создаём подколлекцию,
    // если parentIdForNewCollection == null и creatingCollection == true → root.
    var creatingCollection by remember { mutableStateOf(false) }
    var parentIdForNewCollection by remember { mutableStateOf<String?>(null) }
    var parentNameForNewCollection by remember { mutableStateOf<String?>(null) }
    // Если задано — следующий пост, созданный в AddScreen, попадёт в эту коллекцию автоматически
    var targetCollectionIdForAdd by remember { mutableStateOf<String?>(null) }
    var businessAddDestination by remember { mutableStateOf(BusinessAddDestination.Hub) }
    var viewUserProfileId by remember { mutableStateOf<String?>(null) }
    var openPostId by remember { mutableStateOf<String?>(null) }
    var createCampaignOverlay by remember { mutableStateOf(false) }
    var linkedRequestIdForAdd by remember { mutableStateOf<String?>(null) }
    var linkedOfferIdForAdd by remember { mutableStateOf<String?>(null) }
    var linkedOfferTitleForAdd by remember { mutableStateOf<String?>(null) }
    var activeOffer by remember { mutableStateOf<AdOffer?>(null) }
    var addPickForRequestId by remember { mutableStateOf<String?>(null) }
    var selectedOfferId by remember { mutableStateOf<String?>(null) }
    var profileOfferCache by remember { mutableStateOf<AdOffer?>(null) }
    var profileSurfaceOrdinal by rememberSaveable { mutableIntStateOf(0) }
    var viewedUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userInAddForm by remember { mutableStateOf(false) }
    // Reset sub-destinations when leaving add tab
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(currentRoute) {
        if (currentRoute != "add") {
            businessAddDestination = BusinessAddDestination.Hub
            userInAddForm = false
        }
    }

    // Сообщаем родителю: пользователь реально в процессе создания (не на экране выбора Hub)
    LaunchedEffect(currentRoute, businessAddDestination, userInAddForm) {
        val inFlow = when {
            currentRoute != "add" -> false
            currentUserProfile?.isBusiness == true -> businessAddDestination != BusinessAddDestination.Hub
            else -> userInAddForm  // обычный юзер — только когда реально в форме (не Hub)
        }
        onCreationFlowActive(inFlow)
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

    // Регистрируем функцию сброса оверлеев в родителе (синхронный вызов без LaunchedEffect)
    SideEffect {
        onRegisterReset {
            activeRequest = null
            postToSave = null
            collectionStack = emptyList()
            creatingCollection = false
            parentIdForNewCollection = null
            parentNameForNewCollection = null
            viewUserProfileId = null
            openPostId = null
            selectedOfferId = null
            profileOfferCache = null
            addPickForRequestId = null
            activeOffer = null
            createCampaignOverlay = false
            linkedOfferIdForAdd = null
            linkedOfferTitleForAdd = null
            targetCollectionIdForAdd = null
            onAskModalOpenChange(false)
        }
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
        NavHost(
            navController = navController,
            startDestination = "feed",
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {

            composable("feed") {
                val acceptedOfferIds = remember(feedOffersForHome, currentUser?.uid) {
                    val uid = currentUser?.uid ?: return@remember emptySet()
                    feedOffersForHome.filter { uid in it.acceptedBy }.map { it.id }.toSet()
                }
                FeedScreen(
                    posts = feedPostsForHome,
                    requests = feedRequests,
                    users = allUsers,
                    followingUserIds = currentUserProfile?.following?.toSet() ?: emptySet(),
                    savedPostIds = savedPostIds,
                    activeOffers = feedOffersForHome,
                    acceptedOfferIds = acceptedOfferIds,
                    trustCoins = currentUserProfile?.trustCoins ?: 0,
                    onOfferClick = { offer -> activeOffer = offer },
                    onUserClick = { uid -> viewUserProfileId = uid },
                    onAskPackClick = { onAskModalOpenChange(true) },
                    onRequestClick = { selectedRequest -> activeRequest = selectedRequest },
                    onSignalRequestOpen = { activeRequest = it },
                    onHelpRequest = { req -> addPickForRequestId = req.id },
                    onSaveClick = { postToSave = it },
                    onUserProfileClick = { viewUserProfileId = it },
                    onRequestAuthorProfileClick = { viewUserProfileId = it },
                    viewerUid = currentUser?.uid,
                    onAudienceRate = { postId, stars -> ratePost(postId, stars) },
                    onOpenPost = { openPostId = it },
                    onWalletClick = { navController.navigate("profile") },
                    isLoadingMore = isLoadingMore,
                    canLoadMore = canLoadMore,
                    onLoadMore = onLoadMore
                )
            }

            composable("explore") {
                ExploreScreen(onUserProfileClick = { viewUserProfileId = it })
            }

            composable("add") {
                if (currentUserProfile?.isBusiness == true) {
                    when (businessAddDestination) {
                        BusinessAddDestination.Hub -> AddHubScreen(
                            isBusiness = true,
                            campaignBalance = currentUserProfile?.trustCoins ?: 0,
                            onCampaign = { businessAddDestination = BusinessAddDestination.Campaign },
                            onPost = { businessAddDestination = BusinessAddDestination.Post },
                            onAskPack = { onAskModalOpenChange(true) }
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
                                linkedOfferIdForAdd = null
                                linkedOfferTitleForAdd = null
                                // если пост был адресован в коллекцию — возвращаем пользователя в неё
                                val backToCollectionId = targetCollectionIdForAdd
                                targetCollectionIdForAdd = null
                                businessAddDestination = BusinessAddDestination.Hub
                                if (backToCollectionId != null) {
                                    navController.navigate("profile") { launchSingleTop = true }
                                    val target = userCollections.find { it.id == backToCollectionId }
                                    if (target != null) collectionStack = listOf(target)
                                } else {
                                    navController.navigate("feed")
                                }
                            },
                            currentUserProfile = currentUserProfile,
                            onBack = { businessAddDestination = BusinessAddDestination.Hub },
                            requestId = linkedRequestIdForAdd,
                            offerId = linkedOfferIdForAdd,
                            offerTitle = linkedOfferTitleForAdd,
                            isSponsored = linkedOfferIdForAdd != null,
                            targetCollectionId = targetCollectionIdForAdd
                        )
                    }
                } else {
                    if (userInAddForm) {
                        AddScreen(
                            onPostAdded = {
                                linkedRequestIdForAdd = null
                                linkedOfferIdForAdd = null
                                linkedOfferTitleForAdd = null
                                val backToCollectionId = targetCollectionIdForAdd
                                targetCollectionIdForAdd = null
                                userInAddForm = false
                                if (backToCollectionId != null) {
                                    navController.navigate("profile") { launchSingleTop = true }
                                    val target = userCollections.find { it.id == backToCollectionId }
                                    if (target != null) collectionStack = listOf(target)
                                } else {
                                    navController.navigate("feed")
                                }
                            },
                            currentUserProfile = currentUserProfile,
                            onBack = { userInAddForm = false },
                            requestId = linkedRequestIdForAdd,
                            offerId = linkedOfferIdForAdd,
                            offerTitle = linkedOfferTitleForAdd,
                            isSponsored = linkedOfferIdForAdd != null,
                            targetCollectionId = targetCollectionIdForAdd
                        )
                    } else {
                        AddHubScreen(
                            isBusiness = false,
                            onPost = { userInAddForm = true },
                            onAskPack = { onAskModalOpenChange(true) }
                        )
                    }
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
                        onCollectionClick = { collectionStack = listOf(it) },
                        onCreateCollection = {
                            parentIdForNewCollection = null
                            parentNameForNewCollection = null
                            creatingCollection = true
                        },
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
                onDismiss = { onAskModalOpenChange(false) },
                onCreated = {
                    onAskModalOpenChange(false)
                    navController.navigate("feed") { launchSingleTop = true }
                },
                users = allUsers,
                currentUserProfile = currentUserProfile
            )
        }

        if (activeRequest != null) {
            val requestAuthor = allUsers.find { it.uid == activeRequest!!.userId }
                ?: UserProfile(uid = activeRequest!!.userId, name = "Member")
            Surface(modifier = Modifier.fillMaxSize().zIndex(5f), color = Color.White) {
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
            // Если коллекция была обновлена в реалтайме — берём свежую версию из стрима
            val liveCollection = userCollections.find { it.id == activeCollection.id } ?: activeCollection
            val isOwner = liveCollection.userId == currentUser?.uid
            val children = userCollections.filter { it.parentId == liveCollection.id }
            CollectionDetailScreen(
                collection = liveCollection,
                posts = posts.filter { liveCollection.postIds.contains(it.id) },
                savedPostIds = savedPostIds,
                users = allUsers,
                subCollections = children,
                canEdit = isOwner,
                // back: pop top of stack — окажемся либо в родителе, либо вернёмся в профиль
                onBack = { collectionStack = collectionStack.dropLast(1) },
                onSaveClick = { postToSave = it },
                onUserProfileClick = { viewUserProfileId = it },
                viewerUid = currentUser?.uid,
                onAudienceRate = { postId, stars -> ratePost(postId, stars) },
                onOpenPost = { openPostId = it },
                onCreateSubCollection = {
                    parentIdForNewCollection = liveCollection.id
                    parentNameForNewCollection = liveCollection.name
                    creatingCollection = true
                },
                // push: добавляем подколлекцию поверх стека
                onSubCollectionClick = { sub -> collectionStack = collectionStack + sub },
                onAddPost = {
                    // Создание нового поста сразу с привязкой к коллекции.
                    // ВАЖНО: закрываем оверлей коллекции, иначе AddScreen открывается ПОД ним
                    // (CollectionDetailScreen рендерится поверх NavHost). Стек восстановится
                    // в onPostAdded после публикации — вернём пользователя ровно в эту коллекцию.
                    targetCollectionIdForAdd = liveCollection.id
                    collectionStack = emptyList()
                    if (currentUserProfile?.isBusiness == true) {
                        businessAddDestination = BusinessAddDestination.Post
                    } else {
                        userInAddForm = true
                    }
                    navController.navigate("add") { launchSingleTop = true }
                },
                onRename = { newName ->
                    coroutineScope.launch {
                        runCatching { collectionRepo.renameCollection(liveCollection.id, newName) }
                    }
                },
                onDelete = {
                    val deletedId = liveCollection.id
                    coroutineScope.launch {
                        runCatching { collectionRepo.deleteCollection(deletedId) }
                    }
                    // Закрываем удалённую коллекцию (popим из стека)
                    collectionStack = collectionStack.dropLast(1)
                }
            )
        }

        // Диалог создания коллекции / подколлекции
        if (creatingCollection && currentUser != null) {
            CreateCollectionDialog(
                parentId = parentIdForNewCollection,
                parentName = parentNameForNewCollection,
                onDismiss = {
                    creatingCollection = false
                    parentIdForNewCollection = null
                    parentNameForNewCollection = null
                },
                onCreated = { /* стрим Firestore сам обновит UI */ }
            )
        }

        if (viewUserProfileId != null && currentUser != null) {
            BackHandler { viewUserProfileId = null }
            Surface(modifier = Modifier.fillMaxSize().zIndex(7f), color = Color.White) {
                PublicUserProfileScreen(
                    userId = viewUserProfileId!!,
                    viewerUid = currentUser.uid,
                    viewerProfile = currentUserProfile,
                    allUsers = allUsers,
                    userPosts = posts.filter { it.userId == viewUserProfileId },
                    onBack = { viewUserProfileId = null },
                    onPostClick = { openPostId = it },
                    onCollectionClick = { collectionStack = listOf(it) },
                    onOfferClick = { offer ->
                        profileOfferCache = offer
                        selectedOfferId = offer.id
                    }
                )
            }
        }

        if (openPostId != null && currentUser != null) {
            BackHandler { openPostId = null }
            val detailPost = posts.find { it.id == openPostId }
            if (detailPost != null) {
                Surface(modifier = Modifier.fillMaxSize().zIndex(7f), color = Color.White) {
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
            BackHandler {
                selectedOfferId = null
                profileOfferCache = null
            }
            Surface(modifier = Modifier.fillMaxSize().zIndex(7f), color = Color.White) {
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
            Surface(modifier = Modifier.fillMaxSize().zIndex(8f), color = Color.White) {
                CreateOfferScreen(
                    userProfile = businessProfileForCampaign,
                    onOfferCreated = { createCampaignOverlay = false },
                    onBack = { createCampaignOverlay = false }
                )
            }
        }

        if (viewedUserProfile != null) {
            Box(modifier = Modifier.fillMaxSize().zIndex(10f)) {
                UserProfileBottomSheet(
                    userProfile = viewedUserProfile!!,
                    onDismiss = { viewedUserProfile = null }
                )
            }
        }

        if (activeOffer != null && currentUser != null) {
            AcceptOfferSheet(
                offer = activeOffer!!,
                viewerUid = currentUser.uid,
                isAccepted = currentUser.uid in activeOffer!!.acceptedBy,
                onDismiss = { activeOffer = null },
                onAccepted = { offerId, offerTitle, _ ->
                    linkedOfferIdForAdd = offerId
                    linkedOfferTitleForAdd = offerTitle
                    activeOffer = null
                    // Skip Hub in both cases — go straight to the post form
                    if (currentUserProfile?.isBusiness == true) {
                        businessAddDestination = BusinessAddDestination.Post
                    } else {
                        userInAddForm = true
                    }
                    navController.navigate("add") { launchSingleTop = true }
                }
            )
        }
    }
}
