package com.example.recommend.data.repository

import com.example.recommend.data.model.AdOffer
import com.example.recommend.data.model.toAdOfferOrNull
import com.example.recommend.trustListDataRoot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OfferRepository(private val db: FirebaseFirestore) {

    fun getOffersForBusiness(uid: String): Flow<List<AdOffer>> = callbackFlow {
        val listener = db.trustListDataRoot()
            .collection("offers")
            .whereEqualTo("businessId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(
                        snapshot.documents
                            .mapNotNull { it.toAdOfferOrNull() }
                            .sortedByDescending { it.createdAt }
                    )
                }
            }
        awaitClose { listener.remove() }
    }

    fun getActiveOffers(): Flow<List<AdOffer>> = callbackFlow {
        val listener = db.trustListDataRoot()
            .collection("offers")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(
                        snapshot.documents
                            .mapNotNull { it.toAdOfferOrNull() }
                            .filter { it.status.equals("active", ignoreCase = true) }
                            .sortedByDescending { it.createdAt }
                    )
                }
            }
        awaitClose { listener.remove() }
    }

    /** Count of offers where [uid] is listed as a promoter. */
    fun getParticipatingOffersCountStream(uid: String): Flow<Int> = callbackFlow {
        val listener = db.trustListDataRoot()
            .collection("offers")
            .whereArrayContains("promoterUserIds", uid)
            .addSnapshotListener { snapshot, _ -> trySend(snapshot?.size() ?: 0) }
        awaitClose { listener.remove() }
    }

    suspend fun acceptOffer(offerId: String, userId: String, coins: Int): Unit =
        suspendCancellableCoroutine { cont ->
            val offerRef = db.trustListDataRoot().collection("offers").document(offerId)
            val userRef = db.trustListDataRoot().collection("users").document(userId)
            db.runTransaction { tx ->
                tx.update(offerRef, "promoterUserIds", FieldValue.arrayUnion(userId))
                tx.update(userRef, "trustCoins", FieldValue.increment(coins.toLong()))
            }
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}
