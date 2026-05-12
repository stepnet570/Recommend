# Recommend

Социальная сеть честных локальных рекомендаций внутри trust-круга («pack») с встроенным маркетплейсом микро-инфлюенсеров.

## TrustScore — система рейтинга

**Что это.** TrustScore — числовая мера доверия к источнику (0–5 в БД, 0–10 в UI). Чем выше — тем сильнее рекомендации этого юзера влияют на ленту и тем привлекательнее он для рекламодателей.

**Формула:**
```
trustScore           = avg(ratings to user's posts) * activity_coefficient
activity_coefficient = min(1.0, posts_count/10 + followers_count/50)
```

- `avg(ratings)` — средний рейтинг (1–5★), который другие юзеры ставят постам этого пользователя
- `posts_count` — количество постов автора (10 постов = 100% бонус активности)
- `followers_count` — количество подписчиков (50 подписчиков = 100% бонус)
- `activity_coefficient` клампится в 1.0 — потолок защищает от накрутки активностью

**Шкала в UI** (`TrustScoreRing`): значение из БД × 2 → 0..10. Делается на клиенте в `ProfileScreen.kt`.

### Когда пересчитывается

Триггеры (Firebase Cloud Functions, регион `us-central1`):

| Событие | Триггер | Что пересчитывается |
|---|---|---|
| Создан / удалён пост | `onPostWritten` | trustScore автора (изменился `posts_count`) |
| Изменилось поле `ratings` у поста | `onPostWritten` | trustScore автора (изменился `avg`) |
| Юзер подписался / отписался | `onUserWritten` (диф массива `following`) | trustScore цели подписки (изменился `followers_count`) |

Дополнительно есть callable-функция `recalcTrustScore({uid})` для ручного пересчёта (тесты, бэкфилл, админ-операции).

### Архитектура

```
Android (Kotlin)           Firestore                    Cloud Functions (Node.js)
─────────────────          ─────────────                ───────────────────────────
ratePost()      ─writes──> posts/{id}.ratings    ─trigger─> onPostWritten
addPost()       ─writes──> posts/{new doc}       ─trigger─> onPostWritten
follow/unfollow ─writes──> users/{me}.following  ─trigger─> onUserWritten
                                                              ↓ (transaction)
                                              users/{uid}.trustScore + trustScoreMeta
```

Запись в `users/{uid}.trustScore` идёт **транзакционно** на стороне сервера — гарантирует, что параллельные триггеры не перетрут друг друга.

### Клиентский fallback

Параллельно с CF клиент сам пересчитывает trustScore (`TrustScoreRepository.kt`) после rate / addPost / follow. Это:
- работает, если CF временно недоступна или не задеплоена
- использует ту же формулу — двойная запись детерминированно сходится к одному значению
- не создаёт безопасности (клиент всё ещё может писать `trustScore` напрямую) — это технический долг, закрыть Firestore Security Rules после стабилизации CF

### Поле `trustScoreMeta` (для дебага)

При каждом пересчёте CF записывает рядом с `trustScore` блок метаданных:
```js
trustScoreMeta: {
  avgRating: 4.2,
  postsCount: 5,
  followersCount: 10,
  activityCoefficient: 0.7,
  updatedAt: <timestamp>
}
```
Помогает быстро понять, почему у юзера такой score, без перепрогона формулы.

### Файлы

| Где | Что |
|---|---|
| `functions/index.js` | Cloud Functions с триггерами |
| `functions/lib/trustScore.js` | Чистая формула (single source of truth) |
| `functions/lib/triggers.js` | Логика handler'ов (тестируется без Firebase) |
| `functions/test/*.test.js` | 39 юнит-тестов (формула + триггеры + recalc с моком) |
| `app/.../data/repository/TrustScoreRepository.kt` | Клиентская реализация той же формулы |
| `app/.../data/model/UserProfile.kt` | Поле `trustScore: Double` |
| `app/.../ui/profile/ProfileScreen.kt` | Отрисовка через `TrustScoreRing` (×2 → шкала 0–10) |

### Деплой

```bash
cd functions
npm install     # один раз
npm test        # должно показать 39/39 passing
firebase deploy --only functions
```

Детали и тестовый сценарий через emulator — в `functions/README.md`.
