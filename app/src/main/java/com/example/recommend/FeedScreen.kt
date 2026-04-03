package com.example.recommend

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelGold
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.ConvexCardBox
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
    /** uid -> stars 1..5 from other users */
    val ratingsByUser: Map<String, Int> = emptyMap(),
    /** uids who liked the post (Firestore map `likes` keys) */
    val likesByUser: Set<String> = emptySet()
)

fun Post.averageAudienceRatingStars(): Int? {
    if (ratingsByUser.isEmpty()) return null
    val avg = ratingsByUser.values.average()
    return avg.roundToInt().coerceIn(1, 5)
}

data class PackRequest(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val tags: List<String> = emptyList(),
    val location: String = "",
    val selectedUsers: List<String> = emptyList(),
    val status: String = "active",
    val createdAt: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    posts: List<Post>,
    requests: List<PackRequest> = emptyList(),
    users: List<UserProfile> = emptyList(),
    /** Used for Friend's pick, radar & Social pulse (not your own posts). */
    followingUserIds: Set<String> = emptySet(),
    savedPostIds: Set<String> = emptySet(),
    activeOffers: List<AdOffer> = emptyList(),
    onOfferAccept: (AdOffer) -> Unit = {},
    onAskPackClick: () -> Unit,
    onRequestClick: (PackRequest) -> Unit,
    onSaveClick: (String) -> Unit,
    onUserProfileClick: (String) -> Unit = {},
    onRequestAuthorProfileClick: (String) -> Unit = {},
    viewerUid: String? = null,
    onAudienceRate: (String, Int) -> Unit = { _, _ -> },
    onOpenPost: ((String) -> Unit)? = null,
    onLikeToggle: (String) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val networkPosts = remember(posts, viewerUid, followingUserIds) {
        val uid = viewerUid
        when {
            uid.isNullOrBlank() -> posts.filter { it.userId.isNotBlank() }
            followingUserIds.isNotEmpty() -> posts.filter { it.userId in followingUserIds }
            else -> posts.filter { it.userId.isNotBlank() && it.userId != uid }
        }
    }

    fun launchMapsExternal(query: String) {
        val q = query.ifBlank { "restaurants" }
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(q)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    var pendingMapQuery by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent
        ) { paddingValues ->

            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { SearchBarWidget() }

            item {
                LiquidRadar(
                    posts = networkPosts,
                    users = users,
                    onUserProfileClick = onUserProfileClick,
                    onOpenInMaps = { pendingMapQuery = it }
                )
            }

            if (requests.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = RichPastelCoral, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Signals from your pack", style = AppTextStyles.Heading2)
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(requests) { req ->
                                val author = users.find { it.uid == req.userId } ?: UserProfile(name = "Friend")

                                Box(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(180.dp)
                                        .shadow(18.dp, RoundedCornerShape(32.dp))
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF3A3A3A), Color(0xFF1A1A1A), Color(0xFF2C2C2C))
                                            )
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp).fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable(enabled = req.userId.isNotBlank()) {
                                                    onRequestAuthorProfileClick(req.userId)
                                                }
                                        ) {
                                            AsyncImage(
                                                model = author.avatar.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${author.uid}" },
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceMuted)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                "${author.name.split(" ")[0]} is looking for",
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        Text(
                                            req.text,
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 24.sp,
                                            modifier = Modifier.clickable { onRequestClick(req) }
                                        )

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                req.tags.take(2).forEach { tag ->
                                                    Box(
                                                        modifier = Modifier
                                                            .background(MutedPastelTeal.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(tag, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Button(
                                                onClick = { onRequestClick(req) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = scheme.primary,
                                                    contentColor = scheme.onPrimary
                                                ),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                                modifier = Modifier.height(32.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Help", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                BentoBoxGrid(
                    networkPosts = networkPosts,
                    hasSubscriptions = followingUserIds.isNotEmpty(),
                    onAskPackClick = onAskPackClick,
                    onOpenMapsTrending = { pendingMapQuery = "popular restaurants" }
                )
            }

            if (activeOffers.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(activeOffers, key = { it.id }) { offer ->
                            ExclusiveOfferCard(offer = offer, onAccept = { onOfferAccept(offer) })
                        }
                    }
                }
            }

            item {
                Text(
                    text = "All updates",
                    style = AppTextStyles.Heading2,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            if (posts.isEmpty()) {
                item { EmptyFeedMessage() }
            } else {
                items(posts) { post ->
                    PostCard(
                        post = post,
                        isSaved = savedPostIds.contains(post.id),
                        onSaveClick = onSaveClick,
                        users = users,
                        onUserProfileClick = onUserProfileClick,
                        viewerUid = viewerUid,
                        onAudienceRate = onAudienceRate,
                        onOpenPost = onOpenPost,
                        onLikeToggle = onLikeToggle
                    )
                }
            }
        }
        }

        pendingMapQuery?.let { query ->
            AlertDialog(
                onDismissRequest = { pendingMapQuery = null },
                title = {
                    Text("Open maps?", style = AppTextStyles.Heading2.copy(fontSize = 20.sp))
                },
                text = {
                    Text(
                        "Google Maps will open. Stay here — tap Cancel. To return to TrustList from Maps, use the system Back button.",
                        style = AppTextStyles.BodyMedium,
                        color = DarkPastelAnthracite.copy(alpha = 0.75f)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            launchMapsExternal(query)
                            pendingMapQuery = null
                        }
                    ) {
                        Text("Open", fontWeight = FontWeight.Bold, color = MutedPastelTeal)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingMapQuery = null }) {
                        Text("Cancel", fontWeight = FontWeight.Medium)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarWidget() {
    ConvexCardBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        elevation = 22.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = MutedPastelTeal)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Where's the best ceja de bife?",
                style = AppTextStyles.BodyMedium,
                color = DarkPastelAnthracite.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
fun LiquidRadar(
    posts: List<Post>,
    users: List<UserProfile> = emptyList(),
    onUserProfileClick: (String) -> Unit = {},
    onOpenInMaps: (String) -> Unit = {}
) {
    val radarPosts = posts.take(3)
    val mapQuery = radarPosts.firstOrNull()?.location?.takeIf { it.isNotBlank() }
        ?: radarPosts.firstOrNull()?.title?.takeIf { it.isNotBlank() }
        ?: "near me"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.92f)
                    .clip(
                        RoundedCornerShape(
                            topStartPercent = 60,
                            topEndPercent = 40,
                            bottomEndPercent = 50,
                            bottomStartPercent = 50
                        )
                    )
                    .background(SurfaceMuted.copy(alpha = 0.9f))
                    .drawBehind {
                        val dotRadius = 2.5f
                        val spacing = 50f
                        var x = 0f
                        while (x < size.width) {
                            var y = 0f
                            while (y < size.height) {
                                drawCircle(color = MutedPastelTeal.copy(alpha = 0.35f), radius = dotRadius, center = Offset(x, y))
                                y += spacing
                            }
                            x += spacing
                        }
                    }
            )

            if (radarPosts.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MutedPastelTeal.copy(alpha = 0.6f), modifier = Modifier.size(32.dp).alpha(0.85f))
                    Text("Radar is empty. Follow friends!", style = AppTextStyles.BodySmall, color = MutedPastelTeal)
                }
            } else {
                val positions = listOf(
                    Alignment.TopStart to Pair(16.dp, 24.dp),
                    Alignment.CenterEnd to Pair((-16).dp, (-20).dp),
                    Alignment.BottomCenter to Pair((-40).dp, (-32).dp)
                )

                radarPosts.forEachIndexed { index, post ->
                    val pos = positions[index % positions.size]
                    val author = users.find { it.uid == post.userId }
                    val pinAvatar = author?.avatar?.takeIf { it.isNotEmpty() }
                        ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=${post.userId}"
                    Box(modifier = Modifier.align(pos.first).offset(x = pos.second.first, y = pos.second.second)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SurfacePastel.copy(alpha = 0.98f),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .clickable(enabled = post.userId.isNotBlank()) { onUserProfileClick(post.userId) }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = pinAvatar,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp).clip(CircleShape).background(SurfaceMuted),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    val shortTitle = if (post.title.length > 12) post.title.take(12) + "..." else post.title
                                    Text(
                                        "${post.authorName.split(" ")[0]} rec. \"$shortTitle\"",
                                        style = AppTextStyles.BodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkPastelAnthracite
                                    )
                                }
                            }
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MutedPastelTeal, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
        TextButton(onClick = { onOpenInMaps(mapQuery) }) {
            Icon(Icons.Filled.Map, contentDescription = null, tint = MutedPastelTeal, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Open in Maps", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, color = MutedPastelTeal)
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
                "+${offer.rewardCoins} TrustCoins",
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
fun BentoBoxGrid(
    networkPosts: List<Post>,
    hasSubscriptions: Boolean,
    onAskPackClick: () -> Unit,
    onOpenMapsTrending: () -> Unit = {}
) {
    val highlightPost = networkPosts
        .firstOrNull { !it.imageUrl.isNullOrBlank() }
        ?: networkPosts.firstOrNull()
    val scheme = MaterialTheme.colorScheme

    val socialSubtitle = when {
        networkPosts.isEmpty() && hasSubscriptions -> "No posts from people you follow yet."
        networkPosts.isEmpty() -> "Follow people in Community to see their picks here."
        hasSubscriptions -> "${networkPosts.size} places from people you follow."
        else -> "${networkPosts.size} places from others on TrustList."
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (highlightPost != null) {
            val sponsorModifier = if (highlightPost.isSponsored) {
                Modifier.border(2.dp, MutedPastelGold, RoundedCornerShape(32.dp))
            } else Modifier

            ConvexCardBox(
                modifier = Modifier.fillMaxWidth().height(260.dp).then(sponsorModifier),
                shape = RoundedCornerShape(32.dp),
                elevation = 24.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!highlightPost.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = highlightPost.imageUrl,
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize().background(SoftPastelMint),
                            contentScale = ContentScale.Crop,
                            placeholder = ColorPainter(SoftPastelMint),
                            error = ColorPainter(SoftPastelMint)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF4A4A4A), Color(0xFF6E6E6E), Color(0xFF2C2C2C))
                                    )
                                )
                        )
                    }
                    if (highlightPost.isSponsored) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MutedPastelGold.copy(alpha = 0.95f),
                            shadowElevation = 4.dp
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
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)), startY = 250f)))
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.White).border(1.dp, Color.White, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text("FRIEND'S PICK", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(highlightPost.title, color = Color.White, style = AppTextStyles.Heading2.copy(fontSize = 24.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(highlightPost.description, color = Color.White.copy(alpha = 0.85f), style = AppTextStyles.BodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        } else {
            ConvexCardBox(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                shape = RoundedCornerShape(32.dp),
                elevation = 20.dp
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No friend picks yet — follow people or wait for their recommendations.",
                        style = AppTextStyles.BodyMedium,
                        color = DarkPastelAnthracite.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        ConvexCardBox(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), elevation = 22.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Social pulse", style = AppTextStyles.Heading2.copy(fontSize = 18.sp))
                    Text(
                        socialSubtitle,
                        style = AppTextStyles.BodyMedium,
                        color = DarkPastelAnthracite.copy(alpha = 0.55f)
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MutedPastelTeal.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ConvexCardBox(
                modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onAskPackClick() },
                shape = RoundedCornerShape(32.dp),
                elevation = 22.dp
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(scheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Text("Ask your\npack", style = AppTextStyles.Heading2.copy(fontSize = 18.sp), lineHeight = 22.sp)
                        Text("Send a signal", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.55f))
                    }
                }
            }

            ConvexCardBox(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clickable { onOpenMapsTrending() },
                shape = RoundedCornerShape(32.dp),
                elevation = 22.dp
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceMuted), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = MutedPastelTeal)
                    }
                    Column {
                        Text("Explore\nmaps", style = AppTextStyles.Heading2.copy(fontSize = 18.sp), lineHeight = 22.sp)
                        Text("Nearby picks", color = RichPastelCoral, style = AppTextStyles.BodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
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
    onOpenPost: ((String) -> Unit)? = null,
    onLikeToggle: (String) -> Unit = {}
) {
    val authorUser = users.find { it.uid == post.userId }
    val avatarModel = authorUser?.avatar?.takeIf { it.isNotEmpty() }
        ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=${post.userId.ifEmpty { post.authorName }}"
    val iconTint = MutedPastelTeal
    val isOwnPost = post.userId.isNotBlank() && viewerUid != null && post.userId == viewerUid
    val canRateOthers = viewerUid != null && !isOwnPost
    val myAudienceStars = viewerUid?.let { post.ratingsByUser[it] } ?: 0
    val audienceAvg = post.averageAudienceRatingStars()
    val cardBorder = if (post.isSponsored) {
        Modifier.border(2.dp, MutedPastelGold, RoundedCornerShape(32.dp))
    } else Modifier

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
                AsyncImage(
                    model = avatarModel,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceMuted),
                    contentScale = ContentScale.Crop
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(post.title, style = AppTextStyles.Heading2.copy(fontSize = 20.sp), modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Author", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f), fontSize = 10.sp)
                        Row {
                            repeat(post.rating.coerceIn(1, 5)) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = MutedPastelGold, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(post.location, style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.65f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(post.description, style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.85f), lineHeight = 20.sp)
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (audienceAvg != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Community (${post.ratingsByUser.size})",
                            style = AppTextStyles.BodySmall,
                            color = DarkPastelAnthracite.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            repeat(audienceAvg) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = MutedPastelTeal, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (canRateOthers) {
                    Text(
                        "Your rating",
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.55f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (star in 1..5) {
                            val selected = myAudienceStars >= star
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Rate $star",
                                tint = if (selected) MutedPastelGold else SurfaceMuted,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { onAudienceRate(post.id, star) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = SurfaceMuted)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val liked = viewerUid != null && post.likesByUser.contains(viewerUid)
                        IconButton(
                            onClick = { onLikeToggle(post.id) },
                            enabled = viewerUid != null
                        ) {
                            Icon(
                                if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (liked) "Unlike" else "Like",
                                tint = if (liked) RichPastelCoral else iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            "${post.likesByUser.size}",
                            style = AppTextStyles.BodyMedium,
                            color = DarkPastelAnthracite.copy(alpha = 0.5f)
                        )
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

@Composable
fun EmptyFeedMessage() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Text("Recommendations will appear here.", style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.5f))
    }
}
