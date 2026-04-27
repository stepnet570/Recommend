# TrustList — брифинг для валидации в aicofounder.com

> Копируй блоки в соответствующие фазы AI cofounder (Problem → Audience → Solution → Research → Plan). Внизу — готовые prompt-ы для прогонки.

---

## 1. Одним предложением (Elevator Pitch)

TrustList — социальная сеть честных локальных рекомендаций внутри «стаи» (круга доверия), со встроенным маркетплейсом микро-инфлюенсеров: бизнес платит TrustCoins пользователям за нативные посты.

---

## 2. Проблема (Problem Statement)

Люди перестали доверять отзывам в Google Maps, Yelp, 2ГИС и Instagram: накрученные 5 звёзд, проплаченные блогеры, бот-комментарии. При этом лучший способ найти «где поесть / к кому сходить / что купить» — это спросить друга, но в мессенджерах эти рекомендации теряются и не систематизируются. Малый бизнес тратит бюджет на таргет с CTR <1%, хотя нативный пост от знакомого конвертит в разы лучше.

**Боль № 1 (для пользователя):** «Не знаю, какому отзыву верить».
**Боль № 2 (для бизнеса):** «Не могу дотянуться до реальных клиентов без блогеров за $1000+».
**Боль № 3 (для микро-инфлюенсера):** «У меня 500 подписчиков-друзей, но я никак не монетизирую своё влияние».

---

## 3. Целевая аудитория (ICP)

**B2C — ядро:**
- 22–38 лет, городские жители (1M+), активные в соцсетях, но устали от Instagram.
- Пьют кофе в местных кофейнях, ходят к конкретному парикмахеру, ищут стоматолога через Telegram-чаты.
- Гео-пилот: Москва / Тбилиси / Стамбул / Лиссабон / Берлин (экспат-хабы — в них максимальный запрос на «куда пойти»).

**B2B — монетизация:**
- Локальный малый бизнес: кофейни, бьюти-салоны, независимые бренды одежды, ресторанчики, студии йоги.
- Бюджет на маркетинг: $100–2000/мес.
- Сейчас: тратят на Instagram Ads / блогеров / Google Ads с плохой атрибуцией.

**B2C — креатор (вторичный ICP):**
- Обычные пользователи с 100–2000 реальных связей, не профессиональные блогеры.

---

## 4. Уникальное Value Proposition (UVP)

| Для кого | Что получает | Как |
|----------|-------------|-----|
| Пользователь | Честные рекомендации от своих | Feed отфильтрован по `following` + TrustScore |
| Микро-инфлюенсер | Доход без выхода в «блогерство» | TrustCoins за принятые офферы |
| Бизнес | Нативная реклама по CPA, а не CPM | Оплата только за пост от юзера с ≥X TrustScore |

**Отличие от Yelp / Google Reviews:** у нас нет анонимных отзывов — только от людей из твоего круга.
**Отличие от Instagram:** нативная монетизация встроена в продукт, не надо договариваться в DM.
**Отличие от Nextdoor:** фокус не на соседстве, а на доверии (pack = твой выбор, а не адрес).

---

## 5. Гипотезы для валидации (главное — это)

**H1 (Desirability):** Люди 25–35 готовы удалить/игнорировать Google Maps ради платформы, где рекомендации только от своих. → Тест: 30 проблемных интервью.

**H2 (Viability B2B):** Локальный бизнес готов платить $50–200 за 1 нативный пост от юзера с TrustScore 8+ (vs. $500+ за блогера). → Тест: 15 звонков владельцам кофеен/салонов.

**H3 (Retention):** Пользователь вернётся в приложение ≥3 раз в неделю, если в Feed'е каждый день появляется ≥5 рекомендаций от знакомых. → Тест: cohort retention D7/D30 после пилота.

**H4 (Supply):** Обычные юзеры (не блогеры) готовы принимать бренд-офферы, если оплата прозрачная и пост остаётся их. → Тест: 10 интервью с потенциальными креаторами.

**H5 (Viral loop):** Механика «Зов стаи» (Pack Call) — основной driver роста, CAC < $2. → Тест: замер k-factor в первые 2 недели после пилота.

---

## 6. MVP — что уже сделано

Android / Kotlin / Jetpack Compose + Firebase Firestore.

Готово: Feed с постами, Pack Calls, Exclusive Deals; TrustScore Ring вместо звёзд; создание постов; профиль; поиск; режим бизнеса с созданием AdOffer кампаний; accept-offer flow; авторизация; CRUD для всех сущностей (posts, users, offers, requests, collections).

Не хватает: логика расчёта TrustScore, кошелёк TrustCoins, full offer-flow (accept → sponsored post → списание), follow/unfollow, нотификации, onboarding, Image upload, iOS-версия.

---

## 7. Конкуренты и позиционирование

| Продукт | Сила | Слабость vs TrustList |
|---------|------|----------------------|
| Google Maps / Yelp | Охват | Анонимные отзывы, накрутка |
| Nextdoor | Локальность | Скучный контент, старая аудитория |
| Instagram / TikTok | Контент | Нет структурированных рекомендаций + накрутка |
| BeReal | Доверие/friends-only | Нет монетизации, угасает |
| Partiful / Gem | Круг друзей | Не про рекомендации |
| Telegram-чаты («Куда сходить в Тбилиси») | Локальность + доверие | Хаос, не монетизируется |

**Main threat:** Instagram добавит «Friends-only reviews». → Наш moat: TrustCoins-экономика + TrustScore.

---

## 8. Модель монетизации

1. **Take rate 20–30%** с каждой сделки business → user (основной).
2. **Subscription для бизнеса** ($29/мес за доступ к аналитике, приоритет в Exclusive Deals).
3. **TrustCoins — premium-подписка для юзера** ($3.99/мес за +15% к TrustScore gain, кастомизацию профиля).

Unit-экономика (гипотеза):
- ARPU business: $80/мес → 25% take rate → $20 на пользователя-бизнес
- Один бизнес-юзер «кормит» ~30 креаторов → нужен баланс supply/demand 1:30

---

## 9. Риски (что может убить проект)

1. **Холодный старт:** без критической массы «своих» в городе feed пустой → решение: запуск по городам через closed beta + инвайты.
2. **Деградация доверия:** если TrustScore можно накрутить — весь продукт мёртв → решение: scoring на основе действий в pack-графе, не лайков.
3. **Регуляторика:** FTC / EU DSA требуют маркировки спонсорских постов → уже есть `isSponsored` флаг, нужен видимый label.
4. **Chicken-and-egg:** бизнесу нужны юзеры, юзерам — ценный контент → решение: стартовать города с фокусом на creator-first (как Doordash делал).

---

## 10. 3 вопроса, ради которых стоит идти в aicofounder

1. **Reddit / community research:** Где на Reddit / в твитах / форумах больше всего жалоб на «фейковые отзывы» и «не нашёл нормальной кофейни»? → Это наш organic-discovery канал.
2. **Market sizing:** Сколько в реальности стоит рынок micro-influencer marketing для локального бизнеса в выбранных 5 городах?
3. **Competitor deep dive:** Кто уже делал «друзья + рекомендации + монетизация» и почему закрылся? (Burb, Nextdoor Biz, Swarm — что пошло не так?)

---

## Готовые prompt-ы для прогона в aicofounder.com

### Prompt 1 — Problem validation
```
I'm building TrustList — a social network for honest local recommendations inside a trust circle ("pack"), with a built-in micro-influencer marketplace where small businesses pay TrustCoins to users for native posts.

Search Reddit, Hacker News, X/Twitter and local expat forums for evidence that real people (age 22–38, urban) actively complain about:
1. Fake reviews on Google Maps / Yelp / Instagram
2. Not trusting influencer recommendations
3. Difficulty finding local services through current apps

Return: top 20 quotes with links, grouped by pain level.
```

### Prompt 2 — Competitor autopsy
```
Find past startups that tried "friends-only recommendations" or "micro-influencer marketplace for local business" and failed or pivoted. For each: year, funding raised, reason for failure, lesson for TrustList.

Must include: Burb, Foursquare Swarm, Path, Nextdoor Business, Gowalla, and any recent YC batches.
```

### Prompt 3 — B2B willingness to pay
```
I'm pricing a micro-influencer campaign on TrustList: a business pays X TrustCoins (= $Y) to get 1 native post from a user with TrustScore ≥ 8 and 500+ real connections.

Research: what do local coffee shops / beauty salons / independent brands currently pay for 1 Instagram micro-influencer post (1k–10k followers) in Moscow, Tbilisi, Berlin, Lisbon, Istanbul? What CAC do they tolerate?

Output: price corridor + 3 pricing strategies I could A/B test.
```

### Prompt 4 — Cold-start playbook
```
TrustList has classic chicken-and-egg: users need content, content needs users, businesses need both. Suggest a creator-first cold-start playbook for city #1 (pick the best city from: Tbilisi, Lisbon, Berlin, Istanbul). 

Include: first 100 creators acquisition plan, which 10 businesses to bring first, viral loop mechanics using existing "Pack Call" feature, and week-by-week milestones for first 90 days.
```

### Prompt 5 — TrustScore anti-fraud
```
Design an anti-gaming scoring model for TrustScore (0–10) that resists:
- self-upvoting rings
- buying fake followers
- sponsored-post dilution
Constraints: must be computable in Firestore (no heavy ML for v1), must feel fair to the user, must be explainable in 1 sentence.
```

---

## Стоит ли aicofounder.com твоих денег — мой вердикт

Плюсы под твой кейс:
- Structured flow (Problem → Research → Plan) — экономит недели, если идея сырая. Твоя уже не сырая.
- Reddit-ресёрч и citation-based reports — реально полезны для H1 и Prompt 1 выше.
- Критичный AI («won't say "amazing idea"») — полезнее, чем ChatGPT-да-человек.

Минусы под твой кейс:
- У тебя уже **MVP в коде** и CLAUDE.md на 200 строк. Фазы Ideation / Problem ты прошёл. Платить за них жалко.
- «Canvas» и «Ultraplan» дублируют то, что уже делают Linear/Notion + этот самый воркспейс.

**Что делать (практично):**
1. Возьми trial / free tier. Прогони ТОЛЬКО Prompt 1, 2 и 3 из списка выше — это 3 research-отчёта, которые в ChatGPT получить сложно (нет native Reddit-поиска с цитатами).
2. Не плати годовую подписку, пока не увидишь, что отчёты reality-based, а не «AI hallucination с красивыми графиками».
3. Если не зашло — то же самое сделаем здесь через Reddit API + Perplexity + ручной прозвон, выйдет дешевле.

Вердикт: **$20–30 за 1 месяц — стоит. Больше — не стоит, твой продукт уже в executions-фазе.** Главная ценность там — валидация B2B pricing (Prompt 3) и cold-start плейбук (Prompt 4). Всё остальное — nice-to-have.
