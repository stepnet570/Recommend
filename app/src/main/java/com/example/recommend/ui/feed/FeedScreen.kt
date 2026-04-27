package com.example.recommend.ui.feed

import com.example.recommend.*
import com.example.recommend.ui.theme.*
import com.example.recommend.data.model.*

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.roundToInt
import java.util.Locale
import java.util.concurrent.TimeUnit
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
import androidx.compose.ui.graphics.Brush
import com.example.recommend.ui.theme.PrimaryGradient
import com.example.recommend.ui.theme.AppDark
import com.example.recommend.ui.theme.AppBackground
import com.example.recommend.ui.theme.AppViolet
import com.example.recommend.ui.theme.AppMuted
import com.example.recommend.ui.theme.AppTeal
import com.example.recommend.ui.theme.AppGold
import com.example.recommend.ui.theme.AppWhite
import com.example.recommend.ui.theme.PrimaryGradientLinear



/** Label for the collective trust score; [ratingCount] is how many people rated (may be 0). */
private fun audienceAverageRatingLabel(ratingCount: Int): String =
    if (ratingCount == 0) "No ratings yet" else "$ratingCount ratings"

/**
 * TrustScoreRing — premium replacement for star ratings.
 * Displays a circular arc filled proportionally to [score] (0..10),
 * rendered with the Violet→Teal gradient from the design system.
 */
@Composable
fun TrustScoreRing(
    score: Float,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp
) {
    val sweepAngle = (score / 10f).coerceIn(0f, 1f) * 300f
    val trackColor = Color(0xFFE8E6E2)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Track arc (background)
            drawArc(
                color = trackColor,
                startAngle = 120f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            // Filled arc — Violet→Teal via sweep gradient
            if (sweepAngle > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colorStops = arrayOf(
                            0.0f to AppViolet,
                            1.0f to AppTeal
                        ),
                        center = Offset(this.size.width / 2f, this.size.height / 2f)
                    ),
                    startAngle = 120f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (score <= 0f) "—" else if (score % 1f == 0f) score.toInt().toString() else String.format("%.1f", score),
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.ExtraBold,  // wght=800, matches HTML SVG font-weight="800"
                fontSize = (size.value * 0.26f).sp,
                color = AppDark,
                maxLines = 1
            )
        }
    }
}

/**
 * Interactive trust score picker — replaces YourRatingStarsRow.
 * Shows 5 dot-buttons that map to scores 2, 4, 6, 8, 10.
 */
@Composable
private fun YourTrustScorePicker(
    myStars: Int,       // existing 1-5 star value stored in Firestore
    onPickStar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (star in 1..5) {
            val filled = myStars >= star
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) AppTeal.copy(alpha = 0.15f)
                        else Color(0xFFE8E6E2)
                    )
                    .clickable { onPickStar(star) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${star * 2}",
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (filled) AppTeal else AppMuted
                )
            }
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    posts: List<Post>,
    requests: List<PackRequest> = emptyList(),
    users: List<UserProfile> = emptyList(),
    followingUserIds: Set<String> = emptySet(),
    savedPostIds: Set<String> = emptySet(),
    activeOffers: List<AdOffer> = emptyList(),
    acceptedOfferIds: Set<String> = emptySet(),
    trustCoins: Int = 0,
    onOfferClick: (AdOffer) -> Unit = {},
    onAskPackClick: () -> Unit,
    onRequestClick: (PackRequest) -> Unit,
    /** Open request detail (e.g. own signal card). */
    onSignalRequestOpen: (PackRequest) -> Unit = {},
    /** Help on a friend's request: open add-pick flow for [PackRequest.id]. */
    onHelpRequest: (PackRequest) -> Unit = {},
    onSaveClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    onUserProfileClick: (String) -> Unit = {},
    onRequestAuthorProfileClick: (String) -> Unit = {},
    viewerUid: String? = null,
    onAudienceRate: (String, Int) -> Unit = { _, _ -> },
    onOpenPost: ((String) -> Unit)? = null,
    onWalletClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val currentUserName = remember(viewerUid, users) {
        users.find { it.uid == viewerUid }?.name
            ?.trim()?.split(Regex("\\s+"))?.firstOrNull()
            .orEmpty()
    }

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

    Scaffold(containerColor = AppBackground) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                FeedGreetingRow(
                    userName = currentUserName,
                    trustCoins = trustCoins,
                    onWalletClick = onWalletClick
                )
            }
            item {
                FeedSearchBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it }
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
                        onAuthorClick = onUserClick,
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
                        acceptedOfferIds = acceptedOfferIds,
                        onOfferClick = onOfferClick
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
    onAuthorClick: (String) -> Unit = {},
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
                    onAuthorClick = onAuthorClick,
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
    onAuthorClick: (String) -> Unit = {},
    onRecipientProfileClick: (String) -> Unit = {}
) {
    val isMine = viewerUid != null && request.authorId == viewerUid
    val authorName = authorUser?.name?.takeIf { it.isNotBlank() } ?: "Member"
    val recipients = request.selectedUsers.distinct().filter { it.isNotBlank() }

    Box(
        modifier = Modifier
            .width(PackSignalCardWidth)
            .height(PackSignalCardHeight)   // fixed height — both variants identical
            .clip(RoundedCornerShape(24.dp))
            .background(brush = PrimaryGradientLinear)
            .clickable { onSignalRequestOpen(request) }
    ) {
        // Decorative circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(x = 210.dp, y = (-20).dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Tag pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.20f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("🐺", fontSize = 12.sp)
                    Text(
                        text = if (isMine) "My call" else "Pack call",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                // User row — always present, same height
                // Mine: show who is being asked (recipients)
                // Others: show who is asking (author)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable {
                        if (isMine) { /* no-op or open recipients */ }
                        else onAuthorClick(request.userId)
                    }
                ) {
                    if (isMine) {
                        // Show first 3 recipients or "+" placeholder
                        if (recipients.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Send to pack",
                                fontFamily = BodyFontFamily,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        } else {
                            recipients.take(3).forEach { uid ->
                                val profile = users.find { it.uid == uid }
                                if (profile != null) {
                                    FeedUserAvatar(
                                        imageUrl = profile.avatar,
                                        displayName = profile.name,
                                        fallbackSeed = uid,
                                        size = 22.dp,
                                        modifier = Modifier.clickable { onRecipientProfileClick(uid) }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(Color.White.copy(alpha = 0.25f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            if (recipients.size > 3) {
                                Text(
                                    text = "+${recipients.size - 3}",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    } else {
                        // Show author
                        if (authorUser != null) {
                            FeedUserAvatar(
                                imageUrl = authorUser.avatar,
                                displayName = authorName,
                                fallbackSeed = request.userId,
                                size = 22.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "$authorName is asking",
                            fontFamily = BodyFontFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Question text
                Text(
                    text = request.text,
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 21.sp
                )
            }

            // Action button pinned to bottom
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .clickable { if (isMine) onSignalRequestOpen(request) else onHelpRequest(request) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = if (isMine) "My picks" else "Reply",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AppViolet
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = AppViolet,
                        modifier = Modifier.size(14.dp)
                    )
                }
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
                color = AppDark
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
        modifier = Modifier.width(72.dp)
    ) {
        // Gradient ring — Violet → Teal like Instagram stories
        Box(
            modifier = Modifier
                .size(68.dp)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            // Gradient ring background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = PrimaryGradientLinear, shape = CircleShape)
            )
            // White gap between ring and avatar
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(AppBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                FeedUserAvatar(
                    imageUrl = user.avatar,
                    displayName = user.name,
                    fallbackSeed = user.uid,
                    size = 56.dp
                )
            }
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

/** Greeting header row — "Good morning, [Name] 👋" + coins chip */
@Composable
private fun FeedGreetingRow(
    userName: String,
    trustCoins: Int,
    onWalletClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Good morning,",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = AppMuted
            )
            Text(
                text = if (userName.isNotBlank()) "$userName 👋" else "Hey there 👋",
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = AppDark
            )
        }

        // Coins chip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(AppGold.copy(alpha = 0.10f))
                .clickable(onClick = onWalletClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = AppGold,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = trustCoins.toString(),
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = AppGold
            )
            Text(
                text = "TC",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = AppGold.copy(alpha = 0.7f)
            )
        }
    }
}

/** Standalone search bar below the greeting */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    AppTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = "Search the pack...",
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        containerColor = AppWhite,
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = AppMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

@Composable
private fun ExclusiveDealsSection(
    offers: List<AdOffer>,
    acceptedOfferIds: Set<String> = emptySet(),
    onOfferClick: (AdOffer) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Deals for you",
            style = AppTextStyles.Heading2.copy(fontSize = 20.sp),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            offers.forEach { offer ->
                DealCard(
                    offer = offer,
                    isAccepted = offer.id in acceptedOfferIds,
                    onClick = { onOfferClick(offer) }
                )
            }
        }
    }
}

@Composable
fun DealCard(
    offer: AdOffer,
    isAccepted: Boolean = false,
    onClick: () -> Unit
) {
    ConvexCardBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = 22.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: megaphone icon + businessName + status badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = MutedPastelGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = offer.businessName,
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAccepted) MutedPastelTeal.copy(alpha = 0.12f)
                            else MutedPastelGold.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isAccepted) "✓ Accepted" else "Active",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = AppTextStyles.BodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAccepted) MutedPastelTeal else MutedPastelGold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Row 2: offer title
            Text(
                text = offer.title,
                style = AppTextStyles.BodyMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DarkPastelAnthracite
            )

            Spacer(Modifier.height(12.dp))

            // Row 3: three info chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Chip 1: reward
                InfoChip(
                    icon = {
                        Icon(
                            Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MutedPastelGold,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    text = "+${offer.rewardCoins} TC",
                    bgColor = MutedPastelGold.copy(alpha = 0.10f),
                    textColor = MutedPastelGold
                )
                // Chip 2: duration / days left
                val daysLeft = if (offer.expiresAt > 0) {
                    val d = TimeUnit.MILLISECONDS.toDays(
                        offer.expiresAt - System.currentTimeMillis()
                    ).toInt()
                    if (d > 0) "$d days left" else "Last day"
                } else {
                    "${offer.durationDays}d"
                }
                InfoChip(
                    icon = {
                        Icon(
                            Icons.Filled.Timer,
                            contentDescription = null,
                            tint = RichPastelCoral,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    text = daysLeft,
                    bgColor = RichPastelCoral.copy(alpha = 0.10f),
                    textColor = RichPastelCoral
                )
                // Chip 3: min trust score
                InfoChip(
                    icon = {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MutedPastelTeal,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    text = "Score ${String.format("%.1f", offer.minTrustScore)}+",
                    bgColor = MutedPastelTeal.copy(alpha = 0.10f),
                    textColor = MutedPastelTeal
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: @Composable () -> Unit,
    text: String,
    bgColor: Color,
    textColor: Color
) {
    Surface(shape = RoundedCornerShape(8.dp), color = bgColor) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(
                text = text,
                style = AppTextStyles.BodySmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
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

/**
 * "Зов стаи" card — full Violet→Teal gradient background,
 * matching the design screenshot.
 */
@Composable
private fun PackFriendRequestCard(
    request: PackRequest,
    author: UserProfile,
    onOpen: () -> Unit,
    onAuthorClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(brush = PrimaryGradientLinear)
            .clickable(onClick = onOpen)
    ) {
        // Decorative circles for depth
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 200.dp, y = (-30).dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = 220.dp, y = 80.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tag pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.20f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🐺", fontSize = 13.sp)
                Text(
                    text = "Pack call",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            // Question text
            Text(
                text = request.text,
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            // Author + answer count
            Text(
                text = "${author.name.ifBlank { "Friend" }} is asking",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.clickable(onClick = onAuthorClick)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Reply button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Reply",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AppViolet
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = AppViolet,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
                modifier = Modifier
                    .background(
                        brush = PrimaryGradient,
                        shape = RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Text("Ask the pack", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ─── Category → emoji + gradient ─────────────────────────────────────────────

data class CategoryStyle(val emoji: String, val gradient: Brush)

fun categoryStyle(category: String): CategoryStyle {
    val c = category.trim().lowercase()
    return when {
        c.contains("coffee") || c.contains("кофе") -> CategoryStyle(
            emoji = "☕",
            gradient = Brush.linearGradient(
                listOf(Color(0xFFF5E6D3), Color(0xFFEDD5B3)),
                start = Offset(0f, 0f), end = Offset(500f, 500f)
            )
        )
        c.contains("food") || c.contains("еда") || c.contains("ramen") || c.contains("рест") -> CategoryStyle(
            emoji = "🍜",
            gradient = Brush.linearGradient(
                listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)),
                start = Offset(0f, 0f), end = Offset(500f, 500f)
            )
        )
        c.contains("place") || c.contains("bar") || c.contains("бар") || c.contains("resto") -> CategoryStyle(
            emoji = "🗺️",
            gradient = Brush.linearGradient(
                listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB)),
                start = Offset(0f, 0f), end = Offset(500f, 500f)
            )
        )
        else -> CategoryStyle(
            emoji = "⭐",
            gradient = Brush.linearGradient(
                listOf(AppViolet.copy(alpha = 0.12f), AppTeal.copy(alpha = 0.12f)),
                start = Offset(0f, 0f), end = Offset(500f, 500f)
            )
        )
    }
}

// ─── FeedPostCard — точно по HTML .post-card ─────────────────────────────────

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
    val authorName = post.authorName.ifBlank { authorUser?.name.orEmpty().ifBlank { "Pack member" } }
    val isOwnPost = viewerUid != null && post.userId == viewerUid
    val myRating = viewerUid?.let { post.ratingsByUser[it] } ?: 0
    var showRatingDialog by remember { mutableStateOf(false) }

    val trustScore = remember(post.ratingsByUser) {
        if (post.ratingsByUser.isEmpty()) 0f
        else ((post.averageAudienceRatingStars() ?: 0) * 2).toFloat()
    }.coerceIn(0f, 10f)

    val catStyle = remember(post.category) { categoryStyle(post.category) }

    // post-card: white, radius 20dp, shadow, thin border
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppWhite,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
            .then(if (onOpenPost != null) Modifier.clickable { onOpenPost(post.id) } else Modifier)
    ) {
        Column {

            // ── post-img: 160dp, gradient or real image ───────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                if (!post.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(SurfaceMuted),
                        error = ColorPainter(SurfaceMuted)
                    )
                } else {
                    // Gradient placeholder with category emoji
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(catStyle.gradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(catStyle.emoji, fontSize = 52.sp)
                    }
                }

                // sponsored-badge
                if (post.isSponsored) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(AppGold, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Sponsored",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = AppWhite
                        )
                    }
                }
            }

            // ── post-body ─────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // loc-tag: "📍 location"
                if (post.location.isNotBlank()) {
                    Text(
                        text = "📍 ${post.location}",
                        fontFamily = BodyFontFamily,
                        fontSize = 11.sp,
                        color = AppMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // post-header: [pill + title] | [TrustScoreRing]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        // pill-violet: category
                        if (post.category.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(AppViolet.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${catStyle.emoji} ${post.category}",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = AppViolet
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        // post-title: Syne 15/700
                        Text(
                            text = post.title,
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                    // TrustScoreRing — кликабельный, открывает рейтинг
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = !isOwnPost && viewerUid != null) {
                                showRatingDialog = true
                            }
                    ) {
                        TrustScoreRing(
                            score = trustScore,
                            size = 44.dp,
                            strokeWidth = 3.5.dp
                        )
                    }
                }

                // post-desc
                if (post.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = post.description,
                        fontFamily = BodyFontFamily,
                        fontSize = 13.sp,
                        color = AppMuted,
                        lineHeight = 19.5.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Link preview (kept from original)
                post.resourceUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    Spacer(Modifier.height(8.dp))
                    PostLinkPreviewCard(url)
                }

                Spacer(Modifier.height(12.dp))

                // post-footer: author-row | actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // author-row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(enabled = post.userId.isNotBlank()) {
                            onUserProfileClick(post.userId)
                        }
                    ) {
                        // av: 28dp gradient circle with initial
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    Brush.linearGradient(listOf(AppViolet, AppTeal)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = authorName.take(1).uppercase(),
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = AppWhite
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = authorName,
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = AppDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Pack member",
                                fontFamily = BodyFontFamily,
                                fontSize = 11.sp,
                                color = AppMuted
                            )
                        }
                    }

                    // Подсказка: нажми на кольцо чтобы оценить
                    if (!isOwnPost && viewerUid != null) {
                        Text(
                            text = if (myRating > 0) "★".repeat(myRating) + "☆".repeat(5 - myRating)
                                   else "Rate ↑",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = if (myRating > 0) AppGold else AppMuted,
                            modifier = Modifier.clickable { showRatingDialog = true }
                        )
                    }
                }
            }
        }
    }

    // ── Rating dialog ──────────────────────────────────────────────────────────
    if (showRatingDialog) {
        RatingDialog(
            postTitle = post.title,
            currentRating = myRating,
            onDismiss = { showRatingDialog = false },
            onRate = { stars ->
                onAudienceRate(post.id, stars)
                showRatingDialog = false
            }
        )
    }
}

// ─── RatingDialog ─────────────────────────────────────────────────────────────

@Composable
fun RatingDialog(
    postTitle: String,
    currentRating: Int,
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(currentRating) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppWhite,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Rate this pick",
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AppDark
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = postTitle,
                    fontFamily = BodyFontFamily,
                    fontSize = 13.sp,
                    color = AppMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                // 5 звёзд
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        Text(
                            text = if (star <= selected) "★" else "☆",
                            fontSize = 36.sp,
                            color = if (star <= selected) AppGold else AppMuted.copy(alpha = 0.4f),
                            modifier = Modifier.clickable { selected = star }
                        )
                    }
                }
                if (selected > 0) {
                    Text(
                        text = when (selected) {
                            1 -> "Not worth it"
                            2 -> "Below average"
                            3 -> "Decent"
                            4 -> "Really good"
                            5 -> "Must try!"
                            else -> ""
                        },
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = AppViolet,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected > 0) PrimaryGradientLinear
                        else Brush.horizontalGradient(listOf(AppDisabled, AppDisabled))
                    )
                    .clickable(enabled = selected > 0) { onRate(selected) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Confirm rating",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (selected > 0) AppWhite else AppOnDisabled
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    color = AppMuted
                )
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
/**
 * Rich link row: favicon (via Google s2), host / Maps title, snippet; tap opens via [openExternalUrl] (Maps app for short links).
 */
@Composable
private fun PostLinkPreviewCard(url: String) {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return
    val uri = remember(trimmed) { runCatching { trimmed.toUri() }.getOrNull() }
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
                    color = AppDark,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.background(SurfaceMuted, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(post.category.uppercase(), style = AppTextStyles.BodySmall, fontWeight = FontWeight.Bold, color = DarkPastelAnthracite.copy(alpha = 0.7f))
                    }
                    if (post.isSponsored) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    PrimaryGradientLinear
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "✦ Sponsored",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
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
                                            "https://www.google.com/maps/search/?api=1&query=$q".toUri()
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
                            color = AppDark,
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

            // Trust Score row (second instance — alternate card layout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Trust Score",
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.45f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        audienceAverageRatingLabel(post.ratingsByUser.size),
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.35f)
                    )
                }
                val trustScore2 = if (post.ratingsByUser.isEmpty()) {
                    0f
                } else {
                    ((audienceAvgRounded ?: 0) * 2).toFloat()
                }
                TrustScoreRing(
                    score = trustScore2.coerceIn(0f, 10f),
                    size = 52.dp,
                    strokeWidth = 4.dp
                )
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
                                color = DarkPastelAnthracite.copy(alpha = 0.45f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            YourTrustScorePicker(
                                myStars = myAudienceStars,
                                onPickStar = { star -> onAudienceRate(post.id, star) }
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
