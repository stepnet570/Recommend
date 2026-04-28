package com.example.recommend
import com.example.recommend.ui.feed.*
import com.example.recommend.ui.profile.*

import com.example.recommend.data.model.*
import com.example.recommend.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

@Composable
fun PublicUserProfileScreen(
    userId: String,
    viewerUid: String,
    viewerProfile: UserProfile?,
    allUsers: List<UserProfile>,
    userPosts: List<Post>,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit = {},
    onCollectionClick: (PostCollection) -> Unit = {},
    onOfferClick: (AdOffer) -> Unit = {}
) {
    val profile = remember(userId, allUsers) { allUsers.find { it.uid == userId } }
    val db = FirebaseFirestore.getInstance()

    var theirCollections by remember { mutableStateOf<List<PostCollection>>(emptyList()) }
    var theirOffers by remember { mutableStateOf<List<AdOffer>>(emptyList()) }
    var followersCount by remember { mutableStateOf(0) }

    val isOwnProfile = userId == viewerUid
    val isFollowing = viewerProfile?.following?.contains(userId) == true

    var selectedTab by rememberSaveable(userId) { mutableIntStateOf(0) }

    val trustScore = remember(profile?.trustScore) {
        ((profile?.trustScore ?: 0.0) * 2.0).coerceIn(0.0, 10.0).toFloat()
    }

    DisposableEffect(userId) {
        val regs = mutableListOf<ListenerRegistration>()
        regs += db.trustListDataRoot()
            .collection("collections")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    theirCollections = snapshot.documents.mapNotNull { doc ->
                        PostCollection(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            name = doc.getString("name") ?: "",
                            postIds = (doc.get("postIds") as? List<*>)?.mapNotNull { it as? String }
                                ?: emptyList(),
                            parentId = doc.getString("parentId"),
                            coverPostId = doc.getString("coverPostId"),
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                }
            }
        regs += db.trustListDataRoot()
            .collection("offers")
            .whereEqualTo("businessId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    theirOffers = snapshot.documents
                        .mapNotNull { it.toAdOfferOrNull() }
                        .sortedByDescending { it.createdAt }
                }
            }
        onDispose { regs.forEach { it.remove() } }
    }

    LaunchedEffect(userId) {
        db.trustListDataRoot()
            .collection("users")
            .whereArrayContains("following", userId)
            .get()
            .addOnSuccessListener { followersCount = it.size() }
    }

    fun toggleFollow() {
        if (viewerProfile == null || userId.isBlank() || isOwnProfile) return
        val ref = db.trustListDataRoot().collection("users").document(viewerUid)
        if (isFollowing) {
            ref.update("following", FieldValue.arrayRemove(userId))
        } else {
            ref.update("following", FieldValue.arrayUnion(userId))
        }
    }

    if (profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppTeal)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {

        // ── Cover banner + avatar + follow button ─────────────────────────────
        item {
            Box(modifier = Modifier.fillMaxWidth()) {

                // Gradient banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(PrimaryGradientLinear)
                )

                // Back button — top-left, on gradient
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Avatar — bottom-left, overlapping banner by 36dp
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .align(Alignment.BottomStart)
                        .offset(y = 36.dp)
                        .size(72.dp)
                        .background(AppWhite, CircleShape)
                        .border(3.dp, AppWhite, CircleShape)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = profile.avatar.ifEmpty {
                            "https://api.dicebear.com/7.x/avataaars/svg?seed=${profile.uid}"
                        },
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                // Follow / Unfollow button — bottom-right, overlapping banner by 20dp
                if (!isOwnProfile && viewerProfile != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp)
                            .offset(y = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isFollowing) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.5.dp, Color(0xFFE0DDD8), RoundedCornerShape(14.dp))
                                    .background(AppWhite, RoundedCornerShape(14.dp))
                                    .clickable { toggleFollow() }
                                    .padding(horizontal = 18.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    "Following",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = AppDark
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PrimaryGradientLinear)
                                    .clickable { toggleFollow() }
                                    .padding(horizontal = 18.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    "Follow",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = AppWhite
                                )
                            }
                        }
                    }
                }
            }

            // Space to clear avatar overlap
            Spacer(Modifier.height(48.dp))
        }

        // ── Name / handle / bio ───────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Авто-уменьшение шрифта при длинном имени, max 2/3 ширины
                var nameFontSize by remember(profile.name) { mutableStateOf(18.sp) }
                Text(
                    text = profile.name.ifBlank { "User" },
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = nameFontSize,
                    color = AppDark,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth(2f / 3f),
                    onTextLayout = { result ->
                        if (result.hasVisualOverflow && nameFontSize > 13.sp) {
                            nameFontSize = (nameFontSize.value * 0.85f).sp
                        }
                    }
                )
                if (profile.handle.isNotBlank()) {
                    Text(
                        text = profile.handle,
                        fontFamily = BodyFontFamily,
                        fontSize = 13.sp,
                        color = AppMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                val bio = profile.bio.ifBlank { "" }
                if (bio.isNotBlank()) {
                    Text(
                        text = bio,
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = AppDark.copy(alpha = 0.75f),
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // ── Stats: Picks | Followers | TrustScore ring ────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Picks
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${userPosts.size}",
                        fontFamily = HeadingFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = AppDark
                    )
                    Text(
                        text = "Picks",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AppMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(SurfaceMuted)
                )

                // Followers
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$followersCount",
                        fontFamily = HeadingFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = AppDark
                    )
                    Text(
                        text = "Followers",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AppMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(SurfaceMuted)
                )

                // TrustScore ring
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TrustScoreRing(
                        score = trustScore,
                        size = 40.dp,
                        strokeWidth = 3.5.dp
                    )
                    Text(
                        text = "TrustScore",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AppMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // ── Tabs + Picks / Collections / Offers grid ──────────────────────────
        personalRecsAndCollectionsSection(
            selectedTab = selectedTab,
            onSelectedTab = { selectedTab = it },
            myPosts = userPosts,
            collections = theirCollections,
            myOffers = theirOffers,
            isBusiness = profile.isBusiness,
            onPostClick = onPostClick,
            onCollectionClick = onCollectionClick,
            onCreateCampaign = {},
            onOfferClick = onOfferClick,
            onOfferPauseToggle = {},
            recsTabLabel = "Picks",
            collectionsTabLabel = "Collections",
            isViewerOwner = false
        )
    }
}
