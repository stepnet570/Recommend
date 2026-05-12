package com.example.recommend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.data.model.PostCollection
import com.example.recommend.ui.theme.*

/**
 * Bottom sheet с действиями над постом внутри коллекции.
 * Long-press на пост → этот sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionPostActionsSheet(
    postTitle: String,
    isCurrentCover: Boolean,
    onDismiss: () -> Unit,
    onSetAsCover: () -> Unit,
    onClearCover: () -> Unit = {},
    onMove: () -> Unit,
    onRemove: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppMuted.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = postTitle,
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = AppDark,
                maxLines = 2
            )
            Text(
                text = "What do you want to do with this pick?",
                fontFamily = BodyFontFamily,
                fontSize = 13.sp,
                color = AppMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            ActionRow(
                icon = if (isCurrentCover) Icons.Filled.Star else Icons.Filled.StarBorder,
                tint = AppGold,
                title = if (isCurrentCover) "Remove as cover" else "Set as cover",
                subtitle = if (isCurrentCover)
                    "Use the first post as cover again"
                else
                    "Show this image on the collection card",
                onClick = {
                    if (isCurrentCover) onClearCover() else onSetAsCover()
                }
            )
            ActionRow(
                icon = Icons.Filled.DriveFileMove,
                tint = AppViolet,
                title = "Move to another collection",
                subtitle = "Pick a destination — root or sub",
                onClick = onMove
            )
            ActionRow(
                icon = Icons.Filled.RemoveCircleOutline,
                tint = Color(0xFFC0392B),
                title = "Remove from this collection",
                subtitle = "The post stays in your library",
                onClick = onRemove
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppWhite)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppDark
            )
            Text(
                subtitle,
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                color = AppMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
    Spacer(Modifier.height(10.dp))
}

// ─── Move-to picker ───────────────────────────────────────────────────────────

/**
 * Bottom sheet для выбора целевой коллекции.
 * Показывает дерево root → sub. Текущая коллекция [currentCollectionId] заблокирована.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToCollectionSheet(
    allCollections: List<PostCollection>,
    currentCollectionId: String,
    onDismiss: () -> Unit,
    onPick: (collectionId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ordered = remember(allCollections) { buildCollectionTree(allCollections) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppMuted.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Move to…",
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = AppDark
            )
            Text(
                "Pick a destination",
                fontFamily = BodyFontFamily,
                fontSize = 13.sp,
                color = AppMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (ordered.size <= 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppWhite)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No other collections to move to. Create one first.",
                        fontFamily = BodyFontFamily,
                        fontSize = 13.sp,
                        color = AppMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ordered, key = { it.collection.id }) { node ->
                        MoveTargetRow(
                            collection = node.collection,
                            depth = node.depth,
                            isCurrent = node.collection.id == currentCollectionId,
                            onClick = {
                                if (node.collection.id != currentCollectionId) {
                                    onPick(node.collection.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveTargetRow(
    collection: PostCollection,
    depth: Int,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isCurrent) SurfaceMuted else AppWhite
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (isCurrent) AppViolet.copy(alpha = 0.3f) else SurfaceMuted,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = !isCurrent) { onClick() }
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
                maxLines = 1
            )
            Text(
                "${collection.postIds.size} picks",
                fontFamily = BodyFontFamily,
                fontSize = 11.sp,
                color = AppMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (isCurrent) {
            Icon(
                Icons.Filled.CheckCircle,
                null,
                tint = AppViolet,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── tree ordering ────────────────────────────────────────────────────────────

internal data class TreeNode(val collection: PostCollection, val depth: Int)

/** depth-first: root → его дети сразу за ним. */
internal fun buildCollectionTree(all: List<PostCollection>): List<TreeNode> {
    val byParent = all.groupBy { it.parentId }
    val roots = (byParent[null] ?: emptyList()).sortedByDescending { it.createdAt }
    val result = mutableListOf<TreeNode>()
    roots.forEach { root ->
        result += TreeNode(root, 0)
        val children = (byParent[root.id] ?: emptyList()).sortedByDescending { it.createdAt }
        children.forEach { child -> result += TreeNode(child, 1) }
    }
    return result
}
