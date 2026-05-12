package com.example.recommend.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.ui.theme.AppDark
import com.example.recommend.ui.theme.AppGold
import com.example.recommend.ui.theme.AppMuted
import com.example.recommend.ui.theme.AppTeal
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.AppViolet
import com.example.recommend.ui.theme.AppWhite
import com.example.recommend.ui.theme.SurfaceMuted
import kotlinx.coroutines.launch

/**
 * MonetizationOnboardingSheet — 3-step bottom sheet that explains how a user can earn
 * TrustCoins by accepting native sponsored offers from local businesses.
 *
 * Triggered contextually: first tap on Exclusive Deals / sponsored block in the Feed.
 *
 * @param userTrustScore current user's TrustScore, used to render progress
 * @param requiredTrustScore minimum TrustScore needed to unlock the first tier of offers
 * @param welcomeCoins amount of TrustCoins given as a sign-up welcome bonus
 * @param onDismiss called when sheet is closed WITHOUT completing (skip / swipe-down).
 *                  The "seen" flag should NOT be set here so the user can come back to it.
 * @param onSeeOffers called when user reaches the final step and taps the CTA "Show me deals".
 *                    Only here the "seen" flag should be persisted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonetizationOnboardingSheet(
    userTrustScore: Double,
    requiredTrustScore: Double = 3.0,
    welcomeCoins: Int = 50,
    onDismiss: () -> Unit,
    onSeeOffers: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppWhite,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceMuted)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        emoji = "💰",
                        title = "Two ways to earn here",
                        body = "Promote local businesses you trust → get TrustCoins. " +
                            "Or switch to business mode and pay coins to micro-influencers who post about you.",
                        accent = AppGold,
                        bonusBadge = "+$welcomeCoins TC starter pack on your first switch to business"
                    )
                    1 -> OnboardingProgressPage(
                        userTrustScore = userTrustScore,
                        requiredTrustScore = requiredTrustScore
                    )
                    2 -> OnboardingPage(
                        emoji = "🚀",
                        title = "Pick your first deal",
                        body = "Tap any deal to see the brief. " +
                            "Accept → write your post → coins land on your balance.",
                        accent = AppViolet,
                        bonusBadge = null
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Progress dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isActive) 10.dp else 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isActive) AppTeal else AppMuted.copy(alpha = 0.35f))
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // CTA
            val isLastPage = pagerState.currentPage == 2
            val ctaLabel = if (isLastPage) "Show me deals" else "Next"

            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppViolet, AppTeal),
                            start = Offset(0f, 0f),
                            end = Offset(800f, 0f)
                        )
                    )
                    .clickable {
                        if (isLastPage) {
                            onSeeOffers()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ctaLabel,
                    style = AppTextStyles.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = AppWhite
                )
            }

            // Skip option (only on intermediate pages — final page closes via CTA)
            if (!isLastPage) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Skip",
                    style = AppTextStyles.BodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = AppMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onDismiss() },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    emoji: String,
    title: String,
    body: String,
    accent: Color,
    bonusBadge: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // Hero bubble with gradient + emoji
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppViolet, AppTeal),
                        start = Offset(0f, 0f),
                        end = Offset(300f, 300f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 44.sp)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = title,
            style = AppTextStyles.Heading2.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp
            ),
            color = AppDark,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = body,
            style = AppTextStyles.BodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
            color = AppMuted,
            textAlign = TextAlign.Center
        )

        if (bonusBadge != null) {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.10f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = bonusBadge,
                    style = AppTextStyles.BodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = accent
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Step 2 — TrustScore progress page. Shows the user where they are vs the threshold.
 * If they already qualify, shows an "unlocked" state.
 */
@Composable
private fun OnboardingProgressPage(
    userTrustScore: Double,
    requiredTrustScore: Double
) {
    val qualifies = userTrustScore >= requiredTrustScore
    val progress = (userTrustScore / requiredTrustScore.coerceAtLeast(0.1)).coerceIn(0.0, 1.0).toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // Custom ring showing progress towards required TrustScore
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                drawArc(
                    color = Color(0xFFE8E6E2),
                    startAngle = 120f,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = stroke
                )
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(AppViolet, AppTeal),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    startAngle = 120f,
                    sweepAngle = 300f * progress,
                    useCenter = false,
                    style = stroke
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1f", userTrustScore),
                    style = AppTextStyles.Heading1.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold),
                    color = AppDark
                )
                Text(
                    text = "TrustScore",
                    style = AppTextStyles.BodySmall.copy(fontSize = 11.sp),
                    color = AppMuted
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = if (qualifies) "You're already in!" else "Almost there",
            style = AppTextStyles.Heading2.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp
            ),
            color = AppDark,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (qualifies) {
                "Your TrustScore is high enough to take any deal in the feed. Pick the one that fits."
            } else {
                "Reach TrustScore ${"%.1f".format(requiredTrustScore)} to unlock first deals. " +
                    "Post real picks, get rated by the pack — score grows fast."
            },
            style = AppTextStyles.BodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
            color = AppMuted,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))
    }
}
