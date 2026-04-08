package com.example.recommend.ui.profile

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

    init {
        val uid = currentUid
        if (uid != null) {
            viewModelScope.launch { userRepo.getUserStream(uid).collect { _userProfile.value = it } }
            viewModelScope.launch { postRepo.getPostsStream().collect { _allPosts.value = it } }
            viewModelScope.launch { collectionRepo.getCollectionsStream(uid).collect { _userCollections.value = it } }
            viewModelScope.launch { offerRepo.getOffersForBusiness(uid).collect { _myOffers.value = it } }
            viewModelScope.launch { userRepo.getFollowersCountStream(uid).collect { _followersCount.value = it } }
            viewModelScope.launch { offerRepo.getParticipatingOffersCountStream(uid).collect { _participatingPromoCampaignsCount.value = it } }
        }
    }
}
