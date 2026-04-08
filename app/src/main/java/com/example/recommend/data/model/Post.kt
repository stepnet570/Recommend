package com.example.recommend.data.model

import kotlin.math.roundToInt

data class Post(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val rating: Int = 5,
    val imageUrl: String? = null,
    val authorName: String = "Alex",
    val authorHandle: String = "@alex",
    val isSponsored: Boolean = false,
    val ratingsByUser: Map<String, Int> = emptyMap(),
    val likesByUser: Set<String> = emptySet(),
    /** Set when this post is a reply to a pack «signal» (request). */
    val replyToRequestId: String? = null,
    /** External link (e.g. website) for signal replies / picks. */
    val resourceUrl: String? = null
)

fun Post.averageAudienceRatingStars(): Int? {
    if (ratingsByUser.isEmpty()) return null
    val avg = ratingsByUser.values.average()
    return avg.roundToInt().coerceIn(1, 5)
}
