package com.example.recommend
import com.example.recommend.ui.feed.*

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.BodyFontFamily
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.SoftPastelMint
import com.example.recommend.ui.theme.SurfaceMuted
import com.example.recommend.ui.theme.PrimaryGradient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    request: PackRequest,
    /** Profile of the person who created the pack signal (shown large in the header). */
    requestAuthor: UserProfile,
    users: List<UserProfile>,
    savedPostIds: Set<String>,
    viewerUid: String?,
    onBack: () -> Unit,
    onUserProfileClick: (String) -> Unit = {},
    onSaveClick: (String) -> Unit = {},
    onAudienceRate: (String, Int) -> Unit = { _, _ -> },
    onOpenPost: ((String) -> Unit)? = null,
    /** Opens Add with this request id as [Post.replyToRequestId]. */
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SoftPastelMint,
        topBar = {
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MutedPastelTeal)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = request.userId.isNotBlank()) {
                                onUserProfileClick(request.userId)
                            }
                            .padding(end = 8.dp)
                    ) {
                        FeedUserAvatar(
                            imageUrl = requestAuthor.avatar.takeIf { it.isNotBlank() },
                            displayName = requestAuthor.name,
                            fallbackSeed = requestAuthor.uid,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = requestAuthor.name.ifBlank { "Member" },
                                style = AppTextStyles.Heading1.copy(fontSize = 26.sp, lineHeight = 30.sp),
                                color = DarkPastelAnthracite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Who needs a pick",
                                style = AppTextStyles.BodySmall,
                                color = DarkPastelAnthracite.copy(alpha = 0.55f)
                            )
                            Text(
                                text = "The pack is collecting recommendations",
                                style = AppTextStyles.BodySmall,
                                color = DarkPastelAnthracite.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                color = Color.White,
                tonalElevation = 4.dp,
                shadowElevation = 10.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(54.dp)
                        .background(brush = PrimaryGradient, shape = RoundedCornerShape(16.dp))
                        .clickable { onAddRecommendation() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add a pick",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A2A24)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = request.text,
                            style = AppTextStyles.Heading2.copy(fontSize = 22.sp, lineHeight = 28.sp),
                            color = DarkPastelAnthracite
                        )
                        if (request.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                request.tags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(MutedPastelTeal.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            tag,
                                            style = AppTextStyles.BodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MutedPastelTeal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (request.selectedUsers.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sent to",
                            style = AppTextStyles.BodySmall,
                            fontWeight = FontWeight.Bold,
                            color = DarkPastelAnthracite.copy(alpha = 0.55f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        PackSignalRecipientsCarousel(
                            selectedUserIds = request.selectedUsers,
                            users = users,
                            onRecipientClick = onUserProfileClick
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Recommendations (${replyPosts.size})",
                    style = AppTextStyles.Heading2.copy(fontSize = 18.sp),
                    color = DarkPastelAnthracite,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            if (postsLoading && replyPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MutedPastelTeal)
                    }
                }
            } else if (replyPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No picks yet. Be the first to add one!",
                            style = AppTextStyles.BodyMedium,
                            color = DarkPastelAnthracite.copy(alpha = 0.55f)
                        )
                    }
                }
            } else {
                items(replyPosts, key = { it.id }) { post ->
                    PostCard(
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
