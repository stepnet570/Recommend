package com.example.recommend
import com.example.recommend.ui.feed.*
import com.example.recommend.ui.theme.*
import com.example.recommend.data.model.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun CollectionDetailScreen(
    collection: PostCollection,
    posts: List<Post>,
    savedPostIds: Set<String> = emptySet(),
    users: List<UserProfile> = emptyList(),
    onBack: () -> Unit,
    onSaveClick: (String) -> Unit,
    onUserProfileClick: (String) -> Unit = {},
    viewerUid: String? = null,
    onAudienceRate: (String, Int) -> Unit = { _, _ -> },
    onOpenPost: ((String) -> Unit)? = null
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding()
    ) {
        // ── Top bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppDark)
            }
            Text(
                text = "Collection",
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AppDark,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Collection header ─────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    AppViolet.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        // "📂 N picks" pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(AppViolet.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "📂 ${posts.size} picks",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppViolet,
                                fontFamily = BodyFontFamily
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Collection name
                        Text(
                            text = collection.name,
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = AppDark
                        )

                        // Subtitle: by user · date
                        val ownerUser = users.find { it.uid == collection.userId }
                        val ownerName = ownerUser?.name?.ifBlank { null }
                            ?: collection.userId.take(8)
                        Text(
                            text = "By $ownerName",
                            fontSize = 13.sp,
                            color = AppMuted,
                            fontFamily = BodyFontFamily,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )

                        // ── Cover mosaic ──────────────────────────────────
                        if (posts.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Main tile (flex: 2)
                                val mainPost = posts.firstOrNull()
                                CollectionCoverTile(
                                    post = mainPost,
                                    modifier = Modifier
                                        .weight(2f)
                                        .fillMaxHeight(),
                                    emojiSize = 40.sp
                                )

                                // Side column (flex: 1)
                                if (posts.size > 1) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CollectionCoverTile(
                                            post = posts.getOrNull(1),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            emojiSize = 24.sp
                                        )
                                        if (posts.size > 2) {
                                            CollectionCoverTile(
                                                post = posts.getOrNull(2),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                emojiSize = 24.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────
            if (posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No posts in this collection yet",
                            style = AppTextStyles.BodyMedium,
                            color = AppDark.copy(alpha = 0.55f)
                        )
                    }
                }
            } else {
                // ── Post list (compact cards) ─────────────────────────────
                item {
                    Spacer(Modifier.height(4.dp))
                }
                items(posts, key = { it.id }) { post ->
                    CollectionPostItem(
                        post = post,
                        onClick = { onOpenPost?.invoke(post.id) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp)
                    )
                }
            }
        }
    }
}

// ── Cover mosaic tile ─────────────────────────────────────────────────────────

@Composable
private fun CollectionCoverTile(
    post: Post?,
    modifier: Modifier = Modifier,
    emojiSize: androidx.compose.ui.unit.TextUnit
) {
    val emoji = remember(post?.category) { categoryEmojiFor(post?.category ?: "") }
    val bgBrush = remember(post?.category) { categoryGradientFor(post?.category ?: "") }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(0.dp)) // Clipped by parent row
            .background(bgBrush),
        contentAlignment = Alignment.Center
    ) {
        if (!post?.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = post!!.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(emoji, fontSize = emojiSize)
        }
    }
}

// ── Compact post item ─────────────────────────────────────────────────────────

@Composable
private fun CollectionPostItem(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trustScore = remember(post.ratingsByUser) {
        if (post.ratingsByUser.isEmpty()) 0f
        else ((post.averageAudienceRatingStars() ?: 0) * 2).toFloat().coerceIn(0f, 10f)
    }
    val emoji = categoryEmojiFor(post.category)
    val bgBrush = categoryGradientFor(post.category)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = AppWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgBrush),
                contentAlignment = Alignment.Center
            ) {
                if (!post.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(emoji, fontSize = 24.sp)
                }
            }

            // Title + location
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.title,
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AppDark,
                    maxLines = 1
                )
                if (post.location.isNotBlank()) {
                    Text(
                        text = "📍 ${post.location}",
                        fontSize = 11.sp,
                        color = AppMuted,
                        fontFamily = BodyFontFamily,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1
                    )
                }
            }

            // TrustScore ring
            TrustScoreRing(score = trustScore, size = 36.dp, strokeWidth = 3.5.dp)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun categoryEmojiFor(category: String): String = when (category.lowercase()) {
    "food"     -> "🍜"
    "coffee"   -> "☕"
    "places"   -> "🌿"
    "events"   -> "🎉"
    "shopping" -> "🛍"
    "beauty"   -> "💅"
    "services" -> "🔧"
    else       -> "📍"
}

private fun categoryGradientFor(category: String): Brush =
    Brush.linearGradient(
        when (category.lowercase()) {
            "food"     -> listOf(Color(0xFFF5E6D3), Color(0xFFEDD5B3))
            "coffee"   -> listOf(Color(0xFFE8D5C4), Color(0xFFD4B896))
            "places"   -> listOf(Color(0xFFD4ECD4), Color(0xFFB8D8B8))
            "events"   -> listOf(Color(0xFFF0E0F0), Color(0xFFDEC8DE))
            "shopping" -> listOf(Color(0xFFE0EAF5), Color(0xFFC8D8EE))
            "beauty"   -> listOf(Color(0xFFF5E0F0), Color(0xFFEDC8E8))
            "services" -> listOf(Color(0xFFE8EAE0), Color(0xFFD4D8C4))
            else       -> listOf(
                AppViolet.copy(alpha = 0.12f),
                AppTeal.copy(alpha = 0.12f)
            )
        }
    )
