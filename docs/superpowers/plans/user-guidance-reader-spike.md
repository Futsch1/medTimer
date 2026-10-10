# User guidance reader spike

**Status:** Complete. The reader, source layout, destinations, link policy, build process, and targeted release-like test strategy are selected and verified; related intro/warning tests remain implementation work for their dependent tasks.
**Scope:** [Task 2](user-guidance-tasks.md#task-2--complete-the-bounded-reader-spike), under the [agreed plan](user-guidance.md).
**Time box:** One focused working session, capped at four engineering hours. Stop before feature completeness; task 3 owns the finished first slice.

## Decision

Use **build-time Markdown-to-static-HTML conversion**, shown in Android `WebView` from bundled local assets. Keep English Markdown as the only content source. Do not add a second production Markdown-rendering path.

| Option | Benefits | Costs / decision |
| --- | --- | --- |
| Lightweight Android Markdown reader (for example, Markwon) | Native text rendering; simple links and font scaling. | Adds a runtime Markdown renderer and image integration. Maven Central reports Markwon 4.6.2 as latest, last updated 2021-02-08; rejected because maintenance status is not adequate for a new dependency. |
| Build-time HTML + local WebView | Markdown stays portable; output is static and can be reviewed/package-checked; WebView provides semantic headings, links, image alt text, and scalable text without app-side Markdown parsing. | Adds a small Gradle generator and WebView reader, plus a build-time parser dependency. The generated HTML is disposable and never edited by hand. Selected. |

The generator uses CommonMark Java **0.30.0** as a buildSrc-only dependency, pinned in `gradle/libs.versions.toml` (Maven metadata last updated 2026-08-06). AndroidX WebKit **1.17.1** is pinned for `WebViewAssetLoader`, which serves files on the reserved `appassets.androidplatform.net` origin. Neither the app nor reader downloads guide content. No toolchain upgrade was needed.

## Source and destinations

- Canonical source: `docs/help/README.md` (Help contents), `docs/help/<topic-id>.md`, and `docs/help/images/<descriptive-name>.<ext>`.
- Current topic ID: `getting-started`; generated destination: `getting-started.html`. Contents is `index.html` (generated from `README.md`). Planned future IDs are `notifications-missing-or-late` and `more-tasks`; add each only with its Markdown source.
- Internal Markdown links use relative `.md` paths. The generator changes these destinations to `.html`, including `README.md` → `index.html`; image paths remain relative.
- The generated page uses semantic HTML, responsive local images, image alt text, visible keyboard focus, and system color scheme. `HelpReaderDialogFragment` applies Android's configured font scale to WebView text zoom.
- `WebViewAssetLoader` serves local assets only. JavaScript, file/content access, and network loads are disabled. Clicked HTTP(S) links open in the external browser; non-HTTP(S) navigation is blocked. Internal topic links stay in the same WebView; Android Back follows its history and exits Help at the contents root.
- The contents page says the guide is English-only. The app's Help menu label uses the existing supported UI locales; Markdown declares `lang="en"` in generated HTML.

## Build and read

The app registers a generated asset directory for each Android variant. Editing Markdown or an image and building the app regenerates HTML deterministically; contributors edit only `docs/help/`, never `app/build/generated/`.

```bash
./gradlew :app:generateFullDebugHelpGuideAssets
./gradlew :app:assembleFullRelease :app:assembleFossRelease
```

The current test demonstration was opened from the main overflow **Help** item; the app has no `INTERNET` permission and the reader blocks WebView network loads. The demo image is synthetic **Medicine A** app UI; Task 4 must replace/recapture guide screenshots through the existing English Screengrab workflow and record its focused command and provenance.

## Spike evidence

- Contents → Getting started internal navigation rendered from generated HTML; the generated getting-started page linked back to contents. The focused `HelpReaderTest` exercises both links and Android Back. TalkBack was enabled for a structural review: the WebView exposes headings/text, the link name, the image's alt text, and a named toolbar Back control.
- At Android font scale 1.5, text enlarged and remained scrollable. The reader opened the local image while the app has no `INTERNET` permission and WebView network loads are disabled, demonstrating offline asset rendering.
- `./gradlew assembleDebug lint :app:assembleFullRelease :app:assembleFossRelease --console=plain` — **BUILD SUCCESSFUL**. Inspected both unsigned release APKs; each contains `assets/index.html`, `assets/getting-started.html`, and `assets/images/medicine-reminder.png`.
- Focused UI test `./gradlew :app:connectedFullDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.futsch1.medtimer.HelpReaderTest -Pandroid.testInstrumentationRunnerArguments.method=helpGuideNavigatesAndReturnsWithBack --console=plain` — **BUILD SUCCESSFUL** (1 instrumented test; rerun against the final WebView client).
- Generated-link spot check confirmed `getting-started.html` from contents, `index.html` from the topic, and the relative image asset path.
- Markwon metadata checked at Maven Central; AndroidX WebKit and CommonMark artifacts/versions verified from repository metadata. No dependency update or toolchain change. The narrowly annotated `MissingOnRenderProcessGone` suppression is for a WebKit 1.17.1 lint detector false-positive: the Kotlin client implements the Android callback (confirmed in compiled bytecode), but the detector does not recognize the override.

## Targeted release-like verification strategy

Keep the production debug suppression and warning logic unchanged. For focused first-launch/warning UI tests, expose the existing build-mode decision through an injectable `@IsDebugBuild` binding; in the Hilt test only, replace that binding with `false`, clear test app state through the normal harness, and exercise the same intro/warning branches that release builds use. Existing warning JVM tests already inject `false`; intro coverage and the MainActivity seam remain with their dependent implementation tasks. Do not treat a debug-only UI run as release evidence.

Task 2's reader spike is complete. The intro/warning tests and final `assembleDebug`/`lint` release gates remain later implementation/release work, not blockers to this reader decision.

_Last reviewed: 2026-10-10._
