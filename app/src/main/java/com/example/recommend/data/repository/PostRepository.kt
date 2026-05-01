package com.example.recommend.data.repository

import com.example.recommend.data.model.Post
import com.example.recommend.data.model.toPostFromDoc
import com.example.recommend.trustListDataRoot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PostRepository(private val db: FirebaseFirestore) {

    companion object {
        /** Number of posts per page — first page is real-time, further pages are one-shot. */
        const val PAGE_SIZE = 20L
    }

    /** Real-time stream of the first [PAGE_SIZE] posts, ordered newest-first. */
    fun getPostsStream(): Flow<List<Post>> = callbackFlow {
        val listener = db.trustListDataRoot()
            .collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.map { it.toPostFromDoc() })
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * One-shot fetch of the next page of posts older than [beforeTimestamp].
     * Returns an empty list when there are no more posts.
     */
    suspend fun loadMorePosts(beforeTimestamp: Long): List<Post> =
        suspendCancellableCoroutine { cont ->
            db.trustListDataRoot()
                .collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .whereLessThan("createdAt", beforeTimestamp)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener { snap ->
                    cont.resume(snap.documents.map { it.toPostFromDoc() })
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    fun toggleLike(postId: String, uid: String, currentlyLiked: Boolean) {
        val ref = db.trustListDataRoot().collection("posts").document(postId)
        if (currentlyLiked) {
            ref.update("likesByUser", FieldValue.arrayRemove(uid))
        } else {
            ref.update("likesByUser", FieldValue.arrayUnion(uid))
        }
    }

    suspend fun addPost(post: Post) {
        val map = hashMapOf(
            "userId" to post.userId,
            "title" to post.title,
            "description" to post.description,
            "category" to post.category,
            "location" to post.location,
            "rating" to post.rating,
            "imageUrl" to (post.imageUrl ?: ""),
            "authorName" to post.authorName,
            "authorHandle" to post.authorHandle,
            "sponsored" to post.isSponsored,
            "replyToRequestId" to (post.replyToRequestId ?: ""),
            "resourceUrl" to (post.resourceUrl ?: ""),
            "createdAt" to System.currentTimeMillis()
        )
        suspendCancellableCoroutine { cont ->
            db.trustListDataRoot()
                .collection("posts")
                .add(map)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
