package com.example.recommend

import com.example.recommend.data.model.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recommend.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

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

    val db = FirebaseFirestore.getInstance()
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
            // back-btn
            Text(
                text = "←",
                fontSize = 22.sp,
                color = AppDark,
                modifier = Modifier.clickable { onBack() }
            )

            // title "Pick"
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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

                // Sponsored badge
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
                // Header row: [category + title + location] | [TrustScoreRing]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // category pill
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

                        // detail-title
                        Text(
                            text = post.title,
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = AppDark,
                            lineHeight = 28.sp
                        )

                        // loc-tag
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

                    // TrustScoreRing
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

                // detail-desc
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
                        // 44dp gradient avatar
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

                        // Author name + pack label
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

                        // Trust score badge
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

                Spacer(Modifier.height(16.dp))

                // ── detail-actions ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 💬 Comments
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SurfaceMuted, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💬 Comments",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = AppDark
                        )
                    }

                    // 📨 Share
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(SurfaceMuted, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📨", fontSize = 20.sp)
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
