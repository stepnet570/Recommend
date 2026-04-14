package com.example.recommend

import com.example.recommend.data.model.*
import com.example.recommend.ui.theme.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelTeal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun SaveCollectionDialog(
    postId: String,
    onDismiss: () -> Unit
) {
    var newCollectionName by remember { mutableStateOf("") }
    var collections by remember { mutableStateOf<List<PostCollection>>(emptyList()) }

    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val scheme = MaterialTheme.colorScheme

    val postSavedInAnyCollection = collections.any { it.postIds.contains(postId) }

    LaunchedEffect(Unit) {
        db.trustListDataRoot()
            .collection("collections")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    collections = snapshot.documents.mapNotNull { doc ->
                        PostCollection(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            name = doc.getString("name") ?: "",
                            postIds = (doc.get("postIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                }
            }
    }

    fun createCollection() {
        val name = newCollectionName.trim()
        if (name.isEmpty()) return
        val newCol = hashMapOf(
            "userId" to userId,
            "name" to name,
            "postIds" to listOf(postId),
            "createdAt" to System.currentTimeMillis()
        )
        db.trustListDataRoot()
            .collection("collections").add(newCol)
        newCollectionName = ""
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Save to collection", style = AppTextStyles.Heading2.copy(fontSize = 20.sp))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "New collection",
                    modifier = Modifier.fillMaxWidth(),
                    style = AppTextStyles.BodyMedium,
                    color = DarkPastelAnthracite.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        placeholder = { Text("Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.primary)
                    )
                    Button(
                        onClick = { createCollection() },
                        enabled = newCollectionName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary)
                    ) {
                        Text("Create", fontWeight = FontWeight.SemiBold, color = scheme.onPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Or choose a collection",
                    modifier = Modifier.fillMaxWidth(),
                    style = AppTextStyles.BodyMedium,
                    color = DarkPastelAnthracite.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(collections) { collection ->
                        val isSaved = collection.postIds.contains(postId)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val ref = db.trustListDataRoot()
                                        .collection("collections").document(collection.id)

                                    if (isSaved) {
                                        ref.update("postIds", FieldValue.arrayRemove(postId))
                                    } else {
                                        ref.update("postIds", FieldValue.arrayUnion(postId))
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSaved) SurfaceMuted else AppWhite
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(collection.name, style = AppTextStyles.BodyMedium, fontWeight = FontWeight.Medium)
                                if (isSaved) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MutedPastelTeal)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedPastelTeal)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = postSavedInAnyCollection,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, color = scheme.onPrimary)
                    }
                }
                if (!postSavedInAnyCollection) {
                    Text(
                        "Choose a collection or create a new one",
                        style = AppTextStyles.BodySmall,
                        color = DarkPastelAnthracite.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
