<div align="center">

<img src="docs/icon-preview.png" alt="GitYou icon (light and dark)" width="240"/>

# GitYou

**A native, expressive GitHub client for Android**

Kotlin · Jetpack Compose · Material 3 Expressive

[![Build](https://github.com/oustery/GitYou/actions/workflows/build.yml/badge.svg)](https://github.com/oustery/GitYou/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose%20M3%20Expressive-1.5--alpha-4285F4.svg)](https://developer.android.com/develop/ui/compose/material3/expressive)

[English](README.md) · [Русский](README.ru.md)

</div>

---

## Why GitYou

The official GitHub mobile app covers the basics, but a native client can go further: a fully expressive Material 3 design system, per-ABI APKs that stay small on old devices, and direct REST API coverage without web-view shortcuts.

GitYou is built **Kotlin-first** with Jetpack Compose and Material 3 Expressive: spring-driven motion, wavy progress indicators, a morphing star button and a light/dark icon that follows the system theme.

## Features

| Area | What you get |
|---|---|
| **Auth** | OAuth Device Flow (enter a code on github.com) with a personal access token fallback |
| **Home** | Your repositories, refreshed with pull-to-refresh |
| **Search** | Live, debounced repository search with best-match / stars / recent-updates sorting |
| **Repository** | Overview with stats, README (rendered markdown), star/fork actions, code browser with file viewer |
| **Issues & PRs** | Open/closed lists, fully rendered markdown bodies and comments, comments, creating issues with a live markdown preview — all from the app |
| **Notifications** | Unread feed with All/Unread filters, swipe a card to mark it read, mark all, tap-through to the conversation |
| **Profile** | Avatar, bio, followers/following, repositories and stars tabs |
| **Theming** | System / light / dark themes, four accent palettes (Indigo, Ocean, Forest, Sunrise), true-black AMOLED mode, Material You dynamic color (Android 12+), in-app text scale, toggleable expressive motion |
| **Localization** | English and Russian with an in-app language switcher (Android 13+), fully parity-checked |
| **Adaptive layout** | Bottom navigation on phones, navigation rail on expanded widths |

## Architecture

MVVM + Repository over unidirectional Compose state:

```
app/src/main/java/com/gityou/app/
├── data/
│   ├── remote/        # Retrofit services, DTOs, OAuth Device Flow logic
│   ├── repo/          # Auth / Repos / Issues / Notifications repositories
│   ├── store/         # DataStore: token, OAuth client id, theme settings
│   └── util/          # Link-header pagination, error keys, formatters
├── di/                # Hilt wiring
└── ui/
    ├── theme/         # Material 3 Expressive design system (see docs/DESIGN_SYSTEM.md)
    ├── components/    # Reusable expressive components
    ├── navigation/    # Routes + NavHost
    └── screens/       # auth, home, search, repo, issue, notifications, profile, settings
```

- **Stack**: Kotlin 2.2, Compose BOM, Material 3 1.5.0-alpha (Expressive APIs), Retrofit 2 + kotlinx.serialization, OkHttp 4, Hilt, Coil 3, Markwon, DataStore.
- **minSdk 26 / targetSdk 36 / compileSdk 36.**

## Building

```bash
# Debug APKs (one per ABI: armeabi-v7a, arm64-v8a, x86, x86_64)
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest
```

### Per-ABI APKs from CI

Every push to `main` runs [`.github/workflows/build.yml`](.github/workflows/build.yml): unit tests, then `assembleDebug` + `assembleRelease`. Because ABI splits are enabled, each build produces **a separate APK per architecture** — download them from the run's *Artifacts* section (`GitYou-debug-apks`, `GitYou-release-apks`).

### Release signing

CI signs release APKs from repository secrets:

| Secret | Meaning |
|---|---|
| `KEYSTORE_BASE64` | base64-encoded `.jks` keystore |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | signing key alias |
| `KEY_PASSWORD` | key password |

Without those secrets the release APKs are simply unsigned — local builds always work.

### OAuth Device Flow setup

1. Open **GitHub → Settings → Developer settings → OAuth Apps → New OAuth App**.
2. Fill in any homepage/callback URL (unused by Device Flow) and check **Enable Device Flow**.
3. Paste the **Client ID** either into `gradle.properties` (`GITYOU_CLIENT_ID=…`) or into the in-app field on the auth screen.

No OAuth App yet? Sign in with a personal access token (`repo`, `read:user`, `notifications`) — Device Flow becomes available the moment a Client ID is configured.

## Design system

The full token reference — palette, typography, shapes, motion and component guidance — lives in [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md). Highlights:

- Independent light/dark schemes around an **indigo** primary and a **coral** tertiary accent (plus Ocean, Forest and Sunrise accent palettes, and a true-black AMOLED variant);
- shape family 12/20/32/48 dp;
- `MotionScheme.expressive()` (toggleable) with spring-based state changes and expressive navigation transitions;
- theme-aware adaptive launcher icon (`values-night`) plus a monochrome layer for themed icons;
- Material You dynamic color behind a user setting;
- **Roboto Flex** variable font instantiated per weight for editorial display type.

## Roadmap

- [ ] Diff viewer for pull requests
- [ ] Release browsing
- [ ] Code syntax highlighting
- [ ] Gists
- [ ] Repository settings management

## Screenshots

> Coming soon — the app is under active development. CI artifacts above produce installable APKs.

## License

[MIT](LICENSE) © 2026 Oustery
