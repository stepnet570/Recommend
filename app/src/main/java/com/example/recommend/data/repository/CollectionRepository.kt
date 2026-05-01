package com.example.recommend.data.repository

import com.example.recommend.data.model.PostCollection
import com.example.recommend.trustListDataRoot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CollectionRepository(private val db: FirebaseFirestore) {

    private fun collectionsRef() = db.trustListDataRoot().collection("collections")

    private fun DocumentSnapshot.toPostCollection(): PostCollection = PostCollection(
        id = id,
        userId = getString("userId") ?: "",
        name = getString("name") ?: "",
        postIds = (get("postIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        parentId = getString("parentId"),
        coverPostId = getString("coverPostId"),
        createdAt = getLong("createdAt") ?: 0L
    )

    /** Все коллекции пользователя (root + sub в одном потоке — фильтруй на UI по parentId). */
    fun getCollectionsStream(uid: String): Flow<List<PostCollection>> = callbackFlow {
        val listener = collectionsRef()
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.map { it.toPostCollection() })
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Постранично загруженные коллекции пользователя.
     * Сортировка по createdAt DESC, лимит управляется параметром.
     * Используется для UI-пагинации с кнопкой "Load more" (увеличиваем [limit]).
     */
    fun getCollectionsStreamLimited(uid: String, limit: Long): Flow<List<PostCollection>> = callbackFlow {
        val listener = collectionsRef()
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.map { it.toPostCollection() })
                }
            }
        awaitClose { listener.remove() }
    }

    /** Создать коллекцию. Возвращает id новой коллекции. */
    suspend fun createCollection(
        uid: String,
        name: String,
        parentId: String? = null,
        seedPostId: String? = null
    ): String = suspendCancellableCoroutine { cont ->
        val data = hashMapOf<String, Any?>(
            "userId" to uid,
            "name" to name.trim(),
            "postIds" to if (seedPostId != null) listOf(seedPostId) else emptyList<String>(),
            "parentId" to parentId,
            "createdAt" to System.currentTimeMillis()
        )
        collectionsRef().add(data)
            .addOnSuccessListener { ref -> cont.resume(ref.id) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    suspend fun renameCollection(collectionId: String, newName: String): Unit =
        suspendCancellableCoroutine { cont ->
            collectionsRef().document(collectionId)
                .update("name", newName.trim())
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /**
     * Удалить коллекцию. Дети (если есть) промоутятся в root (parentId = null),
     * посты не теряем.
     */
    suspend fun deleteCollection(collectionId: String): Unit =
        suspendCancellableCoroutine { cont ->
            val ref = collectionsRef()
            // 1) сначала находим детей и выставляем им parentId = null
            ref.whereEqualTo("parentId", collectionId).get()
                .addOnSuccessListener { childSnap ->
                    val batch = db.batch()
                    childSnap.documents.forEach { child ->
                        batch.update(child.reference, "parentId", null)
                    }
                    batch.delete(ref.document(collectionId))
                    batch.commit()
                        .addOnSuccessListener { cont.resume(Unit) }
                        .addOnFailureListener { cont.resumeWithException(it) }
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun savePostToCollection(collectionId: String, postId: String): Unit =
        suspendCancellableCoroutine { cont ->
            collectionsRef().document(collectionId)
                .update("postIds", FieldValue.arrayUnion(postId))
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun removePostFromCollection(collectionId: String, postId: String): Unit =
        suspendCancellableCoroutine { cont ->
            collectionsRef().document(collectionId)
                .update("postIds", FieldValue.arrayRemove(postId))
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /** Поставить пост обложкой коллекции. Передай null чтобы сбросить. */
    suspend fun setCoverPost(collectionId: String, postId: String?): Unit =
        suspendCancellableCoroutine { cont ->
            collectionsRef().document(collectionId)
                .update("coverPostId", postId)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /**
     * Атомарно переместить пост из одной коллекции в другую.
     * Используем batch — гарантия что не получим "пост в обеих" или "ни в одной" при сбое сети.
     */
    suspend fun movePostBetweenCollections(
        fromCollectionId: String,
        toCollectionId: String,
        postId: String
    ): Unit = suspendCancellableCoroutine { cont ->
        if (fromCollectionId == toCollectionId) {
            cont.resume(Unit); return@suspendCancellableCoroutine
        }
        val batch = db.batch()
        val from = collectionsRef().document(fromCollectionId)
        val to = collectionsRef().document(toCollectionId)
        batch.update(from, "postIds", FieldValue.arrayRemove(postId))
        batch.update(to, "postIds", FieldValue.arrayUnion(postId))
        batch.commit()
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }
}
