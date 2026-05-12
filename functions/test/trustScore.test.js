/**
 * Юнит-тесты на чистую формулу trustScore (без Firebase).
 * Запуск: node --test test/trustScore.test.js
 */
const test = require("node:test");
const assert = require("node:assert/strict");
const { computeTrustScore } = require("../lib/trustScore");

test("новый юзер: 0 постов, 0 подписчиков → trustScore = 0", () => {
  const r = computeTrustScore({ posts: [], followersCount: 0 });
  assert.equal(r.trustScore, 0);
  assert.equal(r.avgRating, 0);
  assert.equal(r.activityCoefficient, 0);
  assert.equal(r.ratingsCount, 0);
});

test("есть пост, но без рейтингов → trustScore = 0 (avg = 0)", () => {
  const r = computeTrustScore({
    posts: [{ ratings: {} }],
    followersCount: 0,
  });
  assert.equal(r.trustScore, 0);
  assert.equal(r.postsCount, 1);
  assert.equal(r.activityCoefficient, 0.1); // 1/10
});

test("1 пост, 1 рейтинг 5★, 0 подписчиков → 0.5", () => {
  // avg = 5, coef = min(1, 1/10 + 0/50) = 0.1, score = 5 * 0.1 = 0.5
  const r = computeTrustScore({
    posts: [{ ratings: { userB: 5 } }],
    followersCount: 0,
  });
  assert.equal(r.avgRating, 5);
  assert.equal(r.activityCoefficient, 0.1);
  assert.equal(r.trustScore, 0.5);
});

test("3 поста, разные рейтинги, 1 подписчик → 0.6", () => {
  // ratings: 5, 4, 3 → avg 4
  // coef = 3/10 + 1/50 = 0.3 + 0.02 = 0.32
  // score = 4 * 0.32 = 1.28 → но shipping value: 4 * 0.32 = 1.28
  const r = computeTrustScore({
    posts: [
      { ratings: { u1: 5 } },
      { ratings: { u1: 4 } },
      { ratings: { u1: 3 } },
    ],
    followersCount: 1,
  });
  assert.equal(r.avgRating, 4);
  assert.equal(r.activityCoefficient, 0.32);
  assert.equal(r.trustScore, 1.28);
});

test("активность забивается в 1.0 при множестве постов/подписчиков", () => {
  // 20 постов → 2.0; +200 подписчиков → 4.0; суммарно 6.0 → clamp to 1.0
  const posts = Array.from({ length: 20 }, () => ({ ratings: { x: 5 } }));
  const r = computeTrustScore({ posts, followersCount: 200 });
  assert.equal(r.activityCoefficient, 1.0);
  assert.equal(r.trustScore, 5.0);
});

test("граничный случай: ровно 10 постов, 0 подписчиков → coef = 1.0", () => {
  const posts = Array.from({ length: 10 }, () => ({ ratings: { x: 4 } }));
  const r = computeTrustScore({ posts, followersCount: 0 });
  assert.equal(r.activityCoefficient, 1.0);
  assert.equal(r.trustScore, 4.0);
});

test("мусорные рейтинги (0, -1, 99, 'abc') игнорируются / клиппятся 1..5", () => {
  const r = computeTrustScore({
    posts: [
      { ratings: { a: 0, b: -1, c: 99, d: "abc", e: 5 } },
    ],
    followersCount: 0,
  });
  // 0 и -1 отбрасываются (n <= 0), 99 клиппится до 5, 'abc' → NaN отбрасывается, 5 остаётся
  // ratingsCount = 2 (99→5, 5), sum = 10, avg = 5
  assert.equal(r.ratingsCount, 2);
  assert.equal(r.avgRating, 5);
});

test("следующее число рейтингов — несколько постов, не пересекающиеся юзеры", () => {
  // Юзер B оценил пост1 на 5, юзер C оценил пост2 на 3 → avg = (5+3)/2 = 4
  const r = computeTrustScore({
    posts: [
      { ratings: { B: 5 } },
      { ratings: { C: 3 } },
    ],
    followersCount: 0,
  });
  assert.equal(r.ratingsCount, 2);
  assert.equal(r.avgRating, 4);
  // coef = 2/10 = 0.2 → score = 4 * 0.2 = 0.8
  assert.equal(r.trustScore, 0.8);
});

test("дробное округление trustScore до 3 знаков", () => {
  // avg = (1+2)/2 = 1.5; posts=2 → coef = 0.2; score = 0.3
  const r = computeTrustScore({
    posts: [{ ratings: { a: 1, b: 2 } }, { ratings: {} }],
    followersCount: 0,
  });
  assert.equal(r.avgRating, 1.5);
  assert.equal(r.trustScore, 0.3);
});

test("реальный сценарий: микро-инфлюенсер с 5 постами и 10 подписчиками", () => {
  // 5 постов, по 3 рейтинга в каждом (всего 15), средняя 4.2
  const ratings = [4, 5, 5, 3, 4, 5, 4, 4, 4, 5, 3, 5, 4, 4, 4]; // avg = 4.2
  let i = 0;
  const posts = Array.from({ length: 5 }, () => ({
    ratings: { a: ratings[i++], b: ratings[i++], c: ratings[i++] },
  }));
  const r = computeTrustScore({ posts, followersCount: 10 });
  assert.equal(r.avgRating, 4.2);
  // coef = 5/10 + 10/50 = 0.5 + 0.2 = 0.7
  assert.equal(r.activityCoefficient, 0.7);
  // score = 4.2 * 0.7 = 2.94
  assert.equal(r.trustScore, 2.94);
});

test("монотонность: больше хороших рейтингов → trustScore не падает", () => {
  const base = computeTrustScore({
    posts: [{ ratings: { a: 5 } }],
    followersCount: 0,
  });
  const more = computeTrustScore({
    posts: [{ ratings: { a: 5, b: 5 } }],
    followersCount: 0,
  });
  assert.ok(more.trustScore >= base.trustScore);
});

test("новый подписчик не уменьшает score (монотонность по followers)", () => {
  const before = computeTrustScore({
    posts: [{ ratings: { a: 4 } }],
    followersCount: 0,
  });
  const after = computeTrustScore({
    posts: [{ ratings: { a: 4 } }],
    followersCount: 1,
  });
  assert.ok(after.trustScore >= before.trustScore);
});
