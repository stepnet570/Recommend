package com.example.recommend.ui.add

import com.example.recommend.*
import com.example.recommend.ui.theme.*
import com.example.recommend.data.model.*

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
import androidx.compose.material.icons.filled.*
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
import com.example.recommend.ui.theme.AppDark
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.concurrent.Executors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddScreen(
    onPostAdded: () -> Unit,
    currentUserProfile: UserProfile? = null,
    onBack: (() -> Unit)? = null,
    /** Pack signal id when user taps Help — stored on post as [Post.replyToRequestId]. */
    requestId: String? = null,
    /** Offer id when creating a sponsored post after accepting a deal. */
    offerId: String? = null,
    /** Offer title shown in the sponsored banner so the user knows what content to create. */
    offerTitle: String? = null,
    isSponsored: Boolean = false
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var isUploading by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> selectedImageUri = uri }

    val compressExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val categories = listOf("🍜 Food", "☕ Coffee", "🌿 Places", "🎉 Events", "🛍 Shopping", "💅 Beauty", "🔧 Services")

    fun writePostDocument(imageUrl: String?) {
        val currentUser = auth.currentUser
        val authorName = currentUserProfile?.name?.takeIf { it.isNotBlank() }
            ?: currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Anonymous"

        val postData = hashMapOf<String, Any>(
            "userId" to (currentUser?.uid ?: ""),
            "title" to title,
            "description" to description,
            "category" to selectedCategory,
            "location" to location,
            "rating" to 5,
            "authorName" to authorName,
            "authorHandle" to "@${authorName.lowercase()}",
            "createdAt" to System.currentTimeMillis()
        )
        if (!imageUrl.isNullOrBlank()) postData["imageUrl"] = imageUrl
        val replyRid = requestId?.trim().orEmpty()
        if (replyRid.isNotEmpty()) postData["replyToRequestId"] = replyRid
        if (isSponsored && !offerId.isNullOrBlank()) {
            postData["isSponsored"] = true
            postData["offerId"] = offerId
        }

        db.trustListDataRoot()
            .collection("posts")
            .add(postData)
            .addOnSuccessListener {
                isUploading = false
                Toast.makeText(context, "Recommendation published!", Toast.LENGTH_SHORT).show()
                onPostAdded()
            }
            .addOnFailureListener { e ->
                isUploading = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun savePostToFirebase() {
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
                                mainHandler.post { writePostDocument(download.toString()) }
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
            writePostDocument(null)
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
            if (onBack != null) {
                IconButton(onClick = onBack, enabled = !isUploading) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppDark)
                }
            } else {
                Spacer(Modifier.width(48.dp))
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
                onClick = { savePostToFirebase() },
                enabled = canPublish
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AppViolet,
                        strokeWidth = 2.dp
                    )
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp, ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Sponsored banner
            if (isSponsored && !offerId.isNullOrBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppGold.copy(alpha = 0.08f))
                            .border(1.dp, AppGold.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(12.dp, 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪙", fontSize = 14.sp)
                        Column {
                            Text(
                                "Sponsored post · deal accepted",
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = AppGold, fontFamily = BodyFontFamily
                            )
                            if (!offerTitle.isNullOrBlank()) {
                                Text(offerTitle, fontSize = 12.sp, color = AppGold.copy(alpha = 0.75f), fontFamily = BodyFontFamily)
                            }
                        }
                    }
                }
            }

            // Reply to signal banner
            if (!requestId.isNullOrBlank()) {
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
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = AppViolet, fontFamily = BodyFontFamily
                        )
                    }
                }
            }

            // Photo
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AddFieldLabel("Photo")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceMuted)
                            .border(2.dp, Color(0xFFDDD8D0), RoundedCornerShape(16.dp))
                            .clickable(enabled = !isUploading) {
                                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected photo",
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
                                Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White)
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🖼️", fontSize = 28.sp, color = AppDark.copy(alpha = 0.4f))
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

            // Category chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddFieldLabel("Category")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val label = cat.substringAfter(" ")
                            val isSelected = selectedCategory == label
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(
                                        if (isSelected) PrimaryGradientLinear
                                        else androidx.compose.ui.graphics.Brush.linearGradient(listOf(AppWhite, AppWhite))
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isSelected) Color.Transparent else Color(0xFFE8E6E0),
                                        RoundedCornerShape(32.dp)
                                    )
                                    .clickable { selectedCategory = label }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    cat,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else AppDark,
                                    fontFamily = BodyFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // Location
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AddFieldLabel("Location")
                    AppTextField(
                        value = location,
                        onValueChange = { location = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "City or address",
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = AppTeal, modifier = Modifier.size(20.dp)) },
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

            // Publish button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            if (canPublish) PrimaryGradientLinear
                            else DisabledGradient
                        )
                        .clickable(enabled = canPublish) { savePostToFirebase() },
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

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
fun AddFieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = AppMuted,
        fontFamily = BodyFontFamily
    )
}

// Legacy — kept for source compatibility, not used in UI
data class CategoryItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val id: String)
