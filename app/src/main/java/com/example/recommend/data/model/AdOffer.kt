package com.example.recommend.data.model

import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatAdOfferDate(createdAt: Long): String {
    if (createdAt <= 0L) return ""
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(createdAt))
}

data class AdOffer(
    val id: String = "",
    val businessId: String = "",
    val businessName: String = "",
    val title: String = "",
    val description: String = "",
    val rewardCoins: Int = 0,
    val minTrustScore: Double = 0.0,
    val status: String = "active",
    val createdAt: Long = 0L,
    val durationDays: Int = 30,
    val expiresAt: Long = 0L,
    val maxAcceptances: Int = 100,
    val acceptedCount: Int = 0,
    val acceptedBy: List<String> = emptyList()
) {
    val slotsLeft: Int get() = (maxAcceptances - acceptedCount).coerceAtLeast(0)

    fun isAcceptableBy(uid: String): Boolean =
        status == "active" &&
        (expiresAt == 0L || System.currentTimeMillis() < expiresAt) &&
        uid !in acceptedBy &&
        acceptedCount < maxAcceptances
}

fun DocumentSnapshot.toAdOfferOrNull(): AdOffer? {
    if (!exists()) return null
    @Suppress("UNCHECKED_CAST")
    val acceptedBy = (get("acceptedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    return AdOffer(
        id = id,
        businessId = getString("businessId") ?: "",
        businessName = getString("businessName") ?: "",
        title = getString("title") ?: "",
        description = getString("description") ?: "",
        rewardCoins = getLong("rewardCoins")?.toInt() ?: 0,
        minTrustScore = getDouble("minTrustScore") ?: 0.0,
        status = getString("status") ?: "active",
        createdAt = getLong("createdAt") ?: 0L,
        durationDays = getLong("durationDays")?.toInt() ?: 30,
        expiresAt = getLong("expiresAt") ?: 0L,
        maxAcceptances = getLong("maxAcceptances")?.toInt() ?: 100,
        acceptedCount = getLong("acceptedCount")?.toInt() ?: 0,
        acceptedBy = acceptedBy
    )
}
