package com.example.recommend

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelGold
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.SoftPastelMint
import com.example.recommend.ui.theme.SurfaceMuted
import com.example.recommend.ui.theme.SurfacePastel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessOfferDetailScreen(
    offer: AdOffer,
    onBack: () -> Unit,
    onPauseToggle: (AdOffer) -> Unit
) {
    BackHandler(onBack = onBack)

    val isActive = offer.status.equals("active", ignoreCase = true)
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = SoftPastelMint,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Campaign",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = RichPastelCoral,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                offer.title,
                                style = AppTextStyles.Heading2.copy(fontSize = 22.sp),
                                fontWeight = FontWeight.Bold,
                                color = DarkPastelAnthracite
                            )
                            val dateStr = formatAdOfferDate(offer.createdAt)
                            if (dateStr.isNotEmpty()) {
                                Text(
                                    "Created $dateStr",
                                    style = AppTextStyles.BodySmall,
                                    color = DarkPastelAnthracite.copy(alpha = 0.45f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isActive -> SurfacePastel
                            offer.status.equals("paused", ignoreCase = true) -> SurfaceMuted
                            else -> SurfaceMuted
                        }
                    ) {
                        Text(
                            offer.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = AppTextStyles.BodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MutedPastelTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "What users should do",
                        style = AppTextStyles.BodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkPastelAnthracite.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        offer.description,
                        style = AppTextStyles.BodyMedium,
                        color = DarkPastelAnthracite.copy(alpha = 0.9f),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Reward",
                                style = AppTextStyles.BodySmall,
                                color = DarkPastelAnthracite.copy(alpha = 0.5f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${offer.rewardCoins}",
                                    style = AppTextStyles.Heading2.copy(fontSize = 26.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = RichPastelCoral
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "TrustCoins",
                                    style = AppTextStyles.BodySmall,
                                    color = DarkPastelAnthracite.copy(alpha = 0.55f)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Min. trust",
                                style = AppTextStyles.BodySmall,
                                color = DarkPastelAnthracite.copy(alpha = 0.5f)
                            )
                            Text(
                                String.format(Locale.US, "%.1f", offer.minTrustScore),
                                style = AppTextStyles.Heading2.copy(fontSize = 22.sp),
                                fontWeight = FontWeight.Bold,
                                color = MutedPastelGold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onPauseToggle(offer) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) SurfaceMuted else scheme.primary,
                    contentColor = if (isActive) DarkPastelAnthracite else scheme.onPrimary
                )
            ) {
                Icon(
                    if (isActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isActive) "Pause campaign" else "Resume campaign",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
