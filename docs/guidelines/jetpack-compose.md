# Coding Guidelines — Jetpack Compose

**Audience:** all contributors.
**Status:** Compose is the **agreed target** for new UI in medTimer, but it is **not yet in the codebase**.
This document defines the conventions so the first Compose PR — and the migration that follows — lands on a shared baseline.

For the general Kotlin/Android conventions Compose code must also obey, see [kotlin-android.md](kotlin-android.md); for testing, see [testing.md](testing.md).

## Adoption strategy — per-screen migration via `ComposeView`

medTimer today is built on Fragments + XML layouts + the Navigation component.
Compose lands incrementally:

- **New screens are pure Compose**, hosted in a Fragment via `ComposeView` (`setContent { … }`).
  The Navigation graph keeps routing; only the Fragment's view is Compose.
- **Existing screens are not rewritten** unless the change requires it (substantial feature work on that screen).
  Don't bundle "while we're here, port to Compose" into an unrelated PR.
- **Keep the Navigation component**, not `navigation-compose`, while both worlds coexist.
  Introducing `navigation-compose` would mean running two routing systems in parallel — defer that decision until most screens are Compose.

## Stack

When Compose lands, the stack will be:

- **Compose BOM** (latest stable) — pin every Compose artifact through the BOM in `gradle/libs.versions.toml`; never mix BOM-managed and hand-pinned Compose
  versions.
- **Material 3 Expressive** — the design system. Delivered via `androidx.compose.material3:material3` (the Expressive components and `MaterialExpressiveTheme`
  ship in the same artifact).
  `MedTimerTheme` wraps `MaterialExpressiveTheme`, not the classic `MaterialTheme`.
  Do not pull in Material 2 alongside it.
- **Hilt + Compose** (`androidx.hilt:hilt-navigation-compose`) — obtain ViewModels via `hiltViewModel()`.
- **Lifecycle Compose** (`androidx.lifecycle:lifecycle-runtime-compose`) — for `collectAsStateWithLifecycle()`. Reserved for the few `StateFlow` / `Flow`
  sources you still consume from a composable (see [State holders](#state-holders)); the default state-holder pattern doesn't need it.
- **kotlinx-collections-immutable** (`org.jetbrains.kotlinx:kotlinx-collections-immutable`) — required. State-holder list properties are `ImmutableList` /
  `PersistentList`, not `List`, so Compose treats them as stable and can skip recomposition.
- **Compose Compiler** — Kotlin Compose Compiler plugin (Kotlin 2.x), enabled per module. Strong-skipping mode is on by default.

## Module placement

Compose theme and reusable composables live in **`:core:ui`** — the same module that already holds shared XML resources.

- `MedTimerTheme` (Material 3 Expressive colors, typography, shapes, and motion), reusable design-system composables (buttons, cards, dialogs, etc.), and
  `@Preview`-friendly sample data go in `:core:ui`.
- Feature-specific composables (screens, screen-specific components) stay in the relevant `:feature:*` module.
- Do not create `:core:designsystem` as a separate module until `:core:ui` is genuinely overloaded; one shared UI module is enough at the current size.

## Composable conventions

Follow
Compose's [API guidelines for Jetpack Compose-based libraries](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md)
and the [Compose architecture guide](https://developer.android.com/develop/ui/compose/architecture).

- **Composable functions are `PascalCase` and return `Unit`.**
  A function returning a value should not be `@Composable`.
- **Function names describe what the UI is**, not what it does: `MedicineCard`, not `ShowMedicine`.
- **Modifier is the first optional parameter**, defaults to `Modifier`, and is passed through.
  Modifier order is significant — don't reorder modifiers without understanding the effect.
- **No side effects in the composable body.**
  Don't call suspend functions, launch coroutines, register observers, or mutate external state directly from the body — use the Effect APIs (next section).
- **Hoist state.**
  Leaf composables are stateless: they take state in via parameters and report events via lambdas.
  Stateful wrappers live one level up.

## Screen pattern — stateful `Screen` + stateless `ScreenContent`

Every screen is split in two:

```kotlin
// Stateful: pulls state from the ViewModel, exposes events as lambdas.
@Composable
fun OverviewScreen(
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = hiltViewModel(),
) {
    OverviewScreenContent(
        state = viewModel.state,
        onMarkTaken = viewModel::onMarkTaken,
        onSnooze = viewModel::onSnooze,
        modifier = modifier,
    )
}

// Stateless: takes the rendered state + lambdas. Pure function of its inputs.
@Composable
fun OverviewScreenContent(
    state: OverviewScreenState,
    onMarkTaken: (ReminderId) -> Unit,
    onSnooze: (ReminderId) -> Unit,
    modifier: Modifier = Modifier,
) { /* … */
}
```

- **`@Preview` and screenshot tests target the stateless `*Content` composable** — that is the unit you can render with fabricated state (construct a
  `MutableOverviewScreenState` and pass it as `OverviewScreenState`).
- **The stateful `Screen` composable is the only place `hiltViewModel()` is called.**
  Don't reach for a ViewModel from a leaf composable.
- The stateless half should not import the ViewModel type.
- `state` is the screen's state-holder interface ([next section](#state-holders)); the composable reads its properties directly. No
  `collectAsStateWithLifecycle()` is needed — Compose snapshot state is already reactive.

## State holders

medTimer's default state pattern for Compose screens is a **state-holder pair**: an immutable interface that the UI sees, and a mutable implementation that the
ViewModel writes to.
This replaces the more common `StateFlow<UiState>` + `collectAsStateWithLifecycle()` pattern as the default; see [When to use
`StateFlow` instead](#when-to-use-stateflow-instead) below for the exceptions.

### The pattern

```kotlin
// 1. Read-only interface — the UI's contract.
interface OverviewScreenState {
    val isInSelectionMode: Boolean
    val isLoading: Boolean
    val reminders: ImmutableList<ReminderItem>

    // Derived values are part of the contract.
    val selectedReminders: ImmutableList<ReminderItem>
}

// 2. Mutable implementation — owned by the ViewModel.
class MutableOverviewScreenState : OverviewScreenState {
    override var isInSelectionMode by mutableStateOf(false)
    override var isLoading by mutableStateOf(false)
    override var reminders by mutableStateOf(persistentListOf<ReminderItem>())

    override val selectedReminders by derivedStateOf {
        reminders.filter { it.isSelected }.toImmutableList()
    }

    // Internal state (e.g. debounce buffers) lives here but stays off the interface.
    var debouncedSearchText by mutableStateOf("")
}

// 3. ViewModel exposes the interface; keeps the mutable instance private.
@HiltViewModel
class OverviewViewModel @Inject constructor(
    remindersRepository: RemindersRepository,
) : ViewModel() {

    private val _state = MutableOverviewScreenState()
    val state: OverviewScreenState get() = _state

    init {
        remindersRepository.observeAll()
            .onEach { _state.reminders = it.toPersistentList() }
            .launchIn(viewModelScope)
    }

    fun onMarkTaken(id: ReminderId) { /* updates _state */
    }
    fun onSnooze(id: ReminderId) { /* updates _state */
    }
}
```

### Rules

- **Name the pair `XxxScreenState` (interface) + `MutableXxxScreenState` (class).**
  One pair per screen, colocated with the `XxxScreen` composable and its `ViewModel`.
- **Every interface property has a `val`-shaped contract** even when the implementation is `var` — the UI must not be able to mutate state.
- **List/collection properties are `ImmutableList` / `PersistentList`** (from `kotlinx.collections.immutable`), not `List`.
  This is what makes the state holder a stable parameter; without it, Compose can't skip recomposition.
- **Derived values use `by derivedStateOf { … }`** in the mutable class and surface as `val` on the interface.
  This is the analog of a `combine`d `StateFlow` — only recomputes when its inputs change, and only triggers recomposition for readers when the derived value
  actually changes.
- **Mutate from the ViewModel only.**
  Composables read `state.*`; they never write.
  All writes go through ViewModel methods (`onMarkTaken`, `enterSelectionMode`, …) that touch `_state.*`.
- **Internal-only fields stay off the interface.**
  Debounce buffers, scratch state, and other implementation details live on the `Mutable…ScreenState` class but are not in the interface — the UI never sees
  them.
- **Compose nested state holders.**
  When a sub-feature has its own state surface (a filter bar, a multi-select tracker), give it its own `XxxState` / `MutableXxxState` pair and expose the
  interface as a property on the parent state.
  Hierarchical state holders keep each layer testable on its own.
- **The ViewModel does not expose a `StateFlow` of the screen state.**
  Repository `Flow`s are collected inside the ViewModel and written into `_state.*`; the screen reads `state.*` directly.

### Why this pattern (vs. `StateFlow<UiState>`)

- Granular recomposition: only the composables that read a specific property recompose when it changes, instead of every reader of a `UiState` data class.
- No allocation of a new immutable `UiState` on every change — snapshot state mutates in place under Compose's transactional model.
- `derivedStateOf` integrates with the snapshot system, so derived properties don't trigger spurious recompositions when their inputs change but the derived
  value doesn't.
- Hierarchical composition: nested state holders fall out naturally; the equivalent with `StateFlow<UiState>` quickly becomes a `combine` pyramid.

### When to use `StateFlow` instead

Stick with `StateFlow<T>` (and `collectAsStateWithLifecycle()` at the call site) when:

- The state needs **non-Compose consumers** — a Fragment view, a Worker, a widget update path, a JVM test that doesn't run on the Compose runtime.
- The state is **shared across screens** and lives on a non-`ViewModel` scope (e.g. an application-scoped repository state).
- You need `Flow` operators (`debounce`, `combine`, `distinctUntilChanged`) on the publicly exposed shape itself, not just internally.

In all of these, the screen-local UI state can still use the state-holder pattern; the `StateFlow` lives upstream and is collected inside the ViewModel into
`_state.*`.

### Local UI state

State that lives entirely within one composable — and would never be of interest to the ViewModel — stays in the composable:

- Use `remember { mutableStateOf(…) }` for transient local state (e.g. whether a dropdown is expanded).
- Use `rememberSaveable { mutableStateOf(…) }` for local state that **must** survive configuration change or process death (text-field draft, selected tab).

If two composables need to coordinate, hoist the state up; if the screen needs it, hoist it into the screen's state holder.

## State hoisting and UDF

Follow the [state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting)
and [unidirectional data flow](https://developer.android.com/develop/ui/compose/architecture#udf) pattern.

- State flows **down** as the state-holder interface (or as individual parameters for leaf composables); events flow **up** as lambdas.
- The ViewModel owns the `Mutable…ScreenState` instance and is the only writer.
- The screen-level composable consumes `viewModel.state` directly.
- Leaf composables take the smallest slice they need — a `ReminderItem`, not the whole screen state — so their recomposition scope stays tight.

## Side effects — use them sparingly

Composables must be side-effect free.
When a composable genuinely needs to interact with the outside world, reach for the right Effect API — and only then.

### Use an Effect to…

Synchronize with something outside React-style state — and only then:

- **`LaunchedEffect(key1, …)`** — run a suspend block tied to the composable's lifecycle.
  Restarts when keys change; cancels on exit.
- **`DisposableEffect(key1, …)`** — set up something with explicit cleanup (a listener, an observer).
  Must `onDispose { … }`.
- **`rememberCoroutineScope()`** — launch coroutines **from event handlers** (a click, a swipe), not from the composable body.
- **`SideEffect { … }`** — push Compose state to non-Compose code (analytics, third-party SDK) after a successful composition.
- **`produceState(initialValue, key1, …)`** — convert a non-Compose async source (e.g. a callback API) into a `State<T>`.
- **`snapshotFlow { … }`** — convert Compose `State` reads into a cold `Flow` for use inside a `LaunchedEffect`.

### Don't use an Effect to…

- **Collect a Flow from a ViewModel for UI state.**
  Use `collectAsStateWithLifecycle()`.
  A `LaunchedEffect { flow.collect { … } }` for UI state reintroduces the lifecycle bugs `collectAsStateWithLifecycle` solves.
- **Derive data for rendering.**
  Compute it during composition; the Compose Compiler will skip unchanged work.
  Use `derivedStateOf` only when the derived value updates less frequently than its inputs *and* you can prove it earns its keep.
- **Respond to a user event.**
  Put the logic in the event handler (`onClick`, `onValueChange`), where intent is known.
- **Reset state when a prop changes.**
  Pass a different `key` to the composable to reset its subtree, or compute the value during composition.
- **Notify the parent of a change.**
  Call the parent's lambda from the event handler that caused the change, not from an Effect that watches the value.

### When you do use an Effect

- **Always clean up.**
  `DisposableEffect` requires `onDispose`; `LaunchedEffect`'s coroutine is cancelled for you on exit but any subscriptions you opened inside it must be torn
  down before you return.
- **Include every reactive value as a key**, or wrap a callback in `rememberUpdatedState` so the Effect captures the latest value without restarting.
- **One Effect, one concern.**

## Performance

Follow [Compose performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices).

- **Provide stable keys for `LazyColumn` / `LazyRow` items.**
  `items(reminders, key = { it.id }) { … }` lets Compose skip recomposition for items that just moved.
- **Pass minimal, immutable parameters.**
  `Header(title: String, subtitle: String)` over `Header(news: News)` — the smaller the input set, the smaller the recomposition scope.
- **Mark stable data with `@Immutable` / `@Stable`** when the compiler can't infer it (custom classes that wrap collections, third-party types).
  Don't sprinkle these annotations; use them where the compiler would otherwise mark the type unstable.
- **Defer state reads.**
  Lambda-based modifiers (`Modifier.offset { … }`, `Modifier.drawBehind { … }`) read state in the layout/draw phase, skipping composition.
- **Don't `remember` what your ViewModel already cached.**
  Calculations belong in the ViewModel; `remember` is for per-composition memoization.

## Material 3 Expressive theming and previews

- **One theme: `MedTimerTheme`** in `:core:ui`, wrapping `MaterialExpressiveTheme`.
  Define colors via Material 3's `ColorScheme`, with light and dark schemes.
  Support dynamic colors (`dynamicLightColorScheme` / `dynamicDarkColorScheme`) on Android 12+ when the user enables them, with a fall-back to the static brand
  colors.
- **Prefer Expressive components** (`ButtonGroup`, expressive `FloatingActionButton` variants, the Expressive motion scheme) over their classic Material 3
  counterparts when both exist — that is the whole reason for choosing the Expressive theme.
- **Type, shape, and motion** come from `Typography`, `Shapes`, and the Expressive `MotionScheme` in the theme — don't hard-code `TextStyle`, corner radius, or
  animation specs per screen.
- **Every screen-level composable has a `@Preview`** for light **and** dark.
  Use a `@MedTimerPreview` multipreview annotation (`@Preview` + dark variant + minimum supported font scale) defined in `:core:ui`.
- Previews **must render without a real ViewModel** — that is the whole point of the stateless `*Content` split.

## Testing — pointer

See [testing.md](testing.md) for the full Compose testing section.
In brief:

- The stateless `*Content` composable is the unit under test.
- Use `createComposeRule()` from `ui-test-junit4`; Robolectric makes the test runnable on the JVM (no emulator).
- Query by semantics first (`onNodeWithText`, `onNodeWithContentDescription`), then by `testTag` only as a last resort.
- Don't assert on recomposition counts or memoization — those are compiler details.

## Sources

- Android
  Developers — [Thinking in Compose](https://developer.android.com/develop/ui/compose/mental-model), [Compose architecture](https://developer.android.com/develop/ui/compose/architecture), [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state), [State hoisting](https://developer.android.com/develop/ui/compose/state-hoisting), [Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects), [Compose performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices), [Compose lifecycle](https://developer.android.com/develop/ui/compose/lifecycle) (
  2025–2026).
- [Compose API guidelines for Jetpack Compose-based libraries](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md) —
  AndroidX.
- [Material 3 for Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
  and [Material 3 Expressive](https://developer.android.com/develop/ui/compose/designsystems/material3/expressive).
- [Now in Android](https://github.com/android/nowinandroid) — reference patterns for Hilt + Compose + multi-module.

_Last reviewed: 2026-05-26 · Compose: target standard, not yet adopted._
