package com.example.recommend.data.repository

import com.example.recommend.data.model.PostCollection
import com.example.recommend.trustListDataRoot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CollectionRepository(private val db: FirebaseFirestore) {

    fun getCollectionsStream(uid: String): Flow<List<PostCollection>> = callbackFlow {
        val listener = db.trustListDataRoot()
            .collection("collections")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { doc ->
                        PostCollection(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            name = doc.getString("name") ?: "",
                            postIds = (doc.get("postIds") as? List<*>)
                                ?.mapNotNull { it as? String } ?: emptyList(),
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    })
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun savePostToCollection(collectionId: String, postId: String): Unit =
        suspendCancellableCoroutine { cont ->
            db.trustListDataRoot()
                .collection("collections")
                .document(collectionId)
                .update("postIds", FieldValue.arrayUnion(postId))
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}
