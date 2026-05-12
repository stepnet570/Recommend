/**
 * Логика триггеров TrustScore — чистый JS без firebase-admin.
 * Тестируется детерминированно: подменяешь `recalc` колбэк на спай.
 */

function ratingsEqual(a, b) {
  if (!a && !b) return true;
  if (!a || !b) return false;
  const ka = Object.keys(a);
  const kb = Object.keys(b);
  if (ka.length !== kb.length) return false;
  for (const k of ka) if (a[k] !== b[k]) return false;
  return true;
}

function diffArray(prev = [], next = []) {
  const prevSet = new Set(prev);
  const nextSet = new Set(next);
  const added = next.filter((x) => !prevSet.has(x));
  const removed = prev.filter((x) => !nextSet.has(x));
  return { added, removed };
}

/**
 * Логика реакции на изменение документа `posts/{postId}`.
 * @param {Object} event — Firestore document-written event
 * @param {Function} recalc — async (uid) => void
 */
async function handlePostWritten(event, recalc) {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();

  // Удалили пост → пересчёт прежнему автору
  if (before && !after) {
    const uid = before.userId;
    if (uid) await recalc(uid);
    return;
  }

  // Создали пост → пересчёт автору
  if (!before && after) {
    const uid = after.userId;
    if (uid) await recalc(uid);
    return;
  }

  // Update — пересчёт только если поменялись ratings или сменился владелец
  if (before && after) {
    const ratingsChanged = !ratingsEqual(before.ratings, after.ratings);
    const ownerChanged = before.userId !== after.userId;
    if (ratingsChanged || ownerChanged) {
      if (after.userId) await recalc(after.userId);
      if (ownerChanged && before.userId && before.userId !== after.userId) {
        await recalc(before.userId);
      }
    }
  }
}

/**
 * Логика реакции на изменение документа `users/{userId}`.
 * Пересчёт идёт всем, кого добавили/убрали из массива `following`.
 */
async function handleUserWritten(event, recalc) {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();

  const prevFollowing = Array.isArray(before?.following) ? before.following : [];
  const nextFollowing = Array.isArray(after?.following) ? after.following : [];

  const { added, removed } = diffArray(prevFollowing, nextFollowing);
  if (added.length === 0 && removed.length === 0) return;

  const targets = Array.from(new Set([...added, ...removed]));
  await Promise.all(targets.map((uid) => recalc(uid)));
}

module.exports = {
  ratingsEqual,
  diffArray,
  handlePostWritten,
  handleUserWritten,
};
