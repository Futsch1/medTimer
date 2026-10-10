# Beginner guidance and offline help

**Status:** Agreed direction; reader technology remains subject to a bounded implementation spike.
**Audience:** Contributors implementing user guidance and maintaining user documentation.

## Goal

Help beginners create their first medicine and reminder, understand that these are separate steps, and check the upcoming reminder. Make notification troubleshooting available after initial setup and after warning dismissal.

Success is a clearer, complete beginner path, not comprehensive feature documentation. Assess usefulness through subsequent GitHub and Google Play feedback; recruited usability testing is not available. Do not claim measured improvement without evidence or add analytics for this work.

## Agreed scope

- A canonical, task-oriented English user guide committed as Markdown.
- Offline help bundled with the installed app, matching that app version.
- A permanent **Help** entry in the main overflow menu, in release builds and both flavors.
- Small, state-based hints on existing setup screens; no separate tutorial mode.
- A shortened, skippable intro focused on orientation and notification-permission context.
- Screenshots captured through the existing instrumented-test/Screengrab infrastructure.
- Explicit documentation maintenance requirements for contributors and AI agents.
- Lightweight mechanical checks, not AI-based correctness gates.

Excluded initially: GitHub Pages, search, runtime content downloads, a multi-version website, a broad UI redesign, coach-mark overlays, tutorial progress tracking, and a test-notification feature.

## Content ownership

The canonical guide is user-facing, not an internal feature specification. Instrumented tests and code are evidence for drafting and verification, not a complete definition of user needs or feature coverage. Tests that seed data may bypass the setup actions beginners need explained.

Short contextual hints remain separately authored Android strings, using the existing translation workflow. They are adaptations of the guide and must be reviewed alongside it when behavior changes. LLM assistance may draft, extract, or synchronize wording; no content schema or algorithmic extraction/binding is required. Generated presentation artifacts must not become additional manually edited sources.

The full guide initially remains English. When accessed from another app language, make that language limitation clear. Do not require translated manuals to ship the first iteration.

Keep content portable: standard Markdown, links, and images. A possible layout is `docs/help/README.md` plus task topics and an images directory; confirm the exact layout in the reader spike. Index active topics and link the guide from the product README.

`CONTEXT.md` is a domain glossary, not the guide or this implementation plan. No glossary changes or ADR are needed just to record these delivery choices.

## Initial help structure

1. **Getting started**
   - Add a medicine, then add a reminder for it.
   - Explain the fixed-time versus interval choice without recommending a medical schedule.
   - Walk through a simple daily, fixed-time example with synthetic data.
   - Find the upcoming reminder and check that its time matches the intended setup.
   - Distinguish scheduled reminders from confirmed notification delivery.
2. **Notifications missing or late**
   - Explain relevant app settings and Android permissions/settings using verified current behavior.
   - Distinguish a scheduling/setup issue from a delivery issue.
   - Keep troubleshooting accessible even after Overview warnings are dismissed.
   - Avoid promises of guaranteed delivery or exhaustive device-manufacturer coverage.
3. **More tasks**
   - Reviewed, migrated material from the existing use-case document.
   - Keep advanced material out of the beginner path.

No public website is required. Markdown remains readable in the repository.

## Contextual guidance

Hints depend on actual state, not whether they were previously viewed:

| Context | Guidance |
| --- | --- |
| No medicines | Explain the medicine-then-reminder relationship and offer the next action. |
| A medicine has no reminders | Explain that the medicine alone does not schedule notifications and point to Add reminder. |
| Choosing a reminder type | Give brief fixed-time/interval guidance with access to the related help topic; do not silently choose a type. |
| After reminder creation | Point toward checking the upcoming reminder. |

An empty Overview day alone must not trigger first-setup guidance: a valid schedule may have nothing due that day. Account for interrupted setup, existing users, and legitimate medicines without reminders. Guidance must remain non-blocking and avoid repeatedly interrupting normal use.

The exact post-creation presentation is an implementation detail to settle during the first vertical slice; it must not introduce tutorial-progress state.

## Intro and Help

Shorten the current six-slide feature tour to orientation:

- What MedTimer does and its privacy approach.
- Add a medicine, then add a reminder.
- Why notification permission is needed.

Preserve the current permission-request behavior, including skipping the intro. Link to Getting started rather than teaching Analysis, tags, and stock before setup.

The intro appears on first launch in production; only its existing replay menu item is debug-only, intentionally for testing without resetting app data. Keep that distinction. The release Help entry provides rereading access and does not require an intro replay item.

Help opens a simple contents page. Contextual links open relevant topics directly. Support normal Back navigation; do not add a separate Help tab.

## Reader spike: choose the simplest overall solution

Compare a lightweight Markdown reader with build-time conversion to static HTML displayed locally. Do not build both production paths.

Demonstrate:

- One topic with a screenshot.
- An internal topic link and return navigation.
- Offline operation.
- Readable large text, basic accessibility, and normal Back behavior.
- Clean integration with both flavors and release packaging.

Constraints:

- No custom Markdown parser, JavaScript, runtime downloads, or documentation framework.
- No manually maintained HTML copies.
- Any necessary dependency must be verified as maintained and pinned using repository conventions; do not upgrade the toolchain as part of this work.
- Include build tooling and maintenance cost in the comparison, not just reader code size.
- If local HTML is selected, use safe local-asset handling and define how internal and external links behave.

Document the chosen build/read procedure in contributor guidance after the spike. Do not create an ADR unless an actual hard-to-reverse, surprising trade-off warrants one.

## Existing documentation migration

Review `docs/UseCases.md` against current code, tests, labels, and screenshots. Move useful sections into task-oriented guide topics rather than maintaining a second manual. Advanced topic migration must not inflate the beginner path.

Update README references. Once replacement coverage is ready, archive the old document with a superseded notice, following documentation guidelines, and retain a short pointer at the original path for existing links. Do not retire instructions before their replacements are available. Migration can be staged alongside the beginner work.

Leave architectural documentation, such as `docs/reminder_flow.md`, distinct from user help.

## Screenshot workflow

Reuse the existing test harness, robots, locale handling, and Screengrab capture mechanism. Add focused English-only documentation scenarios with descriptive image names; do not require the all-locale store screenshot run for help changes.

Use synthetic medication data. Capture first-use states as needed; existing populated store screenshots do not cover the beginner journey. Prefer stable seeded data/time and deterministic UI assertions before capture.

Commit approved guide images so ordinary builds do not require an emulator. Regenerate explicitly when depicted UI or instructions become inaccurate. Document provenance and the focused capture command. Run each added/changed instrumented test locally against an emulator; do not run the full instrumented suite locally.

## Contributor and agent maintenance

Extend `docs/guidelines/documentation-guidelines.md` and link the operational checklist from `AGENTS.md`:

1. Identify guide topics and contextual strings affected by user-visible changes.
2. Update them in the same PR as the behavior/UI change.
3. Review screenshots and regenerate only those now inaccurate.
4. Record relevant tests/code in the PR or contributor-facing evidence when drafting or substantively revising a topic; do not clutter user instructions with implementation references.
5. Explain in the PR when no documentation change is needed.
6. Never use real medication data in documentation, screenshots, or agent prompts.

LLM assistance does not replace verification of labels, navigation, defaults, or behavior. Derived reader assets are generated deterministically; agents edit source content, not generated copies.

## Verification

Mechanical CI checks should verify:

- Guide generation/packaging succeeds with the selected approach.
- Local topic links and image references resolve.
- App help destinations exist.

Do not require an LLM invocation or screenshot emulator on every documentation build/PR. Link validation does not establish semantic accuracy.

Automated tests should cover:

- No medicines versus valid setup with no reminders on the selected day.
- A medicine without reminders, interrupted setup, and guidance disappearing when its condition stops applying.
- Help availability in release configuration and both flavors.
- Contents/topic navigation, offline images, and Back behavior.
- Shortened intro and preserved skip/permission behavior.

Include large-text and accessibility checks in the reader review. Run relevant JVM tests and focused instrumented tests, then `assembleDebug` and `lint` before merging. Existing debug suppression of intro/warnings means debug-only checks are insufficient evidence for the release experience; choose a targeted release-like test strategy without weakening production checks.

Current Overview warnings for battery optimization and exact reminders are dismissed persistently. The exact-reminders warning currently checks the app preference rather than independently certifying system permission. Treat warnings as helpful prompts, not proof of reliable delivery. A broader notification diagnostics redesign is outside this plan.

## Implementation sequence

1. **Inventory and content draft:** inspect beginner setup and notification paths; draft the initial tasks from tests and code; list gaps without blocking on complete feature coverage.
2. **Bounded reader spike:** select one minimal approach and establish source layout, topic destinations, asset handling, and packaging.
3. **First vertical slice:** ship Getting started in bundled Help, a permanent menu entry, and empty-medicine/no-reminder guidance with focused tests.
4. **Complete beginner guidance:** add reminder-type and post-creation guidance; shorten the intro while preserving permissions.
5. **Troubleshooting:** add verified notification help and relevant contextual entry points.
6. **Migration and maintenance:** migrate reviewed use cases, update references, archive only superseded content, and formalize agent/contributor and CI checks. Maintenance rules should be in place before the first release of the new help.
7. **Review and release:** verify both flavors, offline use, large text, focused UI behavior, build, and lint; assess subsequent GitHub/Play feedback and adjust.

## Completion criteria

- A beginner can follow a complete medicine-to-upcoming-reminder path without relying on remembered intro slides or external docs.
- Help and notification troubleshooting are accessible offline in release builds.
- Guidance follows actual context and does not mistake an empty day for missing setup.
- English Markdown is the canonical guide; hints are reviewed adaptations and rendering artifacts are generated.
- Documentation screenshots have a reproducible focused capture path.
- Contributor/agent maintenance and mechanical validation are documented and implemented.

These are implementation acceptance criteria, not evidence that users' difficulties have been eliminated.

_Last reviewed: 2026-10-10._
