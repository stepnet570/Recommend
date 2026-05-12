package com.example.recommend

import com.example.recommend.data.model.*
import com.example.recommend.data.repository.CommentRepository
import com.example.recommend.ui.feed.TrustScoreRing
import com.example.recommend.ui.feed.categoryStyle

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recommend.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun PostDetailScreen(
    post: Post,
    users: List<UserProfile>,
    savedPostIds: Set<String>,
    viewerUid: String?,
    onBack: () -> Unit,
    onSaveClick: (String) -> Unit,
    onUserProfileClick: (String) -> Unit,
    onAudienceRate: (String, Int) -> Unit
) {
    BackHandler(onBack = onBack)

    val db = remember { FirebaseFirestore.getInstance() }
    val commentRepo = remember { CommentRepository(db) }
    val scope = rememberCoroutineScope()

    val author = remember(users, post.userId) { users.find { it.uid == post.userId } }
    val catStyle = remember(post.category) { categoryStyle(post.category) }
    val trustScore = remember(post) {
        if (post.ratingsByUser.isEmpty()) 0f
        else ((post.averageAudienceRatingStars() ?: 0) * 2.0).toFloat().coerceIn(0f, 10f)
    }

    // Author info
    val authorInitial = (author?.name ?: post.authorName).trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val authorTrustScore = author?.trustScore ?: 0.0
    val authorTrustText = String.format("⭐ %.1f", authorTrustScore * 2.0)

    // ── Comments state ────────────────────────────────────────────────────────
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(post.id) {
        commentRepo.getCommentsStream(post.id).collect { comments = it }
    }

    val scrollState = rememberScrollState()

    fun submitComment() {
        val uid = viewerUid ?: return
        val txt = draft.trim()
        if (txt.isEmpty() || sending) return
        sending = true
        scope.launch {
            runCatching { commentRepo.addComment(post.id, uid, txt) }
            draft = ""
            sending = false
            // After post sent — bring the new comment into view
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppWhite)
    ) {
        // ── detail-topbar ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppWhite)
                .border(
                    width = 1.dp,
                    color = SurfaceMuted,
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "←",
                fontSize = 22.sp,
                color = AppDark,
                modifier = Modifier.clickable { onBack() }
            )

            Text(
                text = "Pick",
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AppDark
            )

            Spacer(Modifier.weight(1f))

            // save-btn 🔖
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(SurfaceMuted, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSaveClick(post.id) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (savedPostIds.contains(post.id)) "🔖" else "🔖",
                    fontSize = 20.sp
                )
            }
        }

        // ── scrollable content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // ── detail-img ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(catStyle.gradient),
                contentAlignment = Alignment.Center
            ) {
                if (post.imageUrl != null) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(catStyle.emoji, fontSize = 70.sp)
                }

                if (post.isSponsored) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(AppGold, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
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

            // ── detail-body ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .background(AppViolet.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${catStyle.emoji} ${post.category.replaceFirstChar { it.uppercaseChar() }}",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = AppViolet
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = post.title,
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = AppDark,
                            lineHeight = 28.sp
                        )

                        if (post.location.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "📍 ${post.location}",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = AppMuted
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TrustScoreRing(score = trustScore, size = 56.dp)
                        Text(
                            text = "trust",
                            fontFamily = BodyFontFamily,
                            fontSize = 10.sp,
                            color = AppMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (post.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = post.description,
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = AppMuted,
                        lineHeight = 22.sp
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── detail-author-card ─────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (author != null) onUserProfileClick(author.uid) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(AppViolet, AppTeal),
                                        start = Offset(0f, 0f),
                                        end = Offset(120f, 120f)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val avatarUrl = author?.avatar ?: ""
                            if (avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = authorInitial,
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = AppWhite
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = author?.name ?: post.authorName,
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AppDark
                            )
                            Text(
                                text = "Pack · member",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = AppMuted
                            )
                        }

                        if (authorTrustScore > 0) {
                            Box(
                                modifier = Modifier
                                    .background(AppWhite, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = authorTrustText,
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = AppViolet
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Comments and Share buttons are temporarily hidden.
                // Comment input is still accessible via the pinned bar at the bottom.

                // ── Comments section ───────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (comments.isEmpty()) "Comments"
                                   else "Comments · ${comments.size}",
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppDark
                        )

                        Spacer(Modifier.height(12.dp))

                        if (comments.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceMuted, RoundedCornerShape(14.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Be the first to comment",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 13.sp,
                                    color = AppMuted
                                )
                            }
                        } else {
                            comments.forEach { c ->
                                CommentRow(
                                    comment = c,
                                    author = users.find { it.uid == c.userId },
                                    onUserClick = onUserProfileClick
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Comment input bar (pinned to bottom) ─────────────────────────────
        CommentInputBar(
            value = draft,
            onValueChange = { draft = it },
            enabled = viewerUid != null && !sending,
            sending = sending,
            onSend = { submitComment() }
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────── *
 *  Comment row
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun CommentRow(
    comment: Comment,
    author: UserProfile?,
    onUserClick: (String) -> Unit
) {
    val initial = (author?.name ?: "?").trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val timeAgo = remember(comment.createdAt) {
        val diff = System.currentTimeMillis() - comment.createdAt
        val minutes = diff / 60_000L
        val hours = minutes / 60L
        val days = hours / 24L
        when {
            days > 0    -> "${days}d"
            hours > 0   -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else        -> "now"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar 36dp
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    Brush.linearGradient(listOf(AppViolet, AppTeal)),
                    CircleShape
                )
                .clip(CircleShape)
                .clickable(enabled = author != null) {
                    author?.let { onUserClick(it.uid) }
                },
            contentAlignment = Alignment.Center
        ) {
            val url = author?.avatar.orEmpty()
            if (url.isNotBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = initial,
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AppWhite
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = author?.name ?: "Member",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = AppDark,
                    modifier = Modifier
                        .clickable(enabled = author != null) {
                            author?.let { onUserClick(it.uid) }
                        }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = timeAgo,
                    fontFamily = BodyFontFamily,
                    fontSize = 11.sp,
                    color = AppMuted
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = comment.text,
                fontFamily = BodyFontFamily,
                fontSize = 14.sp,
                color = AppDark,
                lineHeight = 20.sp
            )
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────── *
 *  Comment input bar — pinned to bottom of screen
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    sending: Boolean,
    onSend: () -> Unit
) {
    val canSend = enabled && value.trim().isNotEmpty()

    Surface(
        color = AppWhite,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = if (enabled) "Write a comment…" else "Sign in to comment",
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default
                ),
                shape = RoundedCornerShape(20.dp),
                enabled = enabled
            )

            // Send button — gradient pill
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) Brush.linearGradient(listOf(AppViolet, AppTeal))
                        else Brush.linearGradient(listOf(SurfaceMuted, SurfaceMuted))
                    )
                    .clickable(enabled = canSend) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AppWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "→",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canSend) AppWhite else AppMuted
                    )
                }
            }
        }
    }
}
