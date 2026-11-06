# GitYou 1.1.0 — UI/UX improvement plan

This document records the second real-device review (November 2026), the problems it
surfaced, and how each problem was fixed in release 1.1.0. It complements
[DESIGN_SYSTEM.md](DESIGN_SYSTEM.md), which describes the design tokens themselves.

## How the plan was built

Ten real-device screenshots (GitHub client flows) were audited against the
Material 3 Expressive guidelines: hierarchy, coherence, restraint, craft,
accessibility. Four defects stood out, one of them critical.

## Findings and fixes

### 1. Markdown rendering — critical

**Finding.** Issue and pull-request bodies were printed as plain `Text()`.
Dependabot bodies (which are markdown wrapped in HTML: `<details>`,
`<summary>`, `<p><em>`, `<blockquote><h2>`, `<ul><li>`) leaked raw tags
straight into the reading surface. READMEs used bare `Markwon.create` — no
HTML support, no tables, no task lists, no images, default link styling.

**Fix.**

- A shared `MarkdownText` component backed by a Hilt-provided
  `MarkdownFactory`: Markwon with the **html**, **image** (OkHttp network
  scheme handler reusing the app client), **tables**, **task list**,
  **strikethrough** and **linkify** plugins.
- A `MarkdownPreprocessor` normalizes constructs Markwon cannot handle:
  `<details>` is unwrapped, `<summary>` becomes a bold line, `<br>` becomes a
  newline. Covered by unit tests.
- The Markwon theme follows the Compose color scheme on every rebuild: link
  color, inline-code and code-block backgrounds, table stripes, task-list
  checkmarks.
- Applied everywhere markdown can appear: repository README, issue/PR body,
  issue comments and the new issue editor.

### 2. Typography — expressive but flat

**Finding.** All text used the system sans-serif; display sizes carried no
extra personality, and the type scale relied on weight alone.

**Fix.** The app now ships the **Roboto Flex variable font** (a single ~1.7 MB
TTF) instantiated per weight through `FontVariation.Settings` — 400 for body,
600/700 for titles, 800 for display. Display and headline styles gain tight
negative letter-spacing for an editorial voice. The in-app **text scale**
setting (S/M/L/XL) multiplies the system font scale through `LocalDensity`
and is honored by Compose text and markdown alike.

### 3. Color — one scheme for every taste

**Finding.** A single indigo/coral palette; dark mode was comfortable but
notched-panel OLED screens showed gray blocks where pure black was possible.

**Fix.**

- Four hand-crafted accent palettes — **Indigo** (default), **Ocean**,
  **Forest**, **Sunrise** — each a full primary + tertiary tonal family for
  light and dark, sharing neutral/error roles so the app stays recognizable.
- A **true black (AMOLED)** variant that deepens the surface container scale
  in dark mode.
- Palette selection and AMOLED live in Settings → Appearance; dynamic color
  (Material You) still overrides the brand palette when enabled.

### 4. Motion and components — baseline instead of expressive

**Finding.** The app had wavy progress and a morphing star, but navigation
was instant, notifications had no interaction model beyond taps, and search
offered no control over results.

**Fix.**

- **Navigation transitions**: restrained spring slide + fade (no bounce),
  consistent across tabs and detail screens.
- **Notifications**: All/Unread **filter chips**, **swipe-to-dismiss** marks a
  card read (spring return, quieter read style afterwards), the trailing
  button marks read without opening.
- **Search**: sort by best match / stars / recent updates via filter chips,
  wired to the GitHub `sort` parameter with combined debounced flows.
- **Issue editor**: Write/**Preview** tabs with live markdown rendering.
- The **expressive animations** setting switches the global
  `MotionScheme.expressive()` to `MotionScheme.standard()` for users who
  prefer calmer interfaces.

### 5. Settings — previously three controls

**Fix.** Settings now covers:

| Group | Settings |
|---|---|
| Appearance | Theme mode, dynamic color, accent palette, AMOLED, text scale, expressive animations |
| Behavior | Start screen, in-app language (Android 13+, platform per-app locales), markdown rendering on/off |
| Account | Profile card, sign out |
| About | Version, design note, project link, open-source acknowledgements |

## Verification

- `MarkdownPreprocessorTest` (9 cases) covers details/summary/br normalization.
- `./gradlew testDebugUnitTest assembleDebug` green on JDK 21 before release.
- All third-party APIs (Markwon plugin signatures, `SwipeToDismissBox`,
  `MotionScheme.standard()`, variable-font `Font` overload) verified against
  the pinned dependency versions during implementation.

## Deliberately out of scope

- Full HTML-table → markdown-table conversion (pipe tables render; HTML
  tables degrade gracefully without leaking tags).
- Code syntax highlighting in markdown (listed in the README roadmap).
- Collapsible `<details>` sections (content renders inline instead).
