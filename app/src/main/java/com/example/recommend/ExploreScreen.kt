package com.example.recommend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.RichPastelCoral
import com.example.recommend.ui.theme.SoftPastelMint
import com.example.recommend.ui.theme.SurfaceMuted
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        if (currentUser == null) return@LaunchedEffect

        db.trustListDataRoot()
            .collection("users").document(currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    currentUserProfile = snapshot.toUserProfileOrNull()
                }
            }

        db.trustListDataRoot()
            .collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val loadedUsers = snapshot.documents.mapNotNull { it.toUserProfileOrNull() }
                    users = loadedUsers
                }
            }
    }

    fun toggleFollow(targetUid: String) {
        if (currentUser == null || currentUserProfile == null) return

        val userRef = db.trustListDataRoot()
            .collection("users").document(currentUser.uid)

        val isFollowing = currentUserProfile!!.following.contains(targetUid)

        if (isFollowing) {
            userRef.update("following", FieldValue.arrayRemove(targetUid))
        } else {
            userRef.update("following", FieldValue.arrayUnion(targetUid))
        }
    }

    val filteredUsers = users.filter { it.uid != currentUser?.uid }.filter {
        searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.handle.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftPastelMint)
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp)
    ) {
        Text("Community", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name or @handle...", style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.45f)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MutedPastelTeal) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                focusedBorderColor = scheme.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                Text("No users found.", style = AppTextStyles.BodyMedium, color = DarkPastelAnthracite.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredUsers) { otherUser ->
                    val isFollowing = currentUserProfile?.following?.contains(otherUser.uid) == true

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                AsyncImage(
                                    model = otherUser.avatar.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${otherUser.uid}" },
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(56.dp).clip(CircleShape).background(SurfaceMuted)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(otherUser.name, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(otherUser.handle, color = RichPastelCoral, style = AppTextStyles.BodySmall, fontWeight = FontWeight.Medium)
                                    if (otherUser.bio.isNotEmpty()) {
                                        Text(otherUser.bio, style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.55f), maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { toggleFollow(otherUser.uid) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) SurfaceMuted else scheme.primary,
                                    contentColor = if (isFollowing) DarkPastelAnthracite else scheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(if (isFollowing) "Unfollow" else "Follow", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
