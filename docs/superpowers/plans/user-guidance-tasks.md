# Beginner guidance and offline help — implementation checklist

**Source:** [Agreed plan](user-guidance.md). That plan remains the scope and decision authority.
**Audience:** Contributors implementing the plan step by step.
**Status:** Task 1 complete; source inspection accepted in place of a live walkthrough. Task 2 is next. Check off work only after its acceptance criteria are verified.

## How to use this checklist

Work in the numbered order below. Each implementation task includes its own content and tests; do not defer all verification to the final review. Tasks 1–2 establish the evidence and reader choice; subsequent tasks deliver small, usable additions. A task can be a separate PR, but all release gates must be complete before the first help release.

Read the [coding](../../guidelines/kotlin-android.md), [testing](../../guidelines/testing.md), and
[documentation](../../guidelines/documentation-guidelines.md) guidelines before implementation.

For every implementation task:

- Prefer test-first JVM coverage for state and behavior; add focused instrumented coverage where Android UI integration is necessary.
- Run every added or changed instrumented test locally against an emulator. Do not run the full instrumented or coverage suite locally.
- Review affected guide text, contextual strings, and images alongside the behavior change.
- Record verification commands and results in the PR. Debug-only checks are not sufficient evidence for release behavior.

### Scope guardrails

- English Markdown is canonical; hints are separately authored, translated Android strings.
- Bundle help with the installed version. No runtime downloads, JavaScript, custom Markdown parser, or manually maintained HTML copies.
- No analytics, tutorial-progress state, coach marks, new Help tab, test-notification feature, or broad notification diagnostics redesign.
- Use synthetic medication data only. Do not recommend a medical schedule or promise notification delivery.
- Ask before dependency/toolchain upgrades or Room schema changes. Verify and pin any necessary new dependency using repository conventions.
- Do not add glossary changes or an ADR merely to record these delivery choices.

## Progress and dependencies

| Done | Task | Depends on | Verifiable outcome |
| --- | --- | --- | --- |
| [x] | 1. Inventory behavior and draft content | None | Evidence-backed beginner and notification drafts |
| [ ] | 2. Complete the bounded reader spike | 1 | One selected reader and reproducible packaging path |
| [ ] | 3. Ship the first beginner Help slice | 2 | Offline Getting started plus state-based setup hints |
| [ ] | 4. Establish focused documentation screenshots | 3 | Reproducible English captures bundled with the guide |
| [ ] | 5. Guide reminder-type selection | 3 | Non-blocking type guidance and direct topic access |
| [ ] | 6. Guide checking the upcoming reminder | 3 | Post-creation next step without tutorial tracking |
| [ ] | 7. Shorten the intro without changing permissions | 3 | Orientation-only intro, including skip coverage |
| [ ] | 8. Ship notification troubleshooting | 3 | Verified offline help, accessible after warning dismissal |
| [ ] | 9. Migrate reviewed use cases | 3, 8 | Task-oriented replacements and safe archival |
| [ ] | 10. Formalize contributor and agent maintenance | 2, 4 | Operational documentation checklist and capture procedure |
| [ ] | 11. Add mechanical validation to CI | 2, 3 | Packaging, local-link, image, and destination checks |
| [ ] | 12. Complete release review | 4–11 | Both flavors and release-like behavior verified |

Tasks 4–8 can be developed independently after task 3. Tasks 10–11 may start earlier once their dependencies are ready; neither may be postponed beyond the first release.

## Task 1 — Inventory behavior and draft content

**Depends on:** None.

- [x] Trace adding a medicine, adding its reminder, choosing a type, and checking the upcoming reminder through current implementation and tests. Source inspection accepted by the project owner in place of a live walkthrough for task 1; no live verification claimed.
- [x] Inspect relevant implementation and tests; distinguish tests that own creation flows from fixtures that bypass them.
- [x] Inventory notification preferences, Android permissions/settings, Overview warnings, dismissal behavior, and intro skip/permission behavior.
- [x] Record current labels, navigation, defaults, and supporting code/tests in contributor-facing notes or the PR, not in user instructions.
- [x] Draft Getting started: medicine first, reminder second, a synthetic daily fixed-time example, and checking the expected upcoming time.
- [x] Draft Notifications missing or late: separate setup/scheduling problems from delivery problems and flag anything still unverified.
- [x] Inventory useful sections of `docs/UseCases.md` and map them to proposed beginner, troubleshooting, or advanced topics.
- [x] List content and screenshot gaps without requiring complete feature coverage before proceeding.

Evidence and verification: [Task 1 inventory (2026-10-10)](user-guidance-inventory-2026-10-10.md).
Review drafts: [Getting started](user-guidance-getting-started-draft.md) and
[Notifications missing or late](user-guidance-notifications-draft.md). These do not settle task 2's production source layout.

**Acceptance decision:** The project owner accepts the source inspection and drafts as sufficient for task 1; a live walkthrough is not required for this inventory. The dated inventory records the original verification boundary and is retained as a snapshot. Its pending walkthrough does not block task 2. This decision does not waive later UI, screenshot, permission, or release verification requirements.

**Acceptance:** The beginner draft covers the complete medicine-to-upcoming-reminder path, distinguishes scheduling from confirmed delivery, and identifies unresolved claims. Existing instructions remain available until replacements are ready.

## Task 2 — Complete the bounded reader spike

**Depends on:** Task 1. **Decision checkpoint:** Choose the reader before production implementation.

- [ ] Compare a lightweight Markdown reader with build-time conversion to local static HTML, including build tooling, dependencies, accessibility, and maintenance cost.
- [ ] Time-box the experiment before starting; record the bound and outcome in the PR or contributor-facing notes.
- [ ] Demonstrate one topic containing a screenshot, an internal topic link, return navigation, and offline rendering.
- [ ] Review large text, screen-reader usability, and normal Android Back behavior.
- [ ] Demonstrate integration and release packaging in both `full` and `foss`.
- [ ] Choose one production approach; do not retain two production rendering paths.
- [ ] Confirm the canonical source layout, topic identifiers/destinations, contents page, image layout, and packaging procedure. `docs/help/README.md` is a candidate, not a pre-decided requirement.
- [ ] Define internal and external link behavior. If choosing HTML, specify safe local-asset handling and keep JavaScript disabled.
- [ ] Verify any necessary dependency is maintained and pinned; do not upgrade the toolchain.
- [ ] Document how to build/read the guide and how derived assets are generated deterministically.
- [ ] Select a targeted release-like testing strategy that exercises intro/warning behavior without weakening production checks.

**Acceptance:** One reproducible approach satisfies all spike demonstrations and constraints. Remaining tasks can use settled topic destinations and packaging rather than inventing a second source of content.

## Task 3 — Ship the first beginner Help slice

**Depends on:** Task 2.

- [ ] Finalize and bundle Getting started using the selected source layout and reader.
- [ ] Add a simple Help contents page and a permanent main overflow Help entry in release builds and both flavors.
- [ ] Make the English-only guide limitation clear when the app language is not English.
- [ ] Add non-blocking no-medicines guidance explaining the medicine-then-reminder relationship and offering the next action.
- [ ] Add guidance for a medicine without reminders, explaining that a medicine alone does not schedule notifications and pointing to Add reminder.
- [ ] Derive guidance from actual medicine/reminder state, not prior hint viewing or an empty Overview day.
- [ ] Cover interrupted setup and existing users; hide guidance when its condition stops applying without penalizing legitimate medicines without reminders.
- [ ] Decide the post-creation presentation for task 6 during this slice, without adding tutorial-progress state.
- [ ] Add JVM tests for relevant state transitions, including a valid schedule with nothing due on the selected day.
- [ ] Add and run focused UI tests for menu access, contents/topic navigation, contextual links, and Back behavior.
- [ ] Link the active guide from the product README and index its topics.

**Acceptance:** A user can open Help offline and follow the complete setup path. Setup hints reflect actual state and never treat an empty day as missing setup. Release-like and both-flavor verification demonstrate Help availability.

## Task 4 — Establish focused documentation screenshots

**Depends on:** Task 3.

- [ ] Reuse the existing instrumented harness, robots, locale handling, and Screengrab mechanism rather than creating a second capture framework.
- [ ] Add focused English-only scenarios for first-use states and the beginner setup path, with descriptive image names.
- [ ] Use synthetic data and stable time/data arrangements compatible with the harness. Assert the intended UI state before capture.
- [ ] Exercise setup actions in the scenario that owns the creation flow; do not present seeded fixtures as evidence that creation works.
- [ ] Run each added or changed screenshot test locally on an emulator.
- [ ] Review and commit approved guide images, reference them from the relevant topics, and verify offline rendering.
- [ ] Document image provenance, the exact focused capture command, and when regeneration is necessary.
- [ ] Ensure ordinary builds use committed images and do not require an emulator or the all-locale store screenshot run.

**Acceptance:** Contributors can regenerate just the documentation images. Bundled screenshots match their instructions, resolve offline, and contain no real medication data.

## Task 5 — Guide reminder-type selection

**Depends on:** Task 3.

- [ ] Add short fixed-time versus interval guidance at the existing choice point using translatable Android strings.
- [ ] Link directly to the related guide topic and verify Back returns to the choice context.
- [ ] Keep the choice explicit: do not silently choose a type or recommend a medical schedule.
- [ ] Review guide wording and hint wording together against current behavior.
- [ ] Add and run focused coverage for guidance visibility, topic navigation, and unchanged selection behavior.

**Acceptance:** Users can understand the two choices and open relevant offline help without losing control of their selection.

## Task 6 — Guide checking the upcoming reminder

**Depends on:** Task 3.

- [ ] Implement the non-blocking post-creation presentation settled in task 3.
- [ ] Point users toward checking the upcoming reminder and comparing its time with their intended setup.
- [ ] Keep the wording clear that a scheduled reminder is not confirmation of notification delivery.
- [ ] Avoid recurring interruptions to normal use and do not introduce tutorial-progress persistence.
- [ ] Test the creation-to-upcoming-reminder path, including return navigation and a reminder not due on the selected Overview day.
- [ ] Update guide wording and regenerate affected screenshots only if they are now inaccurate.

**Acceptance:** Reminder creation has an understandable next step, works for legitimate schedules, and does not imply guaranteed delivery.

## Task 7 — Shorten the intro without changing permissions

**Depends on:** Task 3.

- [ ] Replace the six-slide feature tour with orientation covering MedTimer and privacy, medicine then reminder, and notification-permission context.
- [ ] Link to Getting started rather than teaching Analysis, tags, or stock before setup.
- [ ] Preserve first-launch production presentation and current notification-permission request behavior, including when skipping the intro.
- [ ] Keep the existing intro replay menu item debug-only; do not make replay a prerequisite for Help access.
- [ ] Review new/changed strings with the existing translation workflow.
- [ ] Add and run focused tests for first launch, normal completion, skipping, permission behavior, Help access, and the replay-menu distinction.
- [ ] Exercise the release-like strategy chosen in task 2 rather than relying on debug suppression behavior.

**Acceptance:** The shorter intro is skippable and permission handling is unchanged. Release users can reread guidance through Help without an intro replay menu.

## Task 8 — Ship notification troubleshooting

**Depends on:** Task 3; use the inventory from task 1.

- [ ] Verify and finalize Notifications missing or late against current app and Android behavior.
- [ ] Explain relevant preferences and permissions/settings, with separate setup/scheduling and delivery checks.
- [ ] Describe Overview warnings as helpful prompts, not proof of reliable delivery. Account for the exact-reminders warning checking the app preference rather than independently certifying system permission.
- [ ] Add the topic to Help contents and appropriate existing troubleshooting contexts without broadening into a diagnostics redesign.
- [ ] Keep access available after battery/exact-reminder warnings have been persistently dismissed.
- [ ] Avoid delivery guarantees and claims of exhaustive manufacturer-specific coverage.
- [ ] Add and run focused tests for direct topic navigation, offline use, and access after warning dismissal, including release-like warning behavior.
- [ ] Capture any needed images through task 4's focused workflow.

**Acceptance:** Notification help is verified, bundled, and permanently reachable even when warnings no longer appear.

## Task 9 — Migrate reviewed use cases

**Depends on:** Tasks 3 and 8.

- [ ] Review each candidate section of `docs/UseCases.md` against current labels, code, tests, and screenshots.
- [ ] Migrate useful material into task-oriented topics under More tasks; keep advanced material outside the beginner path.
- [ ] Track replacement coverage so no instructions are retired before their replacement is available.
- [ ] Update topic indexes, Help contents, product README references, and other inbound links as coverage becomes ready.
- [ ] Once replacement coverage is ready, move the superseded document to `docs/archive/` with a superseded notice, retaining a short replacement pointer at the original path for existing links.
- [ ] Check migrated local links and images, and verify bundled topic navigation.
- [ ] Leave architectural material such as `docs/reminder_flow.md` separate from user help.

**Acceptance:** There is one actively maintained user manual, useful existing instructions remain covered, and old links lead to their replacements.

## Task 10 — Formalize contributor and agent maintenance

**Depends on:** Tasks 2 and 4. **Release gate:** Complete before the first new-help release.

- [ ] Extend `docs/guidelines/documentation-guidelines.md` with the guide source layout, reader/build procedure, and focused screenshot workflow.
- [ ] Add an operational checklist: identify affected topics and hints; update them in the behavior PR; review images; regenerate only inaccurate images.
- [ ] Require supporting code/tests in the PR or contributor-facing evidence for drafts and substantive topic revisions, without placing implementation references in user instructions.
- [ ] Require a PR explanation when no documentation change is needed.
- [ ] State that LLM assistance does not replace verifying labels, navigation, defaults, and behavior.
- [ ] State that agents edit canonical sources, not generated presentation artifacts, and never use real medication data.
- [ ] Link the operational checklist from `AGENTS.md` and update relevant documentation indexes.

**Acceptance:** A human or agent can find the exact maintenance and capture procedure, including evidence expectations and privacy boundaries.

## Task 11 — Add mechanical validation to CI

**Depends on:** Tasks 2 and 3. **Release gate:** Complete before the first new-help release.

- [ ] Add a reproducible local/CI check for generation and packaging using the chosen reader approach.
- [ ] Validate local topic links, fragment targets where used, and image references.
- [ ] Validate that app-linked help destinations exist.
- [ ] Add checker tests with intentionally broken links, missing images, and missing destinations so failures are actionable.
- [ ] Run the checks against migrated topics as task 9 lands.
- [ ] Integrate checks into the appropriate existing CI workflow and document the local command.
- [ ] Keep checks lightweight: no LLM invocation, screenshot emulator, or all-locale capture requirement for documentation builds/PRs.
- [ ] Make clear that mechanical checks do not establish semantic correctness.

**Acceptance:** Broken bundled references and destinations fail mechanically; valid content builds without external content downloads or an emulator.

## Task 12 — Complete release review

**Depends on:** Tasks 4–11.

- [ ] Follow the full beginner journey with fresh synthetic data: medicine, reminder, type choice, upcoming-time check.
- [ ] Verify no-medicine, medicine-without-reminders, interrupted-setup, existing-user, and valid-empty-day states; confirm guidance disappears appropriately.
- [ ] Verify release Help availability and the English-language notice in both flavors.
- [ ] Verify contents, direct topic links, screenshots, external-link policy, and normal Back behavior offline.
- [ ] Review large text, screen-reader operation, and non-blocking hint presentation.
- [ ] Verify intro first launch, completion, skip, permission behavior, and troubleshooting access after warning dismissal using the selected release-like strategy.
- [ ] Run relevant JVM tests for both flavors, focused instrumented tests, mechanical documentation checks, `./gradlew assembleDebug`, and `./gradlew lint`.
- [ ] Confirm release packaging for both flavors includes matching guide assets using the build procedure from task 2.
- [ ] Record results and outstanding limitations; do not mark blocked or unverified checks as passed.
- [ ] Confirm contributor/agent maintenance rules are in place before releasing.
- [ ] After release, review subsequent GitHub and Google Play feedback for follow-up work. Do not claim measured improvement without evidence or add analytics.

**Acceptance:** All [plan completion criteria](user-guidance.md#completion-criteria) are met and verification evidence is recorded. This establishes implementation completeness, not proof that user difficulties have been eliminated.

_Last reviewed: 2026-10-10._
