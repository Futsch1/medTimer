# medTimer — Agent Guide

Terse, machine-facing summary for AI coding assistants.
For the detailed conventions — coding style, architecture, testing, docs — read [`docs/guidelines/`](docs/guidelines/README.md).
AI-specific hygiene (boundaries, secrets, attribution) lives in this file under [AI hygiene](#ai-hygiene).
When this file and the guidelines disagree, the guidelines win; flag the drift in your PR.

## Project Overview

medTimer is an Android medication reminder app (Kotlin, `compileSdk = 36`, `minSdk = 28`, JDK 21).
Package: `com.futsch1.medtimer`.

## Build Variants

The app has two product flavors on the `distribution` dimension:

| Flavor | Purpose               | GMS dependency                 |
| ------ | --------------------- | ------------------------------ |
| `full` | Google Play Store     | Yes (`play-services-location`) |
| `foss` | FOSS app repositories | No                             |

Flavor-specific source sets:

- `app/src/full/` — GMS implementations (`GmsGeofenceRegistrar`, `GmsLocationProvider`, `GeofenceBroadcastReceiver`) and the flavor manifest (location
  permissions + broadcast receiver).
- `app/src/foss/` — No-op stubs (`NoOpGeofenceRegistrar`, `NoOpLocationProvider`).
- `app/src/testFull/` — Unit tests that import GMS classes (`GeofenceRegistrarTest`, `GeofenceBroadcastReceiverTest`).

See [`docs/guidelines/kotlin-android.md`](docs/guidelines/kotlin-android.md#flavor-split--full-vs-foss) for the discipline.

## Repository Layout

```
app/                  Application module
core/common/          Pure-Kotlin utilities
core/domain/          Domain models + repository interfaces (no Android deps)
core/database/        Room entities, DAOs, repository impls, entity↔model mappers
core/datastore/       DataStore preferences
core/ui/              Shared resources (strings, drawables, themes, navigation graphs); Compose theme will live here too
feature/reminders/    Reminder scheduling, notification processing
feature/ui/           Overview UI
app/schemas/          Room database migration schemas
```

Architectural rules and module dependency direction: [`docs/guidelines/kotlin-android.md`](docs/guidelines/kotlin-android.md#architecture--mvvm--hilt).
**`*Entity` types from `:core:database` must not appear in any other module's API** — depend on the clean models in `core/domain/model/`.

## Build Commands

```bash
# Standard verification — builds both flavors
./gradlew assembleDebug

# Build a specific flavor
./gradlew assembleFullDebug   # with GMS geofencing
./gradlew assembleFossDebug   # without GMS

# Run JVM unit tests (fast, no device needed)
./gradlew testFullDebugUnitTest   # full flavor (includes GMS-specific tests)
./gradlew testFossDebugUnitTest   # foss flavor (no-op stubs)

# Lint (enforced on CI; abortOnError = true, warningsAsErrors = true)
./gradlew lint

# Full coverage report (requires connected device/emulator — very slow)
./gradlew jacocoFullDebugCodeCoverage
```

**Instrumented tests:** when you add or change one, run *just that test* locally against an emulator before pushing
(e.g. `./gradlew :app:connectedFullDebugAndroidTest --tests <FQN>`). **Do not run the full `connectedAndroidTest` suite or `JacocoDebugCodeCoverage` locally**
— they need an emulator and take far too long. CI handles the broader matrix.
See [`docs/guidelines/testing.md`](docs/guidelines/testing.md) for testing conventions.

## CI / GitHub Actions

- **`build.yml`** — runs on every push and PR to `main`; runs `assembleRelease` + `bundleRelease` (builds both flavors); signs and uploads `aab-full`,
  `apk-full`, and `apk-foss` artifacts; creates GitHub releases (AAB + both APKs) on version tags.
- **`test.yml`** — runs on every push and PR to `main`; runs fuzzing unit tests on both flavors (`testFullDebug testFossDebug -Dfuzzing=true`), FOSS unit
  tests (`testFossDebugUnitTest`), instrumented tests on an API 36 emulator via `JacocoDebugCodeCoverage` (full flavor), and lint.
- **`compatibilityTest.yml`** — runs instrumented tests (`connectedFullDebugAndroidTest`) on API 28 and 36 emulators in a matrix.
- Other workflows: `monkeyTest`, `firebaseTest`, `codeql`, `scorecard`, `storeListing`.

The build must pass `assembleDebug` and `lint` before merging.

### Fix pipeline issues

To fix pipeline issues, use the `gh` command line tool to access GitHub Actions logs.

## Conventions — quick reference

Full detail is in [`docs/guidelines/`](docs/guidelines/README.md). The one-liners:

- **Code style:** Kotlin only (no new Java); Android Lint and SonarQube are enforced; Kotlin official style, 160-char line length. → [
  `kotlin-android.md`](docs/guidelines/kotlin-android.md#enforcement-source-of-truth)
- **Architecture:** MVVM + Hilt; `Flow` / `StateFlow` / `SharedFlow` (no LiveData); Navigation component with Safe Args. → [
  `kotlin-android.md`](docs/guidelines/kotlin-android.md#architecture--mvvm--hilt)
- **Compose:** target standard for new UI, not yet adopted; per-screen migration via `ComposeView`. → [`jetpack-compose.md`](docs/guidelines/jetpack-compose.md)
- **Testing:** prefer JVM unit tests over instrumented; test-driven for bug fixes and features. → [`testing.md`](docs/guidelines/testing.md)
- **Translations:** consider all locales when changing strings; escape `'` as `\'`; use `\n` for newline. → [
  `kotlin-android.md`](docs/guidelines/kotlin-android.md#translations)

## AI hygiene

**Never:**

- Commit secrets — keystores (`*.jks`), `keystore.properties`, Play / Firebase service-account JSONs, `fastlane/*.json` upload credentials, `local.properties`.
  Only `*.example` templates belong in git; CI signs releases via GitHub Actions secrets.
- Externalize real user medication data — no prompts, no external services, no log statements, no `print stored reminders to debug` shortcuts.
  Use synthetic data ("Vitamin X 500 mg", "Medicine A") in prompts, tests, examples, and bug reproductions.
- Commit, fixture, or attach to bug reports any exported CSV/PDF artifacts or `adb backup` output — they contain user medication data.
- `@Suppress` or weaken an Android Lint / SonarQube finding to make code "pass" — fix the underlying issue.
  If a suppression is genuinely needed, comment *why* and reference the rule.
- Add a dependency the agent "remembered" without verifying group/artifact, that it's maintained, and that it's pinned via `gradle/libs.versions.toml`.

**Ask first** (get a human decision before proceeding):

- Room schema changes or new migrations — `app/schemas/` is user-visible and effectively irreversible once shipped.
- Dependency / AGP / Kotlin / KSP / Hilt upgrades, especially across majors.

**Commits** (soft conventions for AI-assisted work; deviate freely for quick personal iterations):

- Prefer `#<issue-number> <short description>` for issue-linked work; sentence-case otherwise.
  Match the surrounding branch history.
- Branches typically follow `<issue-number>-<kebab-description>` when tied to an issue.
- For **substantial** AI contributions (a full feature, function, or the bulk of a change), add a `Co-Authored-By: Claude <noreply@anthropic.com>` trailer.
  Trivial completions don't need attribution; the human is always the primary author.
