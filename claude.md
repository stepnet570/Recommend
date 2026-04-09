# TrustList — Project Context for Claude

## 🎯 Проект: TrustList
Социальная сеть для честных локальных рекомендаций внутри круга доверия ("стаи").
Встроенная биржа микро-инфлюенсеров: бизнес платит TrustCoins пользователям за нативное размещение.

---

## 🛠 Стек
- **Android** / Kotlin / Jetpack Compose (Material 3)
- **Backend:** Firebase Firestore + Firebase Auth
- **DB path:** `artifacts/trustlist-production/public/data/<коллекция>`

---

## 💾 Схема Firestore

| Коллекция | Ключевые поля |
|-----------|--------------|
| `users` | uid, email, name, avatar, following[], isBusiness, trustCoins, trustScore |
| `posts` | userId, title, description, category, location, rating, imageUrl, isSponsored |
| `collections` | userId, name, postIds[] |
| `requests` | — (Зов стаи) |
| `answers` | — (ответы на запросы) |
| `offers` | businessId, title, rewardCoins, minTrustScore, status |

---

## 🎨 Дизайн-система "Artisan Pastel"

```kotlin
Surface (Фон):        #F0FAF5  // SoftPastelMint
On-Surface (Текст):   #2D3A36  // DarkPastelAnthracite
Primary (Кнопки):     #FF7C6B  // RichPastelCoral
Secondary (Иконки):   #6EBFD6  // MutedPastelTeal
Gold (Монетизация):   #D4AF37  // MutedPastelGold

Заголовки: Syne (геометрический)
Текст:     DM Sans
```

---

## ✅ Что уже сделано
- FeedScreen с постами, запросами ("Зов стаи"), блоком "Эксклюзивные сделки"
- AddScreen — создание поста (с категорией, рейтингом, локацией)
- ProfileScreen — профиль пользователя
- ExploreScreen — поиск/рекомендации
- CreateOfferScreen — бизнес создаёт AdOffer
- AddHubScreen — хаб для бизнес-аккаунта (пост или кампания)
- MainAppScreen — навигация + маршрутизация по ролям (isBusiness)
- Firebase Auth + Firestore CRUD

---

## 🔥 Что нужно сделать (приоритеты)
1. **TrustScore** — логика начисления/пересчёта рейтинга доверия
2. **TrustCoins wallet** — экран баланса, история транзакций
3. **Принятие оффера** — полный флоу: принять → создать sponsored пост → списать коины у бизнеса
4. **Социальный граф** — подписки/отписки, лента только от "стаи"
5. **Коллекции** — создание, редактирование, добавление поста в коллекцию
6. **Notifications** — уведомления о новых запросах, принятых офферах
7. **Бизнес-кабинет** — аналитика кампаний (сколько постов создано, охват)
8. **Onboarding** — первый запуск, выбор роли (user / business)
9. **Image upload** — загрузка фото в Firebase Storage
10. **Deep links / sharing** — шаринг поста/коллекции

---

## 🤖 Активные роли (agency-agents)

### 🏗 Senior Android Developer
Используй при: написании Kotlin/Compose кода, Firebase интеграции, архитектурных решениях.
**Правила:**
- Строго следовать дизайн-системе Artisan Pastel (цвета, шрифты выше)
- Все Firestore операции — по пути `artifacts/trustlist-production/public/data/`
- Compose-first, никакого XML
- Разбивать на переиспользуемые @Composable функции

### 🎨 UI Designer  
Используй при: проектировании новых экранов, компонентов, улучшении существующего UI.
**Правила:**
- Дизайн-токены строго из Artisan Pastel
- Компоненты: ConvexCardBox, AppTextStyles — переиспользовать
- Золотой акцент (#D4AF37) — только для монетизации/спонсорства
- Material 3 компоненты как база

### 📐 UX Architect
Используй при: проектировании флоу, навигации, новых фичей.
**Правила:**
- Два типа пользователей: isBusiness true/false — разные UX-флоу
- Минимум экранов для ключевых действий (принять оффер → 2 тапа)
- Социальный граф = основной источник контента в ленте

### 📸 Instagram Curator (Growth)
Используй при: планировании виральных механик, UGC, роста аудитории.
**Применение для TrustList:**
- Механики шаринга постов/коллекций вне приложения
- "Зов стаи" как вирусный инструмент (пригласи друга ответить)
- Визуальное оформление sponsored-постов (нативно, не как реклама)

---

## ⚡ Быстрые команды

**Сгенерировать новый экран:**
> Роль: Senior Android Developer + UI Designer.
> Создай [ИмяЭкрана].kt в стиле Artisan Pastel. Используй Firestore путь выше.

**Добавить бизнес-логику:**
> Роль: Senior Android Developer.
> Добавь логику [описание] в [файл]. Данные из Firestore коллекции [имя].

**Спроектировать новый флоу:**
> Роль: UX Architect.
> Спроектируй флоу для [фича]. Учти два типа пользователей (isBusiness).

**Улучшить UI компонент:**
> Роль: UI Designer.
> Улучши компонент [имя] используя токены Artisan Pastel. Добавь состояния: loading, empty, error.

---

## 📁 Ключевые файлы
```
app/src/main/java/com/example/recommend/
├── MainAppScreen.kt      # Навигация и маршрутизация
├── FeedScreen.kt         # Лента + Зов стаи + Офферы
├── AddScreen.kt          # Создание поста
├── ProfileScreen.kt      # Профиль
├── ExploreScreen.kt      # Поиск
├── CreateOfferScreen.kt  # Бизнес: создать кампанию
├── AddHubScreen.kt       # Бизнес: хаб действий
└── ui/theme/             # Цвета, шрифты, компоненты
```

---

## 🚨 Важные ограничения
- **НЕ менять** путь Firestore (`trustlist-production`)
- **НЕ использовать** XML layouts — только Compose
- **НЕ хардкодить** цвета — только из `ui/theme/`
- Firebase project ID: `trustlist-fc435`
