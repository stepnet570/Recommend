package com.example.recommend

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
import androidx.compose.material.icons.filled.AddPhotoAlternate
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.BodyFontFamily
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.SoftPastelMint
import com.example.recommend.ui.theme.SurfaceMuted
import com.example.recommend.ui.theme.PrimaryGradient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.concurrent.Executors

/**
 * Standalone flow for adding a “pick” to a pack signal — not the main [AddScreen] recommendation form.
 * Collects title, photo, description, and an optional resource link; saves as a `posts` doc with [Post.replyToRequestId].
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val scheme = MaterialTheme.colorScheme

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
                    context,
                    uri,
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

    Scaffold(
        containerColor = SoftPastelMint,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add a pick",
                        style = AppTextStyles.BodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkPastelAnthracite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss, enabled = !isUploading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MutedPastelTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoftPastelMint,
                    titleContentColor = DarkPastelAnthracite,
                    navigationIconContentColor = MutedPastelTeal
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MutedPastelTeal.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Reply to pack signal",
                        style = AppTextStyles.BodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedPastelTeal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "PHOTO",
                            style = AppTextStyles.BodySmall,
                            color = DarkPastelAnthracite.copy(alpha = 0.45f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceMuted)
                                .border(2.dp, SurfaceMuted, RoundedCornerShape(20.dp))
                                .clickable(enabled = !isUploading) {
                                    pickImage.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = MutedPastelTeal,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Tap to add a photo (optional)",
                                        style = AppTextStyles.BodySmall,
                                        color = DarkPastelAnthracite.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "TITLE",
                            style = AppTextStyles.BodySmall,
                            color = DarkPastelAnthracite.copy(alpha = 0.45f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            placeholder = { Text("Name of the place or tip") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.primary),
                            enabled = !isUploading
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "DESCRIPTION",
                            style = AppTextStyles.BodySmall,
                            color = DarkPastelAnthracite.copy(alpha = 0.45f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            placeholder = { Text("Why you recommend it…") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.primary),
                            enabled = !isUploading
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "LINK",
                            style = AppTextStyles.BodySmall,
                            color = DarkPastelAnthracite.copy(alpha = 0.45f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = resourceUrl,
                            onValueChange = { resourceUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            placeholder = { Text("https://…") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.primary),
                            enabled = !isUploading
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { submit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(brush = PrimaryGradient, shape = RoundedCornerShape(16.dp)),
                    enabled = !isUploading && title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF1A2A24),
                        disabledContainerColor = Color(0xFFE8F5F0),
                        disabledContentColor = Color(0xFF6B8C80)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF1A2A24),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Publish pick",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1A2A24)
                        )
                    }
                }
            }
        }
    }
}
