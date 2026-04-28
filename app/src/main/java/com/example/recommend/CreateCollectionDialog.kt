package com.example.recommend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.recommend.data.repository.CollectionRepository
import com.example.recommend.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * Универсальный диалог создания коллекции в стиле Artisan Pastel.
 *
 * @param parentId если задан — создаём подколлекцию внутри [parentId]
 * @param parentName имя родителя (для подзаголовка), null для root
 */
@Composable
fun CreateCollectionDialog(
    parentId: String? = null,
    parentName: String? = null,
    onDismiss: () -> Unit,
    onCreated: (collectionId: String) -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val db = remember { FirebaseFirestore.getInstance() }
    val repo = remember { CollectionRepository(db) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val isSubCollection = parentId != null
    val canSubmit = name.isNotBlank() && !isSaving

    fun submit() {
        val n = name.trim()
        if (n.isEmpty() || isSaving) return
        isSaving = true
        scope.launch {
            try {
                val newId = repo.createCollection(uid, n, parentId)
                onCreated(newId)
                onDismiss()
            } catch (_: Throwable) {
                isSaving = false
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

                // ── Header pill: gradient icon + title + subtitle ─────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Gradient icon tile (matches FAB / TrustScoreRing visual language)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSubCollection) "📂" else "✨",
                            fontSize = 24.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSubCollection) "New sub-collection" else "New collection",
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = AppDark
                        )
                        if (parentName != null) {
                            Text(
                                text = "inside · $parentName",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = AppViolet,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        } else {
                            Text(
                                text = "Group your picks the way you want",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = AppMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Hairline divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SurfaceMuted)
                )

                // ── Body ──────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "Name",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = AppMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AppTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        placeholder = if (isSubCollection) "e.g. Brunch spots" else "e.g. Restaurants",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${name.length} / 60",
                        fontFamily = BodyFontFamily,
                        fontSize = 11.sp,
                        color = AppMuted,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                // ── Actions ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel — outlined ghost button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, SurfaceMuted, RoundedCornerShape(16.dp))
                            .background(AppWhite)
                            .clickable(enabled = !isSaving) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = AppDark
                        )
                    }

                    // Create — gradient primary button
                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (canSubmit) PrimaryGradient else DisabledGradient)
                            .clickable(enabled = canSubmit) { submit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSaving) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(AppWhite.copy(alpha = 0.4f))
                                )
                            }
                            Text(
                                text = if (isSaving) "Saving…" else "Create",
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
}
