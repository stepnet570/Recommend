package com.example.recommend

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.recommend.data.model.PostCollection
import com.example.recommend.data.repository.CollectionRepository
import com.example.recommend.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun SaveCollectionDialog(
    postId: String,
    onDismiss: () -> Unit
) {
    var newCollectionName by remember { mutableStateOf("") }
    var collections by remember { mutableStateOf<List<PostCollection>>(emptyList()) }
    // Локальный «оптимистичный» сет — отражает желаемое состояние ДО ответа Firestore.
    // Ключ — collectionId, value — true если пост должен быть сохранён в эту коллекцию.
    var optimistic by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val repo = remember { CollectionRepository(db) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val scope = rememberCoroutineScope()

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
                            parentId = doc.getString("parentId"),
                            coverPostId = doc.getString("coverPostId"),
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                    // Если Firestore подтвердил состояние — выкидываем оптимистичные значения
                    // которые с ним совпадают. Несовпадающие оставляем (запись ещё в полёте).
                    optimistic = optimistic.filter { (cid, desired) ->
                        val actual = collections.find { it.id == cid }?.postIds?.contains(postId) == true
                        actual != desired
                    }
                }
            }
    }

    /** Эффективное состояние с учётом оптимистичных правок. */
    fun isSaved(c: PostCollection): Boolean =
        optimistic[c.id] ?: c.postIds.contains(postId)

    val savedAnywhere = collections.any { isSaved(it) }
    val sortedTree = remember(collections) { buildTreeOrder(collections) }

    fun toggleSave(collection: PostCollection) {
        val newDesired = !isSaved(collection)
        // 1) Оптимистично обновляем UI
        optimistic = optimistic + (collection.id to newDesired)
        // 2) Пишем в Firestore
        scope.launch {
            try {
                if (newDesired) {
                    repo.savePostToCollection(collection.id, postId)
                } else {
                    repo.removePostFromCollection(collection.id, postId)
                }
            } catch (e: Throwable) {
                // Откатываем оптимистичный стейт + говорим пользователю
                optimistic = optimistic - collection.id
                Toast.makeText(
                    context,
                    "Couldn't save: ${e.message ?: "network error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun createRootCollection() {
        val name = newCollectionName.trim()
        if (name.isEmpty()) return
        scope.launch {
            try {
                repo.createCollection(uid = userId, name = name, parentId = null, seedPostId = postId)
                newCollectionName = ""
            } catch (e: Throwable) {
                Toast.makeText(context, "Create failed: ${e.message ?: "error"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(AppBackground)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Header ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryGradient),
                        contentAlignment = Alignment.Center
                    ) { Text("📌", fontSize = 22.sp) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Save to collection",
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = AppDark
                        )
                        Text(
                            text = if (savedAnywhere) "Saved · tap another to add to multiple"
                                   else "Tap any collection to save",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = if (savedAnywhere) AppTeal else AppMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))

                // ── Quick-create row ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it.take(60) },
                        placeholder = "New collection name",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    val canCreate = newCollectionName.isNotBlank()
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (canCreate) PrimaryGradient else DisabledGradient)
                            .clickable(enabled = canCreate) { createRootCollection() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Create",
                            tint = if (canCreate) AppWhite else AppOnDisabled
                        )
                    }
                }

                // ── Tree of existing collections ─────────────────────────────
                if (sortedTree.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No collections yet — type a name above to start.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = AppMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedTree, key = { it.collection.id }) { node ->
                            val collection = node.collection
                            val saved = isSaved(collection)
                            val depth = node.depth

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = (depth * 16).dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (saved) AppViolet.copy(alpha = 0.08f) else AppWhite)
                                    .border(
                                        width = if (saved) 1.5.dp else 1.dp,
                                        color = if (saved) AppViolet.copy(alpha = 0.5f) else SurfaceMuted,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { toggleSave(collection) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AppViolet.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (depth > 0) "↳" else "📂",
                                        fontSize = if (depth > 0) 16.sp else 18.sp,
                                        color = AppViolet
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        collection.name,
                                        fontFamily = HeadingFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AppDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    // Счётчик показывает реальное состояние из Firestore.
                                    // Оптимистичное "сохранил/не сохранил" уже передаёт галочка ✓
                                    // справа — счётчик дублировать его смысла нет, и +1 до
                                    // подтверждения Firestore только сбивает пользователя.
                                    Text(
                                        "${collection.postIds.size} picks",
                                        fontFamily = BodyFontFamily,
                                        fontSize = 11.sp,
                                        color = AppMuted,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (saved) AppTeal.copy(alpha = 0.15f) else SurfaceMuted
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (saved) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "Saved",
                                            tint = AppTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Done button ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryGradient)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Done",
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

// ─── tree ordering helpers ────────────────────────────────────────────────────

private data class CollectionNode(val collection: PostCollection, val depth: Int)

/** Сортируем "depth-first": каждый root, сразу за ним его дети. */
private fun buildTreeOrder(all: List<PostCollection>): List<CollectionNode> {
    val byParent = all.groupBy { it.parentId }
    val roots = (byParent[null] ?: emptyList()).sortedByDescending { it.createdAt }
    val result = mutableListOf<CollectionNode>()
    roots.forEach { root ->
        result += CollectionNode(root, 0)
        val children = (byParent[root.id] ?: emptyList()).sortedByDescending { it.createdAt }
        children.forEach { child ->
            result += CollectionNode(child, 1)
        }
    }
    return result
}
