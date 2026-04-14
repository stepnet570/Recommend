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

> ⚠️ Реальные токены из `ui/theme/Color.kt` — используй только их, не CLAUDE.md как источник правды.

```kotlin
// ui/theme/Color.kt — актуальные значения
val AppLime       = Color(0xFF7AE23A)  // градиент-старт, активные иконки
val AppTeal       = Color(0xFF3BD4C0)  // градиент-конец, Primary-кнопки
val AppDark       = Color(0xFF1A2A24)  // DarkPastelAnthracite — основной текст
val AppBackground = Color(0xFFF6FEFB)  // SoftPastelMint — фон экранов
val AppMuted      = Color(0xFF6B8C80)  // MutedPastelTeal — второстепенный текст
val AppGold       = Color(0xFFD4AF37)  // MutedPastelGold — монетизация, TrustCoins
val AppBorder     = Color(0x263BD4C0)  // полупрозрачный Teal для обводок
val AppSurface    = Color(0xFF1A3328)  // тёмная поверхность (тёмные карточки)
val AppWhite      = Color(0xFFFFFFFF)  // белые карточки/оверлеи
val SurfaceMuted  = Color(0xFFE8F5F0)  // светло-зелёный фон секций

// Градиент (используется везде: кнопки, активные иконки, FAB)
Brush.horizontalGradient(listOf(AppLime, AppTeal))  // #7AE23A → #3BD4C0

// Типографика
Заголовки: Syne (HeadingFontFamily) — R.font.syne_variable
Текст:     DM Sans (BodyFontFamily)  — R.font.dm_sans_variable
```

**Алиасы в коде (для совместимости):**
```kotlin
SoftPastelMint      = AppBackground  // #F6FEFB
DarkPastelAnthracite = AppDark       // #1A2A24
RichPastelCoral     = AppTeal        // #3BD4C0 (исторически — это Teal, не Coral!)
MutedPastelTeal     = AppMuted       // #6B8C80
MutedPastelGold     = AppGold        // #D4AF37
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
- AcceptOfferSheet — флоу принятия оффера
- PostDetailScreen, RequestDetailScreen, CollectionDetailScreen — детальные экраны
- PublicUserProfileScreen — публичный профиль другого пользователя
- BusinessOfferDetailScreen — детали кампании для бизнеса
- Firebase Auth + Firestore CRUD
- FirestorePaths — централизованный путь к данным
- Data-слой: PostRepository, UserRepository, OfferRepository, RequestRepository, CollectionRepository
- Модели: Post, UserProfile, AdOffer, PackRequest, PostCollection, Answer

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
├── MainAppScreen.kt               # Bottom nav + dialog выхода из creation flow
├── navigation/AppNavigation.kt    # NavHost + ВСЕ overlay-состояния (activeRequest, openPostId и т.д.)
├── ui/feed/FeedScreen.kt          # Лента + Зов стаи + Офферы
├── ui/feed/FeedViewModel.kt       # Все данные для Feed + текущий пользователь
├── ui/add/AddScreen.kt            # Создание поста
├── ui/profile/ProfileScreen.kt    # Профиль
├── ui/profile/ProfileViewModel.kt # Данные профиля
├── ui/explore/ExploreScreen.kt    # Поиск
├── ui/auth/AuthScreen.kt          # Авторизация
├── CreateOfferScreen.kt           # Бизнес: создать кампанию
├── AddHubScreen.kt                # Бизнес: хаб действий
├── AcceptOfferSheet.kt            # Принятие оффера (bottom sheet)
├── FirestorePaths.kt              # Единый путь к данным Firestore
├── data/model/                    # Post, UserProfile, AdOffer, PackRequest, PostCollection
├── data/repository/               # PostRepository, UserRepository, OfferRepository, ...
└── ui/theme/                      # Color.kt, Type.kt, Theme.kt, Convex.kt
```

---

## 🚨 Важные ограничения
- **НЕ менять** путь Firestore (`trustlist-production`)
- **НЕ использовать** XML layouts — только Compose
- **НЕ хардкодить** цвета — только из `ui/theme/Color.kt` (переменные `App*`)
- **НЕ хардкодить** `Color(0xFF...)` инлайн — только именованные токены
- Firebase project ID: `trustlist-fc435`

---

## ⚠️ Известный технический долг (учитывай при разработке)

| Проблема | Где | Влияние |
|----------|-----|---------|
| Лента не фильтрует по `following` | `FeedViewModel.feedPostsForHome` | Соцграф не работает |
| Нет пагинации | `PostRepository`, `UserRepository` | Масштабируемость |
| 20+ параметров в `AppNavigation` | `navigation/AppNavigation.kt` | Сложность поддержки |
| `authorName = "Alex"` дефолт | `data/model/Post.kt` | Баги в продакшене |
| `isMinifyEnabled = false` в release | `app/build.gradle.kts` | Размер APK, безопасность |
| `applicationId = "com.example.recommend"` | `app/build.gradle.kts` | Нужно сменить до релиза |
