package com.example.recommend

import com.example.recommend.data.model.*
import com.example.recommend.ui.theme.*
import com.example.recommend.ui.add.AddFieldLabel

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.ui.theme.AppTextStyles
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOfferScreen(
    userProfile: UserProfile,
    onOfferCreated: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var rewardCoinsText by remember { mutableStateOf("") }
    var minTrustScore by remember { mutableFloatStateOf(0f) }
    var selectedDurationDays by remember { mutableIntStateOf(7) }
    var maxAcceptancesText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    fun launchCampaign() {
        val t = title.trim()
        if (t.isEmpty()) {
            Toast.makeText(context, "Enter a campaign title", Toast.LENGTH_SHORT).show()
            return
        }
        val desc = description.trim()
        if (desc.isEmpty()) {
            Toast.makeText(context, "Describe what users should do", Toast.LENGTH_SHORT).show()
            return
        }
        val coins = rewardCoinsText.trim().toIntOrNull()
        if (coins == null || coins <= 0) {
            Toast.makeText(context, "Enter a valid reward (TrustCoins)", Toast.LENGTH_SHORT).show()
            return
        }
        val maxAcceptances = maxAcceptancesText.trim().toIntOrNull()
        if (maxAcceptances == null || maxAcceptances <= 0) {
            Toast.makeText(context, "Enter how many people can accept this offer", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return
        isSubmitting = true

        val now = System.currentTimeMillis()
        val data = hashMapOf(
            "businessId" to uid,
            "businessName" to userProfile.name.ifBlank { userProfile.handle },
            "title" to t,
            "description" to desc,
            "rewardCoins" to coins,
            "minTrustScore" to minTrustScore.toDouble(),
            "status" to "active",
            "createdAt" to now,
            "durationDays" to selectedDurationDays,
            "expiresAt" to (now + selectedDurationDays * 86_400_000L),
            "maxAcceptances" to maxAcceptances,
            "acceptedCount" to 0,
            "acceptedBy" to emptyList<String>()
        )

        db.trustListDataRoot()
            .collection("offers")
            .add(data)
            .addOnSuccessListener {
                isSubmitting = false
                Toast.makeText(context, "Campaign launched!", Toast.LENGTH_SHORT).show()
                onOfferCreated()
            }
            .addOnFailureListener { e ->
                isSubmitting = false
                Toast.makeText(context, e.localizedMessage ?: "Failed to save", Toast.LENGTH_SHORT).show()
            }
    }

    val canLaunch = !isSubmitting && title.isNotBlank() && description.isNotBlank()
        && (rewardCoinsText.toIntOrNull() ?: 0) > 0
        && (maxAcceptancesText.toIntOrNull() ?: 0) > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding()
            .imePadding()
    ) {
        // ── Top bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, enabled = !isSubmitting) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppDark)
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            Text(
                "Ad Campaign",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = AppDark
            )
            Spacer(Modifier.width(48.dp))
        }

        // ── Content ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Header card — gold accent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppGold.copy(alpha = 0.08f))
                    .border(1.dp, AppGold.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📣", fontSize = 28.sp)
                Column {
                    Text(
                        "New offer",
                        fontFamily = HeadingFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AppDark
                    )
                    Text(
                        "Reach trusted users with TrustCoins rewards",
                        fontSize = 12.sp,
                        color = AppMuted,
                        fontFamily = BodyFontFamily
                    )
                }
            }

            // Campaign title
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AddFieldLabel("Campaign title")
                AppTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Summer trust drop",
                    singleLine = true,
                    containerColor = AppWhite
                )
            }

            // What should users do
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AddFieldLabel("What should users do?")
                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    placeholder = "Visit our café, leave an honest review, show this screen…",
                    singleLine = false,
                    minLines = 4,
                    containerColor = AppWhite
                )
            }

            // Reward
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AddFieldLabel("Reward per person (TrustCoins)")
                AppTextField(
                    value = rewardCoinsText,
                    onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) rewardCoinsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "e.g. 50",
                    singleLine = true,
                    containerColor = AppWhite,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = {
                        Text("🪙 TC", fontSize = 13.sp, color = AppGold, fontWeight = FontWeight.Bold, fontFamily = BodyFontFamily)
                    }
                )
            }

            // Min trust score
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AddFieldLabel("Min trust score")
                    Text(
                        String.format(java.util.Locale.US, "%.1f / 10", minTrustScore),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppViolet,
                        fontFamily = BodyFontFamily
                    )
                }
                Text(
                    "Only users at or above this score can accept",
                    fontSize = 12.sp,
                    color = AppMuted,
                    fontFamily = BodyFontFamily
                )
                AppSlider(
                    value = minTrustScore,
                    onValueChange = { minTrustScore = it },
                    valueRange = 0f..10f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0", fontSize = 11.sp, color = AppMuted, fontFamily = BodyFontFamily)
                    Text("10", fontSize = 11.sp, color = AppMuted, fontFamily = BodyFontFamily)
                }
            }

            // Duration
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AddFieldLabel("Campaign duration")
                val durations = listOf(7, 14, 30)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    durations.forEach { days ->
                        val isSelected = selectedDurationDays == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected)
                                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(AppGold.copy(alpha = 0.15f), AppGold.copy(alpha = 0.15f)))
                                    else
                                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(AppWhite, AppWhite))
                                )
                                .border(
                                    1.5.dp,
                                    if (isSelected) AppGold.copy(alpha = 0.6f) else Color(0xFFE8E6E2),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedDurationDays = days },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$days days",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AppGold else AppDark,
                                fontFamily = BodyFontFamily
                            )
                        }
                    }
                }
            }

            // Max acceptances
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AddFieldLabel("Max participants")
                AppTextField(
                    value = maxAcceptancesText,
                    onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) maxAcceptancesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "e.g. 20",
                    singleLine = true,
                    containerColor = AppWhite,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = {
                        Text("people", fontSize = 13.sp, color = AppMuted, fontFamily = BodyFontFamily)
                    }
                )
            }

            // Summary pill (shows when filled in)
            val maxPeople = maxAcceptancesText.toIntOrNull() ?: 0
            val reward = rewardCoinsText.toIntOrNull() ?: 0
            if (reward > 0 || maxPeople > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppGold.copy(alpha = 0.06f))
                        .border(1.dp, AppGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(14.dp, 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🪙", fontSize = 14.sp)
                    Text(
                        text = buildAnnotatedString {
                            append("Budget: ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AppGold)) {
                                val total = if (reward > 0 && maxPeople > 0) reward * maxPeople else 0
                                if (total > 0) append("${total} TC total") else append("–")
                            }
                            if (selectedDurationDays > 0) {
                                append(" · $selectedDurationDays days")
                            }
                        },
                        fontSize = 13.sp,
                        fontFamily = BodyFontFamily,
                        color = AppDark
                    )
                }
            }

            // Launch button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (canLaunch) PrimaryGradientLinear else DisabledGradient)
                    .clickable(enabled = canLaunch) { launchCampaign() },
                contentAlignment = Alignment.Center
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        "Launch campaign",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (canLaunch) Color.White else AppOnDisabled
                    )
                }
            }
        }
    }
}
