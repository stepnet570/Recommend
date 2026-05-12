# TrustList — QA Stage 1: Smoke + Auth + Navigation

**Цель этапа:** отсечь критические дефекты, которые блокируют дальнейшее тестирование (Этапы 2–6). Если падает хотя бы один **Critical** — фикс перед переходом к Этапу 2.

**Среда:** Android-эмулятор + Firebase production (`trustlist-production`).
**Тест-аккаунты:** заведи 3 — `qa_user_1@trustlist.test`, `qa_user_2@trustlist.test`, `qa_business_1@trustlist.test` (последний — `isBusiness=true` в Firestore вручную).

**Как пользоваться:** проходишь по строкам слева направо, статус помечаешь в bug-tracker.xlsx (PASS / FAIL / BLOCKED). На каждый FAIL — заводишь баг в том же xlsx.

---

## A. Smoke — запуск приложения

| ID | Шаг | Ожидаемый результат | Severity если FAIL |
|----|-----|---------------------|--------------------|
| A1 | Свежеустановленное APK, запуск без сессии | Открывается AuthScreen, режим Sign in, белая карточка с иконкой 🐺 | Blocker |
| A2 | Запуск с активной сессией (после логина) | Сразу MainAppScreen на табе Feed | Blocker |
| A3 | Свернуть приложение → развернуть через 30с | Состояние таба и скролла сохраняется, нет повторного запроса логина | Major |
| A4 | Поворот экрана на любом экране | Нет краша, форма Auth/Feed не сбрасывается | Major |
| A5 | Старт в режиме самолёта (offline) | Открывается, но показывает empty/loading; не виснет | Major |
| A6 | Шрифты Syne (заголовки) и DM Sans (body) применены | Заголовки гротеск с засечками, body — sans-serif | Minor |
| A7 | Цвета: фон `#F8F7F4`, акцент Violet→Teal градиент | Соответствует Artisan Pastel | Minor |
| A8 | Logcat при старте | Нет ERROR/FATAL по тегам `TrustListApp`, `AuthScreen`, `Firestore` | Major |
| A9 | Время от тапа по иконке до интерактивного экрана | < 3 сек на эмуляторе | Minor |
| A10 | Tag `TrustListApp` пишет `ensureUserProfile completed for uid=...` (если есть сессия) | Лог присутствует | Minor |

---

## B. Регистрация (Sign up)

| ID | Шаг | Ожидаемый результат | Severity если FAIL |
|----|-----|---------------------|--------------------|
| B1 | Тап «No account? Sign up» | Появляются поля Full name, Handle; «Forgot password» исчезает | Major |
| B2 | Sign up с пустыми email/password | Banner «Fill in all fields» | Major |
| B3 | Sign up с email = `"   "` (только пробелы) | Banner с осмысленной ошибкой (не «Fill in all fields») | Minor |
| B4 | Email = `abc`, password = `123456` | Banner «Invalid email format» | Major |
| B5 | Email валидный, password = `12345` (<6) | Banner «Password too weak. Use at least 6 characters» | Major |
| B6 | Email уже существует в Firestore | Banner «This email is already registered. Sign in instead» | Major |
| B7 | Все валидно → Submit | Спиннер на кнопке, потом MainAppScreen | Blocker |
| B8 | После B7 проверить Firestore | По пути `artifacts/trustlist-production/public/data/users/{uid}` создан документ с `email`, `name`, `handle`, `trustCoins` | Blocker |
| B9 | Sign up без Full name (пусто) | name = `Email_до_собаки.capitalize()` (например, `Qa_user_1`) | Minor |
| B10 | Sign up с handle, который уже есть у другого юзера | Должна быть валидация уникальности (проверь — если нет, заводи баг) | Major |
| B11 | Длинный name (>50 симв.) и handle (>30) | UI не ломается; либо обрезается, либо валидация | Minor |
| B12 | Кириллица/эмодзи в handle | Либо принимается консистентно, либо валидируется | Minor |
| B13 | **Edge:** Sign up успешен в Auth, но Firestore.set падает (отключи интернет после Auth.create) | Юзер должен иметь возможность повторить регистрацию. **Подозрение на баг:** auth.signOut() оставляет FirebaseAuth-юзера → повторная регистрация даст «email already in use» | Critical |
| B14 | Toggle Sign up → Sign in после ввода name/handle | Поля name/handle очищаются (UX-ожидание) | Minor |
| B15 | Двойной тап по «Create account» | Нет двойного запроса (кнопка disabled во время загрузки) | Major |

---

## C. Логин (Sign in)

| ID | Шаг | Ожидаемый результат | Severity если FAIL |
|----|-----|---------------------|--------------------|
| C1 | Email + password корректны | Спиннер → MainAppScreen | Blocker |
| C2 | Email верный, пароль неверный | «Wrong password. Check spelling and Caps Lock» | Major |
| C3 | Email несуществующий | «No account for this email. Sign up first» | Major |
| C4 | Email = `notanemail` | «Invalid email format» | Major |
| C5 | 6+ подряд неверных попыток | «Too many attempts. Try again later» | Minor |
| C6 | Иконка глаза в поле пароля | Toggle между скрытым/видимым паролем | Minor |
| C7 | Тап «Forgot password?» | **Известный баг:** в коде `/* TODO */`, никакой реакции — заведи баг | Major |
| C8 | После logout сразу повторный логин с другим аккаунтом | Работает, профиль и trustCoins подтягиваются нового юзера | Blocker |
| C9 | Закрыть приложение полностью (kill task) → открыть | Сессия сохранена, открывается MainAppScreen, не AuthScreen | Major |
| C10 | Loading на кнопке Sign in | Градиент с alpha=0.5, спиннер 24dp белый | Minor |

---

## D. Навигация (bottom nav + детали)

| ID | Шаг | Ожидаемый результат | Severity если FAIL |
|----|-----|---------------------|--------------------|
| D1 | Тап по Home | FeedScreen, иконка Home подсвечена градиентом Violet→Teal | Blocker |
| D2 | Тап по Search (компас) | ExploreScreen, иконка подсвечена | Blocker |
| D3 | Тап по «+» (центральная) | `isBusiness=false` → AddScreen; `isBusiness=true` → AddHubScreen | Blocker |
| D4 | Тап по Profile | ProfileScreen своего юзера | Blocker |
| D5 | Двойной тап по уже активному табу | Нет дублирования стека, скролл к началу или ничего | Minor |
| D6 | Открыть пост из Feed | PostDetailScreen с заголовком, рейтингом TrustScoreRing, фото | Blocker |
| D7 | Back из PostDetail | Возврат в Feed на ту же позицию | Major |
| D8 | Открыть Pack Call из Feed | RequestDetailScreen с описанием и формой ответа | Blocker |
| D9 | Open Public Profile (тап по аватарке автора поста) | PublicUserProfileScreen с постами автора | Major |
| D10 | Открыть коллекцию из ProfileScreen | CollectionDetailScreen | Major |
| D11 | Зайти в AddScreen, ввести часть данных, попытаться перейти на Profile | Диалог «Leave this screen? Your progress will be lost» | Major |
| D12 | В диалоге «Stay» | Остаёмся в Add, данные не потеряны | Major |
| D13 | В диалоге «Leave» | Переходим на target таб, форма очищается | Minor |
| D14 | Если AddScreen не заполнен (на экране выбора Hub) — переключить таб | Без диалога, переключение мгновенное | Minor |
| D15 | Системная Back из Feed | Сворачивает приложение (или диалог, в зависимости от ОС) | Minor |
| D16 | Активная иконка в bottom nav | Tint = Teal, indicator Lime alpha 15% | Minor |

---

## E. Logout / переключение аккаунтов

| ID | Шаг | Ожидаемый результат | Severity если FAIL |
|----|-----|---------------------|--------------------|
| E1 | Logout из ProfileScreen | Activity recreate → AuthScreen | Blocker |
| E2 | После Logout попытка свернуть/развернуть | Открывается AuthScreen, не предыдущий Feed | Major |
| E3 | Logout → нет утечек listener'ов в Firestore | Logcat без `Firestore Listen failed: PERMISSION_DENIED` | Major |
| E4 | Logout → повторный Sign up на тот же email | «email already in use» (корректно) | Minor |

---

## F. Role-based routing (isBusiness)

| ID | Шаг | Ожидаемый результат | Severity если FAIL |
|----|-----|---------------------|--------------------|
| F1 | Залогиниться `qa_user_1` (`isBusiness=false`) | Таб «+» → AddScreen напрямую | Blocker |
| F2 | Залогиниться `qa_business_1` (`isBusiness=true`) | Таб «+» → AddHubScreen с выбором (Post / Campaign) | Blocker |
| F3 | Из AddHubScreen → Campaign | CreateOfferScreen | Major |
| F4 | Из AddHubScreen → Post | AddScreen в business-режиме | Major |
| F5 | На AddHubScreen есть кнопка «Switch to consumer» (footnote) | Открывается SwitchToBusinessSheet | Minor |

---

## Definition of Done для Этапа 1
1. Все строки имеют статус (PASS/FAIL/BLOCKED).
2. Все Critical/Blocker баги — заведены в трекере и переданы разработчику.
3. Снят минимум один скриншот для каждого Blocker-бага.
4. Logcat-выгрузка по найденным крашам приложена.

После закрытия Critical/Blocker → переходим к **Этапу 2: Feed Core** (лента, Pack Pulse, Pack Call, Exclusive Deals, deep-открытие деталей).
