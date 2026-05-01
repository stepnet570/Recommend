package com.example.recommend.data.repository

import android.util.Log
import com.example.recommend.WELCOME_TRUST_COINS_BONUS
import com.example.recommend.data.model.BusinessData
import com.example.recommend.data.model.UserProfile
import com.example.recommend.ensureUserProfileForAuthUser
import com.example.recommend.data.model.toUserProfileOrNull
import com.example.recommend.trustListDataRoot
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
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

    /** Mark the monetization onboarding as seen for [uid] so it does not appear again. */
    fun markMonetizationOnboardingSeen(uid: String) {
        if (uid.isBlank()) return
        db.trustListDataRoot()
            .collection("users")
            .document(uid)
            .update("hasSeenMonetizationOnboarding", true)
    }

    /**
     * Promote a personal account to business mode (atomic via Firestore transaction).
     * Side effects on first-ever switch (welcomeBonusGranted == false):
     *   - +50 TrustCoins welcome bonus
     *   - welcomeBonusGranted set to true (so subsequent switches are bonus-free)
     *
     * Reactive: any open getUserStream(uid) flow will emit the updated profile,
     * so the UI flips from "What's on your mind?" to "Grow your brand" automatically.
     */
    fun switchToBusiness(uid: String, businessData: BusinessData) {
        if (uid.isBlank()) return
        val ref = db.trustListDataRoot().collection("users").document(uid)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val alreadyGranted = (snap.get("welcomeBonusGranted") as? Boolean) ?: false
            val updates = mutableMapOf<String, Any>(
                "isBusiness" to true,
                "businessProfile" to mapOf(
                    "companyName" to businessData.companyName,
                    "category" to businessData.category,
                    "address" to businessData.address,
                    "businessAvatar" to businessData.businessAvatar
                )
            )
            if (!alreadyGranted) {
                val currentCoins = (snap.get("trustCoins") as? Number)?.toInt() ?: 0
                updates["trustCoins"] = currentCoins + WELCOME_TRUST_COINS_BONUS
                updates["welcomeBonusGranted"] = true
                Log.i(
                    "UserRepository",
                    "switchToBusiness: granting welcome bonus +$WELCOME_TRUST_COINS_BONUS TC " +
                        "to uid=$uid (prev=$currentCoins, new=${currentCoins + WELCOME_TRUST_COINS_BONUS})"
                )
            }
            tx.update(ref, updates)
            null
        }.addOnFailureListener { e ->
            Log.e("UserRepository", "switchToBusiness transaction failed for uid=$uid", e)
        }
    }

    /**
     * Revert a business account back to personal.
     * Drops the businessProfile block via FieldValue.delete() so we don't keep stale data.
     */
    fun switchToPersonal(uid: String) {
        if (uid.isBlank()) return
        db.trustListDataRoot()
            .collection("users")
            .document(uid)
            .update(
                mapOf(
                    "isBusiness" to false,
                    "businessProfile" to FieldValue.delete()
                )
            )
    }
}
