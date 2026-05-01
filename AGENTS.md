# medTimer — Agent Guide

## Project Overview

medTimer is an Android medication reminder app (Kotlin, minSdk 28, targetSdk 36).
Package: `com.futsch1.medtimer`

## Build Variants

The app has two product flavors on the `distribution` dimension:

| Flavor | Purpose               | GMS dependency                 |
|--------|-----------------------|--------------------------------|
| `full` | Google Play Store     | Yes (`play-services-location`) |
| `foss` | FOSS app repositories | No                             |

Flavor-specific source sets:

- `app/src/full/` — GMS implementations (`GmsGeofenceRegistrar`, `GmsLocationProvider`, `GeofenceBroadcastReceiver`) and the flavor manifest (location
  permissions + broadcast receiver)
- `app/src/foss/` — No-op stubs (`NoOpGeofenceRegistrar`, `NoOpLocationProvider`)
- `app/src/testFull/` — Unit tests that import GMS classes (`GeofenceRegistrarTest`, `GeofenceBroadcastReceiverTest`)

## Repository Layout

```
app/src/main/java/com/futsch1/medtimer/
  database/          Room entities, DAOs, repository, mapper functions
  model/             Clean domain model classes (in-progress refactor)
    reminderevent/   Sealed ReminderEvent hierarchy + subclasses
  overview/          Overview fragment, ViewModel, actions, model wrappers
  reminders/         Scheduling, notification processing, alarm management
  medicine/          Medicine editing UI, stock management
  statistics/        Statistics fragment and calendar view
  remindertable/     Sortable/filterable reminder table
  exporters/         CSV and PDF export
  helpers/           Utility classes (time formatting, string formatting)
  preferences/       Persistent preferences and data sources
  widgets/           Home-screen widgets
  location/          GeofenceRegistrar and LocationProvider interfaces
  di/                Hilt dependency injection modules (GsonModule + flavor LocationModules)

app/src/full/        Full-flavor sources (GMS geofencing implementations)
app/src/foss/        FOSS-flavor sources (no-op geofencing stubs)
app/src/test/        JVM unit tests shared across flavors (Robolectric, Mockito, JUnit 4)
app/src/testFull/    JVM unit tests specific to the full flavor (require GMS classes)
app/src/androidTest/ Instrumented UI tests (Espresso, Barista, UIAutomator)
app/schemas/         Room database migration schemas
```

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
./gradlew JacocoDebugCodeCoverage
```

**Do not run `connectedAndroidTest` or `JacocoDebugCodeCoverage` locally, only on explicit request** — instrumented tests require an emulator and take a very
long time. CI handles them.

## CI / GitHub Actions

- **`build.yml`** — runs on every push and PR to `main`; runs `assembleRelease` + `bundleRelease` (builds both flavors); signs and uploads `aab-full`,
  `apk-full`, and `apk-foss` artifacts; creates GitHub releases (AAB + both APKs) on version tags.
- **`test.yml`** — runs on every push and PR to `main`; runs fuzzing unit tests on both flavors (`testFullDebug testFossDebug -Dfuzzing=true`), FOSS unit
  tests (`testFossDebugUnitTest`), instrumented tests on an API 36 emulator via `JacocoDebugCodeCoverage` (full flavor), and lint.
- **`compatibilityTest.yml`** — runs instrumented tests (`connectedFullDebugAndroidTest`) on API 28 and 36 emulators in a matrix.
- Other workflows: `monkeyTest`, `firebaseTest`, `codeql`, `scorecard`, `storeListing`.

The build must pass `assembleDebug` and `lint` before merging.

## Architecture

- **Database layer**: Room (`MedicineDao`, `MedicineRepository`). Entity classes are suffixed `Entity` (e.g. `ReminderEventEntity`, `MedicineEntity`).
- **Domain model layer** (`model/`): Clean Kotlin data/sealed classes, no Room annotations.
- **DI**: Hilt throughout. ViewModels use `@HiltViewModel`. Singletons use `@Singleton`.
- **Reactive data**: Kotlin `Flow` / `StateFlow` / `SharedFlow`. No LiveData.
- **Navigation**: Jetpack Navigation component with Safe Args.

## Code Style

- Kotlin only; no new Java files
- Lint is strict: fix warnings, do not suppress without justification
- Uses SonarQube as additional static code analysis
- Standard Kotlin Code Style with line length set to 160 characters

## Translations

- When adding or changing strings in one of the language files, consider the translation to all other languages the app supports (`localeFilters` in
  `app/build.gradle.kts`)
- Escape `'` with a single `\`
- Use `\n` for newline

## Testing

- Prefer unit tests to Android tests, since the latter are often flaky and take a long time to run
- Tests are important for this app, if implementing a feature or a bugfix, a test driven approach is preferred
