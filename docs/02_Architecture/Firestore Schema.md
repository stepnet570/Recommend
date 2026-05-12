---
id: arch_firestore_schema
type: architecture
tags: [architecture, firestore, schema]
updated: 2026-05-02
---

# Firestore Schema

> ⚠️ Все коллекции лежат под `artifacts/trustlist-production/public/data/<collection>`.
> Источник правды — `FirestorePaths.kt`. **Не хардкодить** путь в репозиториях.

## users
| Поле | Тип | Назначение |
|------|-----|-----------|
| `uid` | string | id (= Auth UID) |
| `email` | string | email |
| `name` | string | отображаемое имя |
| `avatar` | string (url) | URL фото |
| `following` | array<string> | uid'ы кого юзер фолловит = его pack |
| `isBusiness` | bool | бизнес-аккаунт |
| `trustCoins` | number | баланс |
| `trustScore` | number (0–10) | рейтинг рекомендатора |

## posts
| Поле | Тип | Назначение |
|------|-----|-----------|
| `userId` | string | автор |
| `title` | string | заголовок |
| `description` | string | основной текст |
| `category` | string | еда / красота / услуги / ... |
| `location` | string \| geo | место |
| `rating` | number | оценка автора месту |
| `imageUrl` | string | фото |
| `isSponsored` | bool | оплачен ли через Offer |

## collections
| Поле | Тип | Назначение |
|------|-----|-----------|
| `userId` | string | владелец |
| `name` | string | название подборки |
| `postIds` | array<string> | id постов в подборке |

## requests (Pack Call)
> Открытый запрос рекомендации к pack.

## answers
> Ответы на Pack Request (`requestId`, `userId`, `text`, `postId?`).

## offers
| Поле | Тип | Назначение |
|------|-----|-----------|
| `businessId` | string | uid бизнеса |
| `title` | string | название кампании |
| `rewardCoins` | number | сколько платится за пост |
| `minTrustScore` | number | минимум для участия |
| `status` | string | active / paused / completed |

## Принципы
- **Денормализация ок** для скорости чтения фида (например, `authorName` в `posts`).
- **Фильтрация фида** делается по `following` пользователя (см. [[../04_Features/Social Graph]]).
- **TrustScore** пересчитывается на бэкенде (Cloud Functions) при апвоутах.

## TODO по схеме
- [ ] Завести коллекцию `transactions` для истории TrustCoins (см. [[../04_Features/TrustCoins Wallet]])
- [ ] Добавить пагинацию (Firestore `startAfter`)

## Связанное
- [[Data Layer]] — репозитории
- [[../Glossary]]
