package com.example.recommend
import com.example.recommend.ui.feed.*

import com.example.recommend.data.model.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.AppBackground
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelTeal

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Recommendation",
                        style = AppTextStyles.BodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkPastelAnthracite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MutedPastelTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            item {
                PostCard(
                    post = post,
                    isSaved = savedPostIds.contains(post.id),
                    onSaveClick = onSaveClick,
                    users = users,
                    onUserProfileClick = onUserProfileClick,
                    viewerUid = viewerUid,
                    onAudienceRate = onAudienceRate,
                    onOpenPost = null
                )
            }
        }
    }
}
