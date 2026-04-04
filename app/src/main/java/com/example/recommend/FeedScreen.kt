package com.example.recommend

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import java.util.Locale
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.BodyFontFamily
import com.example.recommend.ui.theme.ConvexCardBox
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.HeadingFontFamily
import com.example.recommend.ui.theme.MutedPastelGold
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.SoftPastelMint
import com.example.recommend.ui.theme.SurfaceMuted
import com.example.recommend.ui.theme.SurfacePastel

data class Post(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val rating: Int = 5,
    val imageUrl: String? = null,
    val authorName: String = "Alex",
    val authorHandle: String = "@alex",
    val isSponsored: Boolean = false,
    val ratingsByUser: Map<String, Int> = emptyMap(),
    val likesByUser: Set<String> = emptySet(),
    /** Set when this post is a reply to a pack «signal» (request). */
    val replyToRequestId: String? = null,
    /** External link (e.g. website) for signal replies / picks. */
    val resourceUrl: String? = null
)

fun Post.averageAudienceRatingStars(): Int? {
    if (ratingsByUser.isEmpty()) return null
    val avg = ratingsByUser.values.average()
    return avg.roundToInt().coerceIn(1, 5)
}

/** Label for the collective average; [ratingCount] is how many people rated (may be 0). */
private fun audienceAverageRatingLabel(ratingCount: Int): String =
    "Average reader rating ($ratingCount)"

@Composable
private fun AudienceAverageStarsDisplay(
    ratingCount: Int,
    averageRounded: Int?
) {
    Row {
        if (ratingCount == 0) {
            repeat(5) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = SurfaceMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            val n = (averageRounded ?: 1).coerceIn(1, 5)
            repeat(n) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MutedPastelTeal,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun YourRatingStarsRow(
    myStars: Int,
    onPickStar: (Int) -> Unit,
    starSize: Dp = 24.dp
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (star in 1..5) {
            val selected = myStars >= star
            Icon(
                Icons.Filled.Star,
                contentDescription = "Rate $star",
                tint = if (selected) MutedPastelGold else SurfaceMuted,
                modifier = Modifier
                    .size(starSize)
                    .clickable { onPickStar(star) }
            )
        }
    }
}

private fun initialsFromName(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> (parts[0].take(1) + parts[1].take(1)).uppercase(Locale.getDefault())
        else -> parts[0].take(2).uppercase(Locale.getDefault())
    }
}

/**
 * Circular avatar: loads [imageUrl] when present; otherwise shows initials from [displayName] (or [fallbackSeed]).
 */
@Composable
fun FeedUserAvatar(
    imageUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    fallbackSeed: String = ""
) {
    val labelSource = displayName.trim().ifBlank { fallbackSeed }
    val initials = remember(labelSource, fallbackSeed) { initialsFromName(labelSource) }
    val urlTrim = imageUrl?.trim().orEmpty()
    val model = remember(urlTrim, fallbackSeed, displayName) {
        if (urlTrim.isNotEmpty()) urlTrim
        else {
            val seed = fallbackSeed.ifBlank { displayName }.ifBlank { "user" }
            "https://api.dicebear.com/7.x/avataaars/png?seed=${Uri.encode(seed)}&size=128"
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SurfaceMuted),
        contentAlignment = Alignment.Center
    ) {
        val painter = rememberAsyncImagePainter(model = model)
        when (painter.state) {
            is AsyncImagePainter.State.Success -> {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Text(
                    text = initials,
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp,
                    color = DarkPastelAnthracite.copy(
                        alpha = if (painter.state is AsyncImagePainter.State.Loading) 0.5f else 0.9f
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

data class PackRequest(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val tags: List<String> = emptyList(),
    val location: String = "",
    val selectedUsers: List<String> = emptyList(),
    val status: String = "active",
    val createdAt: Long = 0L,
    /** Derived from `answers` where `requestId` matches (not stored on request doc). */
    val answersCount: Int = 0
) {
    /** Same as [userId] (author of the request). */
    val authorId: String get() = userId
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    posts: List<Post>,
    requests: List<PackRequest> = emptyList(),
    users: List<UserProfile> = emptyList(),
    followingUserIds: Set<String> = emptySet(),
    savedPostIds: Set<String> = emptySet(),
    activeOffers: List<AdOffer> = emptyList(),
    trustCoins: Int = 0,
    onOfferAccept: (AdOffer) -> Unit = {},
    onAskPackClick: () -> Unit,
    onRequestClick: (PackRequest) -> Unit,
    /** Open request detail (e.g. own signal card). */
    onSignalRequestOpen: (PackRequest) -> Unit = {},
    /** Help on a friend's request: open add-pick flow for [PackRequest.id]. */
    onHelpRequest: (PackRequest) -> Unit = {},
    onSaveClick: (String) -> Unit,
    onUserProfileClick: (String) -> Unit = {},
    onRequestAuthorProfileClick: (String) -> Unit = {},
    viewerUid: String? = null,
    onAudienceRate: (String, Int) -> Unit = { _, _ -> },
    onOpenPost: ((String) -> Unit)? = null,
    onWalletClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val friendPackRequests = remember(requests, viewerUid, followingUserIds) {
        val uid = viewerUid
        requests.filter { req ->
            req.status.equals("active", ignoreCase = true) &&
                (uid.isNullOrBlank() || req.userId != uid) &&
                (followingUserIds.isEmpty() || req.userId in followingUserIds)
        }
    }

    val pulseStoryUsers = remember(users, followingUserIds, posts) {
        val fromFollowing = followingUserIds
            .mapNotNull { uid -> users.find { it.uid == uid } }
            .filter { it.uid.isNotBlank() }
            .distinctBy { it.uid }
        if (fromFollowing.isNotEmpty()) fromFollowing.take(24)
        else posts.map { it.userId }
            .distinct()
            .mapNotNull { uid -> users.find { it.uid == uid } }
            .distinctBy { it.uid }
            .take(24)
    }

    val pulseNewPicksCount = remember(posts) {
        posts.size.coerceIn(0, 99)
    }

    val openSignalRequests = remember(requests) {
        requests.filter { it.status.equals("active", ignoreCase = true) }
    }

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                FeedTopBarRow(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    trustCoins = trustCoins,
                    onWalletClick = onWalletClick
                )
            }

            item {
                PackPulseStoriesBlock(
                    storyUsers = pulseStoryUsers,
                    newPicksCount = pulseNewPicksCount,
                    onStoryAvatarClick = { }
                )
            }

            if (openSignalRequests.isNotEmpty()) {
                item {
                    PackSignalsBlock(
                        requests = openSignalRequests,
                        users = users,
                        viewerUid = viewerUid,
                        onSignalRequestOpen = onSignalRequestOpen,
                        onHelpRequest = onHelpRequest,
                        onRecipientProfileClick = onUserProfileClick
                    )
                }
            }

            item {
                PackFriendsSection(
                    requests = friendPackRequests,
                    users = users,
                    onRequestClick = onRequestClick,
                    onRequestAuthorProfileClick = onRequestAuthorProfileClick,
                    onAskPackClick = onAskPackClick
                )
            }

            if (activeOffers.isNotEmpty()) {
                item {
                    ExclusiveDealsSection(
                        offers = activeOffers,
                        onOfferAccept = onOfferAccept
                    )
                }
            }

            item {
                Text(
                    text = "Feed",
                    style = AppTextStyles.Heading2,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            if (posts.isEmpty()) {
                item { EmptyFeedMessage() }
            } else {
                items(posts, key = { it.id }) { post ->
                    FeedPostCard(
                        post = post,
                        isSaved = savedPostIds.contains(post.id),
                        onSaveClick = onSaveClick,
                        users = users,
                        onUserProfileClick = onUserProfileClick,
                        viewerUid = viewerUid,
                        onAudienceRate = onAudienceRate,
                        onOpenPost = onOpenPost
                    )
                }
            }
        }
    }
}

@Composable
fun PackSignalsBlock(
    requests: List<PackRequest>,
    users: List<UserProfile>,
    viewerUid: String?,
    onSignalRequestOpen: (PackRequest) -> Unit,
    onHelpRequest: (PackRequest) -> Unit,
    onRecipientProfileClick: (String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(requests, key = { it.id }) { request ->
                PackSignalRequestCard(
                    request = request,
                    authorUser = users.find { it.uid == request.userId },
                    users = users,
                    viewerUid = viewerUid,
                    onSignalRequestOpen = onSignalRequestOpen,
                    onHelpRequest = onHelpRequest,
                    onRecipientProfileClick = onRecipientProfileClick
                )
            }
        }
    }
}

@Composable
fun PackSignalRecipientsCarousel(
    selectedUserIds: List<String>,
    users: List<UserProfile>,
    onRecipientClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedUserIds.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(selectedUserIds.distinct(), key = { it }) { uid ->
            val profile = users.find { it.uid == uid }
            val shortName = profile?.name?.trim()?.split(Regex("\\s+"))?.firstOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?: "..."
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(48.dp)
                    .clickable { onRecipientClick(uid) }
            ) {
                FeedUserAvatar(
                    imageUrl = profile?.avatar,
                    displayName = profile?.name ?: shortName,
                    fallbackSeed = uid,
                    size = 32.dp
                )
                Text(
                    text = shortName,
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private val PackSignalQuestionStyle =
    AppTextStyles.Heading2.copy(fontSize = 17.sp, lineHeight = 22.sp)

private val PackSignalCardWidth = 280.dp
private val PackSignalCardHeight = 220.dp

/** Avatars of friends this pack signal was sent to ([PackRequest.selectedUsers]). */
@Composable
private fun PackSignalRecipientsStrip(
    selectedUserIds: List<String>,
    users: List<UserProfile>,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 26.dp
) {
    val distinct = selectedUserIds.distinct().filter { it.isNotBlank() }
    if (distinct.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Asked pack",
            style = AppTextStyles.BodySmall,
            color = DarkPastelAnthracite.copy(alpha = 0.5f)
        )
        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            distinct.forEach { uid ->
                val profile = users.find { it.uid == uid }
                FeedUserAvatar(
                    imageUrl = profile?.avatar,
                    displayName = profile?.name?.takeIf { it.isNotBlank() } ?: "Member",
                    fallbackSeed = uid,
                    size = avatarSize,
                    modifier = Modifier.clickable { onUserClick(uid) }
                )
            }
        }
    }
}

@Composable
private fun PackSignalRequestCard(
    request: PackRequest,
    authorUser: UserProfile?,
    users: List<UserProfile>,
    viewerUid: String?,
    onSignalRequestOpen: (PackRequest) -> Unit,
    onHelpRequest: (PackRequest) -> Unit,
    onRecipientProfileClick: (String) -> Unit = {}
) {
    val isMine = viewerUid != null && request.authorId == viewerUid
    val authorName = authorUser?.name?.takeIf { it.isNotBlank() } ?: "Member"

    Card(
        modifier = Modifier
            .width(PackSignalCardWidth)
            .height(PackSignalCardHeight),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftPastelMint),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onSignalRequestOpen(request) },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isMine) {
                    Text(
                        text = "Your signal",
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.55f)
                    )
                    PackSignalRecipientsStrip(
                        selectedUserIds = request.selectedUsers,
                        users = users,
                        onUserClick = onRecipientProfileClick
                    )
                    Text(
                        text = request.text,
                        style = PackSignalQuestionStyle,
                        color = DarkPastelAnthracite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeedUserAvatar(
                            imageUrl = authorUser?.avatar,
                            displayName = authorName,
                            fallbackSeed = request.userId,
                            size = 24.dp
                        )
                        Text(
                            text = "$authorName is asking:",
                            style = AppTextStyles.BodySmall,
                            color = DarkPastelAnthracite.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    PackSignalRecipientsStrip(
                        selectedUserIds = request.selectedUsers,
                        users = users,
                        onUserClick = onRecipientProfileClick
                    )
                    Text(
                        text = request.text,
                        style = PackSignalQuestionStyle,
                        color = DarkPastelAnthracite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Button(
                onClick = {
                    if (isMine) onSignalRequestOpen(request) else onHelpRequest(request)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isMine) "See my pick" else "Help",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Instagram-style story row + pack pulse header. [newPicksCount] drives the “new picks” badge.
 */
@Composable
fun PackPulseStoriesBlock(
    storyUsers: List<UserProfile>,
    newPicksCount: Int,
    onStoryAvatarClick: (String) -> Unit = {}
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Pack pulse",
                style = AppTextStyles.Heading2,
                color = DarkPastelAnthracite
            )
            Text(
                text = "🔥 $newPicksCount new picks",
                fontFamily = BodyFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = RichPastelCoral
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(storyUsers, key = { it.uid }) { user ->
                PackPulseStoryItem(
                    user = user,
                    onAvatarClick = {
                        Toast.makeText(context, "Scroll to post", Toast.LENGTH_SHORT).show()
                        onStoryAvatarClick(user.uid)
                    }
                )
            }
        }
    }
}

@Composable
private fun PackPulseStoryItem(
    user: UserProfile,
    onAvatarClick: () -> Unit
) {
    val label = remember(user.name) {
        val first = user.name.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        if (first.isNotEmpty()) first else "User"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(78.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            FeedUserAvatar(
                imageUrl = user.avatar,
                displayName = user.name,
                fallbackSeed = user.uid,
                size = 72.dp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = AppTextStyles.BodySmall,
            color = DarkPastelAnthracite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedTopBarRow(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    trustCoins: Int,
    onWalletClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    "Where's the best ceja de bife?",
                    style = AppTextStyles.BodyMedium,
                    color = DarkPastelAnthracite.copy(alpha = 0.45f)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = scheme.primary,
                unfocusedBorderColor = SurfaceMuted,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MutedPastelTeal)
            }
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onWalletClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = MutedPastelTeal,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = trustCoins.toString(),
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = DarkPastelAnthracite
            )
        }
    }
}

@Composable
private fun ExclusiveDealsSection(
    offers: List<AdOffer>,
    onOfferAccept: (AdOffer) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Deals for you",
            style = AppTextStyles.Heading2.copy(fontSize = 20.sp),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(offers, key = { it.id }) { offer ->
                ExclusiveOfferCard(offer = offer, onAccept = { onOfferAccept(offer) })
            }
        }
    }
}

@Composable
fun ExclusiveOfferCard(offer: AdOffer, onAccept: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .width(208.dp)
            .height(136.dp)
            .border(1.5.dp, MutedPastelGold, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = SurfacePastel,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = MutedPastelGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    offer.businessName.ifBlank { offer.title },
                    style = AppTextStyles.BodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = DarkPastelAnthracite
                )
            }
            Text(
                "+${offer.rewardCoins} TC",
                style = AppTextStyles.BodySmall,
                color = MutedPastelTeal,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary
                )
            ) {
                Text(
                    "Accept",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun PackFriendsSection(
    requests: List<PackRequest>,
    users: List<UserProfile>,
    onRequestClick: (PackRequest) -> Unit,
    onRequestAuthorProfileClick: (String) -> Unit,
    onAskPackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Friends are asking",
            style = AppTextStyles.Heading2.copy(fontSize = 20.sp),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        if (requests.isEmpty()) {
            PackAskEmptyCard(onAskPackClick = onAskPackClick)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(requests, key = { it.id }) { req ->
                    PackFriendRequestCard(
                        request = req,
                        author = users.find { it.uid == req.userId } ?: UserProfile(name = "Friend", uid = req.userId),
                        onOpen = { onRequestClick(req) },
                        onAuthorClick = { onRequestAuthorProfileClick(req.userId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PackFriendRequestCard(
    request: PackRequest,
    author: UserProfile,
    onOpen: () -> Unit,
    onAuthorClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .height(168.dp),
        shape = RoundedCornerShape(24.dp),
        color = SoftPastelMint,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = request.text,
                style = AppTextStyles.BodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = DarkPastelAnthracite,
                modifier = Modifier.clickable(onClick = onOpen)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAuthorClick)
            ) {
                FeedUserAvatar(
                    imageUrl = author.avatar,
                    displayName = author.name.ifBlank { "Friend" },
                    fallbackSeed = author.uid,
                    size = 32.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = author.name.ifBlank { "Friend" },
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.55f),
                    modifier = Modifier.weight(1f)
                )
            }
            TextButton(
                onClick = onOpen,
                colors = ButtonDefaults.textButtonColors(contentColor = MutedPastelTeal)
            ) {
                Text("Reply", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PackAskEmptyCard(onAskPackClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfacePastel,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Need a tip? Ask your pack!",
                style = AppTextStyles.Heading2.copy(fontSize = 18.sp)
            )
            Text(
                text = "Friends will suggest where to go and what to try.",
                style = AppTextStyles.BodySmall,
                color = DarkPastelAnthracite.copy(alpha = 0.55f)
            )
            Button(
                onClick = onAskPackClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Ask the pack", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun FeedPostCard(
    post: Post,
    isSaved: Boolean = false,
    onSaveClick: (String) -> Unit = {},
    users: List<UserProfile> = emptyList(),
    onUserProfileClick: (String) -> Unit = {},
    viewerUid: String? = null,
    onAudienceRate: (String, Int) -> Unit = { _, _ -> },
    onOpenPost: ((String) -> Unit)? = null
) {
    val authorUser = users.find { it.uid == post.userId }
    val iconTint = MutedPastelTeal
    val isOwnPost = post.userId.isNotBlank() && viewerUid != null && post.userId == viewerUid
    val canRateOthers = viewerUid != null && !isOwnPost
    val myAudienceStars = viewerUid?.let { post.ratingsByUser[it] } ?: 0
    val audienceAvgRounded = post.averageAudienceRatingStars()
    val cardBorder = if (post.isSponsored) {
        Modifier.border(2.dp, MutedPastelGold, RoundedCornerShape(28.dp))
    } else Modifier

    val openPostModifier = if (onOpenPost != null) {
        Modifier.clickable { onOpenPost(post.id) }
    } else Modifier

    Column(modifier = Modifier.fillMaxWidth()) {
        ConvexCardBox(
            modifier = Modifier
                .fillMaxWidth()
                .then(cardBorder),
            shape = RoundedCornerShape(28.dp),
            elevation = 18.dp
        ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!post.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .then(openPostModifier)
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(SoftPastelMint),
                        error = ColorPainter(SoftPastelMint)
                    )
                    if (post.isSponsored) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MutedPastelGold,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                "Partner",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .then(if (post.imageUrl.isNullOrBlank()) openPostModifier else Modifier),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (post.isSponsored && post.imageUrl.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MutedPastelGold,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            "Partner",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(SurfaceMuted, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        post.category.uppercase(),
                        style = AppTextStyles.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkPastelAnthracite.copy(alpha = 0.7f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = post.userId.isNotBlank()) { onUserProfileClick(post.userId) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeedUserAvatar(
                        imageUrl = authorUser?.avatar,
                        displayName = post.authorName.ifBlank { authorUser?.name.orEmpty() },
                        fallbackSeed = post.userId,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        post.authorName,
                        style = AppTextStyles.BodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkPastelAnthracite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    post.title,
                    style = AppTextStyles.Heading2.copy(fontSize = 20.sp),
                    color = DarkPastelAnthracite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = openPostModifier
                )

                Text(
                    post.description,
                    style = AppTextStyles.BodyMedium,
                    color = DarkPastelAnthracite.copy(alpha = 0.85f),
                    lineHeight = 22.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                post.resourceUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    Spacer(modifier = Modifier.height(10.dp))
                    PostLinkPreviewCard(url)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        audienceAverageRatingLabel(post.ratingsByUser.size),
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.55f),
                        fontWeight = FontWeight.Bold
                    )
                    AudienceAverageStarsDisplay(
                        ratingCount = post.ratingsByUser.size,
                        averageRounded = audienceAvgRounded
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(modifier = Modifier.padding(16.dp)) {
                HorizontalDivider(color = SurfaceMuted)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canRateOthers) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Your rating",
                                style = AppTextStyles.BodySmall,
                                color = DarkPastelAnthracite.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            YourRatingStarsRow(
                                myStars = myAudienceStars,
                                onPickStar = { star -> onAudienceRate(post.id, star) },
                                starSize = 26.dp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onSaveClick(post.id) }) {
                            Icon(
                                if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = { /* Share */ }) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
    }
}

/**
 * Rich link row: favicon (via Google s2), host / Maps title, snippet; tap opens via [openExternalUrl] (Maps app for short links).
 */
@Composable
private fun PostLinkPreviewCard(url: String) {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return
    val uri = remember(trimmed) { runCatching { Uri.parse(trimmed) }.getOrNull() }
    val host = uri?.host.orEmpty()
    val faviconModel = remember(host) {
        if (host.isNotBlank()) "https://www.google.com/s2/favicons?sz=64&domain=$host" else null
    }
    val ctx = LocalContext.current
    val isMapsLink = host.contains("goo.gl", ignoreCase = true) ||
        host.contains("maps.app", ignoreCase = true) ||
        host.contains("google.", ignoreCase = true) && trimmed.contains("maps", ignoreCase = true)
    val headline = when {
        isMapsLink -> "Google Maps"
        host.isNotBlank() -> host.removePrefix("www.")
        else -> "Link"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { ctx.openExternalUrl(trimmed) },
        color = MutedPastelTeal.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (faviconModel != null) {
                AsyncImage(
                    model = faviconModel,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceMuted),
                    contentScale = ContentScale.Fit,
                    error = ColorPainter(SurfaceMuted)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = AppTextStyles.BodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkPastelAnthracite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = trimmed,
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tap to open",
                    style = AppTextStyles.BodySmall,
                    color = MutedPastelTeal,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MutedPastelTeal,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    isSaved: Boolean = false,
    onSaveClick: (String) -> Unit = {},
    users: List<UserProfile> = emptyList(),
    onUserProfileClick: (String) -> Unit = {},
    viewerUid: String? = null,
    onAudienceRate: (String, Int) -> Unit = { _, _ -> },
    onOpenPost: ((String) -> Unit)? = null
) {
    val authorUser = users.find { it.uid == post.userId }
    val iconTint = MutedPastelTeal
    val isOwnPost = post.userId.isNotBlank() && viewerUid != null && post.userId == viewerUid
    val canRateOthers = viewerUid != null && !isOwnPost
    val myAudienceStars = viewerUid?.let { post.ratingsByUser[it] } ?: 0
    val audienceAvgRounded = post.averageAudienceRatingStars()
    val cardBorder = if (post.isSponsored) {
        Modifier.border(2.dp, MutedPastelGold, RoundedCornerShape(32.dp))
    } else Modifier

    Column(modifier = Modifier.fillMaxWidth()) {
        ConvexCardBox(
            modifier = Modifier.fillMaxWidth().then(cardBorder),
            shape = RoundedCornerShape(32.dp),
            elevation = 22.dp
        ) {
            Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clickable(enabled = post.userId.isNotBlank()) { onUserProfileClick(post.userId) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeedUserAvatar(
                    imageUrl = authorUser?.avatar,
                    displayName = post.authorName.ifBlank { authorUser?.name.orEmpty() },
                    fallbackSeed = post.userId,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.authorName, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold)
                    Text(post.authorHandle, style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.5f))
                }
                Text("Today", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f))
            }
            val openPostModifier = if (onOpenPost != null) {
                Modifier.clickable { onOpenPost.invoke(post.id) }
            } else Modifier

            if (!post.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(SoftPastelMint)
                        .then(openPostModifier)
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Post photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(SoftPastelMint),
                        error = ColorPainter(SoftPastelMint)
                    )
                    if (post.isSponsored) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MutedPastelGold.copy(alpha = 0.95f),
                            shadowElevation = 3.dp
                        ) {
                            Text(
                                "Partner",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(20.dp).then(openPostModifier)) {
                Box(modifier = Modifier.background(SurfaceMuted, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(post.category.uppercase(), style = AppTextStyles.BodySmall, fontWeight = FontWeight.Bold, color = DarkPastelAnthracite.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    post.title,
                    style = AppTextStyles.Heading2.copy(fontSize = 20.sp),
                    color = DarkPastelAnthracite,
                    modifier = Modifier.fillMaxWidth()
                )
                if (post.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val locCtx = LocalContext.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val q = Uri.encode(post.location.trim())
                                runCatching {
                                    locCtx.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.google.com/maps/search/?api=1&query=$q")
                                        )
                                    )
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            post.location,
                            style = AppTextStyles.BodyMedium,
                            color = MutedPastelTeal,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MutedPastelTeal.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(post.description, style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.85f), lineHeight = 20.sp)
                post.resourceUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    Spacer(modifier = Modifier.height(10.dp))
                    PostLinkPreviewCard(url)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        audienceAverageRatingLabel(post.ratingsByUser.size),
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.55f),
                        fontWeight = FontWeight.Bold
                    )
                    AudienceAverageStarsDisplay(
                        ratingCount = post.ratingsByUser.size,
                        averageRounded = audienceAvgRounded
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Column(modifier = Modifier.padding(20.dp)) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = SurfaceMuted)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canRateOthers) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Your rating",
                                style = AppTextStyles.BodySmall,
                                color = DarkPastelAnthracite.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            YourRatingStarsRow(
                                myStars = myAudienceStars,
                                onPickStar = { star -> onAudienceRate(post.id, star) },
                                starSize = 26.dp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onSaveClick(post.id) }) {
                            Icon(
                                if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isSaved) "Saved to collection" else "Save",
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { /* Share */ }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = iconTint, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun EmptyFeedMessage() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Text(
            "Recommendations will appear here.",
            style = AppTextStyles.BodyMedium,
            color = DarkPastelAnthracite.copy(alpha = 0.5f)
        )
    }
}
