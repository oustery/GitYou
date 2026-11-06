# Contributing to GitYou

Thanks for your interest in improving GitYou! This document explains how to set up the project and open useful contributions.

## Development environment

- **Android Studio** Ladybug or newer (or any editor + JDK 21)
- **JDK**: 21 (CI uses Temurin 21)
- **Android SDK**: compileSdk 36, minSdk 26
- Clone the repository and open it in Android Studio; Gradle sync downloads everything else.

```bash
./gradlew assembleDebug      # build a debug APK per ABI
./gradlew testDebugUnitTest  # run the unit test suite
```

## Code style

- Kotlin official style (`.editorconfig` is included).
- No secrets in code: tokens belong to the device, signing keys belong to local/CI secrets.
- Strings go through `res/values/strings.xml` (English) and `res/values-ru/strings.xml` (Russian) — every user-facing string must exist in both files.
- Design system tokens (`ui/theme`) are the single source of truth for colors, shapes, typography and motion; avoid one-off styling in screens.

## Pull requests

1. Fork / create a feature branch from `main`.
2. Keep the change focused; separate refactors from features.
3. Make sure `testDebugUnitTest` and a debug build pass locally — CI runs both on every PR.
4. Describe the user-visible change in the PR description.

## Reporting bugs

Open an issue with:

- device/Android version and ABI;
- steps to reproduce;
- expected vs actual behavior.

For security issues (auth, token handling), please do **not** open a public issue — mention it privately to the maintainer instead.
