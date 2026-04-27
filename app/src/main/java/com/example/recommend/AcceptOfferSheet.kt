package com.example.recommend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.data.AcceptOfferResult
import com.example.recommend.data.acceptOffer
import com.example.recommend.data.model.AdOffer
import com.example.recommend.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptOfferSheet(
    offer: AdOffer,
    viewerUid: String,
    isAccepted: Boolean = false,
    onDismiss: () -> Unit,
    onAccepted: (offerId: String, offerTitle: String, rewardCoins: Int) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val db = FirebaseFirestore.getInstance()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Deadline text: prefer computed days-left, fall back to durationDays
    val deadlineText = remember(offer.expiresAt, offer.durationDays) {
        when {
            offer.expiresAt > 0L -> {
                val daysLeft = ((offer.expiresAt - System.currentTimeMillis()) / 86_400_000L)
                    .toInt().coerceAtLeast(0)
                "$daysLeft days left"
            }
            offer.durationDays > 0 -> "${offer.durationDays} days"
            else -> "Flexible"
        }
    }

    // Brand initial (first letter of business name)
    val brandInitial = offer.businessName.firstOrNull()?.uppercase() ?: "B"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            // Custom handle matching design
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Brand box ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryGradientLinear),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = brandInitial,
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── "Exclusive Offer" gold pill ───────────────────────────────
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(AppGold.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "✦ Exclusive Offer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppGold,
                    fontFamily = BodyFontFamily
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Offer title ───────────────────────────────────────────────
            Text(
                text = offer.title,
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = AppDark
            )

            // ── Description ───────────────────────────────────────────────
            Text(
                text = offer.description,
                fontSize = 14.sp,
                color = AppMuted,
                fontFamily = BodyFontFamily,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
            )

            // ── Reward card (gold tinted) ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppGold.copy(alpha = 0.08f))
                    .then(
                        Modifier.let {
                            // subtle gold border via padding trick — use Surface instead
                            it
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🪙", fontSize = 28.sp)
                Column {
                    Text(
                        text = "Reward for this pick",
                        fontSize = 13.sp,
                        color = AppMuted,
                        fontFamily = BodyFontFamily
                    )
                    Text(
                        text = "${offer.rewardCoins} TrustCoins",
                        fontFamily = HeadingFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = AppGold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Requirements card (SurfaceMuted) ──────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceMuted)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Minimum TrustScore",
                        fontSize = 13.sp,
                        color = AppMuted,
                        fontFamily = BodyFontFamily
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = offer.minTrustScore.let {
                                if (it == it.toLong().toDouble()) it.toLong().toString()
                                else String.format("%.1f", it)
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppDark,
                            fontFamily = BodyFontFamily
                        )
                        Text("✓", fontSize = 14.sp, color = AppTeal, fontWeight = FontWeight.Bold)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deadline",
                        fontSize = 13.sp,
                        color = AppMuted,
                        fontFamily = BodyFontFamily
                    )
                    Text(
                        text = deadlineText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppDark,
                        fontFamily = BodyFontFamily
                    )
                }
            }

            // ── Error message ─────────────────────────────────────────────
            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppError.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        style = AppTextStyles.BodySmall,
                        color = AppError,
                        fontFamily = BodyFontFamily
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Accept button / accepted state ────────────────────────────
            if (isAccepted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppTeal.copy(alpha = 0.10f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppTeal)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "You've completed this deal",
                        color = AppTeal,
                        fontWeight = FontWeight.Bold,
                        style = AppTextStyles.BodyMedium
                    )
                }
            } else {
                // Gradient "Accept offer" button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!isLoading) PrimaryGradientLinear else DisabledGradient)
                        .clickable(enabled = !isLoading) {
                            isLoading = true
                            errorMessage = null
                            acceptOffer(db, offer, viewerUid) { result ->
                                isLoading = false
                                when (result) {
                                    is AcceptOfferResult.Success ->
                                        onAccepted(offer.id, offer.title, result.rewardCoins)
                                    is AcceptOfferResult.AlreadyAccepted ->
                                        errorMessage = "You've already accepted this offer."
                                    is AcceptOfferResult.OfferFull ->
                                        errorMessage = "All slots for this offer are taken."
                                    is AcceptOfferResult.OfferExpired ->
                                        errorMessage = "This offer has expired."
                                    is AcceptOfferResult.NotEnoughCoins ->
                                        errorMessage = "Business has insufficient TrustCoins."
                                    is AcceptOfferResult.Error ->
                                        errorMessage = result.message
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Accept offer",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "You keep 100% of coins",
                    fontSize = 13.sp,
                    color = AppMuted,
                    fontFamily = BodyFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
