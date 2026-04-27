# TrustList — Project Context for Claude

## 🎯 Project: TrustList
A social network for honest local recommendations within a trust circle ("pack").
Built-in micro-influencer marketplace: businesses pay TrustCoins to users for native content placement.

---

## 🛠 Stack
- **Android** / Kotlin / Jetpack Compose (Material 3)
- **Backend:** Firebase Firestore + Firebase Auth
- **DB path:** `artifacts/trustlist-production/public/data/<collection>`

---

## 💾 Firestore Schema

| Collection | Key fields |
|------------|-----------|
| `users` | uid, email, name, avatar, following[], isBusiness, trustCoins, trustScore |
| `posts` | userId, title, description, category, location, rating, imageUrl, isSponsored |
| `collections` | userId, name, postIds[] |
| `requests` | — (Pack Call / Зов стаи) |
| `answers` | — (replies to requests) |
| `offers` | businessId, title, rewardCoins, minTrustScore, status |

---

## 🎨 Design System "Artisan Pastel" (updated)

> ⚠️ Source of truth is `ui/theme/Color.kt` — always read the file, not this doc.

```kotlin
// ui/theme/Color.kt — current values
val AppViolet     = Color(0xFF7C6FE0)  // primary accent (replaced Lime), gradient start
val AppLime       = AppViolet          // alias for backward compatibility
val AppTeal       = Color(0xFF3BD4C0)  // gradient end, primary buttons
val AppDark       = Color(0xFF1A2A24)  // main text
val AppBackground = Color(0xFFF8F7F4)  // warm off-white screen background
val AppMuted      = Color(0xFF8A9A95)  // secondary text
val AppGold       = Color(0xFFD4AF37)  // monetization, TrustCoins only
val AppBorder     = Color(0x263BD4C0)  // semi-transparent Teal for outlines
val AppSurface    = Color(0xFF1A3328)  // dark surface (dark cards)
val AppWhite      = Color(0xFFFFFFFF)  // white cards / overlays
val SurfaceMuted  = Color(0xFFF0EEEB)  // warm neutral for card sections

// Primary gradient (buttons, FAB, active icons, TrustScoreRing, Pack Call cards)
Brush.horizontalGradient(listOf(AppViolet, AppTeal))  // #7C6FE0 → #3BD4C0

// Typography
Headings: Syne (HeadingFontFamily) — R.font.syne_variable
Body:     DM Sans (BodyFontFamily)  — R.font.dm_sans_variable
```

**Aliases in code (for compatibility):**
```kotlin
SoftPastelMint       = AppBackground  // #F8F7F4
DarkPastelAnthracite = AppDark        // #1A2A24
RichPastelCoral      = AppTeal        // #3BD4C0
MutedPastelTeal      = AppMuted       // #8A9A95
MutedPastelGold      = AppGold        // #D4AF37
```

---

## ✅ Done
- FeedScreen with posts, Pack Calls (Зов стаи), Exclusive Deals block
- Redesigned Feed header: greeting + TrustCoins chip
- Redesigned Pack Pulse: gradient ring around avatars (Violet→Teal)
- Redesigned Pack Call cards: full gradient background, consistent fixed height
- TrustScoreRing component replacing star ratings (circular arc, 0–10 scale)
- AddScreen — post creation (category, rating, location)
- ProfileScreen — user profile
- ExploreScreen — search / recommendations
- CreateOfferScreen — business creates AdOffer campaign
- AddHubScreen — business action hub (post or campaign)
- MainAppScreen — navigation + role-based routing (isBusiness)
- AcceptOfferSheet — offer acceptance flow
- PostDetailScreen, RequestDetailScreen, CollectionDetailScreen — detail screens
- PublicUserProfileScreen — public profile of another user
- BusinessOfferDetailScreen — campaign details for business
- Firebase Auth + Firestore CRUD
- FirestorePaths — centralized data path
- Data layer: PostRepository, UserRepository, OfferRepository, RequestRepository, CollectionRepository
- Models: Post, UserProfile, AdOffer, PackRequest, PostCollection, Answer

---

## 🔥 TODO (priority order)
1. **TrustScore** — scoring logic, recalculation on new ratings
2. **TrustCoins wallet** — balance screen, transaction history
3. **Offer acceptance flow** — accept → create sponsored post → deduct coins from business
4. **Social graph** — follow/unfollow, feed filtered to pack only
5. **Collections** — create, edit, add post to collection
6. **Notifications** — new requests, accepted offers
7. **Business dashboard** — campaign analytics (posts created, reach)
8. **Onboarding** — first launch, role selection (user / business)
9. **Image upload** — photo upload to Firebase Storage
10. **Deep links / sharing** — share post or collection externally

---

## 🤖 Active Roles (agency-agents)

### 🏗 Senior Android Developer
Use for: Kotlin/Compose code, Firebase integration, architectural decisions.
**Rules:**
- Strictly follow Artisan Pastel design system (colors and fonts above)
- All Firestore operations use path `artifacts/trustlist-production/public/data/`
- Compose-first, no XML layouts
- Break UI into reusable @Composable functions

### 🎨 UI Designer
Use for: designing new screens, components, improving existing UI.
**Rules:**
- Design tokens strictly from Artisan Pastel
- Reuse components: ConvexCardBox, AppTextStyles, TrustScoreRing
- Gold accent (#D4AF37) — monetization and sponsorship only
- Violet (#7C6FE0) — primary interactive accent
- Material 3 components as base

### 📐 UX Architect
Use for: flow design, navigation, new feature planning.
**Rules:**
- Two user types: isBusiness true/false — different UX flows
- Minimum screens for key actions (accept offer → 2 taps)
- Social graph = primary content source in the feed

### 📸 Instagram Curator (Growth)
Use for: viral mechanics, UGC, audience growth.
**Application for TrustList:**
- Post/collection sharing mechanics outside the app
- "Pack Call" as a viral tool (invite a friend to answer)
- Visual styling of sponsored posts (native, not ad-like)

---

## ⚡ Quick Commands

**Generate a new screen:**
> Role: Senior Android Developer + UI Designer.
> Create [ScreenName].kt in Artisan Pastel style. Use Firestore path above.

**Add business logic:**
> Role: Senior Android Developer.
> Add logic [description] to [file]. Data from Firestore collection [name].

**Design a new flow:**
> Role: UX Architect.
> Design flow for [feature]. Account for two user types (isBusiness).

**Improve a UI component:**
> Role: UI Designer.
> Improve component [name] using Artisan Pastel tokens. Add states: loading, empty, error.

---

## 📁 Key Files
```
app/src/main/java/com/example/recommend/
├── MainAppScreen.kt               # Bottom nav + exit creation flow dialog
├── navigation/AppNavigation.kt    # NavHost + ALL overlay states (activeRequest, openPostId, etc.)
├── ui/feed/FeedScreen.kt          # Feed + Pack Call + Deals + TrustScoreRing
├── ui/feed/FeedViewModel.kt       # All Feed data + current user
├── ui/add/AddScreen.kt            # Post creation
├── ui/profile/ProfileScreen.kt    # User profile
├── ui/profile/ProfileViewModel.kt # Profile data
├── ui/explore/ExploreScreen.kt    # Search / explore
├── ui/auth/AuthScreen.kt          # Authentication
├── CreateOfferScreen.kt           # Business: create campaign
├── AddHubScreen.kt                # Business: action hub
├── AcceptOfferSheet.kt            # Offer acceptance (bottom sheet)
├── FirestorePaths.kt              # Single source of truth for Firestore paths
├── data/model/                    # Post, UserProfile, AdOffer, PackRequest, PostCollection
├── data/repository/               # PostRepository, UserRepository, OfferRepository, ...
└── ui/theme/                      # Color.kt, Type.kt, Theme.kt, Convex.kt
```

---

## 🚨 Hard Constraints
- **DO NOT change** Firestore path (`trustlist-production`)
- **DO NOT use** XML layouts — Compose only
- **DO NOT hardcode** colors — only named tokens from `ui/theme/Color.kt` (`App*` variables)
- **DO NOT inline** `Color(0xFF...)` — use named tokens only
- Firebase project ID: `trustlist-fc435`

---

## ⚠️ Known Technical Debt

| Issue | Where | Impact |
|-------|-------|--------|
| ~~Feed not filtered by `following`~~ | ✅ Fixed | — |
| No pagination | `PostRepository`, `UserRepository` | Scalability |
| 20+ parameters in `AppNavigation` | `navigation/AppNavigation.kt` | Maintenance complexity |
| `authorName = "Alex"` default | `data/model/Post.kt` | Production bugs |
| `isMinifyEnabled = false` in release | `app/build.gradle.kts` | APK size, security |
| `applicationId = "com.example.recommend"` | `app/build.gradle.kts` | Must change before release |
