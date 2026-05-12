---
id: feature_offer_acceptance_flow
type: feature
status: in-progress
priority: P1
owner: Stepan
tags: [feature, monetization, business]
updated: 2026-05-02
---

# Offer Acceptance Flow

## Зачем
Это «связующая транзакция» между бизнесом и юзером. Без неё AdOffer не превращается в пост и в TrustCoins.

## Что
Юзер видит оффер → жмёт «Принять» → создаётся sponsored пост (черновик) → после публикации с бизнеса списываются coins, юзеру начисляются.

## UX-флоу (минимум 2 тапа на принятие)
```
Feed (Exclusive Deals) → tap Offer card
  → AcceptOfferSheet (bottom sheet с условиями)
  → tap "Принять"
  → AddScreen в режиме "sponsored" (preset category, location, метка)
  → tap "Опубликовать"
  → Firestore:
       posts: создаётся с isSponsored=true, offerId=...
       offers: статус → "in-progress" → "fulfilled"
       transactions: business -N, user +N
       users.trustCoins: пересчёт обоих
```

## Acceptance Criteria
- [x] AcceptOfferSheet есть
- [ ] При принятии создаётся черновик поста с предзаполненными полями оффера
- [ ] При публикации запускается Firestore-транзакция (атомарно: post + 2 transactions + 2 user updates)
- [ ] Гард: юзер с `trustScore < minTrustScore` не может принять (кнопка disabled + tooltip)
- [ ] Гард: бизнес с балансом < `rewardCoins` не может опубликовать оффер

## Технические затраты
**Затрагиваемые файлы:**
- `AcceptOfferSheet.kt` — добавить навигацию в `AddScreen` с preset
- `ui/add/AddScreen.kt` — режим `sponsored = true`
- `data/repository/OfferRepository.kt` — `acceptOffer()` через `db.runTransaction`
- `data/repository/PostRepository.kt` — `createSponsoredPost()`

## Метрика успеха
- Time-to-accept (открыл оффер → принял) < 30 сек.
- ≥40% открытий AcceptOfferSheet → принятие.

## Риски
- Race condition если без транзакции (юзер дважды принял). **Решение:** Firestore transaction.
- Бизнес не платит — оффер «висит». Решение: TTL и автоотмена.

## Связанное
- [[TrustCoins Wallet]]
- [[TrustScore]]
- [[../02_Architecture/Firestore Schema]]
