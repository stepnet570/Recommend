package com.example.recommend

import androidx.compose.foundation.background
import com.example.recommend.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.data.model.UserProfile
import com.example.recommend.ui.feed.FeedUserAvatar
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.HeadingFontFamily
import com.example.recommend.ui.theme.MutedPastelGold
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.SurfaceMuted
import java.util.Locale

/**
 * Reusable profile header card.
 * Used in ProfileScreen (own profile), PublicUserProfileScreen, and UserProfileBottomSheet.
 */
@Composable
fun UserProfileCard(
    userProfile: UserProfile,
    isOwnProfile: Boolean,
    recsCount: Int = 0,
    followersCount: Int = 0,
    onFollowClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FeedUserAvatar(
            imageUrl = userProfile.avatar.ifBlank { null },
            displayName = userProfile.name,
            fallbackSeed = userProfile.uid,
            size = 80.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = userProfile.name.ifBlank { "Member" },
            fontFamily = HeadingFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = DarkPastelAnthracite,
            textAlign = TextAlign.Center
        )

        if (userProfile.handle.isNotBlank()) {
            Text(
                text = userProfile.handle,
                style = AppTextStyles.BodyMedium,
                color = DarkPastelAnthracite.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val scoreText = if (userProfile.trustScore > 0)
            String.format(Locale.US, "%.1f", userProfile.trustScore)
        else "—"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MutedPastelGold.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = MutedPastelGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(scoreText, fontWeight = FontWeight.Bold, color = MutedPastelGold, fontSize = 14.sp)
            Text(
                " trust score",
                color = MutedPastelGold.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$recsCount",
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DarkPastelAnthracite
                )
                Text(
                    text = "RECS",
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$followersCount",
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DarkPastelAnthracite
                )
                Text(
                    text = "FOLLOWERS",
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${userProfile.following.size}",
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DarkPastelAnthracite
                )
                Text(
                    text = "FOLLOWING",
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!isOwnProfile && onFollowClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(
                        PrimaryGradient,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onFollowClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Follow",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileBottomSheet(
    userProfile: UserProfile,
    onFollowClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserProfileCard(
                userProfile = userProfile,
                isOwnProfile = false,
                onFollowClick = onFollowClick
            )

            if (userProfile.bio.isNotBlank()) {
                Text(
                    text = userProfile.bio,
                    style = AppTextStyles.BodyMedium,
                    color = DarkPastelAnthracite.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
