package com.example.recommend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.recommend.ui.theme.GradientMid
import com.example.recommend.ui.theme.GradientTop
import com.example.recommend.ui.theme.SurfaceMuted
import com.example.recommend.ui.theme.SurfacePastel
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    myPosts: List<Post>,
    collections: List<PostCollection> = emptyList(),
    myOffers: List<AdOffer> = emptyList(),
    onCollectionClick: (PostCollection) -> Unit = {},
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            ConvexCardBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(32.dp),
                elevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = userProfile.avatar.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${userProfile.uid}" },
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(SurfaceMuted),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(userProfile.name, style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                    Text(userProfile.handle, color = RichPastelCoral, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MutedPastelGold.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MutedPastelGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))

                        val scoreText =
                            if (userProfile.trustScore > 0) String.format(Locale.US, "%.1f", userProfile.trustScore) else "New"
                        Text(scoreText, fontWeight = FontWeight.Bold, color = MutedPastelGold, fontSize = 14.sp)
                        if (userProfile.trustScore > 0) {
                            Text(
                                " (trust score)",
                                color = MutedPastelGold.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // TrustCoins wallet — grayscale gradient inset
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            GradientTop,
                                            SurfaceMuted,
                                            GradientMid.copy(alpha = 0.85f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MutedPastelGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Balance: ${userProfile.trustCoins}",
                                    style = AppTextStyles.BodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkPastelAnthracite
                                )
                                Text(
                                    "TrustCoins",
                                    style = AppTextStyles.BodySmall,
                                    color = DarkPastelAnthracite.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        userProfile.bio.ifEmpty { "Hi! I'm on TrustList." },
                        style = AppTextStyles.BodyMedium,
                        color = DarkPastelAnthracite.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = SurfaceMuted)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${myPosts.size}", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                            Text("RECS", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f), fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${userProfile.following.size}", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                            Text("FOLLOWING", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f), fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${userProfile.trustCoins}", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                            Text("COINS", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        },
                        border = BorderStroke(1.dp, RichPastelCoral),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RichPastelCoral),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log out", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        if (!userProfile.isBusiness) {
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = RichPastelCoral,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MutedPastelTeal.copy(alpha = 0.35f)
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My recs", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Collections", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedTab == 0) {
                if (myPosts.isEmpty()) {
                    item {
                        EmptyStateCard("No recommendations yet")
                    }
                } else {
                    items(myPosts) { post ->
                        PostSmallCard(post)
                    }
                }
            } else {
                if (collections.isEmpty()) {
                    item {
                        EmptyStateCard("No collections yet")
                    }
                } else {
                    val chunkedCollections = collections.chunked(2)
                    items(chunkedCollections) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowItems.forEach { collection ->
                                CollectionCard(
                                    collection = collection,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onCollectionClick(collection) }
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        } else {
            item {
                Text(
                    "Ad dashboard",
                    style = AppTextStyles.Heading2.copy(fontSize = 20.sp),
                    color = DarkPastelAnthracite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                Text(
                    "Your live campaigns and rewards.",
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            if (myOffers.isEmpty()) {
                item {
                    EmptyStateCard("No campaigns yet. Tap + to launch your first offer.")
                }
            } else {
                items(myOffers, key = { it.id }) { offer ->
                    AdOfferProfileCard(offer = offer)
                }
            }
        }
    }
}

@Composable
fun AdOfferProfileCard(offer: AdOffer) {
    ConvexCardBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 22.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        offer.title,
                        style = AppTextStyles.BodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = DarkPastelAnthracite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (offer.status.lowercase(Locale.US)) {
                            "active" -> SurfacePastel
                            "paused" -> SurfaceMuted
                            else -> SurfaceMuted
                        }
                    ) {
                        Text(
                            offer.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = AppTextStyles.BodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MutedPastelTeal
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MutedPastelGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${offer.rewardCoins}",
                            style = AppTextStyles.Heading2.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.Bold,
                            color = RichPastelCoral
                        )
                    }
                    Text(
                        "TrustCoins",
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.45f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Min. trust ${String.format(Locale.US, "%.1f", offer.minTrustScore)}",
                style = AppTextStyles.BodySmall,
                color = DarkPastelAnthracite.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
fun EmptyStateCard(text: String) {
    ConvexCardBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 20.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(text, style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.55f), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PostSmallCard(post: Post) {
    ConvexCardBox(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 20.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (post.imageUrl != null) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceMuted),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceMuted), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = MutedPastelTeal.copy(alpha = 0.4f))
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column {
                Text(post.title, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = MutedPastelGold, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${post.rating}/5", color = DarkPastelAnthracite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(" • ${post.category}", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.55f))
                }
            }
        }
    }
}

@Composable
fun CollectionCard(
    collection: PostCollection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ConvexCardBox(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Bookmarks, contentDescription = null, tint = MutedPastelTeal)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                collection.name,
                style = AppTextStyles.BodyMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${collection.postIds.size} posts",
                style = AppTextStyles.BodySmall,
                color = DarkPastelAnthracite.copy(alpha = 0.55f)
            )
        }
    }
}
