/**
 * Recommend — Cloud Functions
 *
 * TrustScore engine. Триггеры:
 *  - onPostWritten        — пост создали или изменили ratings → пересчёт автору
 *  - onUserWritten        — изменился following → пересчёт всем добавленным / убранным
 *
 * Формула (см. CLAUDE.md / задача):
 *   trustScore = avg(ratings to user's posts) * activity_coefficient
 *   activity_coefficient = min(1.0, (posts_count / 10) + (followers_count / 50))
 *
 * Шкала: 0..5 (UI умножает на 2 для 0..10 ring'а).
 * Все записи идут в один и тот же путь, что использует Android-клиент:
 *   artifacts/trustlist-production/public/data/{collection}
 *
 * Атомарность: пересчёт делается транзакционно на документе пользователя,
 * чтобы параллельные триггеры не перетёрли друг друга.
 */

const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions/v2");
const admin = require("firebase-admin");
const { FieldValue } = require("firebase-admin/firestore");
const { computeTrustScore } = require("./lib/trustScore");
const {
  handlePostWritten,
  handleUserWritten,
} = require("./lib/triggers");

admin.initializeApp();
const db = admin.firestore();

// ---------- Firestore paths (mirror Android FirestorePaths.kt) ----------
const ARTIFACTS = "artifacts";
const TRUSTLIST_DOC = "trustlist-production";
const PUBLIC = "public";
const DATA = "data";

function dataRoot() {
  return db
    .collection(ARTIFACTS)
    .doc(TRUSTLIST_DOC)
    .collection(PUBLIC)
    .doc(DATA);
}

const POSTS_PATH = `${ARTIFACTS}/${TRUSTLIST_DOC}/${PUBLIC}/${DATA}/posts/{postId}`;
const USERS_PATH = `${ARTIFACTS}/${TRUSTLIST_DOC}/${PUBLIC}/${DATA}/users/{userId}`;

// ---------- Core: recalc one user ----------
/**
 * Считает trustScore для одного пользователя и пишет в его документ.
 * Делается в транзакции, чтобы не было гонки с параллельными триггерами.
 */
async function recalcTrustScoreForUser(uid) {
  if (!uid) return;

  // 1) Все посты автора
  const postsSnap = await dataRoot()
    .collection("posts")
    .where("userId", "==", uid)
    .get();
  const posts = postsSnap.docs.map((d) => ({ ratings: d.get("ratings") }));

  // 2) Кол-во подписчиков
  const followersSnap = await dataRoot()
    .collection("users")
    .where("following", "array-contains", uid)
    .count()
    .get();
  const followersCount = followersSnap.data().count || 0;

  // 3) Чистая формула (то же, что в lib/trustScore.js — single source of truth)
  const result = computeTrustScore({ posts, followersCount });
  const { trustScore, avgRating, postsCount, activityCoefficient } = result;

  // 4) Транзакционная запись в users/{uid}
  const userRef = dataRoot().collection("users").doc(uid);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(userRef);
    if (!snap.exists) {
      // Профиля ещё нет — создаём минимум, чтобы счёт не потерялся
      tx.set(
        userRef,
        {
          uid,
          trustScore,
          trustScoreMeta: {
            avgRating: +avgRating.toFixed(3),
            postsCount,
            followersCount,
            activityCoefficient: +activityCoefficient.toFixed(3),
            updatedAt: FieldValue.serverTimestamp(),
          },
        },
        { merge: true }
      );
    } else {
      tx.update(userRef, {
        trustScore,
        trustScoreMeta: {
          avgRating: +avgRating.toFixed(3),
          postsCount,
          followersCount,
          activityCoefficient: +activityCoefficient.toFixed(3),
          updatedAt: FieldValue.serverTimestamp(),
        },
      });
    }
  });

  logger.info("trustScore recalculated", {
    uid,
    trustScore,
    avgRating,
    postsCount,
    followersCount,
    activityCoefficient,
  });
}

// ---------- Trigger 1: post written ----------
exports.onPostWritten = onDocumentWritten(
  { document: POSTS_PATH, region: "us-central1" },
  (event) => handlePostWritten(event, recalcTrustScoreForUser)
);

// ---------- Trigger 2: user written (для follow/unfollow) ----------
exports.onUserWritten = onDocumentWritten(
  { document: USERS_PATH, region: "us-central1" },
  (event) => handleUserWritten(event, recalcTrustScoreForUser)
);

// ---------- Экспорт для тестов / адмен-скриптов ----------
exports.recalcTrustScoreForUser = recalcTrustScoreForUser;

// ---------- Callable: ручной пересчёт (для тестов / админ-кейсов) ----------
const { onCall, HttpsError } = require("firebase-functions/v2/https");

exports.recalcTrustScore = onCall(
  { region: "us-central1" },
  async (request) => {
    const uid = request.data?.uid || request.auth?.uid;
    if (!uid) {
      throw new HttpsError(
        "invalid-argument",
        "uid обязателен (либо в data.uid, либо авторизованный вызов)"
      );
    }
    await recalcTrustScoreForUser(uid);
    return { ok: true, uid };
  }
);
