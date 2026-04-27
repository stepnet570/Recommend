package com.example.recommend.ui.explore

import com.example.recommend.*
import com.example.recommend.ui.theme.*
import com.example.recommend.data.model.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@Composable
fun ExploreScreen(onUserProfileClick: (String) -> Unit = {}) {
    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(Unit) {
        if (currentUser == null) return@LaunchedEffect
        db.trustListDataRoot().collection("users").document(currentUser.uid)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) currentUserProfile = snap.toUserProfileOrNull()
            }
        db.trustListDataRoot().collection("users")
            .addSnapshotListener { snap, _ ->
                if (snap != null) users = snap.documents.mapNotNull { it.toUserProfileOrNull() }
            }
    }

    fun toggleFollow(targetUid: String) {
        if (currentUser == null || currentUserProfile == null) return
        val ref = db.trustListDataRoot().collection("users").document(currentUser.uid)
        val isFollowing = currentUserProfile!!.following.contains(targetUid)
        if (isFollowing) ref.update("following", FieldValue.arrayRemove(targetUid))
        else ref.update("following", FieldValue.arrayUnion(targetUid))
    }

    val filteredUsers = users
        .filter { it.uid != currentUser?.uid }
        .filter {
            searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.handle.contains(searchQuery, ignoreCase = true)
        }

    // ── explore-wrap ──────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp)
    ) {
        // Title: "Community"
        Text(
            text = "Community",
            fontFamily = HeadingFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = AppDark,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // Search bar (input-wrap style, white bg)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppWhite, RoundedCornerShape(16.dp))
                .border(1.5.dp, Color(0xFFE8E6E0), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Search, null, tint = AppMuted, modifier = Modifier.size(18.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = AppTextStyles.BodyMedium.copy(color = AppDark),
                cursorBrush = SolidColor(AppViolet),
                decorationBox = { inner ->
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search by name or @handle...",
                                fontFamily = BodyFontFamily,
                                fontSize = 15.sp,
                                color = AppMuted
                            )
                        }
                        inner()
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Users list
        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No users found.",
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    color = AppMuted
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers, key = { it.uid }) { user ->
                    val isFollowing = currentUserProfile?.following?.contains(user.uid) == true
                    CommunityUserCard(
                        user = user,
                        isFollowing = isFollowing,
                        onCardClick = { onUserProfileClick(user.uid) },
                        onFollowClick = { toggleFollow(user.uid) }
                    )
                }
            }
        }
    }
}

// ─── user-card ────────────────────────────────────────────────────────────────

@Composable
private fun CommunityUserCard(
    user: UserProfile,
    isFollowing: Boolean,
    onCardClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppWhite,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Clickable left part: avatar + info
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onCardClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // user-avatar: 52dp circle, gradient bg + initial
                val initial = user.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(AppViolet, AppTeal),
                                start = Offset(0f, 0f),
                                end = Offset(150f, 150f)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatar.isNotBlank()) {
                        AsyncImage(
                            model = user.avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initial,
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = AppWhite
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // user-info
                Column(modifier = Modifier.weight(1f)) {
                    // user-name
                    Text(
                        text = user.name,
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AppDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // user-handle
                    Text(
                        text = user.handle,
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AppMuted
                    )
                    // user-bio (optional)
                    if (user.bio.isNotBlank()) {
                        Text(
                            text = user.bio,
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = AppMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                    // trust-score-badge
                    if (user.trustScore > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 3.dp)
                        ) {
                            Text("⭐", fontSize = 10.sp)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", user.trustScore * 2)} TrustScore",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = AppViolet
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            // Follow / Unfollow button
            if (isFollowing) {
                // unfollow-btn: bordered, dark text
                Box(
                    modifier = Modifier
                        .border(1.5.dp, Color(0xFFE0DDD8), RoundedCornerShape(12.dp))
                        .background(AppWhite, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onFollowClick() }
                        .padding(horizontal = 15.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Following",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AppDark
                    )
                }
            } else {
                // follow-btn: gradient, white text
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(listOf(AppViolet, AppTeal)),
                            RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onFollowClick() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Follow",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AppWhite
                    )
                }
            }
        }
    }
}
