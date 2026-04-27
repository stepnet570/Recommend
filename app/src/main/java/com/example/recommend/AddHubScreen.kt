package com.example.recommend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.ui.theme.*

// ─── AddHubScreen ─────────────────────────────────────────────────────────────
// isBusiness = true  → "Grow your brand" (Ad Campaign + Post a recommendation + Ask the Pack + balance)
// isBusiness = false → "What's on your mind?" (Post a recommendation + Ask the Pack + biz link)

@Composable
fun AddHubScreen(
    isBusiness: Boolean = false,
    campaignBalance: Int = 0,
    onCampaign: () -> Unit = {},
    onPost: () -> Unit = {},
    onAskPack: () -> Unit = {},
    onSwitchToBusiness: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── hub-title ─────────────────────────────────────────────────────────
        Text(
            text = if (isBusiness) "Grow your brand" else "What's on your mind?",
            fontFamily = HeadingFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = AppDark,
            textAlign = TextAlign.Center
        )
        // ── hub-sub ───────────────────────────────────────────────────────────
        Text(
            text = if (isBusiness) "Native ads that people actually trust"
            else "Share a pick or ask your pack",
            fontFamily = BodyFontFamily,
            fontSize = 14.sp,
            color = AppMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // ── Cards ─────────────────────────────────────────────────────────────
        if (isBusiness) {
            HubCard(
                emoji = "📢",
                iconBg = AppGold.copy(alpha = 0.10f),
                title = "Ad Campaign",
                subtitle = "Pay TrustCoins to users who create posts about you",
                onClick = onCampaign
            )
            Spacer(Modifier.height(12.dp))
            HubCard(
                emoji = "📝",
                iconBg = AppViolet.copy(alpha = 0.10f),
                title = "Post a recommendation",
                subtitle = "Share a place or product from your business",
                onClick = onPost
            )
            Spacer(Modifier.height(12.dp))
            HubCard(
                emoji = "🐺",
                iconBg = AppTeal.copy(alpha = 0.10f),
                title = "Ask the Pack",
                subtitle = "Send a signal — get real recs from people you trust",
                onClick = onAskPack
            )
        } else {
            HubCard(
                emoji = "📝",
                iconBg = AppViolet.copy(alpha = 0.10f),
                title = "Post a recommendation",
                subtitle = "Recommend a place, café, or service your pack will love",
                onClick = onPost
            )
            Spacer(Modifier.height(12.dp))
            HubCard(
                emoji = "🐺",
                iconBg = AppTeal.copy(alpha = 0.10f),
                title = "Ask the Pack",
                subtitle = "Send a signal — get real recs from people you trust",
                onClick = onAskPack
            )
        }

        // ── Campaign balance (business only) ──────────────────────────────────
        if (isBusiness) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = AppGold.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = AppGold.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column {
                    Text(
                        text = "🪙 Campaign balance: $campaignBalance TC",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AppGold
                    )
                    Text(
                        text = "Top up to launch more campaigns",
                        fontFamily = BodyFontFamily,
                        fontSize = 12.sp,
                        color = AppMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ── Bottom footnote (user only) ───────────────────────────────────────
        if (!isBusiness) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = AppMuted, fontSize = 12.sp)) {
                        append("Are you a business? ")
                    }
                    withStyle(
                        SpanStyle(
                            color = AppViolet,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    ) {
                        append("Switch to Business →")
                    }
                },
                fontFamily = BodyFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { onSwitchToBusiness() }
            )
        }
    }
}

// ─── HubCard ──────────────────────────────────────────────────────────────────

@Composable
private fun HubCard(
    emoji: String,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AppWhite,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // hub-icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconBg, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }

            // title + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = AppDark
                )
                Text(
                    text = subtitle,
                    fontFamily = BodyFontFamily,
                    fontSize = 13.sp,
                    color = AppMuted,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            // hub-arrow ›
            Text(
                text = "›",
                fontSize = 20.sp,
                color = AppMuted.copy(alpha = 0.5f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}
