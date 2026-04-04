package com.example.recommend

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelGold
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.ConvexCardBox
import com.example.recommend.ui.theme.GradientMid
import com.example.recommend.ui.theme.GradientTop
import com.example.recommend.ui.theme.SurfaceMuted
import com.example.recommend.ui.theme.SurfacePastel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.Locale
import java.util.concurrent.Executors

private enum class ProfileSurface {
    PERSONAL,
    ADS_CAMPAIGNS
}

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    myPosts: List<Post>,
    collections: List<PostCollection> = emptyList(),
    myOffers: List<AdOffer> = emptyList(),
    followersCount: Int = 0,
    participatingPromoCampaignsCount: Int = 0,
    profileSurfaceOrdinal: Int = 0,
    onProfileSurfaceChange: (Int) -> Unit = {},
    onCollectionClick: (PostCollection) -> Unit = {},
    onPostClick: (String) -> Unit = {},
    onCreateCampaign: () -> Unit = {},
    onOfferClick: (AdOffer) -> Unit = {},
    onOfferPauseToggle: (AdOffer) -> Unit = {},
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isAvatarUploading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val compressExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val pickAvatar = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val uid = userProfile.uid
        if (uid.isBlank()) return@rememberLauncherForActivityResult
        isAvatarUploading = true
        compressExecutor.execute {
            val bytes = ImageCompress.compressUriToJpeg(
                context,
                uri,
                ImageCompress.AVATAR_MAX_SIDE_PX,
                ImageCompress.AVATAR_JPEG_QUALITY
            )
            if (bytes == null || bytes.isEmpty()) {
                mainHandler.post {
                    isAvatarUploading = false
                    Toast.makeText(context, "Could not process image", Toast.LENGTH_SHORT).show()
                }
                return@execute
            }
            val ref = storage.reference.child("avatars/$uid/avatar.jpg")
            val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
            ref.putBytes(bytes, metadata)
                .addOnSuccessListener {
                    ref.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            db.trustListDataRoot()
                                .collection("users").document(uid)
                                .update("avatar", downloadUri.toString())
                                .addOnSuccessListener {
                                    mainHandler.post {
                                        isAvatarUploading = false
                                        Toast.makeText(context, "Photo updated", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    mainHandler.post {
                                        isAvatarUploading = false
                                        Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            mainHandler.post {
                                isAvatarUploading = false
                                Toast.makeText(context, "URL failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                .addOnFailureListener { e ->
                    mainHandler.post {
                        isAvatarUploading = false
                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    val profileSurface = if (profileSurfaceOrdinal == 0) ProfileSurface.PERSONAL else ProfileSurface.ADS_CAMPAIGNS

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            ConvexCardBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(32.dp),
                elevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(SurfaceMuted)
                            .clickable(enabled = !isAvatarUploading) {
                                pickAvatar.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = userProfile.avatar.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${userProfile.uid}" },
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isAvatarUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = RichPastelCoral,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                    Text(
                        "Tap photo to change",
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(userProfile.name, style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                    Text(userProfile.handle, color = RichPastelCoral, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MutedPastelGold.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MutedPastelGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))

                        val scoreText =
                            if (userProfile.trustScore > 0) String.format(Locale.US, "%.1f", userProfile.trustScore) else "—"
                        Text(scoreText, fontWeight = FontWeight.Bold, color = MutedPastelGold, fontSize = 14.sp)
                        Text(
                            " (trust score)",
                            color = MutedPastelGold.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    when (profileSurface) {
                        ProfileSurface.PERSONAL -> {
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${myPosts.size}", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
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
                                    Text("${userProfile.following.size}", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
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
                                userProfile.bio.ifEmpty { "Hi! I'm on TrustList." },
                                style = AppTextStyles.BodyMedium,
                                color = DarkPastelAnthracite.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = { onProfileSurfaceChange(1) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MutedPastelTeal),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedPastelTeal)
                            ) {
                                Text("Go to Ads Campaigns", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, color = MutedPastelTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MutedPastelTeal, modifier = Modifier.size(20.dp))
                            }
                        }

                        ProfileSurface.ADS_CAMPAIGNS -> {
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
                                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
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
                                            "Balance: ${userProfile.trustCoins}",
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
                                    Text("${myOffers.size}", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                                    Text(
                                        "LAUNCHED",
                                        style = AppTextStyles.BodySmall,
                                        color = DarkPastelAnthracite.copy(alpha = 0.45f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$participatingPromoCampaignsCount", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
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
                                onClick = { onProfileSurfaceChange(0) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, RichPastelCoral),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RichPastelCoral)
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = RichPastelCoral, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Personal cabinet", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, color = RichPastelCoral)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = SurfaceMuted)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        },
                        border = BorderStroke(1.dp, RichPastelCoral),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RichPastelCoral),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = RichPastelCoral)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log out", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RichPastelCoral)
                    }
                }
            }
        }

        when (profileSurface) {
            ProfileSurface.PERSONAL -> {
                personalRecsAndCollectionsSection(
                    selectedTab = selectedTab,
                    onSelectedTab = { selectedTab = it },
                    myPosts = myPosts,
                    collections = collections,
                    onPostClick = onPostClick,
                    onCollectionClick = onCollectionClick,
                    recsTabLabel = "My recs",
                    collectionsTabLabel = "Collections"
                )
            }

            ProfileSurface.ADS_CAMPAIGNS -> {
                adsDashboardSection(
                    myOffers = myOffers,
                    isBusiness = userProfile.isBusiness,
                    onCreateCampaign = onCreateCampaign,
                    onOfferClick = onOfferClick,
                    onOfferPauseToggle = onOfferPauseToggle,
                    isViewerOwner = true
                )
            }
        }
    }
}

internal fun LazyListScope.adsDashboardSection(
    myOffers: List<AdOffer>,
    isBusiness: Boolean,
    onCreateCampaign: () -> Unit,
    onOfferClick: (AdOffer) -> Unit,
    onOfferPauseToggle: (AdOffer) -> Unit,
    /** When false, viewing someone else: no new campaign, no pause, read-only list. */
    isViewerOwner: Boolean = true
) {
    item {
        Text(
            if (isViewerOwner) "Ad dashboard" else "Launched campaigns",
            style = AppTextStyles.Heading2.copy(fontSize = 20.sp),
            color = DarkPastelAnthracite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        Text(
            if (isViewerOwner) "Your campaigns and rewards." else "Campaigns they launched.",
            style = AppTextStyles.BodySmall,
            color = DarkPastelAnthracite.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
        if (isViewerOwner && isBusiness) {
            OutlinedButton(
                onClick = onCreateCampaign,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RichPastelCoral),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RichPastelCoral)
            ) {
                Icon(Icons.Filled.Campaign, contentDescription = null, tint = RichPastelCoral, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("New campaign", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, color = RichPastelCoral)
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else if (isViewerOwner && !isBusiness) {
            Text(
                "Business accounts can launch sponsored campaigns from the Ads section.",
                style = AppTextStyles.BodySmall,
                color = DarkPastelAnthracite.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
    val showPause = isViewerOwner && isBusiness
    when {
        myOffers.isEmpty() -> item {
            EmptyStateCard(
                if (isViewerOwner) "No campaigns yet. Tap New campaign or + to launch your first offer."
                else "No public campaigns yet."
            )
        }
        else -> items(myOffers, key = { it.id }) { offer ->
            AdOfferProfileCard(
                offer = offer,
                onCardClick = { onOfferClick(offer) },
                showPauseButton = showPause,
                onPauseClick = { onOfferPauseToggle(offer) }
            )
        }
    }
}

internal fun LazyListScope.personalRecsAndCollectionsSection(
    selectedTab: Int,
    onSelectedTab: (Int) -> Unit,
    myPosts: List<Post>,
    collections: List<PostCollection>,
    onPostClick: (String) -> Unit,
    onCollectionClick: (PostCollection) -> Unit,
    recsTabLabel: String = "My recs",
    collectionsTabLabel: String = "Collections"
) {
    item {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = RichPastelCoral,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MutedPastelTeal.copy(alpha = 0.35f)
                )
            },
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onSelectedTab(0) },
                text = { Text(recsTabLabel, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onSelectedTab(1) },
                text = { Text(collectionsTabLabel, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (selectedTab == 0) {
        if (myPosts.isEmpty()) {
            item {
                EmptyStateCard("No recommendations yet")
            }
        } else {
            items(myPosts) { post ->
                PostSmallCard(post = post, onClick = { onPostClick(post.id) })
            }
        }
    } else {
        if (collections.isEmpty()) {
            item {
                EmptyStateCard("No collections yet")
            }
        } else {
            val chunkedCollections = collections.chunked(2)
            items(chunkedCollections) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { collection ->
                        CollectionCard(
                            collection = collection,
                            modifier = Modifier.weight(1f),
                            onClick = { onCollectionClick(collection) }
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AdOfferProfileCard(
    offer: AdOffer,
    onCardClick: (() -> Unit)? = null,
    showPauseButton: Boolean = false,
    onPauseClick: () -> Unit = {}
) {
    val isActive = offer.status.equals("active", ignoreCase = true)
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)
        .then(
            if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier
        )

    ConvexCardBox(
        modifier = cardModifier,
        shape = RoundedCornerShape(24.dp),
        elevation = 22.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        offer.title,
                        style = AppTextStyles.BodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = DarkPastelAnthracite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (offer.status.lowercase(Locale.US)) {
                            "active" -> SurfacePastel
                            "paused" -> SurfaceMuted
                            else -> SurfaceMuted
                        }
                    ) {
                        Text(
                            offer.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = AppTextStyles.BodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MutedPastelTeal
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showPauseButton) {
                        IconButton(onClick = onPauseClick) {
                            Icon(
                                if (isActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isActive) "Pause" else "Resume",
                                tint = MutedPastelTeal
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MutedPastelGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${offer.rewardCoins}",
                                style = AppTextStyles.Heading2.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.Bold,
                                color = RichPastelCoral
                            )
                        }
                        Text(
                            "TrustCoins",
                            style = AppTextStyles.BodySmall,
                            color = DarkPastelAnthracite.copy(alpha = 0.45f)
                        )
                    }
                }
            }
            if (offer.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    offer.description,
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Min. trust ${String.format(Locale.US, "%.1f", offer.minTrustScore)}",
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.55f)
                )
                val dateStr = formatAdOfferDate(offer.createdAt)
                if (dateStr.isNotEmpty()) {
                    Text(
                        dateStr,
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(text: String) {
    ConvexCardBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 20.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(text, style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.55f), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PostSmallCard(post: Post, onClick: () -> Unit = {}) {
    ConvexCardBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = 20.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (post.imageUrl != null) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceMuted),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceMuted), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = MutedPastelTeal.copy(alpha = 0.4f))
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column {
                Text(post.title, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = MutedPastelGold, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${post.rating}/5", color = DarkPastelAnthracite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(" • ${post.category}", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.55f))
                }
            }
        }
    }
}

@Composable
fun CollectionCard(
    collection: PostCollection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ConvexCardBox(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Bookmarks, contentDescription = null, tint = MutedPastelTeal)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                collection.name,
                style = AppTextStyles.BodyMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${collection.postIds.size} posts",
                style = AppTextStyles.BodySmall,
                color = DarkPastelAnthracite.copy(alpha = 0.55f)
            )
        }
    }
}
