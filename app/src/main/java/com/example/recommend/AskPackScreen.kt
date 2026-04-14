package com.example.recommend

import com.example.recommend.data.model.*
import com.example.recommend.ui.theme.*

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.ConvexCardBox
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.SurfaceMuted
import com.example.recommend.ui.theme.AppTeal
import com.example.recommend.ui.theme.AppLime
import com.example.recommend.ui.theme.PrimaryGradientLinear
import com.example.recommend.ui.theme.DisabledGradient
import com.example.recommend.ui.theme.AppOnDisabled
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AskPackScreen(
    onDismiss: () -> Unit,
    users: List<UserProfile>,
    currentUserProfile: UserProfile?
) {
    BackHandler(onBack = onDismiss)

    var step by remember { mutableStateOf(1) }
    var text by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var selectedUsers by remember { mutableStateOf(setOf<String>()) }
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val availableTags = listOf("🍔 Eat", "🍷 Drink", "🆘 Urgent", "📅 Date", "🔧 Services", "🎉 Party")

    val packUsers = users.filter { currentUserProfile?.following?.contains(it.uid) == true }

    LaunchedEffect(Unit) {
        selectedUsers = packUsers.map { it.uid }.toSet()
    }

    fun submitRequest() {
        val currentUser = auth.currentUser ?: return
        if (selectedUsers.isEmpty()) {
            Toast.makeText(context, "Select at least one friend", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val requestData = hashMapOf(
            "userId" to currentUser.uid,
            "text" to text,
            "tags" to selectedTags.toList(),
            "location" to "Current area",
            "selectedUsers" to selectedUsers.toList(),
            "status" to "active",
            "createdAt" to System.currentTimeMillis()
        )

        db.trustListDataRoot()
            .collection("requests")
            .add(requestData)
            .addOnSuccessListener {
                isSubmitting = false
                Toast.makeText(context, "Signal sent to your pack!", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            .addOnFailureListener { e ->
                isSubmitting = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onDismiss() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = MutedPastelTeal)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.height(6.dp).width(if (step >= 1) 24.dp else 8.dp).clip(CircleShape).background(if (step >= 1) AppTeal else SurfaceMuted))
                    Box(modifier = Modifier.height(6.dp).width(if (step >= 2) 24.dp else 8.dp).clip(CircleShape).background(if (step >= 2) AppTeal else SurfaceMuted))
                }
                Spacer(modifier = Modifier.width(48.dp))
            }

            AnimatedContent(
                targetState = step,
                label = "Steps",
                modifier = Modifier.weight(1f)
            ) { targetStep ->
                when (targetStep) {
                    1 -> {
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

                                OutlinedTextField(
                                    value = text,
                                    onValueChange = { text = it },
                                    placeholder = { Text("What are we looking for today?", fontSize = 28.sp, fontWeight = FontWeight.Black, color = DarkPastelAnthracite.copy(alpha = 0.35f)) },
                                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp),
                                    textStyle = AppTextStyles.Heading2.copy(fontSize = 28.sp, lineHeight = 34.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = DarkPastelAnthracite,
                                        unfocusedTextColor = DarkPastelAnthracite
                                    )
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                ConvexCardBox(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    elevation = 20.dp
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MutedPastelTeal, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Signal options", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, color = DarkPastelAnthracite)
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            availableTags.forEach { tag ->
                                                val isSelected = selectedTags.contains(tag)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(32.dp))
                                                        .background(
                                                            if (isSelected)
                                                                PrimaryGradientLinear
                                                            else
                                                                Brush.linearGradient(listOf(Color.White, Color.White))
                                                        )
                                                        .border(1.dp, if (isSelected) Color.Transparent else SurfaceMuted, RoundedCornerShape(32.dp))
                                                        .clickable { selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag }
                                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                                ) {
                                                    Text(
                                                        tag,
                                                        color = if (isSelected) Color.White else DarkPastelAnthracite,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = SurfaceMuted)

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MutedPastelTeal, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Current area", style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.75f))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 32.dp)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(
                                        if (text.isNotBlank())
                                            PrimaryGradientLinear
                                        else
                                            DisabledGradient
                                    )
                                    .clickable(enabled = text.isNotBlank()) { step = 2 },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Next", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (text.isNotBlank()) Color.White else AppOnDisabled)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = if (text.isNotBlank()) Color.White else AppOnDisabled)
                                }
                            }
                        }
                    }
                    2 -> {
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Who to ask?", style = AppTextStyles.Heading1.copy(fontSize = 32.sp))
                                Text("Choose friends to send the signal to", style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.55f), modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Your pack (${packUsers.size})", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold)
                                    if (packUsers.isNotEmpty()) {
                                        Text(
                                            text = if (selectedUsers.size == packUsers.size) "Clear selection" else "Select all",
                                            color = RichPastelCoral,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { selectedUsers = if (selectedUsers.size == packUsers.size) emptySet() else packUsers.map { it.uid }.toSet() }
                                        )
                                    }
                                }

                                if (packUsers.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("Follow someone first in the People tab", style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.55f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(packUsers) { friend ->
                                            val isSelected = selectedUsers.contains(friend.uid)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White, RoundedCornerShape(24.dp))
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 1.dp,
                                                        brush = if (isSelected)
                                                            PrimaryGradientLinear
                                                        else
                                                            DisabledGradient,
                                                        shape = RoundedCornerShape(24.dp)
                                                    )
                                                    .clickable { selectedUsers = if (isSelected) selectedUsers - friend.uid else selectedUsers + friend.uid }
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    AsyncImage(
                                                        model = friend.avatar.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${friend.uid}" },
                                                        contentDescription = null,
                                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceMuted)
                                                    )
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Column {
                                                        Text(friend.name, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold)
                                                        Text(friend.handle, style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.55f))
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier.size(24.dp).clip(CircleShape)
                                                        .background(
                                                            if (isSelected)
                                                                PrimaryGradientLinear
                                                            else
                                                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                                        )
                                                        .border(2.dp, if (isSelected) Color.Transparent else SurfaceMuted, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(PrimaryGradientLinear)
                                    .clickable(enabled = !isSubmitting) { submitRequest() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Send signal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
