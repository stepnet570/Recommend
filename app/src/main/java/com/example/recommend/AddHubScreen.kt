package com.example.recommend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.SoftPastelMint

@Composable
fun AddHubScreen(
    onCampaign: () -> Unit,
    onPost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Create",
            style = AppTextStyles.Heading2.copy(fontSize = 26.sp),
            color = DarkPastelAnthracite
        )
        Text(
            "Choose what you want to add",
            style = AppTextStyles.BodyMedium,
            color = DarkPastelAnthracite.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        HubChoiceCard(
            icon = { Icon(Icons.Filled.Campaign, null, tint = RichPastelCoral, modifier = Modifier.size(40.dp)) },
            title = "Ad campaign",
            subtitle = "Launch an offer and reward users with TrustCoins",
            onClick = onCampaign
        )
        Spacer(modifier = Modifier.height(16.dp))
        HubChoiceCard(
            icon = { Icon(Icons.Filled.Edit, null, tint = MutedPastelTeal, modifier = Modifier.size(40.dp)) },
            title = "Recommendation post",
            subtitle = "Share a place or service you trust",
            onClick = onPost
        )
    }
}

@Composable
private fun HubChoiceCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(SoftPastelMint.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = AppTextStyles.BodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DarkPastelAnthracite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = AppTextStyles.BodySmall,
                    color = DarkPastelAnthracite.copy(alpha = 0.55f)
                )
            }
        }
    }
}
