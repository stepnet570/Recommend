package com.example.recommend.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.data.model.BusinessData
import com.example.recommend.ui.theme.AppDark
import com.example.recommend.ui.theme.AppGold
import com.example.recommend.ui.theme.AppMuted
import com.example.recommend.ui.theme.AppTeal
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.AppViolet
import com.example.recommend.ui.theme.AppWhite
import com.example.recommend.ui.theme.SurfaceMuted

/** Preset categories — quick chips. User can also type a custom one. */
private val PRESET_CATEGORIES = listOf(
    "Restaurant", "Café", "Beauty & Wellness", "Fitness",
    "Retail", "Services", "Education", "Other"
)

/**
 * SwitchToBusinessSheet — minimal form that promotes a personal account to business mode.
 *
 * Two phases: Form (company name + category) → Success (bonus reveal + 2 CTAs).
 * Reversible: user can switch back from Profile.
 *
 * @param welcomeBonusAlreadyGranted current value from UserProfile — used to decide
 *        whether the success page shows the +50 TC reveal.
 * @param onConfirm called when user submits — caller should call userRepo.switchToBusiness.
 *        Sheet immediately transitions to the success page (we don't wait for Firestore
 *        ack — Firestore writes are queued and reactive listeners will catch up).
 * @param onCreateCampaignClick CTA on the success page — open the campaign creation flow.
 * @param onBrowseDealsClick CTA on the success page — close sheet, user can browse Feed deals.
 * @param onDismiss called when the sheet is closed (skip / swipe / after both success CTAs).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SwitchToBusinessSheet(
    welcomeBonusAlreadyGranted: Boolean,
    onConfirm: (BusinessData) -> Unit,
    onCreateCampaignClick: () -> Unit,
    onBrowseDealsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var companyName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    // Phase: false = Form, true = Success page
    var isSuccessPhase by remember { mutableStateOf(false) }

    val canSubmit = companyName.trim().isNotEmpty() &&
        category.trim().isNotEmpty() && !isSubmitting

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
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
        if (isSuccessPhase) {
            SuccessPage(
                bonusGranted = !welcomeBonusAlreadyGranted,
                onCreateCampaign = onCreateCampaignClick,
                onBrowseDeals = onBrowseDealsClick
            )
            return@ModalBottomSheet
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero bubble — gradient + megaphone
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppViolet, AppTeal),
                            start = Offset(0f, 0f),
                            end = Offset(220f, 220f)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📢", fontSize = 32.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Become a business",
                style = AppTextStyles.Heading2.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp
                ),
                color = AppDark,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Launch ad campaigns, pay TrustCoins to micro-influencers in your pack.",
                style = AppTextStyles.BodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = AppMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Quick benefits — 3 bullets
            BenefitRow("Run native ad campaigns paid in TrustCoins")
            Spacer(Modifier.height(8.dp))
            BenefitRow("Reach pack members who actually trust you")
            Spacer(Modifier.height(8.dp))
            BenefitRow("Switch back to personal anytime")

            Spacer(Modifier.height(24.dp))

            // Field — Company name
            FieldLabel("Company name")
            Spacer(Modifier.height(6.dp))
            FormTextField(
                value = companyName,
                onValueChange = { companyName = it },
                placeholder = "e.g. Brew & Co"
            )

            Spacer(Modifier.height(16.dp))

            // Field — Category (chips + free text)
            FieldLabel("Category")
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PRESET_CATEGORIES.forEach { preset ->
                    val selected = category.equals(preset, ignoreCase = true)
                    CategoryChip(
                        label = preset,
                        selected = selected,
                        onClick = { category = if (selected) "" else preset }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            FormTextField(
                value = category,
                onValueChange = { category = it },
                placeholder = "Or type your own"
            )

            Spacer(Modifier.height(24.dp))

            // CTA — gradient button, disabled state when empty
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        if (canSubmit) {
                            Brush.linearGradient(
                                colors = listOf(AppViolet, AppTeal),
                                start = Offset(0f, 0f),
                                end = Offset(800f, 0f)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    AppMuted.copy(alpha = 0.30f),
                                    AppMuted.copy(alpha = 0.30f)
                                )
                            )
                        }
                    )
                    .clickable(enabled = canSubmit) {
                        isSubmitting = true
                        onConfirm(
                            BusinessData(
                                companyName = companyName.trim(),
                                category = category.trim(),
                                address = "",
                                businessAvatar = ""
                            )
                        )
                        // Don't wait for Firestore ack — switch to success phase immediately.
                        // The reactive getUserStream will catch up; meanwhile the user sees
                        // the success state with bonus reveal and the next-step CTAs.
                        isSubmitting = false
                        isSuccessPhase = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = AppWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Become a business",
                        style = AppTextStyles.Heading2.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppWhite
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Secondary — Cancel
            Text(
                text = "Maybe later",
                style = AppTextStyles.BodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = AppMuted,
                modifier = Modifier
                    .clickable(enabled = !isSubmitting) { onDismiss() }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(AppGold.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = AppGold,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = AppTextStyles.BodySmall.copy(fontSize = 13.sp),
            color = AppDark
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = AppTextStyles.BodySmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        ),
        color = AppMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp)
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceMuted)
            .border(1.dp, AppMuted.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        singleLine = true,
        cursorBrush = SolidColor(AppViolet),
        textStyle = AppTextStyles.BodyMedium.copy(color = AppDark),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = AppTextStyles.BodyMedium.copy(fontSize = 15.sp),
                        color = AppMuted
                    )
                }
                inner()
            }
        }
    )
}

/**
 * Success phase — shown after the user submitted the form.
 * Reveals the welcome bonus (when applicable), explains how the platform works,
 * and lets the user pick the next step (create campaign vs browse deals to promote).
 */
@Composable
private fun SuccessPage(
    bonusGranted: Boolean,
    onCreateCampaign: () -> Unit,
    onBrowseDeals: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero — gradient circle with check
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppViolet, AppTeal),
                        start = Offset(0f, 0f),
                        end = Offset(220f, 220f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = AppWhite,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "You're a business now",
            style = AppTextStyles.Heading2.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp
            ),
            color = AppDark,
            textAlign = TextAlign.Center
        )

        if (bonusGranted) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppGold.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "🪙 +50 TC welcome starter pack credited",
                    style = AppTextStyles.BodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = AppGold
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "How TrustList works",
            style = AppTextStyles.BodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = AppDark
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Spend TrustCoins to launch a campaign — local micro-influencers post about you. " +
                "Or earn coins by promoting other businesses your pack would love.",
            style = AppTextStyles.BodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = AppMuted,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // Primary CTA — Create campaign
        Box(
            modifier = Modifier
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
                .clickable { onCreateCampaign() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Create my first campaign",
                style = AppTextStyles.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = AppWhite
            )
        }

        Spacer(Modifier.height(10.dp))

        // Secondary CTA — Browse deals
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(SurfaceMuted)
                .clickable { onBrowseDeals() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Browse deals to promote",
                style = AppTextStyles.BodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = AppDark
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) AppViolet.copy(alpha = 0.12f) else SurfaceMuted
            )
            .border(
                width = 1.dp,
                color = if (selected) AppViolet else AppMuted.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = AppTextStyles.BodySmall.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (selected) AppViolet else AppDark
        )
    }
}
