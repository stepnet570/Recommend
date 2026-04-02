package com.example.recommend

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.SoftPastelMint
import com.example.recommend.ui.theme.SurfaceMuted
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Answer(
    val id: String = "",
    val requestId: String = "",
    val userId: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    request: PackRequest,
    answers: List<Answer>,
    users: List<UserProfile>,
    onBack: () -> Unit,
    onUserProfileClick: (String) -> Unit = {}
) {
    BackHandler(onBack = onBack)

    var answerText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val author = users.find { it.uid == request.userId } ?: UserProfile(name = "Friend")
    val scheme = MaterialTheme.colorScheme

    fun submitAnswer() {
        if (answerText.isBlank() || currentUser == null) return
        isSubmitting = true

        val answerData = hashMapOf(
            "requestId" to request.id,
            "userId" to currentUser.uid,
            "text" to answerText,
            "createdAt" to System.currentTimeMillis()
        )

        db.trustListDataRoot()
            .collection("answers")
            .add(answerData)
            .addOnSuccessListener {
                answerText = ""
                isSubmitting = false
            }
            .addOnFailureListener {
                isSubmitting = false
                Toast.makeText(context, "Failed to send", Toast.LENGTH_SHORT).show()
            }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = SoftPastelMint) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {

            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MutedPastelTeal)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = request.userId.isNotBlank()) { onUserProfileClick(request.userId) }
                ) {
                    AsyncImage(
                        model = author.avatar.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${author.uid}" },
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceMuted)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Signal from ${author.name.split(" ")[0]}", style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkPastelAnthracite)
                        Text("The pack is collecting picks", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.55f))
                    }
                }
            }

            HorizontalDivider(color = SurfaceMuted)

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(request.text, style = AppTextStyles.Heading2.copy(fontSize = 24.sp, lineHeight = 28.sp), color = DarkPastelAnthracite)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                request.tags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(MutedPastelTeal.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(tag, style = AppTextStyles.BodySmall, fontWeight = FontWeight.Bold, color = MutedPastelTeal)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Pack replies (${answers.size})", style = AppTextStyles.Heading2.copy(fontSize = 18.sp), color = DarkPastelAnthracite, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                }

                if (answers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No suggestions yet. Be the first!", style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.55f))
                        }
                    }
                } else {
                    items(answers) { ans ->
                        val ansAuthor = users.find { it.uid == ans.userId } ?: UserProfile(name = "Someone")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(20.dp))
                                .padding(16.dp)
                                .clickable(enabled = ans.userId.isNotBlank()) { onUserProfileClick(ans.userId) }
                        ) {
                            AsyncImage(
                                model = ansAuthor.avatar.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${ansAuthor.uid}" },
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceMuted)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(ansAuthor.name, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(ans.text, style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.85f), lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }

            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = { answerText = it },
                        placeholder = { Text("Your recommendation...", style = AppTextStyles.BodyMedium) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SoftPastelMint,
                            focusedContainerColor = SoftPastelMint,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = scheme.primary
                        ),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (answerText.isNotBlank() && !isSubmitting) scheme.primary else Color.LightGray)
                            .clickable(enabled = answerText.isNotBlank() && !isSubmitting) { submitAnswer() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
