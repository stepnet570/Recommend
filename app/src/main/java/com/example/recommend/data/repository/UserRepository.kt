package com.example.recommend.data.repository

import com.example.recommend.data.model.UserProfile
import com.example.recommend.ensureUserProfileForAuthUser
import com.example.recommend.data.model.toUserProfileOrNull
import com.example.recommend.trustListDataRoot
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserRepository(private val db: FirebaseFirestore) {

    fun getUserStream(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = db.trustListDataRoot()
            .collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toUserProfileOrNull())
            }
        awaitClose { listener.remove() }
    }

    fun getAllUsersStream(): Flow<List<UserProfile>> = callbackFlow {
        val listener = db.trustListDataRoot()
            .collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toUserProfileOrNull() })
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Unit = suspendCoroutine { cont ->
        db.ensureUserProfileForAuthUser(user) { cont.resume(Unit) }
    }

    /** Count of users whose `following` list contains [uid]. */
    fun getFollowersCountStream(uid: String): Flow<Int> = callbackFlow {
        val listener = db.trustListDataRoot()
            .collection("users")
            .whereArrayContains("following", uid)
            .addSnapshotListener { snapshot, _ -> trySend(snapshot?.size() ?: 0) }
        awaitClose { listener.remove() }
    }
}
