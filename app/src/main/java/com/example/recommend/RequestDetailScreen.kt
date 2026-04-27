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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

@Composable
fun RequestDetailScreen(
    request: PackRequest,
    /** Profile of the person who created the pack signal. */
    requestAuthor: UserProfile,
    users: List<UserProfile>,
    savedPostIds: Set<String>,
    viewerUid: String?,
    onBack: () -> Unit,
    onUserProfileClick: (String) -> Unit = {},
    onSaveClick: (String) -> Unit = {},
    onAudienceRate: (String, Int) -> Unit = { _, _ -> },
    onOpenPost: ((String) -> Unit)? = null,
    onAddRecommendation: () -> Unit
) {
    BackHandler(enabled = true, onBack = onBack)

    val requestId = request.id
    var replyPosts by remember(requestId) { mutableStateOf<List<Post>>(emptyList()) }
    var postsLoading by remember(requestId) { mutableStateOf(true) }

    DisposableEffect(requestId) {
        val db = FirebaseFirestore.getInstance()
        val reg: ListenerRegistration = db.trustListDataRoot()
            .collection("posts")
            .whereEqualTo("replyToRequestId", requestId)
            .addSnapshotListener { snapshot, _ ->
                postsLoading = false
                if (snapshot != null) {
                    replyPosts = snapshot.documents
                        .sortedByDescending { it.getLong("createdAt") ?: 0L }
                        .map { it.toPostFromDoc() }
                } else {
                    replyPosts = emptyList()
                }
            }
        onDispose { reg.remove() }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // ── Top bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppDark)
                }
                Text(
                    text = "Pack Call",
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AppDark
                )
            }

            // ── Content ──────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {

                // ── Gradient header card ──────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(PrimaryGradientLinear)
                    ) {
                        // Decorative circle
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .offset(x = 220.dp, y = (-40).dp)
                                .background(Color.White.copy(alpha = 0.07f), CircleShape)
                        )
                        Column(modifier = Modifier.padding(22.dp)) {
                            // "🐺 Pack Call" pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🐺 Pack Call",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BodyFontFamily
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            // Question text
                            Text(
                                text = request.text,
                                fontFamily = HeadingFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                                color = Color.White
                            )
                            Spacer(Modifier.height(6.dp))
                            // Subtitle: author · location
                            val locationStr = request.location
                                .takeIf { it.isNotBlank() }?.let { "· 📍 $it" } ?: "· 📍 Current area"
                            Text(
                                text = "${requestAuthor.name.ifBlank { "Someone" }} asks $locationStr",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                fontFamily = BodyFontFamily
                            )
                            Spacer(Modifier.height(12.dp))
                            // Recipient avatars + answer count
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                request.selectedUsers.take(3).forEach { uid ->
                                    val profile = users.find { it.uid == uid }
                                    FeedUserAvatar(
                                        imageUrl = profile?.avatar,
                                        displayName = profile?.name ?: uid,
                                        fallbackSeed = uid,
                                        size = 28.dp
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${replyPosts.size} answers so far",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontFamily = BodyFontFamily
                                )
                            }
                        }
                    }
                }

                // ── Tags ─────────────────────────────────────────────────
                if (request.tags.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            request.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(AppViolet.copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 12.sp,
                                        color = AppViolet,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = BodyFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // ── "Answers" label ───────────────────────────────────────
                item {
                    Text(
                        text = "Answers",
                        fontFamily = HeadingFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AppDark,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                // ── Answer cards ──────────────────────────────────────────
                if (postsLoading && replyPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AppTeal)
                        }
                    }
                } else if (replyPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No picks yet. Be the first to add one!",
                                style = AppTextStyles.BodyMedium,
                                color = AppDark.copy(alpha = 0.55f)
                            )
                        }
                    }
                } else {
                    items(replyPosts, key = { it.id }) { post ->
                        AnswerCard(
                            post = post,
                            users = users,
                            onOpenPost = onOpenPost,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 10.dp)
                        )
                    }
                }
            }
        }

        // ── Floating "Add your pick" button ──────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, AppBackground, AppBackground)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryGradientLinear)
                    .clickable { onAddRecommendation() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add your pick →",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

// ── Compact answer card (reply post inside a Pack Call) ───────────────────────

@Composable
private fun AnswerCard(
    post: Post,
    users: List<UserProfile>,
    onOpenPost: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val authorUser = users.find { it.uid == post.userId }

    val trustScore = remember(post.ratingsByUser) {
        if (post.ratingsByUser.isEmpty()) 0f
        else ((post.averageAudienceRatingStars() ?: 0) * 2).toFloat().coerceIn(0f, 10f)
    }

    val timeAgo = remember(post.createdAt) {
        val diff = System.currentTimeMillis() - post.createdAt
        val hours = diff / 3_600_000L
        val days = hours / 24L
        when {
            days > 0  -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            else      -> "just now"
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onOpenPost != null)
                    Modifier.clickable { onOpenPost(post.id) }
                else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        color = AppWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeedUserAvatar(
                    imageUrl = authorUser?.avatar,
                    displayName = post.authorName.ifBlank { authorUser?.name ?: "?" },
                    fallbackSeed = post.userId,
                    size = 36.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName.ifBlank { authorUser?.name ?: "Member" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppDark,
                        fontFamily = BodyFontFamily
                    )
                    Text(
                        text = timeAgo,
                        fontSize = 12.sp,
                        color = AppMuted,
                        fontFamily = BodyFontFamily
                    )
                }
                TrustScoreRing(score = trustScore, size = 36.dp, strokeWidth = 3.5.dp)
            }

            Spacer(Modifier.height(10.dp))

            // Post title
            Text(
                text = post.title,
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = AppDark
            )

            // Post description
            if (post.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = post.description,
                    fontSize = 13.sp,
                    color = AppMuted,
                    lineHeight = 19.sp,
                    fontFamily = BodyFontFamily,
                    maxLines = 3
                )
            }
        }
    }
}
