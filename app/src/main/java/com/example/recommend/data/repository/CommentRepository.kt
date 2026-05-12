package com.example.recommend.data.repository

import com.example.recommend.data.model.Comment
import com.example.recommend.trustListDataRoot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for post comments.
 *
 * Storage path:
 *   artifacts/trustlist-production/public/data/posts/{postId}/comments/{commentId}
 */
class CommentRepository(private val db: FirebaseFirestore) {

    /** Real-time stream of comments for a post, oldest first (chat-style). */
    fun getCommentsStream(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = db.trustListDataRoot()
            .collection("posts").document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(
                        snapshot.documents.map { doc ->
                            Comment(
                                id = doc.id,
                                postId = postId,
                                userId = doc.getString("userId") ?: "",
                                text = doc.getString("text") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        }
                    )
                }
            }
        awaitClose { listener.remove() }
    }

    /** Add a new comment to a post. */
    suspend fun addComment(postId: String, userId: String, text: String) {
        val map = hashMapOf(
            "userId" to userId,
            "text" to text,
            "createdAt" to System.currentTimeMillis()
        )
        suspendCancellableCoroutine { cont ->
            db.trustListDataRoot()
                .collection("posts").document(postId)
                .collection("comments")
                .add(map)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
