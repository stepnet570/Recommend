/**
 * Тест recalcTrustScoreForUser против мок-Firestore.
 * Не использует firebase-admin реально — подменяет initializeApp и firestore.
 * Запуск: node --test test/recalc.test.js
 */
const test = require("node:test");
const assert = require("node:assert/strict");
const Module = require("module");

// ---------- Mock firebase-admin ----------
const captured = {
  pathsRead: [],
  writes: [],
  transactionsRun: 0,
};

let postsByAuthor = {}; // uid -> array of { id, ratings }
let followersByUid = {}; // uid -> number
let userDocs = {}; // uid -> data | null (null = doesn't exist)

function makePostsQuery(uid) {
  captured.pathsRead.push(`posts where userId==${uid}`);
  const docs = (postsByAuthor[uid] || []).map((p, i) => ({
    id: `post${i}`,
    get: (field) => p[field],
  }));
  return Promise.resolve({ docs, size: docs.length });
}

function makeFollowersQuery(uid) {
  return {
    count: () => ({
      get: () => {
        captured.pathsRead.push(`users count(following ⊇ ${uid})`);
        return Promise.resolve({ data: () => ({ count: followersByUid[uid] || 0 }) });
      },
    }),
  };
}

function makeUserRef(uid) {
  return {
    _uid: uid,
    _kind: "userRef",
  };
}

function fakeDataRoot() {
  return {
    collection: (name) => {
      if (name === "posts") {
        return {
          where: (field, op, value) => ({ get: () => makePostsQuery(value) }),
        };
      }
      if (name === "users") {
        return {
          where: (field, op, value) => makeFollowersQuery(value),
          doc: (uid) => makeUserRef(uid),
        };
      }
      throw new Error("unexpected collection " + name);
    },
  };
}

const fakeAdmin = {
  initializeApp: () => {},
  firestore: () => {
    const fn = () => ({
      // db.collection(...).doc(...).collection(...).doc(...) chain → dataRoot()
      collection: () => ({
        doc: () => ({
          collection: () => ({
            doc: () => fakeDataRoot(),
          }),
        }),
      }),
      runTransaction: async (cb) => {
        captured.transactionsRun += 1;
        const tx = {
          get: async (ref) => {
            const data = userDocs[ref._uid];
            return { exists: data != null, get: (k) => data?.[k] };
          },
          set: (ref, data, opts) => {
            captured.writes.push({ uid: ref._uid, mode: "set", data, opts });
          },
          update: (ref, data) => {
            captured.writes.push({ uid: ref._uid, mode: "update", data });
          },
        };
        await cb(tx);
      },
    });
    return fn();
  },
};
// Подменяем require('firebase-admin') и require('firebase-admin/firestore')
// ДО загрузки index.js — index.js теперь импортит FieldValue из модульного пути.
const origResolve = Module._resolveFilename;
Module._resolveFilename = function (request, ...rest) {
  if (request === "firebase-admin") return "firebase-admin-mock";
  if (request === "firebase-admin/firestore") return "firebase-admin-firestore-mock";
  return origResolve.call(this, request, ...rest);
};
require.cache["firebase-admin-mock"] = {
  id: "firebase-admin-mock",
  filename: "firebase-admin-mock",
  loaded: true,
  exports: fakeAdmin,
};
require.cache["firebase-admin-firestore-mock"] = {
  id: "firebase-admin-firestore-mock",
  filename: "firebase-admin-firestore-mock",
  loaded: true,
  exports: {
    FieldValue: {
      serverTimestamp: () => "<SERVER_TS>",
    },
  },
};

// firebase-functions/v2 не должен требовать прода — но на всякий случай stub'нём logger
// (он сам по себе работает, просто хочется тихих тестов)
process.env.FUNCTIONS_EMULATOR = "true";

const { recalcTrustScoreForUser } = require("../index");

// ---------- Сброс перед каждым тестом ----------
function reset() {
  captured.pathsRead.length = 0;
  captured.writes.length = 0;
  captured.transactionsRun = 0;
  postsByAuthor = {};
  followersByUid = {};
  userDocs = {};
}

// ---------- Тесты ----------
test("recalc: пустой uid → не читает Firestore, не пишет", async () => {
  reset();
  await recalcTrustScoreForUser("");
  assert.equal(captured.pathsRead.length, 0);
  assert.equal(captured.writes.length, 0);
});

test("recalc: новый юзер без постов и без подписчиков → trustScore = 0, создаёт документ", async () => {
  reset();
  userDocs.alice = null; // не существует
  await recalcTrustScoreForUser("alice");

  assert.equal(captured.transactionsRun, 1);
  assert.equal(captured.writes.length, 1);
  const w = captured.writes[0];
  assert.equal(w.mode, "set");
  assert.equal(w.uid, "alice");
  assert.equal(w.data.trustScore, 0);
  assert.equal(w.data.trustScoreMeta.postsCount, 0);
  assert.equal(w.data.trustScoreMeta.followersCount, 0);
  assert.equal(w.data.trustScoreMeta.activityCoefficient, 0);
  assert.deepEqual(w.opts, { merge: true });
});

test("recalc: существующий юзер с постами и рейтингами → update со score", async () => {
  reset();
  userDocs.alice = { uid: "alice", trustScore: 0 };
  postsByAuthor.alice = [
    { ratings: { b: 5, c: 4 } },
    { ratings: { b: 3 } },
  ];
  followersByUid.alice = 5;

  await recalcTrustScoreForUser("alice");

  assert.equal(captured.writes.length, 1);
  const w = captured.writes[0];
  assert.equal(w.mode, "update");
  assert.equal(w.uid, "alice");
  // avg = (5+4+3)/3 = 4; coef = 2/10 + 5/50 = 0.3; score = 4*0.3 = 1.2
  assert.equal(w.data.trustScore, 1.2);
  assert.equal(w.data.trustScoreMeta.postsCount, 2);
  assert.equal(w.data.trustScoreMeta.followersCount, 5);
  assert.equal(w.data.trustScoreMeta.avgRating, 4);
});

test("recalc: ходит в правильные коллекции (paths)", async () => {
  reset();
  userDocs.bob = { uid: "bob" };
  postsByAuthor.bob = [];
  followersByUid.bob = 0;

  await recalcTrustScoreForUser("bob");

  // Должны быть запросы по posts (where userId==bob) и users (count following⊇bob)
  assert.ok(captured.pathsRead.includes("posts where userId==bob"));
  assert.ok(captured.pathsRead.includes("users count(following ⊇ bob)"));
});

test("recalc: записывает trustScoreMeta с serverTimestamp", async () => {
  reset();
  userDocs.charlie = { uid: "charlie" };
  postsByAuthor.charlie = [{ ratings: { x: 5 } }];
  followersByUid.charlie = 0;

  await recalcTrustScoreForUser("charlie");

  const w = captured.writes[0];
  assert.equal(w.data.trustScoreMeta.updatedAt, "<SERVER_TS>");
});
