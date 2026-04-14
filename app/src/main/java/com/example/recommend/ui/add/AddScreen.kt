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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelGold
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.SoftPastelMint
import com.example.recommend.ui.theme.SurfaceMuted
import com.example.recommend.ui.theme.AppDark
import com.example.recommend.ui.theme.PrimaryGradient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onPostAdded: () -> Unit,
    currentUserProfile: UserProfile? = null,
    onBack: (() -> Unit)? = null,
    /** Pack signal id when user taps Help — stored on post as [Post.replyToRequestId]. */
    requestId: String? = null,
    /** Offer id when creating a sponsored post after accepting a deal. */
    offerId: String? = null,
    isSponsored: Boolean = false
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var rating by remember { mutableStateOf(5) }
    var isUploading by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

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

    val categories = listOf(
        CategoryItem("Service", Icons.Filled.Build, "service"),
        CategoryItem("Shopping", Icons.Filled.ShoppingCart, "shopping"),
        CategoryItem("Food", Icons.Filled.Favorite, "food"),
        CategoryItem("Beauty", Icons.Filled.Face, "beauty")
    )

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
            "rating" to rating,
            "authorName" to authorName,
            "authorHandle" to "@${authorName.lowercase()}",
            "createdAt" to System.currentTimeMillis()
        )
        if (!imageUrl.isNullOrBlank()) {
            postData["imageUrl"] = imageUrl
        }
        val replyRid = requestId?.trim().orEmpty()
        if (replyRid.isNotEmpty()) {
            postData["replyToRequestId"] = replyRid
        }
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
                                mainHandler.post {
                                    writePostDocument(download.toString())
                                }
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

    Scaffold(
        containerColor = Color.White,
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = {
                        Text(
                            "New recommendation",
                            style = AppTextStyles.BodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkPastelAnthracite
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MutedPastelTeal)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = DarkPastelAnthracite,
                        navigationIconContentColor = MutedPastelTeal
                    )
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = if (onBack != null) 8.dp else 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
                if (isSponsored && !offerId.isNullOrBlank()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MutedPastelGold.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Sponsored post · deal accepted",
                                style = AppTextStyles.BodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MutedPastelGold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (!requestId.isNullOrBlank()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MutedPastelTeal.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Reply to signal",
                                style = AppTextStyles.BodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AppDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (onBack == null) {
                    item {
                        Text("New recommendation", style = AppTextStyles.Heading2.copy(fontSize = 24.sp))
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {

                            Text("PHOTO", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f), letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(SurfaceMuted)
                                    .border(2.dp, SurfaceMuted, RoundedCornerShape(24.dp))
                                    .clickable {
                                        pickImage.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
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

                            Spacer(modifier = Modifier.height(24.dp))

                            Text("PLACE NAME", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f))
                            AppTextField(
                                value = title,
                                onValueChange = { title = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text("CATEGORY", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CategoryButton(categories[0], selectedCategory == categories[0].label) { selectedCategory = it }
                                    CategoryButton(categories[1], selectedCategory == categories[1].label) { selectedCategory = it }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CategoryButton(categories[2], selectedCategory == categories[2].label) { selectedCategory = it }
                                    CategoryButton(categories[3], selectedCategory == categories[3].label) { selectedCategory = it }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text("WHERE IS IT?", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f))
                            AppTextField(
                                value = location,
                                onValueChange = { location = it },
                                leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = AppTeal) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text("YOUR RATING", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f))
                            Row(modifier = Modifier.background(SoftPastelMint, RoundedCornerShape(16.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..5).forEach { i ->
                                    Icon(
                                        Icons.Filled.Star,
                                        null,
                                        tint = if (i <= rating) MutedPastelGold else SurfaceMuted,
                                        modifier = Modifier.size(36.dp).clickable { rating = i }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text("WHY DO YOU RECOMMEND IT?", style = AppTextStyles.BodySmall, color = DarkPastelAnthracite.copy(alpha = 0.45f))
                            AppTextField(
                                value = description,
                                onValueChange = { description = it },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                singleLine = false
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            val canPublish = !isUploading && title.isNotBlank() && description.isNotBlank()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        brush = if (canPublish)
                                            PrimaryGradient
                                        else
                                            Brush.horizontalGradient(listOf(SurfaceMuted, SurfaceMuted))
                                    )
                                    .clickable(enabled = canPublish) { savePostToFirebase() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                else Text(
                                    "Publish",
                                    style = AppTextStyles.BodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (canPublish) Color.White else DarkPastelAnthracite.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }
        }
    }
}

data class CategoryItem(val label: String, val icon: ImageVector, val id: String)

@Composable
fun RowScope.CategoryButton(item: CategoryItem, isSelected: Boolean, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(item.label) },
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .then(
                if (isSelected) Modifier.background(
                    brush = PrimaryGradient,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.Transparent else SurfaceMuted,
            contentColor = if (isSelected) Color.White else DarkPastelAnthracite
        )
    ) {
        Icon(item.icon, contentDescription = null, tint = if (isSelected) Color.White else MutedPastelTeal, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            item.label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isSelected) Color.White else DarkPastelAnthracite
        )
    }
}
