---
id: feature_trust_coins_wallet
type: feature
status: backlog
priority: P0
owner: Stepan
tags: [feature, monetization, wallet]
updated: 2026-05-02
---

# TrustCoins Wallet

## Зачем
Без кошелька монетизация не работает — юзеру некуда смотреть баланс, бизнесу некуда заносить деньги.

## Что
Экран «Кошелёк» в Profile + история транзакций. Бизнес может пополнять баланс кампаний, юзер — выводить (этап 2, не сейчас).

## Acceptance Criteria
- [ ] Экран `WalletScreen` (баланс + история)
- [ ] Коллекция `transactions` в Firestore: `userId`, `type` (`earn` / `spend` / `topup`), `amount`, `relatedOfferId?`, `relatedPostId?`, `createdAt`
- [ ] Транзакция создаётся при принятии оффера (юзер +N, бизнес −N)
- [ ] Чип TrustCoins в Feed header кликабелен → открывает Wallet
- [ ] Empty state: «Пока пусто. Принимай офферы или приглашай друзей»

## UX-флоу
```
Profile → tap TrustCoinsChip → WalletScreen
  ├── Header: balance + (для бизнеса) кнопка "Пополнить"
  └── List<Transaction> (LazyColumn, пагинация)
```

## Технические затраты
**Новые файлы:**
- `ui/wallet/WalletScreen.kt`
- `ui/wallet/WalletViewModel.kt`
- `data/model/Transaction.kt`
- `data/repository/TransactionRepository.kt`

**Firestore:**
- Новая коллекция `transactions`
- Composite index: `(userId, createdAt desc)`

## Метрика успеха
- 50%+ юзеров заходят в Wallet хотя бы раз в первую неделю
- Среднее время от первого «earn» до второго ≤ 7 дней (повторное использование)

## Риски
- Деньги ≠ TrustCoins. Юристы. Сначала **не выводить в фиат** — только закрытая экономика.

## Связанное
- [[TrustScore]]
- [[Offer Acceptance Flow]]
