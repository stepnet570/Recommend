package com.example.recommend

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.data.AcceptOfferResult
import com.example.recommend.data.acceptOffer
import com.example.recommend.data.model.AdOffer
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelGold
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.SoftPastelMint
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

    val daysLeft = remember(offer.expiresAt) {
        if (offer.expiresAt <= 0L) null
        else ((offer.expiresAt - System.currentTimeMillis()) / 86_400_000L)
            .toInt().coerceAtLeast(0)
    }

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
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = offer.title,
                    style = AppTextStyles.Heading2.copy(fontSize = 20.sp),
                    color = DarkPastelAnthracite
                )
                Text(
                    text = offer.businessName,
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.55f)
                )
            }

            // Description
            Text(
                text = offer.description,
                style = AppTextStyles.BodyMedium,
                color = DarkPastelAnthracite.copy(alpha = 0.85f)
            )

            // Stats chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OfferStatChip(
                    icon = Icons.Filled.Stars,
                    label = "+${offer.rewardCoins} TC",
                    color = MutedPastelGold
                )
                OfferStatChip(
                    icon = Icons.Filled.Group,
                    label = "${offer.slotsLeft} slots left",
                    color = MutedPastelTeal
                )
                if (daysLeft != null) {
                    OfferStatChip(
                        icon = Icons.Filled.CalendarMonth,
                        label = "${daysLeft}d left",
                        color = RichPastelCoral
                    )
                }
            }

            // Hint
            Text(
                text = "After accepting, you'll create a sponsored post to complete this deal.",
                style = AppTextStyles.BodySmall,
                color = DarkPastelAnthracite.copy(alpha = 0.5f)
            )

            // Error
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RichPastelCoral.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = errorMessage!!,
                        style = AppTextStyles.BodySmall,
                        color = RichPastelCoral,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Accept button or accepted state
            if (isAccepted) {
                Surface(
                    color = MutedPastelTeal.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MutedPastelTeal
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "You've completed this deal",
                            color = MutedPastelTeal,
                            fontWeight = FontWeight.Bold,
                            style = AppTextStyles.BodyMedium
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (!isLoading) {
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
                                        errorMessage = "Sorry, all slots for this offer are taken."
                                    is AcceptOfferResult.OfferExpired ->
                                        errorMessage = "This offer has expired."
                                    is AcceptOfferResult.NotEnoughCoins ->
                                        errorMessage = "Business has insufficient TrustCoins for this reward."
                                    is AcceptOfferResult.Error ->
                                        errorMessage = result.message
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MutedPastelGold,
                        contentColor = Color.White,
                        disabledContainerColor = MutedPastelGold.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Accept deal · +${offer.rewardCoins} coins",
                            style = AppTextStyles.BodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferStatChip(
    icon: ImageVector,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = AppTextStyles.BodySmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
