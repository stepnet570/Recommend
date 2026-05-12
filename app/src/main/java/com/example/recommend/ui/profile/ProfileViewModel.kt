package com.example.recommend.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recommend.data.model.AdOffer
import com.example.recommend.data.model.Post
import com.example.recommend.data.model.PostCollection
import com.example.recommend.data.model.UserProfile
import com.example.recommend.data.repository.CollectionRepository
import com.example.recommend.data.repository.OfferRepository
import com.example.recommend.data.repository.PostRepository
import com.example.recommend.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val postRepo = PostRepository(db)
    private val userRepo = UserRepository(db)
    private val offerRepo = OfferRepository(db)
    private val collectionRepo = CollectionRepository(db)

    private val currentUid: String? get() = auth.currentUser?.uid

    // --- raw backing state ---
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())

    private val _userCollections = MutableStateFlow<List<PostCollection>>(emptyList())
    val userCollections: StateFlow<List<PostCollection>> = _userCollections.asStateFlow()

    // Пагинация коллекций. Растим limit вручную через [loadMoreCollections].
    private val _collectionsLimit = MutableStateFlow(COLLECTIONS_PAGE_SIZE)
    val collectionsLimit: StateFlow<Int> = _collectionsLimit.asStateFlow()

    /** True если на сервере может быть больше коллекций, чем загружено. */
    private val _hasMoreCollections = MutableStateFlow(false)
    val hasMoreCollections: StateFlow<Boolean> = _hasMoreCollections.asStateFlow()

    private var collectionsJob: Job? = null

    private val _myOffers = MutableStateFlow<List<AdOffer>>(emptyList())
    val myOffers: StateFlow<List<AdOffer>> = _myOffers.asStateFlow()

    private val _followersCount = MutableStateFlow(0)
    val followersCount: StateFlow<Int> = _followersCount.asStateFlow()

    private val _participatingPromoCampaignsCount = MutableStateFlow(0)
    val participatingPromoCampaignsCount: StateFlow<Int> = _participatingPromoCampaignsCount.asStateFlow()

    // --- derived state ---

    /** Posts authored by the current user. */
    val myPosts: StateFlow<List<Post>> = _allPosts
        .map { posts ->
            val uid = currentUid ?: return@map emptyList()
            posts.filter { it.userId == uid }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Jobs scoped to the currently authenticated user. */
    private val perUserJobs = mutableListOf<Job>()
    private var lastBoundUid: String? = null

    private val authListener = FirebaseAuth.AuthStateListener { fb ->
        val newUid = fb.currentUser?.uid
        if (newUid == lastBoundUid) return@AuthStateListener
        Log.i("ProfileViewModel", "Auth state changed: $lastBoundUid -> $newUid, rebinding streams")
        lastBoundUid = newUid
        perUserJobs.forEach { it.cancel() }
        perUserJobs.clear()
        collectionsJob?.cancel()
        collectionsJob = null
        // Reset state so the UI doesn't briefly show data from the old account
        _userProfile.value = null
        _allPosts.value = emptyList()
        _userCollections.value = emptyList()
        _myOffers.value = emptyList()
        _followersCount.value = 0
        _participatingPromoCampaignsCount.value = 0
        _hasMoreCollections.value = false
        _collectionsLimit.value = COLLECTIONS_PAGE_SIZE
        if (newUid != null) {
            bindStreamsForUser(newUid)
        }
    }

    private fun bindStreamsForUser(uid: String) {
        perUserJobs += viewModelScope.launch { userRepo.getUserStream(uid).collect { _userProfile.value = it } }
        perUserJobs += viewModelScope.launch { postRepo.getPostsStream().collect { _allPosts.value = it } }
        subscribeCollections(uid, _collectionsLimit.value.toLong())
        perUserJobs += viewModelScope.launch { offerRepo.getOffersForBusiness(uid).collect { _myOffers.value = it } }
        perUserJobs += viewModelScope.launch { userRepo.getFollowersCountStream(uid).collect { _followersCount.value = it } }
        perUserJobs += viewModelScope.launch { offerRepo.getParticipatingOffersCountStream(uid).collect { _participatingPromoCampaignsCount.value = it } }
    }

    init {
        val uid = currentUid
        if (uid != null) {
            lastBoundUid = uid
            bindStreamsForUser(uid)
        }
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    private fun subscribeCollections(uid: String, limit: Long) {
        collectionsJob?.cancel()
        collectionsJob = viewModelScope.launch {
            collectionRepo.getCollectionsStreamLimited(uid, limit).collect { list ->
                _userCollections.value = list
                // Если получили ровно лимит — возможно есть ещё (включаем "Load more").
                _hasMoreCollections.value = list.size.toLong() >= limit
            }
        }
    }

    /** Грузим следующую "страницу" — увеличиваем лимит и пересоздаём подписку. */
    fun loadMoreCollections() {
        val uid = currentUid ?: return
        val newLimit = _collectionsLimit.value + COLLECTIONS_PAGE_SIZE
        _collectionsLimit.value = newLimit
        subscribeCollections(uid, newLimit.toLong())
    }

    companion object {
        const val COLLECTIONS_PAGE_SIZE = 50
    }
}
