# Recommend — Cloud Functions

## Что внутри
`index.js` содержит TrustScore engine с тремя точками входа:

| Функция | Тип | Когда срабатывает |
|---|---|---|
| `onPostWritten` | Firestore trigger | Создание / удаление поста, изменение `ratings` |
| `onUserWritten` | Firestore trigger | Изменение массива `following` у пользователя |
| `recalcTrustScore` | Callable | Ручной запуск (тесты / адмен / батч-миграции) |

Формула:
```
trustScore = avg(ratings to user's posts) * activity_coefficient
activity_coefficient = min(1.0, posts_count/10 + followers_count/50)
```
Запись атомарная (Firestore transaction) на документе пользователя.

## Первичная установка (один раз)
```bash
# в корне проекта Recommend/
npm install -g firebase-tools
firebase login
cd functions
npm install
cd ..
```

## Деплой
```bash
# Только TrustScore функции:
firebase deploy --only functions

# Или одну конкретную:
firebase deploy --only functions:onPostWritten
firebase deploy --only functions:onUserWritten
firebase deploy --only functions:recalcTrustScore
```

Проект уже привязан к `trustlist-fc435` через `.firebaserc`.

## Локальный тест через эмулятор
```bash
# В одном окне — эмулятор:
firebase emulators:start --only functions,firestore

# В другом — Android-приложение запускается с указанием эмулятора,
# либо в emulator UI на http://localhost:4000 руками меняйте документы.
```

## Как проверить, что работает
1. Залогиниться юзером A, создать 3 поста.
2. С другого аккаунта B поставить рейтинги постам A (1–5).
3. В Firestore Console → `artifacts/trustlist-production/public/data/users/{A}`
   увидеть поле `trustScore` и `trustScoreMeta`:
   ```
   trustScore: 0.6 (например)
   trustScoreMeta: {
     avgRating: 4.0,
     postsCount: 3,
     followersCount: 0,
     activityCoefficient: 0.3,  // 3/10 + 0/50
     updatedAt: ...,
     source: "client" | (нет поля если CF)
   }
   ```
4. Подписать ещё одного юзера C на A → followersCount вырастет на 1, trustScore пересчитается.

## Ручной пересчёт для конкретного uid
```bash
# Через firebase shell:
firebase functions:shell
> recalcTrustScore({uid: "ABC123XYZ"})

# Или через curl (если callable открыт):
# (callable требует аутентификацию по умолчанию,
#  правки в onCall можно сделать при необходимости)
```

## Бэкфилл существующих пользователей
Запустить разово после деплоя — нужно, чтобы все юзеры получили актуальный `trustScore`:
```js
// одноразовый скрипт (run from Node CLI):
const admin = require("firebase-admin");
admin.initializeApp({ projectId: "trustlist-fc435" });
const db = admin.firestore();
const root = db.collection("artifacts").doc("trustlist-production")
              .collection("public").doc("data");
(async () => {
  const users = await root.collection("users").get();
  // вызвать recalcTrustScore через https-callable или
  // импортировать функцию recalcTrustScoreForUser из index.js
})();
```

## Известные ограничения
- `count()` aggregation — 1 биллинг-операция чтения на каждый пересчёт. Дёшево, но если followers > 10к — оптимизировать через counters.
- Подписка/отписка — диффим массив `following` на before/after, поэтому батч `arrayUnion(a, b)` за один вызов пересчитает обоих.
- Дабл-запись (клиент + CF) — формула детерминированная, сходятся к одному значению. Когда CF стабилизируется на проде, клиентский fallback можно отключить (просто закомментировать вызовы `trustScoreRepo.recalculateFor(...)` в `PostRepository`, `UserRepository`, `AppNavigation.ratePost`).
