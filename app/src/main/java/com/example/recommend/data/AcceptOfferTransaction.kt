package com.example.recommend.data

import com.example.recommend.data.model.AdOffer
import com.example.recommend.trustListDataRoot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

sealed class AcceptOfferResult {
    data class Success(val rewardCoins: Int) : AcceptOfferResult()
    object AlreadyAccepted : AcceptOfferResult()
    object OfferFull : AcceptOfferResult()
    object OfferExpired : AcceptOfferResult()
    object NotEnoughCoins : AcceptOfferResult()
    data class Error(val message: String) : AcceptOfferResult()
}

/**
 * Atomically accepts [offer] for [uid]:
 * - validates status, expiry, duplicate acceptance, slot count, and business balance
 * - credits user, debits business, increments acceptedCount, appends uid to acceptedBy
 * - marks offer "completed" when all slots are filled
 * - writes a transaction record to the `transactions` collection
 *
 * All Firestore paths via [trustListDataRoot].
 */
fun acceptOffer(
    db: FirebaseFirestore,
    offer: AdOffer,
    uid: String,
    onResult: (AcceptOfferResult) -> Unit
) {
    val dataRoot = db.trustListDataRoot()
    val offerRef = dataRoot.collection("offers").document(offer.id)
    val userRef = dataRoot.collection("users").document(uid)
    val businessRef = dataRoot.collection("users").document(offer.businessId)
    val transactionsRef = dataRoot.collection("transactions")

    db.runTransaction { transaction ->
        val offerSnap = transaction.get(offerRef)
        val businessSnap = transaction.get(businessRef)

        val status = offerSnap.getString("status") ?: "inactive"
        val expiresAt = offerSnap.getLong("expiresAt") ?: 0L
        @Suppress("UNCHECKED_CAST")
        val acceptedBy = (offerSnap.get("acceptedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val acceptedCount = offerSnap.getLong("acceptedCount")?.toInt() ?: 0
        val maxAcceptances = offerSnap.getLong("maxAcceptances")?.toInt() ?: Int.MAX_VALUE
        val reward = offerSnap.getLong("rewardCoins")?.toInt() ?: 0
        val businessCoins = businessSnap.getLong("trustCoins")?.toInt() ?: 0
        val offerTitle = offerSnap.getString("title") ?: ""
        val businessName = offerSnap.getString("businessName") ?: ""

        when {
            status != "active" -> return@runTransaction "expired"
            expiresAt > 0L && System.currentTimeMillis() > expiresAt -> return@runTransaction "time_expired"
            uid in acceptedBy -> return@runTransaction "already_accepted"
            acceptedCount >= maxAcceptances -> return@runTransaction "full"
            businessCoins < reward -> return@runTransaction "no_coins"
        }

        val newCount = acceptedCount + 1

        transaction.update(userRef, "trustCoins", FieldValue.increment(reward.toLong()))
        transaction.update(businessRef, "trustCoins", FieldValue.increment(-reward.toLong()))

        val offerUpdates = mutableMapOf<String, Any>(
            "acceptedCount" to FieldValue.increment(1L),
            "acceptedBy" to FieldValue.arrayUnion(uid)
        )
        if (newCount >= maxAcceptances) offerUpdates["status"] = "completed"
        transaction.update(offerRef, offerUpdates)

        val txDoc = transactionsRef.document()
        transaction.set(
            txDoc, mapOf(
                "uid" to uid,
                "type" to "offer_reward",
                "amount" to reward,
                "offerId" to offer.id,
                "offerTitle" to offerTitle,
                "businessId" to offer.businessId,
                "businessName" to businessName,
                "createdAt" to System.currentTimeMillis()
            )
        )

        "success:$reward"
    }.addOnSuccessListener { result ->
        val mapped = when {
            result == "already_accepted" -> AcceptOfferResult.AlreadyAccepted
            result == "full" -> AcceptOfferResult.OfferFull
            result == "expired" || result == "time_expired" -> AcceptOfferResult.OfferExpired
            result == "no_coins" -> AcceptOfferResult.NotEnoughCoins
            result.startsWith("success:") -> AcceptOfferResult.Success(
                result.substringAfter("success:").toIntOrNull() ?: 0
            )
            else -> AcceptOfferResult.Error(result)
        }
        onResult(mapped)
    }.addOnFailureListener { e ->
        onResult(AcceptOfferResult.Error(e.message ?: "Transaction failed"))
    }
}
