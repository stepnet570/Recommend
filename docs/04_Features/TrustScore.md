---
id: feature_trust_score
type: feature
status: in-progress
priority: P0
owner: Stepan
tags: [feature, trust, monetization]
updated: 2026-05-02
---

# TrustScore

## Зачем
Без TrustScore не работает доверие — основа продукта. Также это **gate** для офферов: бизнес ставит `minTrustScore` в кампании, чтобы пост писали лояльные качественные авторы.

## Что
Числовой рейтинг 0–10 для каждого пользователя — насколько он хорош как рекомендатор.

## Формула (v1, простая)
```
TrustScore = clamp(0..10) of:
  base_score (из апвоутов на постах юзера, нормализованный)
  + bonus за активность pack (юзеры из pack часто реагируют → +)
  − штрафы (репорты, отписки от sponsored постов)
```
Конкретные веса — в [[../99_Inbox/trust-score-formula-v1]] (черновик), уточняем после первых данных.

## Acceptance Criteria
- [ ] Поле `trustScore: Number` в `users` (уже есть)
- [ ] Cloud Function пересчитывает при апвоуте поста
- [ ] Cloud Function пересчитывает при отписке/репорте
- [ ] TrustScoreRing показывает актуальное значение в профиле
- [ ] При создании оффера показываем сколько юзеров попадают под `minTrustScore`

## Технические затраты
**Затрагиваемые файлы:**
- `data/repository/UserRepository.kt` — обновление trustScore
- `functions/` (Firebase Cloud Functions) — пересчёт
- `ui/profile/ProfileScreen.kt` — отображение

## Метрика успеха
- ≥80% активных юзеров имеют TrustScore >0 в первый месяц
- Конверсия в первый принятый оффер ≥15% (для тех у кого score ≥ minTrustScore)

## Риски
- Геймификация (накрутка апвоутов) — нужен антифрод во v2.
- Если формула несправедлива — обиды пользователей. **Делать non-public до стабилизации:** показывать только своё, не других.

## Связанное
- [[TrustCoins Wallet]]
- [[Offer Acceptance Flow]]
- [[../02_Architecture/Firestore Schema]]
