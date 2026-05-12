/**
 * Тесты логики триггеров Cloud Functions без Firestore / emulator.
 * Кормим хендлеры синтетическими событиями и проверяем, кого они зовут recalc'ом.
 * Запуск: node --test test/triggers.test.js
 */
const test = require("node:test");
const assert = require("node:assert/strict");
const {
  ratingsEqual,
  diffArray,
  handlePostWritten,
  handleUserWritten,
} = require("../lib/triggers");

// ---------- Хелперы ----------
function spy() {
  const calls = [];
  const fn = async (uid) => {
    calls.push(uid);
  };
  fn.calls = calls;
  return fn;
}

function snap(data) {
  return data === null ? { data: () => undefined } : { data: () => data };
}

function postEvent(before, after) {
  return {
    data: {
      before: snap(before),
      after: snap(after),
    },
  };
}

const userEvent = postEvent;

// ---------- ratingsEqual ----------
test("ratingsEqual: оба пустые / null — равны", () => {
  assert.equal(ratingsEqual(null, null), true);
  assert.equal(ratingsEqual({}, {}), true);
  assert.equal(ratingsEqual(undefined, {}), false);
});

test("ratingsEqual: одинаковые ключи и значения", () => {
  assert.equal(ratingsEqual({ a: 5, b: 4 }, { a: 5, b: 4 }), true);
});

test("ratingsEqual: разные значения → не равны", () => {
  assert.equal(ratingsEqual({ a: 5 }, { a: 4 }), false);
});

test("ratingsEqual: разное число ключей → не равны", () => {
  assert.equal(ratingsEqual({ a: 5 }, { a: 5, b: 3 }), false);
});

// ---------- diffArray ----------
test("diffArray: пустые → нет diff", () => {
  const { added, removed } = diffArray([], []);
  assert.deepEqual(added, []);
  assert.deepEqual(removed, []);
});

test("diffArray: добавили одного", () => {
  const { added, removed } = diffArray(["A"], ["A", "B"]);
  assert.deepEqual(added, ["B"]);
  assert.deepEqual(removed, []);
});

test("diffArray: убрали одного", () => {
  const { added, removed } = diffArray(["A", "B"], ["A"]);
  assert.deepEqual(added, []);
  assert.deepEqual(removed, ["B"]);
});

test("diffArray: добавили и убрали одновременно", () => {
  const { added, removed } = diffArray(["A", "B"], ["A", "C"]);
  assert.deepEqual(added, ["C"]);
  assert.deepEqual(removed, ["B"]);
});

// ---------- handlePostWritten ----------
test("post создан → recalc автору", async () => {
  const recalc = spy();
  await handlePostWritten(
    postEvent(null, { userId: "alice", ratings: {} }),
    recalc
  );
  assert.deepEqual(recalc.calls, ["alice"]);
});

test("post создан без userId → recalc НЕ зовётся", async () => {
  const recalc = spy();
  await handlePostWritten(postEvent(null, { ratings: {} }), recalc);
  assert.deepEqual(recalc.calls, []);
});

test("post удалён → recalc прежнему автору", async () => {
  const recalc = spy();
  await handlePostWritten(
    postEvent({ userId: "bob", ratings: { x: 5 } }, null),
    recalc
  );
  assert.deepEqual(recalc.calls, ["bob"]);
});

test("post update, ratings не менялись → recalc НЕ зовётся", async () => {
  const recalc = spy();
  await handlePostWritten(
    postEvent(
      { userId: "alice", title: "Old", ratings: { x: 5 } },
      { userId: "alice", title: "New", ratings: { x: 5 } }
    ),
    recalc
  );
  assert.deepEqual(recalc.calls, []);
});

test("post update, новый рейтинг → recalc автору", async () => {
  const recalc = spy();
  await handlePostWritten(
    postEvent(
      { userId: "alice", ratings: { x: 5 } },
      { userId: "alice", ratings: { x: 5, y: 4 } }
    ),
    recalc
  );
  assert.deepEqual(recalc.calls, ["alice"]);
});

test("post update, изменён существующий рейтинг → recalc автору", async () => {
  const recalc = spy();
  await handlePostWritten(
    postEvent(
      { userId: "alice", ratings: { x: 5 } },
      { userId: "alice", ratings: { x: 3 } }
    ),
    recalc
  );
  assert.deepEqual(recalc.calls, ["alice"]);
});

test("post update, сменился владелец → recalc обоим (старый и новый)", async () => {
  const recalc = spy();
  await handlePostWritten(
    postEvent(
      { userId: "alice", ratings: { x: 5 } },
      { userId: "bob", ratings: { x: 5 } }
    ),
    recalc
  );
  assert.deepEqual(recalc.calls.sort(), ["alice", "bob"]);
});

// ---------- handleUserWritten ----------
test("user update, following не менялся → recalc НЕ зовётся", async () => {
  const recalc = spy();
  await handleUserWritten(
    userEvent(
      { uid: "me", following: ["A", "B"], trustScore: 0 },
      { uid: "me", following: ["A", "B"], trustScore: 1 }
    ),
    recalc
  );
  assert.deepEqual(recalc.calls, []);
});

test("user следит за новым → recalc цели (followers count изменился)", async () => {
  const recalc = spy();
  await handleUserWritten(
    userEvent(
      { uid: "me", following: ["A"] },
      { uid: "me", following: ["A", "B"] }
    ),
    recalc
  );
  assert.deepEqual(recalc.calls, ["B"]);
});

test("user отписался → recalc цели", async () => {
  const recalc = spy();
  await handleUserWritten(
    userEvent(
      { uid: "me", following: ["A", "B"] },
      { uid: "me", following: ["A"] }
    ),
    recalc
  );
  assert.deepEqual(recalc.calls, ["B"]);
});

test("user одновременно подписался и отписался → recalc обоим", async () => {
  const recalc = spy();
  await handleUserWritten(
    userEvent(
      { uid: "me", following: ["A", "B"] },
      { uid: "me", following: ["A", "C"] }
    ),
    recalc
  );
  assert.deepEqual(recalc.calls.sort(), ["B", "C"]);
});

test("user создан с following — recalc всем добавленным", async () => {
  const recalc = spy();
  await handleUserWritten(
    userEvent(null, { uid: "new", following: ["A", "B"] }),
    recalc
  );
  assert.deepEqual(recalc.calls.sort(), ["A", "B"]);
});

test("user удалён, был с following — recalc всем (followers упали)", async () => {
  const recalc = spy();
  await handleUserWritten(
    userEvent({ uid: "old", following: ["A", "B"] }, null),
    recalc
  );
  assert.deepEqual(recalc.calls.sort(), ["A", "B"]);
});

// ---------- Защита от инфинит-лупа ----------
test("обновление trustScore юзера НЕ триггерит cascade (following тот же)", async () => {
  // Сценарий: CF записывает trustScore → onUserWritten фаерит снова.
  // Diff массива following пустой → recalc НЕ зовётся → нет лупа.
  const recalc = spy();
  await handleUserWritten(
    userEvent(
      { uid: "me", following: ["A"], trustScore: 0.5 },
      { uid: "me", following: ["A"], trustScore: 1.2 }
    ),
    recalc
  );
  assert.deepEqual(recalc.calls, []);
});
