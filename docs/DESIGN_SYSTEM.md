# GitYou Design System

Implementation-ready reference for the Material 3 Expressive design system used across GitYou. The single source of truth is `app/src/main/java/com/gityou/app/ui/theme/`.

Expressive intensity: **balanced** — expressive hero areas and key actions, quiet utility surfaces.

## 1. Color

Brand idea: *indigo* primary (calm, engineering-tool confidence) with a *coral* tertiary accent reserved for celebratory/contrast moments (starred state, unread markers). Light and dark schemes are designed independently — never inverted.

| Role | Light | Dark |
|---|---|---|
| `primary` / `onPrimary` | `#4648BE` / white | `#BDC3FF` / `#1B2078` |
| `primaryContainer` / `onPrimaryContainer` | `#DEE0FF` / `#00105C` | `#333AA6` / `#EEF0FF` |
| `secondary` family | muted slate-indigo | muted slate-indigo |
| `tertiary` / `onTertiary` (coral) | `#B54630` / white | `#FFB4A5` / `#5F170C` |
| `tertiaryContainer` | `#FFDAD3` | `#7D2C1C` |
| `surface` | `#FBF8FE` | `#131318` |
| `surfaceContainer*` | cool grays, low→highest | dark grays, low→highest |
| `outline` / `outlineVariant` | `#767680` / `#C7C5D0` | `#91909A` / `#46464F` |
| `error` family | M3 red | M3 red |

Rules enforced in code:

- selection/success/failure never rely on color alone (icon + label always change too);
- `error` is used only for real errors — coral is a brand accent, not an error color;
- Material You dynamic color is available on Android 12+ behind a user setting; the branded scheme above is the default everywhere.

## 2. Typography

Editorial hierarchy from the system sans-serif: weight and size carry the hierarchy, body text stays at comfortable reading sizes, display copy is short.

| Token | Weight | Use |
|---|---|---|
| `displaySmall` | SemiBold 36 | Device Flow user code, big single values |
| `headlineLarge/Small` | Bold/SemiBold | Screen heroes, issue titles |
| `titleLarge/Medium/Small` | SemiBold | App bar, card titles, list rows |
| `bodyLarge/Medium/Small` | Regular 16/14/12 | Reading text, metadata |
| `labelLarge/Medium/Small` | SemiBold 14/12/11 | Buttons, chips, uppercase eyebrows |

Eyebrow labels (`EyebrowLabel`) are uppercase `labelSmall` in the primary color and organize list sections.

## 3. Shape

One family: **12 / 20 / 32 / 48 dp** (`extraSmall` 8 dp for micro-chips).

- Cards, rows and sheet content: 20 dp (`medium`).
- Small tags and stat blocks: 12 dp.
- The morphing star button intentionally animates **20 → 48 dp** corners as a state explanation; everything else keeps static shapes.

## 4. Spacing

Scale in multiples of 4: `xs 4 / sm 8 / md 12 / lg 16 / xl 24 / xxl 32 / hero 48` (`ui/theme/Spacing.kt`). Hero sections get `hero`-scale air; dense lists stay at `lg`.

## 5. Motion

- Global: `MotionScheme.expressive()` on `MaterialExpressiveTheme`.
- Spatial changes (star morph): springs with `MediumBouncy` damping, `StiffnessLow` — interruptible by construction.
- Effects (color/alpha): short `tween`/default effects specs, no bouncing.
- Loading: wavy indicators only — `LoadingIndicator` for full-screen states, `LinearWavyProgressIndicator` for pagination and README.
- No infinite decorative motion near reading content (README, issue bodies).

## 6. Components

| Component | Notes |
|---|---|
| `StateScreen` + `UiState` | Every screen renders Loading / Error / Content consistently |
| `ErrorState` / `EmptyState` | Icon + message + retry; error keys map to localized strings |
| `MorphingStarButton` | 84 dp toggle, shape+color spring morph, full semantics (`role`, `stateDescription`) |
| `CircleActionButton` | Secondary actions (fork, open in browser) in the 84 dp language of the star |
| `RepoCard` | Surface container-low, avatar + name + 2-line description + language dot/stats |
| `IssueRow` / `PullRow` | State icon in tinted container; color is never the only signal |
| `GyBottomBar` / `GyNavigationRail` | Phone vs expanded layout; unread badge on the bell |
| `StatBlock` | Number-first stat tiles for repo overview and profile |

## 7. Icon

The launcher icon is a git branch that forks into a **Y** (for GitYou). It is fully adaptive:

- `mipmap-anydpi-v26/ic_launcher.xml` with a color background resource;
- `values/colors.xml` ↔ `values-night/colors.xml` recolor both the background and the foreground strokes for light/dark system themes;
- `ic_launcher_monochrome.xml` supplies the themed-icon layer;
- all geometry stays inside the 66 dp adaptive safe zone.

The in-app identity (auth screen, branded loading) reuses the same vector.
