# GitHub Copilot Instructions — medTimer

medTimer is an Android medication reminder app (Kotlin, `compileSdk = 36`, `minSdk = 28`, JDK 21, multi-module Gradle, two product flavors: `full` with GMS and
`foss` without).

## Where to read first

- **[`AGENTS.md`](../AGENTS.md)** — terse machine-facing summary: build variants, repository layout, commands, CI workflows.
- **[`docs/guidelines/`](../docs/guidelines/README.md)** — the canonical, detailed standards. Read the relevant guide before generating code for that area:
    - [`kotlin-android.md`](../docs/guidelines/kotlin-android.md) — Kotlin + Android conventions: MVVM + Hilt, Coroutines/Flow, multi-module, flavor split,
      Room, translations.
    - [`jetpack-compose.md`](../docs/guidelines/jetpack-compose.md) — Compose conventions (target standard; Compose not yet adopted).
    - [`testing.md`](../docs/guidelines/testing.md) — JVM unit, instrumented, fuzz, and Compose testing.
    - [`documentation-guidelines.md`](../docs/guidelines/documentation-guidelines.md) — docs-as-code, Diátaxis, style.

  AI-specific hygiene (boundaries, secrets, attribution) lives in [`AGENTS.md` → AI hygiene](../AGENTS.md#ai-hygiene).

When this file and the guidelines disagree, the guidelines win.

## Hard rules to bake into every suggestion

- **Kotlin only.** No new Java files.
- **Android Lint is strict** (`abortOnError = true`, `warningsAsErrors = true`) — don't suggest code that introduces warnings; don't add suppressions without
  justification.
- **`*Entity` types from `:core:database` never leave that module.** Outside `:core:database`, depend on the clean models in `core/domain/model/` (e.g.
  `Medicine`, not `MedicineEntity`).
- **Inject `CoroutineDispatcher` via the `@Dispatcher(MedTimerDispatchers.X)` qualifier.** Never reference `Dispatchers.IO`/`Default` directly.
- **No `LiveData` in new code.** Use `StateFlow` / `SharedFlow`.
- **Code must compile for both flavors** — don't import GMS classes from `app/src/main/`; introduce an interface and a per-flavor binding.
- **Translations:** when changing `values/strings.xml`, account for the other locales; escape `'` as `\'`; use `\n` for newline.

## Ask first

Pause and ask the human before:

- Room schema changes or new migrations (`app/schemas/`).
- Dependency / AGP / Kotlin / KSP / Hilt upgrades.

## Build gates (run before suggesting "done")

```bash
./gradlew assembleDebug                            # builds both flavors
./gradlew testFullDebugUnitTest testFossDebugUnitTest
./gradlew lint
```

When you add or change an instrumented test, run that single test locally against an emulator
(`./gradlew :app:connectedFullDebugAndroidTest --tests <FQN>`). Do not run the full `connectedAndroidTest` suite locally — CI handles the broader matrix.

## Commits and attribution

- Commit messages: `#<issue-number> <short description>` for issue-linked work; sentence-case otherwise.
- Branches: `<issue-number>-<kebab-description>`.
- Substantial AI contributions add a `Co-Authored-By: Claude <noreply@anthropic.com>` trailer; trivial completions don't need attribution.
