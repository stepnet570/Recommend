package com.example.recommend.data.repository

import android.util.Log
import com.example.recommend.data.model.Answer
import com.example.recommend.data.model.PackRequest
import com.example.recommend.trustListDataRoot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


class RequestRepository(private val db: FirebaseFirestore) {

    fun getRequestsStream(): Flow<List<PackRequest>> = callbackFlow {
        // No orderBy — avoids requiring a Firestore composite index.
        // Sorting is done client-side so real-time updates always work.
        val listener = db.trustListDataRoot()
            .collection("requests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RequestRepository", "Snapshot listener error: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val requests = snapshot.documents
                        .mapNotNull { doc ->
                            PackRequest(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                text = doc.getString("text") ?: "",
                                tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String }
                                    ?: emptyList(),
                                location = doc.getString("location") ?: "",
                                selectedUsers = (doc.get("selectedUsers") as? List<*>)
                                    ?.mapNotNull { it as? String } ?: emptyList(),
                                status = doc.getString("status") ?: "active",
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        }
                        .sortedByDescending { it.createdAt } // newest first, no index needed
                    Log.d("RequestRepository", "Snapshot received: ${requests.size} requests")
                    trySend(requests)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAnswersStream(): Flow<List<Answer>> = callbackFlow {
        val listener = db.trustListDataRoot()
            .collection("answers")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.map { doc ->
                        Answer(
                            id = doc.id,
                            requestId = doc.getString("requestId") ?: "",
                            userId = doc.getString("userId") ?: "",
                            text = doc.getString("text") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    })
                }
            }
        awaitClose { listener.remove() }
    }
}
