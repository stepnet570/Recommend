package com.example.recommend

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    val isSponsored: Boolean = false
)

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
    savedPostIds: Set<String> = emptySet(),
    onAskPackClick: () -> Unit,
    onRequestClick: (PackRequest) -> Unit,
    onSaveClick: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

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

            item { LiquidRadar(posts = posts) }

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
                                        .clickable { onRequestClick(req) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp).fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(
                                                model = author.avatar.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${author.uid}" },
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceMuted)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                "${author.name.split(" ")[0]} is looking for",
                                                color = MutedPastelTeal.copy(alpha = 0.85f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        Text(req.text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp)

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
                                                Text("Help", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { BentoBoxGrid(posts = posts, onAskPackClick = onAskPackClick) }

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
                        onSaveClick = onSaveClick
                    )
                }
            }
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
fun LiquidRadar(posts: List<Post>) {
    val radarPosts = posts.take(3)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.9f)
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
                Box(modifier = Modifier.align(pos.first).offset(x = pos.second.first, y = pos.second.second)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(16.dp), color = SurfacePastel.copy(alpha = 0.98f), shadowElevation = 4.dp, modifier = Modifier.padding(bottom = 4.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(SurfaceMuted))
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
}

@Composable
fun BentoBoxGrid(posts: List<Post>, onAskPackClick: () -> Unit) {
    val highlightPost = posts.find { it.imageUrl != null } ?: posts.firstOrNull()
    val scheme = MaterialTheme.colorScheme

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
                    if (highlightPost.imageUrl != null) {
                        AsyncImage(model = highlightPost.imageUrl, contentDescription = "Photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.linearGradient(listOf(Color(0xFF4A4A4A), Color(0xFF6E6E6E), Color(0xFF2C2C2C)))
                            )
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)), startY = 250f)))
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                        if (highlightPost.isSponsored) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("TrustList", color = RichPastelCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Text("PARTNER", color = MutedPastelGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
        }

        ConvexCardBox(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), elevation = 22.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Social pulse", style = AppTextStyles.Heading2.copy(fontSize = 18.sp))
                    Text(
                        "${posts.size} places in your network.",
                        style = AppTextStyles.BodyMedium,
                        color = DarkPastelAnthracite.copy(alpha = 0.55f)
                    )
                }
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

            ConvexCardBox(modifier = Modifier.weight(1f).aspectRatio(1f), shape = RoundedCornerShape(32.dp), elevation = 22.dp) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceMuted), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = MutedPastelTeal)
                    }
                    Column {
                        Text("Trending\nnow", style = AppTextStyles.Heading2.copy(fontSize = 18.sp), lineHeight = 22.sp)
                        Text("#DinnerInCordoba", color = RichPastelCoral, style = AppTextStyles.BodySmall, fontWeight = FontWeight.Bold)
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
    onSaveClick: (String) -> Unit = {}
) {
    val iconTint = MutedPastelTeal
    val cardBorder = if (post.isSponsored) {
        Modifier.border(2.dp, MutedPastelGold, RoundedCornerShape(32.dp))
    } else Modifier

    ConvexCardBox(
        modifier = Modifier.fillMaxWidth().then(cardBorder),
        shape = RoundedCornerShape(32.dp),
        elevation = 22.dp
    ) {
        Column {
            if (post.isSponsored) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceMuted)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TrustList", color = RichPastelCoral, style = AppTextStyles.BodySmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("PARTNER", color = MutedPastelGold, style = AppTextStyles.BodySmall, fontWeight = FontWeight.Bold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceMuted))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.authorName, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold)
                    Text(post.authorHandle, style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.5f))
                }
                Text("Today", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f))
            }
            if (post.imageUrl != null) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post photo",
                    modifier = Modifier.fillMaxWidth().height(200.dp).background(SurfaceMuted),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Box(modifier = Modifier.background(SurfaceMuted, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(post.category.uppercase(), style = AppTextStyles.BodySmall, fontWeight = FontWeight.Bold, color = DarkPastelAnthracite.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(post.title, style = AppTextStyles.Heading2.copy(fontSize = 20.sp), modifier = Modifier.weight(1f))
                    Row {
                        repeat(post.rating) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = MutedPastelGold, modifier = Modifier.size(16.dp))
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = SurfaceMuted)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { /* Like */ }) {
                            Icon(Icons.Filled.FavoriteBorder, contentDescription = "Like", tint = iconTint, modifier = Modifier.size(20.dp))
                        }
                        Text("12", style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.5f))
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
                        Text("Reply", style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.5f))
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
