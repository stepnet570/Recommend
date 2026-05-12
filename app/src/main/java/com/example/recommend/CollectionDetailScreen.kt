package com.example.recommend
import com.example.recommend.ui.feed.*
import com.example.recommend.ui.theme.*
import com.example.recommend.data.model.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun CollectionDetailScreen(
    collection: PostCollection,
    posts: List<Post>,
    savedPostIds: Set<String> = emptySet(),
    users: List<UserProfile> = emptyList(),
    subCollections: List<PostCollection> = emptyList(),
    canEdit: Boolean = true,
    onBack: () -> Unit,
    onSaveClick: (String) -> Unit,
    onUserProfileClick: (String) -> Unit = {},
    viewerUid: String? = null,
    onAudienceRate: (String, Int) -> Unit = { _, _ -> },
    onOpenPost: ((String) -> Unit)? = null,
    onCreateSubCollection: () -> Unit = {},
    onSubCollectionClick: (PostCollection) -> Unit = {},
    onAddPost: () -> Unit = {},
    onRename: (newName: String) -> Unit = {},
    onDelete: () -> Unit = {},
    // Tech-debt closure: drag-and-drop замещён long-press → action sheet.
    allCollections: List<PostCollection> = emptyList(),
    onSetCover: (postId: String?) -> Unit = {},
    onMovePost: (postId: String, toCollectionId: String) -> Unit = { _, _ -> },
    onRemovePost: (postId: String) -> Unit = {}
) {
    BackHandler { onBack() }
    var menuOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var actionsForPostId by remember { mutableStateOf<String?>(null) }
    var movePostId by remember { mutableStateOf<String?>(null) }
    // Скрываем "New sub-collection" если уже глубоко (parentId != null = это сама подколлекция)
    val isAlreadySubCollection = collection.parentId != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding()
    ) {
        // ── Top bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppDark)
            }
            Text(
                text = if (isAlreadySubCollection) "Sub-collection" else "Collection",
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AppDark,
                modifier = Modifier.weight(1f)
            )
            if (canEdit) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", tint = AppDark)
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add post") },
                            leadingIcon = {
                                Icon(Icons.Filled.NoteAdd, null, tint = AppViolet, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                menuOpen = false
                                onAddPost()
                            }
                        )
                        if (!isAlreadySubCollection) {
                            DropdownMenuItem(
                                text = { Text("New sub-collection") },
                                leadingIcon = {
                                    Icon(Icons.Filled.CreateNewFolder, null, tint = AppViolet, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    menuOpen = false
                                    onCreateSubCollection()
                                }
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { moreOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = AppDark)
                    }
                    DropdownMenu(
                        expanded = moreOpen,
                        onDismissRequest = { moreOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = {
                                Icon(Icons.Filled.DriveFileRenameOutline, null, tint = AppViolet, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                moreOpen = false
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFC0392B)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, null, tint = Color(0xFFC0392B), modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                moreOpen = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }
        }

        if (showRenameDialog) {
            RenameCollectionDialog(
                currentName = collection.name,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    onRename(newName)
                    showRenameDialog = false
                }
            )
        }
        if (showDeleteConfirm) {
            DeleteCollectionConfirmDialog(
                collectionName = collection.name,
                hasSubCollections = subCollections.isNotEmpty(),
                onDismiss = { showDeleteConfirm = false },
                onConfirm = {
                    showDeleteConfirm = false
                    onDelete()
                }
            )
        }
        // Long-press на пост → action sheet
        actionsForPostId?.let { pid ->
            val targetPost = posts.find { it.id == pid }
            if (targetPost != null) {
                CollectionPostActionsSheet(
                    postTitle = targetPost.title,
                    isCurrentCover = (collection.coverPostId == pid),
                    onDismiss = { actionsForPostId = null },
                    onSetAsCover = {
                        onSetCover(pid)
                        actionsForPostId = null
                    },
                    onClearCover = {
                        onSetCover(null)
                        actionsForPostId = null
                    },
                    onMove = {
                        movePostId = pid
                        actionsForPostId = null
                    },
                    onRemove = {
                        onRemovePost(pid)
                        actionsForPostId = null
                    }
                )
            }
        }
        // Move-to picker
        movePostId?.let { pid ->
            MoveToCollectionSheet(
                allCollections = allCollections,
                currentCollectionId = collection.id,
                onDismiss = { movePostId = null },
                onPick = { destinationId ->
                    onMovePost(pid, destinationId)
                    movePostId = null
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Collection header ─────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    AppViolet.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        // "📂 N picks" pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(AppViolet.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "📂 ${posts.size} picks",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppViolet,
                                fontFamily = BodyFontFamily
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Collection name
                        Text(
                            text = collection.name,
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = AppDark
                        )

                        // Subtitle: by user · date
                        val ownerUser = users.find { it.uid == collection.userId }
                        val ownerName = ownerUser?.name?.ifBlank { null }
                            ?: collection.userId.take(8)
                        Text(
                            text = "By $ownerName",
                            fontSize = 13.sp,
                            color = AppMuted,
                            fontFamily = BodyFontFamily,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )

                        // ── Cover mosaic ──────────────────────────────────
                        // Если задана обложка — она становится главной плиткой (flex:2),
                        // остальные посты идут в боковой колонке.
                        if (posts.isNotEmpty()) {
                            val coverPost = collection.coverPostId
                                ?.let { id -> posts.find { it.id == id } }
                            val mainPost = coverPost ?: posts.firstOrNull()
                            val sidePosts = posts.filter { it.id != mainPost?.id }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Main tile (flex: 2)
                                CollectionCoverTile(
                                    post = mainPost,
                                    modifier = Modifier
                                        .weight(2f)
                                        .fillMaxHeight(),
                                    emojiSize = 40.sp
                                )

                                // Side column (flex: 1)
                                if (sidePosts.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CollectionCoverTile(
                                            post = sidePosts.getOrNull(0),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            emojiSize = 24.sp
                                        )
                                        if (sidePosts.size > 1) {
                                            CollectionCoverTile(
                                                post = sidePosts.getOrNull(1),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                emojiSize = 24.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Sub-collections row ───────────────────────────────────────
            if (!isAlreadySubCollection && (subCollections.isNotEmpty() || canEdit)) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Sub-collections",
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppDark
                        )
                        if (subCollections.isNotEmpty()) {
                            Text(
                                "${subCollections.size}",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = AppMuted
                            )
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(subCollections, key = { it.id }) { sub ->
                            SubCollectionPill(
                                collection = sub,
                                onClick = { onSubCollectionClick(sub) }
                            )
                        }
                        if (canEdit) {
                            item(key = "__add__") {
                                AddSubCollectionPill(onClick = onCreateSubCollection)
                            }
                        }
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────
            if (posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No posts in this collection yet",
                            style = AppTextStyles.BodyMedium,
                            color = AppDark.copy(alpha = 0.55f)
                        )
                    }
                }
            } else {
                // ── Post list (compact cards) ─────────────────────────────
                item {
                    Spacer(Modifier.height(4.dp))
                }
                items(posts, key = { it.id }) { post ->
                    CollectionPostItem(
                        post = post,
                        isCover = post.id == collection.coverPostId,
                        onClick = { onOpenPost?.invoke(post.id) },
                        onLongClick = if (canEdit) {
                            { actionsForPostId = post.id }
                        } else null,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp)
                    )
                }
            }
        }
    }
}

// ── Cover mosaic tile ─────────────────────────────────────────────────────────

@Composable
private fun CollectionCoverTile(
    post: Post?,
    modifier: Modifier = Modifier,
    emojiSize: androidx.compose.ui.unit.TextUnit
) {
    val emoji = remember(post?.category) { categoryEmojiFor(post?.category ?: "") }
    val bgBrush = remember(post?.category) { categoryGradientFor(post?.category ?: "") }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(0.dp)) // Clipped by parent row
            .background(bgBrush),
        contentAlignment = Alignment.Center
    ) {
        if (!post?.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = post!!.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(emoji, fontSize = emojiSize)
        }
    }
}

// ── Compact post item ─────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionPostItem(
    post: Post,
    onClick: () -> Unit,
    isCover: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val trustScore = remember(post.ratingsByUser) {
        if (post.ratingsByUser.isEmpty()) 0f
        else ((post.averageAudienceRatingStars() ?: 0) * 2).toFloat().coerceIn(0f, 10f)
    }
    val emoji = categoryEmojiFor(post.category)
    val bgBrush = categoryGradientFor(post.category)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = AppWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgBrush),
                contentAlignment = Alignment.Center
            ) {
                if (!post.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(emoji, fontSize = 24.sp)
                }
            }

            // Title + location
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.title,
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AppDark,
                    maxLines = 1
                )
                if (post.location.isNotBlank()) {
                    Text(
                        text = "📍 ${post.location}",
                        fontSize = 11.sp,
                        color = AppMuted,
                        fontFamily = BodyFontFamily,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1
                    )
                }
            }

            // Cover badge — звёздочка справа над рингом, если этот пост — обложка
            if (isCover) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppGold.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("★", fontSize = 18.sp, color = AppGold)
                }
            } else {
                // TrustScore ring
                TrustScoreRing(score = trustScore, size = 36.dp, strokeWidth = 3.5.dp)
            }
        }
    }
}

// ── Sub-collection chips ──────────────────────────────────────────────────────

@Composable
private fun SubCollectionPill(
    collection: PostCollection,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppWhite,
        shadowElevation = 1.dp,
        modifier = Modifier
            .clickable { onClick() }
            .widthIn(min = 130.dp, max = 180.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppViolet.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📂", fontSize = 18.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = AppDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${collection.postIds.size} picks",
                    fontFamily = BodyFontFamily,
                    fontSize = 11.sp,
                    color = AppMuted,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun AddSubCollectionPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.5.dp,
                color = AppViolet.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .background(AppViolet.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(AppViolet.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = AppViolet,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "New",
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = AppViolet,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Rename / Delete dialogs (Artisan Pastel style) ────────────────────────────

@Composable
private fun RenameCollectionDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    val canSubmit = name.isNotBlank() && name.trim() != currentName

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(AppBackground)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryGradient),
                        contentAlignment = Alignment.Center
                    ) { Text("✏️", fontSize = 22.sp) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Rename collection",
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = AppDark
                        )
                        Text(
                            "Give it a clearer name",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = AppMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    AppTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        placeholder = "Collection name",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, SurfaceMuted, RoundedCornerShape(16.dp))
                            .background(AppWhite)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Cancel",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = AppDark
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (canSubmit) PrimaryGradient else DisabledGradient)
                            .clickable(enabled = canSubmit) { onConfirm(name.trim()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Save",
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (canSubmit) AppWhite else AppOnDisabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteCollectionConfirmDialog(
    collectionName: String,
    hasSubCollections: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val danger = Color(0xFFC0392B)
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(AppBackground)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(danger.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = danger,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Delete \"$collectionName\"?",
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = AppDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hasSubCollections)
                                "Posts stay in your library. Sub-collections will move to root."
                            else
                                "Posts stay in your library. The collection itself is removed.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = AppMuted,
                            modifier = Modifier.padding(top = 4.dp),
                            lineHeight = 16.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, SurfaceMuted, RoundedCornerShape(16.dp))
                            .background(AppWhite)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Cancel",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = AppDark
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(danger)
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Delete",
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppWhite
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun categoryEmojiFor(category: String): String = when (category.lowercase()) {
    "food"     -> "🍜"
    "coffee"   -> "☕"
    "places"   -> "🌿"
    "events"   -> "🎉"
    "shopping" -> "🛍"
    "beauty"   -> "💅"
    "services" -> "🔧"
    else       -> "📍"
}

private fun categoryGradientFor(category: String): Brush =
    Brush.linearGradient(
        when (category.lowercase()) {
            "food"     -> listOf(Color(0xFFF5E6D3), Color(0xFFEDD5B3))
            "coffee"   -> listOf(Color(0xFFE8D5C4), Color(0xFFD4B896))
            "places"   -> listOf(Color(0xFFD4ECD4), Color(0xFFB8D8B8))
            "events"   -> listOf(Color(0xFFF0E0F0), Color(0xFFDEC8DE))
            "shopping" -> listOf(Color(0xFFE0EAF5), Color(0xFFC8D8EE))
            "beauty"   -> listOf(Color(0xFFF5E0F0), Color(0xFFEDC8E8))
            "services" -> listOf(Color(0xFFE8EAE0), Color(0xFFD4D8C4))
            else       -> listOf(
                AppViolet.copy(alpha = 0.12f),
                AppTeal.copy(alpha = 0.12f)
            )
        }
    )
