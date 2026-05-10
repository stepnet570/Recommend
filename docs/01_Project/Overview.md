---
id: project_overview
type: project
tags:
  - project
  - overview
updated: 2026-05-02
---

🧭 Recommend: OverviewКонцепция: Анти-рекламная экосистема, где доверие конвертируется в ценность.

🎯 Essence (Суть)Recommend — это социальный фильтр, который отсекает информационный шум.User Side: Закрытая сеть рекомендаций внутри «стаи» (Pack).Business Side: Маркетплейс микро-инфлюенсеров, где лояльность вознаграждается $TrustCoins$.

## 👤 Целевая аудитория (ICP)
- **The Explorer (Юзер):** Экспат или активный горожанин (30+), ценит время, устал от SEO-выдачи Google.
- **The Artisan Business (Бизнес):** Локальные кофейни, барбершопы, концепт-сторы в Кордове, живущие за счет качества, а не трафика.

## ⚙️ Механика взаимодействия (Win-Win)
1. **User:** Делится местом -> Получает признание (TS) и валюту (TC).
2. **Business:** Не покупает баннеры -> Покупает "цифровое сарафанное радио", оплачивая только фактические рекомендации.

⚙️ Ключевые Механики 
(System Design)The Pack (Стая): Твой граф доверия. Ты видишь контент только тех, на кого «подписан» как на эксперта в конкретной области.
Pack Call (Зов): Активный запрос помощи. «Парни, где в Кордове лучший флэт-уайт?» — пуш улетает только твоей стае.
TrustScore ($T_s$): Алгоритмическая мера твоего влияния.
$$T_s = f(Quality, Engagement, Consistency)$$TrustCoins ($TC$): Утилитарный токен. 
Бизнес платит за пост $\rightarrow$ Юзер тратит в заведении.


🛠 Tech Stack & LinksFrontend: 
Kotlin / Compose (Android Studio -> Cursor IDE)Backend: 
Firebase (Firestore / Auth / Functions)
Design Style: Artisan Pastel & Modern Monotone

Документация: [[Roadmap]] · [[Glossary]] · [[../02_Architecture/Firestore Schema|Firestore Schema]] · [[../03_Design/Artisan Pastel|Design System]]

🚀 Текущий фокус (Pre-MVP)[x] Ядро экранов (Лента, Профиль)
[ ] Механика Pack Call
[ ] Интеграция TrustCoins (Логика транзакций)
[ ] Landing Page (Tilda)

💡 Почему это сработает? (Для самомотивации)Люди устали от алгоритмов, которые решают за них. В эпоху ИИ-контента человеческое доверие становится самым дефицитным и дорогим ресурсом. Recommend — это способ монетизировать это доверие напрямую, без посредников в виде рекламных площадок.
## Связанное
- [[Stack]] · [[Roadmap]] · [[Glossary]]
- [[../02_Architecture/Firestore Schema]]
