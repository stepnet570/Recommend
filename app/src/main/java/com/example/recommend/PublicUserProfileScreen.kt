package com.example.recommend

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.ConvexCardBox
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.GradientMid
import com.example.recommend.ui.theme.GradientTop
import com.example.recommend.ui.theme.MutedPastelGold
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.SurfaceMuted
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

private enum class PublicProfileSurface {
    PERSONAL,
    ADS_CAMPAIGNS
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var theirCollections by remember { mutableStateOf<List<PostCollection>>(emptyList()) }
    var theirOffers by remember { mutableStateOf<List<AdOffer>>(emptyList()) }
    var followersCount by remember { mutableStateOf(0) }
    var promoterCampaignsCount by remember { mutableStateOf(0) }

    val isOwnProfile = userId == viewerUid
    val isFollowing = viewerProfile?.following?.contains(userId) == true

    var profileSurfaceOrdinal by rememberSaveable(userId) { mutableIntStateOf(0) }
    var selectedTab by rememberSaveable(userId) { mutableIntStateOf(0) }
    val profileSurface = if (profileSurfaceOrdinal == 0) PublicProfileSurface.PERSONAL else PublicProfileSurface.ADS_CAMPAIGNS

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
                            postIds = (doc.get("postIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
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
        db.trustListDataRoot()
            .collection("offers")
            .whereArrayContains("promoterUserIds", userId)
            .get()
            .addOnSuccessListener { promoterCampaignsCount = it.size() }
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

    fun submitUserTrustRating(stars: Int) {
        if (viewerProfile == null || userId.isBlank() || isOwnProfile) return
        val raterUid = viewerUid
        val targetRef = db.trustListDataRoot().collection("users").document(userId)
        val score = stars.coerceIn(1, 5)
        db.runTransaction { transaction ->
            val snap = transaction.get(targetRef)
            val raw = snap.get("trustRatings")
            val existing = when (raw) {
                is Map<*, *> -> raw.mapNotNull { (k, v) ->
                    val key = k as? String ?: return@mapNotNull null
                    val intVal = when (v) {
                        is Long -> v.toInt()
                        is Int -> v
                        else -> null
                    } ?: return@mapNotNull null
                    key to intVal.coerceIn(1, 5)
                }.toMap().toMutableMap()
                else -> mutableMapOf()
            }
            existing[raterUid] = score
            val newAvg = existing.values.map { it.toDouble() }.average()
            transaction.update(
                targetRef,
                mapOf(
                    "trustRatings.$raterUid" to score,
                    "trustScore" to newAvg
                )
            )
            null
        }.addOnFailureListener { e ->
            Toast.makeText(
                context,
                e.localizedMessage ?: "Couldn't save rating",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        style = AppTextStyles.BodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkPastelAnthracite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MutedPastelTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = DarkPastelAnthracite,
                    navigationIconContentColor = MutedPastelTeal
                )
            )
        }
    ) { paddingValues ->
        if (profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RichPastelCoral)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ConvexCardBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        shape = RoundedCornerShape(32.dp),
                        elevation = 22.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(28.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = profile.avatar.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${profile.uid}" },
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceMuted),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(profile.name, style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                            Text(
                                profile.handle,
                                color = RichPastelCoral,
                                style = AppTextStyles.BodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(MutedPastelGold.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = MutedPastelGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val scoreText =
                                    if (profile.trustScore > 0) String.format(Locale.US, "%.1f", profile.trustScore) else "—"
                                Text(scoreText, fontWeight = FontWeight.Bold, color = MutedPastelGold, fontSize = 14.sp)
                                Text(
                                    " (trust score)",
                                    color = MutedPastelGold.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }

                            if (!isOwnProfile && viewerProfile != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    "Your rating for this person",
                                    style = AppTextStyles.BodySmall,
                                    color = DarkPastelAnthracite.copy(alpha = 0.55f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val myUserRating = profile.trustRatings[viewerUid] ?: 0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (star in 1..5) {
                                        val selected = myUserRating >= star
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = "Rate $star",
                                            tint = if (selected) MutedPastelGold else SurfaceMuted,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clickable { submitUserTrustRating(star) }
                                        )
                                        if (star < 5) Spacer(modifier = Modifier.width(6.dp))
                                    }
                                }
                            }

                            when (profileSurface) {
                                PublicProfileSurface.PERSONAL -> {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${userPosts.size}", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                                            Text(
                                                "RECS",
                                                style = AppTextStyles.BodySmall,
                                                color = DarkPastelAnthracite.copy(alpha = 0.45f),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$followersCount", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                                            Text(
                                                "FOLLOWERS",
                                                style = AppTextStyles.BodySmall,
                                                color = DarkPastelAnthracite.copy(alpha = 0.45f),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${profile.following.size}", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                                            Text(
                                                "SUBSCRIPTIONS",
                                                style = AppTextStyles.BodySmall,
                                                color = DarkPastelAnthracite.copy(alpha = 0.45f),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        profile.bio.ifEmpty { "Hi! I'm on TrustList." },
                                        style = AppTextStyles.BodyMedium,
                                        color = DarkPastelAnthracite.copy(alpha = 0.65f),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))
                                    OutlinedButton(
                                        onClick = { profileSurfaceOrdinal = 1 },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MutedPastelTeal),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedPastelTeal)
                                    ) {
                                        Text(
                                            "View ad campaigns",
                                            style = AppTextStyles.BodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MutedPastelTeal
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = MutedPastelTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                PublicProfileSurface.ADS_CAMPAIGNS -> {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            GradientTop,
                                                            SurfaceMuted,
                                                            GradientMid.copy(alpha = 0.85f)
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = MutedPastelGold,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "Balance: ${profile.trustCoins}",
                                                    style = AppTextStyles.BodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkPastelAnthracite
                                                )
                                                Text(
                                                    "TrustCoins",
                                                    style = AppTextStyles.BodySmall,
                                                    color = DarkPastelAnthracite.copy(alpha = 0.55f)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${theirOffers.size}", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                                            Text(
                                                "LAUNCHED",
                                                style = AppTextStyles.BodySmall,
                                                color = DarkPastelAnthracite.copy(alpha = 0.45f),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$promoterCampaignsCount", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                                            Text(
                                                "AS PROMOTER",
                                                style = AppTextStyles.BodySmall,
                                                color = DarkPastelAnthracite.copy(alpha = 0.45f),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                    OutlinedButton(
                                        onClick = { profileSurfaceOrdinal = 0 },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, RichPastelCoral),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RichPastelCoral)
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = RichPastelCoral, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Profile",
                                            style = AppTextStyles.BodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = RichPastelCoral
                                        )
                                    }
                                }
                            }

                            if (!isOwnProfile && viewerProfile != null) {
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = { toggleFollow() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) SurfaceMuted else scheme.primary,
                                        contentColor = if (isFollowing) DarkPastelAnthracite else scheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        if (isFollowing) "Unfollow" else "Follow",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isFollowing) DarkPastelAnthracite else scheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                when (profileSurface) {
                    PublicProfileSurface.PERSONAL -> {
                        personalRecsAndCollectionsSection(
                            selectedTab = selectedTab,
                            onSelectedTab = { selectedTab = it },
                            myPosts = userPosts,
                            collections = theirCollections,
                            onPostClick = onPostClick,
                            onCollectionClick = onCollectionClick,
                            recsTabLabel = "Recs",
                            collectionsTabLabel = "Collections"
                        )
                    }

                    PublicProfileSurface.ADS_CAMPAIGNS -> {
                        adsDashboardSection(
                            myOffers = theirOffers,
                            isBusiness = profile.isBusiness,
                            onCreateCampaign = {},
                            onOfferClick = onOfferClick,
                            onOfferPauseToggle = {},
                            isViewerOwner = false
                        )
                    }
                }
            }
        }
    }
}
