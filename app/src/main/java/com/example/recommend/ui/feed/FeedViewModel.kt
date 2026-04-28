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

    /** Pages loaded via "load more" (older than the real-time first page). */
    private val _olderPosts = MutableStateFlow<List<Post>>(emptyList())

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

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

    /**
     * Feed posts:
     * — real-time first page (_posts) + paginated older posts (_olderPosts), deduplicated
     * — only from following + own posts (cold-start fallback: show all)
     * — no replies to requests (replyToRequestId must be blank)
     */
    val feedPostsForHome: StateFlow<List<Post>> =
        combine(_posts, _olderPosts, _currentUser) { live, older, currentUser ->
            // Merge: live page takes precedence; older pages fill in the rest
            val liveIds = live.map { it.id }.toSet()
            val allPosts = live + older.filter { it.id !in liveIds }

            val uid = currentUser?.uid
            val following = currentUser?.following?.toSet() ?: emptySet()
            val filtered = allPosts.filter { post ->
                post.replyToRequestId.isNullOrBlank() &&
                (post.userId == uid || following.contains(post.userId))
            }
            // Cold start: if not following anyone yet — show everyone
            if (following.isEmpty()) {
                allPosts.filter { it.replyToRequestId.isNullOrBlank() }
            } else {
                filtered
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Active offers shown in the feed carousel.
     * Filtered by two criteria only:
     *  1. minTrustScore — user must meet the offer's minimum trust score
     *  2. own campaigns excluded — don't show a business its own offers
     * No isBusiness gate — all account types see qualifying offers.
     */
    val feedOffersForHome: StateFlow<List<AdOffer>> = combine(_activeOffers, _currentUser) { offers, currentUser ->
        val uid = currentUser?.uid
        val userTrustScore = currentUser?.trustScore ?: 0.0
        offers.filter {
            (uid == null || it.businessId != uid) &&
            it.minTrustScore <= userTrustScore
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Load the next page of posts older than the oldest post currently loaded. */
    fun loadMorePosts() {
        if (_isLoadingMore.value || !_canLoadMore.value) return
        val oldestTimestamp = (_olderPosts.value.lastOrNull() ?: _posts.value.lastOrNull())
            ?.createdAt ?: return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val newPosts = postRepo.loadMorePosts(oldestTimestamp)
                if (newPosts.isEmpty() || newPosts.size < PostRepository.PAGE_SIZE.toInt()) {
                    _canLoadMore.value = false
                }
                if (newPosts.isNotEmpty()) {
                    val existingIds = (_posts.value + _olderPosts.value).map { it.id }.toSet()
                    _olderPosts.value = _olderPosts.value + newPosts.filter { it.id !in existingIds }
                }
            } catch (_: Exception) {
                // Network error — silently ignore; user can scroll back up and retry
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

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
