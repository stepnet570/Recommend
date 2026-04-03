package com.example.recommend

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelGold
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.SoftPastelMint
import com.example.recommend.ui.theme.SurfaceMuted
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
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scheme = MaterialTheme.colorScheme

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

        val uid = auth.currentUser?.uid ?: return
        isSubmitting = true

        val data = hashMapOf(
            "businessId" to uid,
            "businessName" to userProfile.name.ifBlank { userProfile.handle },
            "title" to t,
            "description" to desc,
            "rewardCoins" to coins,
            "minTrustScore" to minTrustScore.toDouble(),
            "status" to "active",
            "createdAt" to System.currentTimeMillis()
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

    Scaffold(
        containerColor = SoftPastelMint,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MutedPastelTeal
                            )
                        }
                    }
                },
                title = {
                    Text(
                        "Ad studio",
                        style = AppTextStyles.Heading2.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold,
                        color = DarkPastelAnthracite
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SoftPastelMint,
                    scrolledContainerColor = SoftPastelMint,
                    titleContentColor = DarkPastelAnthracite,
                    navigationIconContentColor = MutedPastelTeal
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            SoftPastelMint.copy(alpha = 0.45f),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Campaign,
                        contentDescription = null,
                        tint = RichPastelCoral,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "New offer",
                            style = AppTextStyles.BodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkPastelAnthracite
                        )
                        Text(
                            "Reach trusted users with TrustCoins rewards.",
                            style = AppTextStyles.BodySmall,
                            color = DarkPastelAnthracite.copy(alpha = 0.55f)
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Campaign title",
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.65f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Summer trust drop") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MutedPastelTeal,
                            unfocusedBorderColor = SurfaceMuted,
                            focusedLabelColor = MutedPastelTeal
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "What should users do?",
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.65f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text("Visit our café, leave an honest review, show this screen…") },
                        minLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MutedPastelTeal,
                            unfocusedBorderColor = SurfaceMuted
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "Reward (TrustCoins)",
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.65f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rewardCoinsText,
                        onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) rewardCoinsText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 50") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MutedPastelGold,
                            unfocusedBorderColor = SurfaceMuted
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Minimum trust score",
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.65f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        String.format(java.util.Locale.US, "%.1f — only users at or above this score qualify", minTrustScore),
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.45f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = minTrustScore,
                        onValueChange = { minTrustScore = it },
                        valueRange = 0f..5f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = RichPastelCoral,
                            activeTrackColor = RichPastelCoral,
                            inactiveTrackColor = SurfaceMuted
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", style = AppTextStyles.BodySmall, color = MutedPastelTeal)
                        Text("5", style = AppTextStyles.BodySmall, color = MutedPastelTeal)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { launchCampaign() },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = scheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Launch campaign",
                                style = AppTextStyles.BodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = scheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
