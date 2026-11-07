<div align="center">

<img src="docs/icon-preview.png" alt="Иконка GitYou (светлая и тёмная)" width="240"/>

# GitYou

**Нативный выразительный GitHub-клиент для Android**

Kotlin · Jetpack Compose · Material 3 Expressive

[![Build](https://github.com/oustery/GitYou/actions/workflows/build.yml/badge.svg)](https://github.com/oustery/GitYou/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose%20M3%20Expressive-1.5--alpha-4285F4.svg)](https://developer.android.com/develop/ui/compose/material3/expressive)

[English](README.md) · [Русский](README.ru.md)

</div>

---

## Зачем GitYou

Официальное мобильное приложение GitHub закрывает базовые сценарии, но нативный клиент может больше: полноценная выразительная дизайн-система Material 3, отдельные APK под каждую архитектуру, которые остаются компактными даже на старых устройствах, и прямая работа с REST API без веб-вью.

GitYou построен **Kotlin-first** на Jetpack Compose и Material 3 Expressive: spring-анимации, волнистые индикаторы прогресса, морфирующая кнопка звезды и иконка, которая сама переключается между светлой и тёмной темами.

## Возможности

| Раздел | Что внутри |
|---|---|
| **Авторизация** | OAuth Device Flow (код на github.com) + быстрый вход по персональному токену |
| **Главная** | Ваши репозитории с обновлением свайпом вниз |
| **Поиск** | Живой поиск репозиториев по всему GitHub с дебаунсом |
| **Репозиторий** | Обзор со статистикой, README (готовый markdown), звезда/форк, браузер кода с просмотром файлов |
| **Issues и PR** | Списки открытых/закрытых, детали, комментарии, создание issues — прямо из приложения |
| **Уведомления** | Лента непрочитанных, отметить одно/все, переход к обсуждению |
| **Профиль** | Аватар, био, подписчики/подписки, вкладки репозиториев и звёзд |
| **Оформление** | Системная / светлая / тёмная темы, динамический цвет Material You (Android 12+) |
| **Локализация** | Русский и английский с полным паритетом строк |
| **Адаптивность** | Нижняя навигация на телефонах, Navigation Rail на широких экранах |

## Архитектура

MVVM + Repository поверх однонаправленного состояния Compose:

```
app/src/main/java/com/gityou/app/
├── data/
│   ├── remote/        # Retrofit-сервисы, DTO, логика OAuth Device Flow
│   ├── repo/          # Репозитории Auth / Repos / Issues / Notifications
│   ├── store/         # DataStore: токен, Client ID, настройки темы
│   └── util/          # Пагинация по Link-заголовку, ключи ошибок, форматтеры
├── di/                # Связывание через Hilt
└── ui/
    ├── theme/         # Дизайн-система M3 Expressive (см. docs/DESIGN_SYSTEM.md)
    ├── components/    # Переиспользуемые выразительные компоненты
    ├── navigation/    # Маршруты + NavHost
    └── screens/       # auth, home, search, repo, issue, notifications, profile, settings
```

- **Стек**: Kotlin 2.2, Compose BOM, Material 3 1.5.0-alpha (Expressive API), Retrofit 2 + kotlinx.serialization, OkHttp 4, Hilt, Coil 3, Markwon, DataStore.
- **minSdk 26 / targetSdk 36 / compileSdk 36.**

## Сборка

```bash
# Debug APK (отдельный файл под каждую ABI: armeabi-v7a, arm64-v8a, x86, x86_64)
./gradlew assembleDebug

# Юнит-тесты
./gradlew testDebugUnitTest
```

### Per-ABI APK из CI

Каждый пуш в `main` запускает [`.github/workflows/build.yml`](.github/workflows/build.yml): юнит-тесты, затем `assembleDebug` + `assembleRelease`. Благодаря включённым ABI splits каждая сборка выдаёт **отдельный APK под каждую архитектуру** — скачивайте их в разделе *Artifacts* прогона (`GitYou-debug-apks`, `GitYou-release-apks`).

### Подпись release-сборок

CI подписывает release APK из секретов репозитория:

| Секрет | Значение |
|---|---|
| `KEYSTORE_BASE64` | keystore `.jks`, закодированный в base64 |
| `KEYSTORE_PASSWORD` | пароль keystore |
| `KEY_ALIAS` | алиас ключа подписи |
| `KEY_PASSWORD` | пароль ключа |

Если секретов нет — release APK просто собираются без подписи, локальная сборка всегда работает.

### Настройка OAuth Device Flow

1. Откройте **GitHub → Settings → Developer settings → OAuth Apps → New OAuth App**.
2. Заполните любые Homepage/Callback URL (Device Flow их не использует) и включите галочку **Enable Device Flow**.
3. Вставьте **Client ID** либо в `gradle.properties` (`GITYOU_CLIENT_ID=…`), либо в поле на экране входа в приложении.

Пока OAuth App нет — входите персональным токеном (`repo`, `read:user`, `notifications`); Device Flow заработает сразу после указания Client ID.

## Дизайн-система

Полный справочник токенов — палитра, типографика, формы, движение и компоненты — в [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md). Кратко:

- независимые светлые/тёмные схемы с **индиго** в роли primary и **коралловым** акцентом tertiary;
- семейство форм 12/20/32/48 dp;
- `MotionScheme.expressive()` и spring-анимации состояний;
- адаптивная иконка, учитывающая тему (`values-night`), плюс monochrome-слой для themed icons;
- динамический цвет Material You за пользовательской настройкой.

## Roadmap

- [ ] Просмотр diff для pull request’ов
- [ ] Обзор релизов
- [ ] Подсветка синтаксиса кода
- [ ] Gists
- [ ] Управление настройками репозитория

## Скриншоты

> Скоро — приложение в активной разработке. APK для установки собирает CI выше.

## Лицензия

[MIT](LICENSE) © 2026 Oustery
