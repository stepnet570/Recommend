package com.example.recommend.data.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Maps a Firestore `posts` document to [Post] (shared by feed and request detail).
 */
fun DocumentSnapshot.toPostFromDoc(): Post {
    val ratingsRaw = get("ratings")
    val ratingsByUser: Map<String, Int> = when (ratingsRaw) {
        is Map<*, *> -> ratingsRaw.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val intVal = when (v) {
                is Long -> v.toInt()
                is Int -> v
                else -> null
            } ?: return@mapNotNull null
            key to intVal.coerceIn(1, 5)
        }.toMap()
        else -> emptyMap()
    }
    val likesRaw = get("likes")
    val likesByUser: Set<String> = when (likesRaw) {
        is Map<*, *> -> likesRaw.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val on = when (v) {
                is Boolean -> v
                is Number -> v.toLong() != 0L
                else -> v != null
            }
            if (on) key else null
        }.toSet()
        else -> emptySet()
    }
    val replyToRequestId = getString("replyToRequestId") ?: getString("requestId")
    return Post(
        id = id,
        userId = getString("userId") ?: "",
        title = getString("title") ?: "",
        description = getString("description") ?: "",
        category = getString("category") ?: "Food",
        location = getString("location") ?: "",
        rating = getLong("rating")?.toInt() ?: 5,
        imageUrl = getString("imageUrl"),
        authorName = getString("authorName") ?: "User",
        authorHandle = getString("authorHandle") ?: "@user",
        isSponsored = getBoolean("sponsored") == true,
        ratingsByUser = ratingsByUser,
        likesByUser = likesByUser,
        replyToRequestId = replyToRequestId?.takeIf { it.isNotBlank() },
        resourceUrl = getString("resourceUrl")?.takeIf { it.isNotBlank() }
    )
}
