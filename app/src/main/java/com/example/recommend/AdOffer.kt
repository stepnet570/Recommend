package com.example.recommend

import com.google.firebase.firestore.DocumentSnapshot

data class AdOffer(
    val id: String = "",
    val businessId: String = "",
    val businessName: String = "",
    val title: String = "",
    val description: String = "",
    val rewardCoins: Int = 0,
    val minTrustScore: Double = 0.0,
    val status: String = "active",
    val createdAt: Long = 0L
)

fun DocumentSnapshot.toAdOfferOrNull(): AdOffer? {
    if (!exists()) return null
    return AdOffer(
        id = id,
        businessId = getString("businessId") ?: "",
        businessName = getString("businessName") ?: "",
        title = getString("title") ?: "",
        description = getString("description") ?: "",
        rewardCoins = getLong("rewardCoins")?.toInt() ?: 0,
        minTrustScore = getDouble("minTrustScore") ?: 0.0,
        status = getString("status") ?: "active",
        createdAt = getLong("createdAt") ?: 0L
    )
}
