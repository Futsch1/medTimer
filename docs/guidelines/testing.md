# Testing Guidelines

**Audience:** all contributors.
**Stack:** JUnit 4, Mockito (+ `mockito-kotlin`), Robolectric for JVM unit tests; Espresso + Barista + UIAutomator for instrumented tests; JaCoCo for coverage.
Compose tests will use `createComposeRule()` from `androidx.compose.ui:ui-test-junit4`.

For commands, see [`AGENTS.md`](../../AGENTS.md); for what makes code testable in the first place, see [kotlin-android.md](kotlin-android.md).

## Test pyramid — prefer JVM unit tests

Order of preference:

1. **JVM unit tests** (`app/src/test/`, `*/src/test/`).
   Fast, deterministic, no emulator, run on every push.
   This is where most new tests should land.
2. **JVM unit tests with Robolectric** when an Android framework class is genuinely needed (Context, Resources, Looper).
   Still fast, still no emulator.
3. **Instrumented tests** (`app/src/androidTest/`) as a last resort.
   They're slow, flaky, and only run in CI (`compatibilityTest.yml` on API 28 and 36, plus `JacocoDebugCodeCoverage` in `test.yml`).
   **Do not run instrumented tests locally** unless explicitly investigating a CI failure — they require an emulator and take significantly longer than JVM
   tests.

## Test-driven where possible

A test-first approach is preferred for both bug fixes and new features:

- **Bug fixes:** write a failing test that reproduces the bug before the fix.
  This proves the fix works and prevents regression.
- **Features:** write tests alongside the code, not after.
  If a feature ships without tests, capture *why* in the PR (e.g. "UI-only change, manually verified on emulator").
- Pure refactors and translation-only PRs are exempt — the existing tests are what protect the refactor.

## Test behavior, not implementation (black box)

Test the **output for a given input**, through the unit's public surface — not how it computes the result.
Treat the unit (a ViewModel, a repository, a use-case helper) as a black box: arrange inputs and configured collaborators, act through the public method, and
assert on the returned value and observable side effects.

- **Verify outcomes, not interactions.**
  Prefer asserting on the resulting `StateFlow` value or returned object over "method X was called".
  Reserve `Mockito.verify(...)` for genuine side-effect boundaries where the call *is* the behavior — e.g. that a notification was posted, an alarm scheduled,
  or a row written via the repository.
- This keeps tests resilient: a refactor that preserves behavior should not break a single test.
  If a rename or restructuring breaks many tests, they were coupled to the implementation.
- **Don't reach into internals.**
  Avoid reflection and `@VisibleForTesting` access to private state.
  Make the seam testable instead — extract the logic into a class with a public method, or hoist the collaborator behind an interface that the test can stub.

## What to prioritize

Test where the risk and logic are:

- **Reminder scheduling and notification flow** — the heart of the app. Time math, recurrence, snooze, cyclic reminders.
- **Domain rules in `:core:domain`** — pure Kotlin, easy to test, high payoff.
- **Repositories and mappers in `:core:database`** — entity↔model mapping, query correctness, migration helpers.
- **Exporters** (`exporters/`) — CSV and PDF generation, especially edge cases (empty data, large datasets, special characters).
- **ViewModels** — state transitions in response to events, error handling.

Don't write tests for:

- Trivial wiring (`@HiltViewModel` plumbing, manifest declarations).
- Logicless data classes (`data class` with `val`s only).
- Generated code.

## Structure and naming

- **Arrange / Act / Assert**, with the three sections visually separated by blank lines.
- Test functions use **backtick names** describing the behavior:

  ```kotlin
  @Test
  fun `should mark reminder as taken when user taps action`() { /* … */ }
  ```

  The form `should X when Y` makes failure messages readable in CI output.
- `@Test` for a single case; `@RunWith(Parameterized::class)` or table-driven loops for parameterized cases.
- **One assertion focus per test.**
  Multiple `assertEquals` lines that verify the same outcome are fine; tests that interleave Act / Assert for several unrelated outcomes should be split.
- Build complex inputs with **builders or factory functions** (`fakeMedicine(name = …)`) to keep the Arrange block readable.

## Coroutines and Flow in tests

- Inject dispatchers via the `@Dispatcher` qualifier (see [kotlin-android.md](kotlin-android.md)) so tests can replace them with `StandardTestDispatcher` or
  `UnconfinedTestDispatcher`.
- Wrap test bodies in `runTest { … }` from `kotlinx-coroutines-test` — it skips `delay()`, advances virtual time, and surfaces uncaught exceptions.
- Share one `TestCoroutineScheduler` across all test dispatchers in a single test so they run deterministically on one thread.
- For `StateFlow` / `SharedFlow`, prefer **collecting into a list** with `flow.toList(this)` inside `runTest` over polling `.value`.

## Mockito conventions

- Use `mockito-kotlin` (`mock()`, `whenever()`, `verify()`); avoid `Mockito.when(...)` — `when` is a Kotlin keyword.
- **Stub** collaborators to set up inputs.
- **Verify** only at real side-effect boundaries — see [Test behavior, not implementation](#test-behavior-not-implementation-black-box).
- Don't mock data classes or domain models; use real instances built with factories.

## Instrumented tests

Instrumented tests live in `app/src/androidTest/` and run on an emulator.
They exist for flows that genuinely exercise the Android UI runtime — navigation between Fragments, system-permission dialogs, real notifications.

Conventions:

- **Use [Barista](https://github.com/AdevintaSpain/Barista)** wrappers (`clickOn`, `assertDisplayed`, `writeTo`) over raw Espresso `onView(...).perform(...)` —
  Barista calls are shorter and survive Espresso's flakier corners.
- **UIAutomator** for cross-app interactions (notifications, settings screens) where Espresso can't reach.
- Name tests for the user-visible behavior, not the Fragment class name.
- Keep instrumented tests **independent** — each test sets up its own state via repository or intent, doesn't rely on test order.
- **Do not run locally by default.** CI runs them on the appropriate matrix (`compatibilityTest.yml`, `test.yml`).

## Compose tests

When Compose lands (see [jetpack-compose.md](jetpack-compose.md)), Compose tests live alongside JVM unit tests and use **`createComposeRule()`** with *
*Robolectric** so they run on the JVM without an emulator.

- **Target the stateless `*Content` composable**, not the stateful `Screen` that pulls in `hiltViewModel()`.
  Feed it fabricated state and lambdas; assert on what's visible.
- **Query by semantics first.**
  `onNodeWithText`, `onNodeWithContentDescription`, `onNodeWithTag` only as a last resort — and only when the tag has a meaningful name, not `tag_42`.
- **Don't assert on recomposition counts, memoization, or the number of times a lambda fires.**
  Those are Compose Compiler details that change between versions.
- **Use `runComposeUiTest { … }`** (from the same artifact) for tests that drive Compose state without a backing Fragment.

## Coverage

JaCoCo coverage is generated by `./gradlew jacocoFullDebugCodeCoverage` and uploaded to SonarCloud.
Treat the percentage as a **guideline, not a hard gate**:

- Raise coverage where it matters first (the priorities in [What to prioritize](#what-to-prioritize)).
- Don't chase a number by testing wiring or generated code.
- A PR that lowers coverage is acceptable when it deletes tested-but-unused code; flag it in the PR description so reviewers understand why the delta is
  negative.

## Fuzz testing

A subset of tests runs as fuzzers when invoked with `-Dfuzzing=true` (see `test.yml`):

```bash
./gradlew testFullDebug testFossDebug -Dfuzzing=true
```

Add a fuzz test when the unit under test has:

- A **parser, exporter, or formatter** with structured input (CSV, PDF, time strings, recurrence rules).
- **Boundary-sensitive arithmetic** — time math around DST, leap years, midnight rollover.
- **A backup/restore round-trip.**

The fuzz job runs in CI on every push; you don't need to invoke it locally for normal development.

## Sources

- Android
  Developers — [Test apps on Android](https://developer.android.com/training/testing), [Fundamentals of testing](https://developer.android.com/training/testing/fundamentals), [Test Compose layouts](https://developer.android.com/develop/ui/compose/testing) (
  2025–2026).
- [JUnit 4](https://junit.org/junit4/) · [Mockito](https://site.mockito.org/) · [mockito-kotlin](https://github.com/mockito/mockito-kotlin) · [Robolectric](http://robolectric.org/).
- [Barista (AdevintaSpain)](https://github.com/AdevintaSpain/Barista) — Espresso wrappers.
- [kotlinx.coroutines testing guide](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/) — `runTest`, `TestDispatcher`.

_Last reviewed: 2026-05-26._
