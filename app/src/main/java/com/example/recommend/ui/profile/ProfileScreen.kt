package com.example.recommend.ui.profile

import com.example.recommend.*
import com.example.recommend.ui.theme.*
import com.example.recommend.data.model.*

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recommend.ui.feed.TrustScoreRing
import com.example.recommend.ui.feed.categoryStyle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.Locale
import java.util.concurrent.Executors

// ─── ProfileScreen ────────────────────────────────────────────────────────────

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
    onCreateCollection: () -> Unit = {},
    onPostClick: (String) -> Unit = {},
    onCreateCampaign: () -> Unit = {},
    onOfferClick: (AdOffer) -> Unit = {},
    onOfferPauseToggle: (AdOffer) -> Unit = {},
    isViewingOtherUser: Boolean = false,
    onBack: (() -> Unit)? = null,
    onLogout: () -> Unit = {}
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
                context, uri,
                ImageCompress.AVATAR_MAX_SIDE_PX,
                ImageCompress.AVATAR_JPEG_QUALITY
            )
            if (bytes == null || bytes.isEmpty()) {
                mainHandler.post { isAvatarUploading = false; Toast.makeText(context, "Could not process image", Toast.LENGTH_SHORT).show() }
                return@execute
            }
            val ref = storage.reference.child("avatars/$uid/avatar.jpg")
            val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
            ref.putBytes(bytes, metadata)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        db.trustListDataRoot().collection("users").document(uid)
                            .update("avatar", downloadUri.toString())
                            .addOnSuccessListener { mainHandler.post { isAvatarUploading = false; Toast.makeText(context, "Photo updated", Toast.LENGTH_SHORT).show() } }
                            .addOnFailureListener { e -> mainHandler.post { isAvatarUploading = false; Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show() } }
                    }.addOnFailureListener { e -> mainHandler.post { isAvatarUploading = false; Toast.makeText(context, "URL failed: ${e.message}", Toast.LENGTH_SHORT).show() } }
                }
                .addOnFailureListener { e -> mainHandler.post { isAvatarUploading = false; Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show() } }
        }
    }

    val trustScore = remember(userProfile.trustScore) {
        (userProfile.trustScore * 2.0).coerceIn(0.0, 10.0).toFloat()
    }
    val scoreText = if (userProfile.trustScore > 0)
        String.format(Locale.US, "%.1f", userProfile.trustScore * 2.0)
    else "—"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // ── 1. profile-header ────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp)
            ) {
                // Avatar row + Edit / Back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // profile-avatar-wrap: gradient ring
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(AppViolet, AppTeal),
                                    start = Offset(0f, 0f),
                                    end = Offset(240f, 240f)
                                ),
                                CircleShape
                            )
                            .padding(2.5.dp)
                            .clickable(enabled = !isAvatarUploading && !isViewingOtherUser) {
                                pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // profile-avatar-inner: white circle with image
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AppWhite, CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = userProfile.avatar.ifEmpty {
                                    "https://api.dicebear.com/7.x/avataaars/svg?seed=${userProfile.uid}"
                                },
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            if (isAvatarUploading) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = AppTeal, strokeWidth = 2.dp)
                                }
                            }
                        }
                    }

                    // Edit profile / Back / Logout button
                    if (isViewingOtherUser) {
                        ProfileOutlinedButton(text = "← Back") { onBack?.invoke() }
                    } else {
                        ProfileOutlinedButton(text = "Edit profile") { /* TODO: edit */ }
                    }
                }

                // profile-name — max 2/3 ширины, авто-уменьшение при длинном имени
                var nameFontSize by remember(userProfile.name) { mutableStateOf(18.sp) }
                Text(
                    text = userProfile.name.ifBlank { "User" },
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

                // profile-handle + location
                val handleText = buildString {
                    if (userProfile.handle.isNotBlank()) append(userProfile.handle)
                    // add city if available (from bio or name — stub)
                }
                if (handleText.isNotBlank()) {
                    Text(
                        text = handleText,
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = AppMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // profile-bio
                val bio = userProfile.bio.ifBlank { "City explorer. Trust the pack 🐺" }
                Text(
                    text = bio,
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    color = AppDark.copy(alpha = 0.75f),
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )

                // stats-row: Picks | Followers | Following
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStat(value = "${myPosts.size}", label = "Picks")
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(SurfaceMuted))
                    ProfileStat(value = "$followersCount", label = "Followers")
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(SurfaceMuted))
                    ProfileStat(value = "${userProfile.following.size}", label = "Following")
                }

                // trust-score-row: white card — ring + label + coins chip (unified block, matches HTML)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AppWhite,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Ring (score visible inside)
                        TrustScoreRing(
                            score = trustScore,
                            size = 56.dp,
                            strokeWidth = 4.dp
                        )
                        // Labels
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TrustScore",
                                fontFamily = HeadingFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AppDark
                            )
                            Text(
                                text = "Based on ${myPosts.size} picks",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = AppMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        // coins-chip (right-aligned, gold, matches HTML .coins-chip)
                        if (!isViewingOtherUser) {
                            Box(
                                modifier = Modifier
                                    .background(AppGold.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "🪙 ${userProfile.trustCoins}",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AppGold
                                )
                            }
                        }
                    }
                }

                // Logout button (own profile only)
                if (!isViewingOtherUser) {
                    ProfileOutlinedButton(
                        text = "Log out",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = AppMuted, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
                    ) {
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    }
                }
            }
        }

        // ── 2. Tabs + content ────────────────────────────────────────────────
        personalRecsAndCollectionsSection(
            selectedTab = selectedTab,
            onSelectedTab = { selectedTab = it },
            myPosts = myPosts,
            collections = collections,
            myOffers = myOffers,
            isBusiness = userProfile.isBusiness,
            onPostClick = onPostClick,
            onCollectionClick = onCollectionClick,
            onCreateCollection = onCreateCollection,
            onCreateCampaign = onCreateCampaign,
            onOfferClick = onOfferClick,
            onOfferPauseToggle = onOfferPauseToggle,
            isViewerOwner = !isViewingOtherUser
        )
    }
}

// ─── Stat cell ────────────────────────────────────────────────────────────────

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = HeadingFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = AppDark
        )
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = AppMuted,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// ─── Outlined button (Edit profile / Back / Log out) ─────────────────────────

@Composable
private fun ProfileOutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .border(1.5.dp, Color(0xFFE0DDD8), RoundedCornerShape(12.dp))
            .background(AppWhite, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leadingIcon?.invoke()
            Text(
                text = text,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = AppDark
            )
        }
    }
}

// ─── Tabs + grid section ──────────────────────────────────────────────────────

internal fun LazyListScope.personalRecsAndCollectionsSection(
    selectedTab: Int,
    onSelectedTab: (Int) -> Unit,
    myPosts: List<Post>,
    collections: List<PostCollection>,
    myOffers: List<AdOffer> = emptyList(),
    isBusiness: Boolean = false,
    onPostClick: (String) -> Unit,
    onCollectionClick: (PostCollection) -> Unit,
    onCreateCollection: () -> Unit = {},
    onCreateCampaign: () -> Unit = {},
    onOfferClick: (AdOffer) -> Unit = {},
    onOfferPauseToggle: (AdOffer) -> Unit = {},
    recsTabLabel: String = "Picks",
    collectionsTabLabel: String = "Collections",
    isViewerOwner: Boolean = true
) {
    // Tab bar — matches HTML .tabs { padding: 0 20px; border-bottom: 1px solid surf }
    // Active tab has its own 2dp violet underline (HTML: border-bottom-color: violet)
    item {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .padding(horizontal = 20.dp)  // HTML: padding: 0 20px
            ) {
                listOf(recsTabLabel, collectionsTabLabel, "Offers").forEachIndexed { index, label ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectedTab(index) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (selectedTab == index) AppViolet else AppMuted,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        // Active underline — 2dp violet, only under this tab item
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(if (selectedTab == index) AppViolet else Color.Transparent)
                        )
                    }
                }
            }
            // Full-width 1dp border below all tabs (HTML: border-bottom: 1px solid surf)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SurfaceMuted)
            )
        }
        Spacer(Modifier.height(14.dp))
    }

    when (selectedTab) {
        // ── Picks: 2-column grid ──────────────────────────────────────────────
        0 -> {
            if (myPosts.isEmpty()) {
                item { EmptyStateCard("No picks yet") }
            } else {
                val rows = myPosts.chunked(2)
                items(rows) { rowPosts ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPosts.forEach { post ->
                            GridPostCard(
                                post = post,
                                modifier = Modifier.weight(1f),
                                onClick = { onPostClick(post.id) }
                            )
                        }
                        if (rowPosts.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Collections ───────────────────────────────────────────────────────
        1 -> {
            // Показываем только root-коллекции (parentId == null)
            val roots = collections.filter { it.parentId == null }
            // Считаем количество детей для каждого root
            val childCountByParent = collections
                .mapNotNull { it.parentId }
                .groupingBy { it }
                .eachCount()

            // Карточка "+ New collection" + root-коллекции в одной 2-колоночной сетке
            // (только для владельца профиля)
            val gridItems = buildList<CollectionGridItem> {
                if (isViewerOwner) add(CollectionGridItem.AddNew)
                addAll(roots.map { CollectionGridItem.Item(it) })
            }

            if (gridItems.isEmpty()) {
                item { EmptyStateCard("No collections yet") }
            } else {
                val rows = gridItems.chunked(2)
                items(rows) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            when (item) {
                                CollectionGridItem.AddNew -> NewCollectionCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = onCreateCollection
                                )
                                is CollectionGridItem.Item -> CollectionCard(
                                    collection = item.collection,
                                    childCount = childCountByParent[item.collection.id] ?: 0,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onCollectionClick(item.collection) }
                                )
                            }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Offers ────────────────────────────────────────────────────────────
        2 -> {
            adsDashboardSection(
                myOffers = myOffers,
                isBusiness = isBusiness,
                onCreateCampaign = onCreateCampaign,
                onOfferClick = onOfferClick,
                onOfferPauseToggle = onOfferPauseToggle,
                isViewerOwner = isViewerOwner
            )
        }
    }
}

// ─── grid-card: square card with gradient + emoji + dark overlay + title ──────

@Composable
private fun GridPostCard(
    post: Post,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val catStyle = remember(post.category) { categoryStyle(post.category) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        // Background: image or gradient
        if (!post.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(catStyle.gradient),
                contentAlignment = Alignment.Center
            ) {
                Text(catStyle.emoji, fontSize = 36.sp)
            }
        }

        // grid-overlay: only the bottom strip (matches HTML .grid-overlay: position absolute, bottom 0)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF1A2A24).copy(alpha = 0.70f)
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = post.title,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = AppWhite,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Ads dashboard section (unchanged logic, lighter UI) ─────────────────────

internal fun LazyListScope.adsDashboardSection(
    myOffers: List<AdOffer>,
    isBusiness: Boolean,
    onCreateCampaign: () -> Unit,
    onOfferClick: (AdOffer) -> Unit,
    onOfferPauseToggle: (AdOffer) -> Unit,
    onActivateAdStudio: () -> Unit = {},
    isViewerOwner: Boolean = true
) {
    item {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = if (isViewerOwner) "Ad dashboard" else "Launched campaigns",
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AppDark,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = if (isViewerOwner) "Your campaigns and rewards." else "Campaigns they launched.",
                fontFamily = BodyFontFamily,
                fontSize = 13.sp,
                color = AppMuted,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (isViewerOwner && isBusiness) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.horizontalGradient(listOf(AppViolet, AppTeal)),
                            RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onCreateCampaign() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Campaign, null, tint = AppWhite, modifier = Modifier.size(18.dp))
                        Text("New campaign", fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppWhite)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    val showPause = isViewerOwner && isBusiness
    when {
        myOffers.isEmpty() -> item { Box(Modifier.padding(horizontal = 20.dp)) { EmptyStateCard(if (isViewerOwner) "No campaigns yet." else "No public campaigns yet.") } }
        else -> items(myOffers, key = { it.id }) { offer ->
            Box(Modifier.padding(horizontal = 20.dp)) {
                AdOfferProfileCard(
                    offer = offer,
                    onCardClick = { onOfferClick(offer) },
                    showPauseButton = showPause,
                    onPauseClick = { onOfferPauseToggle(offer) }
                )
            }
        }
    }
}

// ─── Helper cards ─────────────────────────────────────────────────────────────

@Composable
fun AdOfferProfileCard(
    offer: AdOffer,
    onCardClick: (() -> Unit)? = null,
    showPauseButton: Boolean = false,
    onPauseClick: () -> Unit = {}
) {
    val isActive = offer.status.equals("active", ignoreCase = true)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppWhite,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .then(if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        offer.title,
                        fontFamily = HeadingFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AppDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (isActive) AppTeal.copy(alpha = 0.10f) else SurfaceMuted,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            offer.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() },
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (isActive) AppTeal else AppMuted
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🪙", fontSize = 14.sp)
                        Text(
                            "${offer.rewardCoins}",
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = AppGold
                        )
                    }
                    Text("TrustCoins", fontFamily = BodyFontFamily, fontSize = 11.sp, color = AppMuted)
                    if (showPauseButton) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceMuted)
                                .clickable { onPauseClick() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (isActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = AppMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            if (offer.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    offer.description,
                    fontFamily = BodyFontFamily,
                    fontSize = 13.sp,
                    color = AppMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(AppWhite, RoundedCornerShape(20.dp))
            .border(1.dp, SurfaceMuted, RoundedCornerShape(20.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = AppMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PostSmallCard(post: Post, onClick: () -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppWhite,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val catStyle = remember(post.category) { categoryStyle(post.category) }
            if (!post.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceMuted),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(catStyle.gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(catStyle.emoji, fontSize = 24.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.title,
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AppDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "📍 ${post.location.ifBlank { post.category }}",
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    color = AppMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// Sealed-тип для смешанной сетки коллекций (включая карточку "+ New collection")
internal sealed interface CollectionGridItem {
    data object AddNew : CollectionGridItem
    data class Item(val collection: PostCollection) : CollectionGridItem
}

@Composable
fun CollectionCard(
    collection: PostCollection,
    modifier: Modifier = Modifier,
    childCount: Int = 0,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppWhite,
        shadowElevation = 2.dp,
        modifier = modifier
            .clickable { onClick() }
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppViolet.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Bookmarks, contentDescription = null, tint = AppViolet, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                collection.name,
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp)
            ) {
                Text(
                    "${collection.postIds.size} picks",
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    color = AppMuted
                )
                if (childCount > 0) {
                    Text(
                        " · 📂 $childCount",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = AppViolet
                    )
                }
            }
        }
    }
}

@Composable
fun NewCollectionCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppViolet.copy(alpha = 0.06f),
        modifier = modifier
            .clickable { onClick() }
            .aspectRatio(1f)
            .border(
                width = 1.5.dp,
                color = AppViolet.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppViolet.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "New collection",
                    tint = AppViolet,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "New collection",
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppViolet,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                "Tap to create",
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                color = AppViolet.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}
