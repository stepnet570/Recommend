package com.example.recommend.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recommend.data.model.AdOffer
import com.example.recommend.data.model.PackRequest
import com.example.recommend.data.model.Post
import com.example.recommend.data.model.PostCollection
import com.example.recommend.data.model.UserProfile
import com.example.recommend.data.repository.CollectionRepository
import com.example.recommend.data.repository.OfferRepository
import com.example.recommend.data.repository.PostRepository
import com.example.recommend.data.repository.RequestRepository
import com.example.recommend.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val postRepo = PostRepository(db)
    private val userRepo = UserRepository(db)
    private val requestRepo = RequestRepository(db)
    private val offerRepo = OfferRepository(db)
    private val collectionRepo = CollectionRepository(db)

    private val currentUid: String? get() = auth.currentUser?.uid

    // --- raw backing state ---
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _allRequests = MutableStateFlow<List<PackRequest>>(emptyList())

    private val _collections = MutableStateFlow<List<PostCollection>>(emptyList())

    private val _activeOffers = MutableStateFlow<List<AdOffer>>(emptyList())
    val activeOffers: StateFlow<List<AdOffer>> = _activeOffers.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- derived state ---

    /** Requests where the current user is the author or a selected recipient. */
    val feedRequests: StateFlow<List<PackRequest>> =
        combine(_allRequests, _posts) { requests, posts ->
            val uid = currentUid ?: return@combine emptyList()
            requests
                .filter { it.userId == uid || it.selectedUsers.contains(uid) }
                .map { req ->
                    req.copy(answersCount = posts.count { it.replyToRequestId == req.id })
                }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Post IDs the current user has saved into any collection. */
    val savedPostIds: StateFlow<Set<String>> = _collections
        .map { collections -> collections.flatMap { it.postIds }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Posts that are not replies to a pack signal — shown on the home feed. */
    val feedPostsForHome: StateFlow<List<Post>> = _posts
        .map { posts -> posts.filter { it.replyToRequestId.isNullOrBlank() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Active offers from other businesses — shown in the feed carousel. */
    val feedOffersForHome: StateFlow<List<AdOffer>> = _activeOffers
        .map { offers ->
            val uid = currentUid
            if (uid.isNullOrBlank()) offers else offers.filter { it.businessId != uid }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        val uid = currentUid
        if (uid != null) {
            viewModelScope.launch {
                postRepo.getPostsStream().collect {
                    _posts.value = it
                    _isLoading.value = false
                }
            }
            viewModelScope.launch { userRepo.getUserStream(uid).collect { _currentUser.value = it } }
            viewModelScope.launch { userRepo.getAllUsersStream().collect { _allUsers.value = it } }
            viewModelScope.launch { requestRepo.getRequestsStream().collect { _allRequests.value = it } }
            viewModelScope.launch { collectionRepo.getCollectionsStream(uid).collect { _collections.value = it } }
            viewModelScope.launch { offerRepo.getActiveOffers().collect { _activeOffers.value = it } }
        }
    }
}
