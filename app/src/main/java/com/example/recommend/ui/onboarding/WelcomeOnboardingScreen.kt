package com.example.recommend.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recommend.data.model.UserProfile
import com.example.recommend.ui.theme.*
import kotlinx.coroutines.launch

/**
 * First-launch onboarding shown after Sign-up:
 *   Screen 1 (WHY)  — what makes Recommend different
 *   Screen 2 (HOW)  — Pack Call mechanic
 *   Screen 3 (DO)   — pick a few suggested people to follow
 *
 * Implemented as a [HorizontalPager] over 3 pages with a top progress strip,
 * a Skip link, and a bottom CTA that advances or finishes the flow.
 *
 * The `onFinish` callback is invoked when the user reaches the end (Get started),
 * taps Skip on any screen, or completes the suggested-follows step. The hosting
 * screen is expected to mark `hasSeenWelcomeOnboarding = true` in Firestore.
 */
@Composable
fun WelcomeOnboardingScreen(
    suggestedUsers: List<UserProfile>,
    onFollowToggle: (UserProfile, Boolean) -> Unit,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    /** Local follow state for the Find-your-pack screen. */
    val followedUids = remember { mutableStateMapOf<String, Boolean>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar: progress dots + Skip ────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProgressDots(currentPage = pagerState.currentPage, pageCount = 3)
                Text(
                    text = "Skip",
                    color = AppMuted,
                    style = AppTextStyles.BodySmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onFinish() }
                )
            }

            // ── Pager ────────────────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> WhyPage()
                    1 -> HowPage()
                    2 -> FindYourPackPage(
                        users = suggestedUsers,
                        followed = followedUids,
                        onToggle = { user ->
                            val newValue = !(followedUids[user.uid] ?: false)
                            followedUids[user.uid] = newValue
                            onFollowToggle(user, newValue)
                        }
                    )
                }
            }

            // ── Bottom CTA ───────────────────────────────────────────────────
            val isLast = pagerState.currentPage == 2
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                GradientPillButton(
                    text = if (isLast) "Get started" else "Next",
                    onClick = {
                        if (isLast) {
                            onFinish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                )
                if (isLast) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Skip for now",
                        color = AppMuted,
                        style = AppTextStyles.BodySmall.copy(fontSize = 13.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onFinish() }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pages
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WhyPage() {
    OnboardingPage(
        hero = { GradientOrb() },
        title = "Recommendations\nfrom people you trust",
        body = "Recommend is a feed where local advice comes only from your circle of trust. " +
            "No algorithms. No hidden ads."
    )
}

@Composable
private fun HowPage() {
    OnboardingPage(
        hero = { SignalPulse() },
        title = "Ask — your\npack answers",
        body = "Not sure where to eat or what to try? Drop a Pack Call — your circle responds " +
            "fast, with no ads in the way."
    )
}

@Composable
private fun FindYourPackPage(
    users: List<UserProfile>,
    followed: Map<String, Boolean>,
    onToggle: (UserProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Find your pack",
            style = AppTextStyles.Heading1.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.6).sp,
                lineHeight = 30.sp
            ),
            color = AppDark,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )
        Text(
            text = "Pick a few people whose taste you trust. Their picks will show up in your feed.",
            style = AppTextStyles.BodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
            color = AppMuted,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (users.isEmpty()) {
            // No seed users yet — show a soft empty state instead of an empty list.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTeal, strokeWidth = 2.5.dp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(users, key = { it.uid }) { user ->
                    SuggestedUserRow(
                        user = user,
                        isFollowed = followed[user.uid] ?: false,
                        onToggle = { onToggle(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    hero: @Composable () -> Unit,
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) { hero() }

        Text(
            text = title,
            style = AppTextStyles.Heading1.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.6).sp,
                lineHeight = 32.sp
            ),
            color = AppDark,
            modifier = Modifier.padding(top = 12.dp, bottom = 14.dp)
        )
        Text(
            text = body,
            style = AppTextStyles.BodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
            color = AppMuted
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Suggested user row (Screen 3)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuggestedUserRow(
    user: UserProfile,
    isFollowed: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceMuted)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar — Coil from URL with gradient fallback initial
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AppViolet, AppTeal))),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatar.isNotBlank()) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = user.name.firstOrNull()?.uppercase() ?: "?",
                    color = AppWhite,
                    style = AppTextStyles.Heading2.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name.ifBlank { "Pack member" },
                style = AppTextStyles.BodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = AppDark,
                maxLines = 1
            )
            val subtitle = user.bio.takeIf { it.isNotBlank() }
                ?: user.handle.takeIf { it.isNotBlank() }
                ?: "Recommend member"
            Text(
                text = subtitle,
                style = AppTextStyles.BodySmall.copy(fontSize = 12.sp),
                color = AppMuted,
                maxLines = 1
            )
        }

        // Plus / checkmark toggle
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .then(
                    if (isFollowed) Modifier.background(
                        Brush.linearGradient(listOf(AppViolet, AppTeal))
                    )
                    else Modifier.border(1.5.dp, AppViolet, CircleShape)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isFollowed) "✓" else "+",
                color = if (isFollowed) AppWhite else AppViolet,
                style = AppTextStyles.Heading2.copy(
                    fontSize = if (isFollowed) 16.sp else 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Decorative hero illustrations
// ─────────────────────────────────────────────────────────────────────────────

/** Screen 1: gradient orb with the wolf emoji and slowly rotating dashed rings. */
@Composable
private fun GradientOrb() {
    val transition = rememberInfiniteTransition(label = "orb")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb-angle"
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .rotate(angle)
                .border(
                    1.5.dp,
                    AppViolet.copy(alpha = 0.20f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(170.dp)
                .rotate(-angle)
                .border(
                    1.5.dp,
                    AppTeal.copy(alpha = 0.15f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AppViolet, AppTeal))),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🐺", fontSize = 60.sp)
        }
    }
}

/** Screen 2: white pulsing circle with the megaphone emoji. */
@Composable
private fun SignalPulse() {
    val transition = rememberInfiniteTransition(label = "signal")
    val pulse1 by transition.animateFloat(
        initialValue = 1.0f, targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Restart
        ),
        label = "signal-pulse1"
    )
    val alpha1 by transition.animateFloat(
        initialValue = 0.7f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Restart
        ),
        label = "signal-alpha1"
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pulse1)
                .alpha(alpha1)
                .border(2.dp, AppTeal, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(AppWhite)
                .border(3.dp, AppViolet, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📣", fontSize = 54.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Progress dots
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProgressDots(currentPage: Int, pageCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(pageCount) { i ->
            val isActive = i == currentPage
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if (isActive) 32.dp else 22.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .then(
                        if (isActive) Modifier.background(
                            Brush.horizontalGradient(listOf(AppViolet, AppTeal))
                        )
                        else Modifier.background(AppBorder)
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable gradient CTA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GradientPillButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Brush.linearGradient(listOf(AppViolet, AppTeal)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                color = AppWhite,
                style = AppTextStyles.Heading2.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "→", color = AppWhite, fontSize = 18.sp)
        }
    }
}
