package com.example.recommend.data.repository

import android.util.Log
import com.example.recommend.trustListDataRoot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.min

/**
 * TrustScore engine — клиентская реализация той же формулы, что в Cloud Functions.
 *
 *   trustScore = avg(ratings to user's posts) * activity_coefficient
 *   activity_coefficient = min(1.0, (posts_count / 10) + (followers_count / 50))
 *
 * Шкала: 0..5 (UI домножает на 2 для 0..10 ring'а).
 *
 * Эта реализация — fallback и/или дублирующая защита для случая, если CF
 * ещё не задеплоена или временно недоступна. Cloud Function — основной
 * источник правды (атомарность гарантируется транзакцией там), но т.к.
 * формула чисто детерминированная — двойная запись просто сходится к
 * одному и тому же значению.
 *
 * Триггеры (вызов из соответствующих репозиториев):
 *  - после ratePost(...)            → recalc авторy поста
 *  - после PostRepository.addPost   → recalc автору
 *  - после follow / unfollow        → recalc цели
 */
class TrustScoreRepository(private val db: FirebaseFirestore) {

    /**
     * Пересчитать trustScore для конкретного пользователя.
     * Безопасно вызывать с фронта — операция идемпотентна (формула детерминированная).
     */
    suspend fun recalculateFor(uid: String) {
        if (uid.isBlank()) return
        try {
            // 1) Все посты автора → собираем все рейтинги
            val postsSnap = db.trustListDataRoot()
                .collection("posts")
                .whereEqualTo("userId", uid)
                .get()
                .await()

            var ratingsSum = 0L
            var ratingsCount = 0
            for (doc in postsSnap.documents) {
                val ratings = doc.get("ratings") as? Map<*, *> ?: continue
                for ((_, v) in ratings) {
                    val n = when (v) {
                        is Number -> v.toInt()
                        is String -> v.toIntOrNull() ?: continue
                        else -> continue
                    }
                    if (n in 1..5) {
                        ratingsSum += n
                        ratingsCount += 1
                    }
                }
            }
            val avgRating = if (ratingsCount > 0) ratingsSum.toDouble() / ratingsCount else 0.0
            val postsCount = postsSnap.size()

            // 2) Подписчики (followers count)
            val followersSnap = db.trustListDataRoot()
                .collection("users")
                .whereArrayContains("following", uid)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            val followersCount = followersSnap.size()

            // 3) Формула
            val activityCoefficient = min(
                1.0,
                postsCount / 10.0 + followersCount / 50.0
            )
            val trustScore = (avgRating * activityCoefficient).round3()

            // 4) Транзакционная запись (защита от гонки с CF)
            val ref = db.trustListDataRoot().collection("users").document(uid)
            db.runTransaction { tx ->
                tx.update(
                    ref,
                    mapOf(
                        "trustScore" to trustScore,
                        "trustScoreMeta" to mapOf(
                            "avgRating" to avgRating.round3(),
                            "postsCount" to postsCount,
                            "followersCount" to followersCount,
                            "activityCoefficient" to activityCoefficient.round3(),
                            "updatedAt" to System.currentTimeMillis(),
                            "source" to "client"
                        )
                    )
                )
                null
            }.await()

            Log.i(
                "TrustScoreRepository",
                "recalc uid=$uid trustScore=$trustScore " +
                    "avg=$avgRating posts=$postsCount followers=$followersCount " +
                    "k=$activityCoefficient"
            )
        } catch (t: Throwable) {
            // Не валим UI — пересчёт best-effort. CF — основной источник правды.
            Log.w("TrustScoreRepository", "recalc failed for uid=$uid", t)
        }
    }

    private fun Double.round3(): Double = Math.round(this * 1000.0) / 1000.0
}
