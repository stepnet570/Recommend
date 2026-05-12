package com.example.recommend.data.model

/**
 * A user comment on a post.
 *
 * Stored as a document in the subcollection
 * `artifacts/trustlist-production/public/data/posts/{postId}/comments/{commentId}`.
 */
data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
