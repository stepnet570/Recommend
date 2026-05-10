---
id: arch_data_layer
type: architecture
tags: [architecture, data]
updated: 2026-05-02
---

# Data Layer

## Принципы
- **Repository pattern** — единственная точка работы с Firestore для каждой сущности.
- ViewModel **не трогает** Firestore напрямую — всегда через репо.
- Все пути берутся из `FirestorePaths.kt`.

## Репозитории
| Repo | Файл | Что делает |
|------|------|-----------|
| `PostRepository` | `data/repository/PostRepository.kt` | CRUD постов, фид, фильтр по following |
| `UserRepository` | `data/repository/UserRepository.kt` | Профили, follow/unfollow, обновление баланса |
| `OfferRepository` | `data/repository/OfferRepository.kt` | Создание кампаний, принятие оффера |
| `RequestRepository` | `data/repository/RequestRepository.kt` | Pack Call: запросы и ответы |
| `CollectionRepository` | `data/repository/CollectionRepository.kt` | Подборки постов |

## Модели
`data/model/`:
- `Post`
- `UserProfile`
- `AdOffer`
- `PackRequest`
- `PostCollection`
- `Answer`

## Известные проблемы
- ❌ Нет пагинации в `PostRepository.observeFeed` — фид грузит всё. Будет тормозить >500 постов.
- ❌ `authorName = "Alex"` дефолт в `Post.kt`. Дыра — может прорваться в прод.

## Связанное
- [[Firestore Schema]]
- [[Navigation]]
