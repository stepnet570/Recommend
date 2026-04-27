package com.example.recommend

import com.example.recommend.data.model.*
import com.example.recommend.ui.theme.*
import com.example.recommend.ui.add.AddFieldLabel

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recommend.ui.theme.AppTextStyles
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.concurrent.Executors

/**
 * Standalone flow for adding a "pick" to a pack signal.
 * Redesigned to match the Artisan Pastel flat design system.
 */
@Composable
fun AddPickScreen(
    requestId: String,
    currentUserProfile: UserProfile?,
    onDismiss: () -> Unit,
    onPickCreated: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var resourceUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> selectedImageUri = uri }

    val compressExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun writePickDocument(imageUrl: String?) {
        val currentUser = auth.currentUser
        val authorName = currentUserProfile?.name?.takeIf { it.isNotBlank() }
            ?: currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Anonymous"

        val postData = hashMapOf<String, Any>(
            "userId" to (currentUser?.uid ?: ""),
            "title" to title.trim(),
            "description" to description.trim(),
            "category" to "pick",
            "location" to "",
            "rating" to 5,
            "authorName" to authorName,
            "authorHandle" to "@${authorName.lowercase()}",
            "createdAt" to System.currentTimeMillis(),
            "replyToRequestId" to requestId
        )
        if (!imageUrl.isNullOrBlank()) postData["imageUrl"] = imageUrl
        val link = resourceUrl.trim()
        if (link.isNotEmpty()) postData["resourceUrl"] = link

        db.trustListDataRoot()
            .collection("posts")
            .add(postData)
            .addOnSuccessListener {
                isUploading = false
                Toast.makeText(context, "Pick added!", Toast.LENGTH_SHORT).show()
                onPickCreated()
                onDismiss()
            }
            .addOnFailureListener { e ->
                isUploading = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun submit() {
        if (title.isBlank()) {
            Toast.makeText(context, "Add a title", Toast.LENGTH_SHORT).show()
            return
        }
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(context, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }
        isUploading = true
        val uri = selectedImageUri
        if (uri != null) {
            compressExecutor.execute {
                val bytes = ImageCompress.compressUriToJpeg(
                    context, uri,
                    ImageCompress.POST_MAX_SIDE_PX,
                    ImageCompress.POST_JPEG_QUALITY
                )
                if (bytes == null || bytes.isEmpty()) {
                    mainHandler.post {
                        isUploading = false
                        Toast.makeText(context, "Could not process image", Toast.LENGTH_SHORT).show()
                    }
                    return@execute
                }
                val path = "posts/${currentUser.uid}/${System.currentTimeMillis()}.jpg"
                val ref = storage.reference.child(path)
                val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
                ref.putBytes(bytes, metadata)
                    .addOnSuccessListener {
                        ref.downloadUrl
                            .addOnSuccessListener { download ->
                                mainHandler.post { writePickDocument(download.toString()) }
                            }
                            .addOnFailureListener { e ->
                                mainHandler.post {
                                    isUploading = false
                                    Toast.makeText(context, "Photo URL: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        mainHandler.post {
                            isUploading = false
                            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        } else {
            writePickDocument(null)
        }
    }

    val canPublish = !isUploading && title.isNotBlank()

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
            IconButton(onClick = onDismiss, enabled = !isUploading) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppDark)
            }
            Text(
                "Add a Pick",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = AppDark
            )
            TextButton(
                onClick = { submit() },
                enabled = canPublish
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AppViolet, strokeWidth = 2.dp)
                } else {
                    Text(
                        "Publish",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (canPublish) AppViolet else AppMuted
                    )
                }
            }
        }

        // ── Content ──────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Reply to signal banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppViolet.copy(alpha = 0.06f))
                        .border(1.dp, AppViolet.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .padding(12.dp, 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🐺", fontSize = 14.sp)
                    Text(
                        "Reply to pack signal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppViolet,
                        fontFamily = BodyFontFamily
                    )
                }
            }

            // Photo
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AddFieldLabel("Photo")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(AppWhite)
                            .border(1.5.dp, Color(0xFFE8E6E2), RoundedCornerShape(20.dp))
                            .clickable(enabled = !isUploading) {
                                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White)
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🖼️", fontSize = 32.sp)
                                Text(
                                    "Tap to add a photo (optional)",
                                    fontSize = 13.sp,
                                    color = AppMuted,
                                    fontFamily = BodyFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // Title
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AddFieldLabel("Title")
                    AppTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Name of the place or tip",
                        singleLine = true,
                        enabled = !isUploading,
                        containerColor = AppWhite
                    )
                }
            }

            // Description
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AddFieldLabel("Why do you recommend it?")
                    AppTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Share what makes it special…",
                        singleLine = false,
                        minLines = 4,
                        enabled = !isUploading,
                        containerColor = AppWhite
                    )
                }
            }

            // Link (optional)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AddFieldLabel("Link (optional)")
                    AppTextField(
                        value = resourceUrl,
                        onValueChange = { resourceUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "https://…",
                        singleLine = true,
                        enabled = !isUploading,
                        containerColor = AppWhite
                    )
                }
            }

            // Publish button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (canPublish) PrimaryGradientLinear else DisabledGradient)
                        .clickable(enabled = canPublish) { submit() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "Publish Pick",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (canPublish) Color.White else AppOnDisabled
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
