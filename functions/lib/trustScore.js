/**
 * TrustScore — чистая формула, без Firebase. Можно юнит-тестировать.
 *
 *   trustScore = avg(ratings to user's posts) * activity_coefficient
 *   activity_coefficient = min(1.0, (posts_count / 10) + (followers_count / 50))
 *
 * Шкала: 0..5 (UI домножает на 2 для 0..10 ring'а).
 */

/**
 * @param {Object} input
 * @param {Array<Object>} input.posts — массив постов автора, у каждого опц. `ratings: {uid: stars}`
 * @param {number} input.followersCount — кол-во подписчиков автора
 * @returns {{
 *   trustScore: number,
 *   avgRating: number,
 *   postsCount: number,
 *   followersCount: number,
 *   activityCoefficient: number,
 *   ratingsCount: number
 * }}
 */
function computeTrustScore({ posts = [], followersCount = 0 } = {}) {
  let ratingsSum = 0;
  let ratingsCount = 0;

  for (const post of posts) {
    const ratings = post?.ratings;
    if (!ratings || typeof ratings !== "object") continue;
    for (const v of Object.values(ratings)) {
      const n = Number(v);
      if (!Number.isFinite(n) || n <= 0) continue;
      // защита от мусора: клиппинг 1..5
      ratingsSum += Math.min(5, Math.max(1, n));
      ratingsCount += 1;
    }
  }

  const avgRating = ratingsCount > 0 ? ratingsSum / ratingsCount : 0;
  const postsCount = posts.length;

  const activityCoefficient = Math.min(
    1.0,
    postsCount / 10 + followersCount / 50
  );

  const trustScore = +(avgRating * activityCoefficient).toFixed(3);

  return {
    trustScore,
    avgRating: +avgRating.toFixed(3),
    postsCount,
    followersCount,
    activityCoefficient: +activityCoefficient.toFixed(3),
    ratingsCount,
  };
}

module.exports = { computeTrustScore };
