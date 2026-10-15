# Beginner guidance and offline help — implementation checklist

**Source:** [Agreed plan](user-guidance.md). That plan remains the scope and decision authority.
**Audience:** Contributors implementing the plan step by step.
**Status:** Task 6 is intentionally skipped by decision because the existing Getting started topic already covers the upcoming-reminder check and the proposed Snackbar conflicted with the creation Toast. Source inspection was accepted for task 1, the reader spike is recorded in [Task 2 evidence](user-guidance-reader-spike.md), and Task 4 capture evidence is recorded below. Check off work only after its acceptance criteria are verified.

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

| Done | Task                                              | Depends on | Verifiable outcome                                                   |
| ---- | ------------------------------------------------- | ---------- | -------------------------------------------------------------------- |
| [x]  | 1. Inventory behavior and draft content           | None       | Evidence-backed beginner and notification drafts                     |
| [x]  | 2. Complete the bounded reader spike              | 1          | One selected reader and reproducible packaging path                  |
| [x]  | 3. Ship the first beginner Help slice             | 2          | Offline Getting started plus state-based setup hints                 |
| [x]  | 4. Establish focused documentation screenshots    | 3          | Reproducible English captures bundled with the guide                 |
| [ ]  | 5. Guide reminder-type selection                  | 3          | Non-blocking type guidance and direct topic access                   |
| [—]  | 6. Guide checking the upcoming reminder (skipped) | 3          | Existing offline guide retained; no extra post-creation interruption |
| [x]  | 7. Shorten the intro without changing permissions | 3          | Orientation-only intro, including skip coverage                      |
| [x]  | 8. Ship notification troubleshooting              | 3          | Verified offline help, accessible after warning dismissal            |
| [x]  | 9. Migrate reviewed use cases                     | 3, 8       | Task-oriented replacements and safe archival                         |
| [ ]  | 10. Formalize contributor and agent maintenance   | 2, 4       | Operational documentation checklist and capture procedure            |
| [x]  | 11. Add mechanical validation to CI               | 2, 3       | Packaging, local-link, image, and destination checks                 |
| [ ]  | 12. Complete release review                       | 4–11       | Both flavors and release-like behavior verified                      |

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

- [x] Compare a lightweight Markdown reader with build-time conversion to local static HTML, including build tooling, dependencies, accessibility, and maintenance cost.
- [x] Time-box the experiment to one focused working session (four-hour cap); record the bound and outcome in [Task 2 evidence](user-guidance-reader-spike.md).
- [x] Demonstrate one topic containing a screenshot, an internal topic link, return navigation, and offline rendering.
- [x] Review enlarged text, TalkBack-accessible headings/links/image alt text, and normal Android Back behavior.
- [x] Demonstrate integration and release packaging in both `full` and `foss`.
- [x] Choose build-time Markdown-to-local-HTML as the sole production rendering path.
- [x] Confirm the canonical source layout, topic identifiers/destinations, contents page, image layout, and packaging procedure in [Task 2 evidence](user-guidance-reader-spike.md).
- [x] Define internal and external link behavior; generated HTML uses safe local-asset handling and disables JavaScript and network loads.
- [x] Verify necessary dependencies are maintained and pinned; no toolchain upgrade.
- [x] Document how to build/read the guide and how derived assets are generated deterministically.
- [x] Select targeted release-like intro/warning tests using a test-only `@IsDebugBuild = false` binding. Their implementation belongs to the intro/warning tasks; production debug suppression stays unchanged.

**Acceptance:** One reproducible approach satisfies all spike demonstrations and constraints. Remaining tasks use settled topic destinations and packaging rather than inventing a second source of content. Evidence: [Task 2 reader spike](user-guidance-reader-spike.md).

## Task 3 — Ship the first beginner Help slice

**Depends on:** Task 2.

- [x] Finalize and bundle Getting started using the selected source layout and reader.
- [x] Add a simple Help contents page and a permanent main overflow Help entry in release builds and both flavors (implemented ahead of task 3 to make the spike usable).
- [x] Make the English-only guide limitation clear in both contents and the directly linked Getting started topic.
- [x] Add non-blocking no-medicines guidance explaining the medicine-then-reminder relationship and offering the next action.
- [x] Add guidance for a medicine without reminders, explaining that a medicine alone does not schedule notifications and pointing to Add reminder.
- [x] Derive guidance from saved medicine/reminder state, not prior hint viewing or an empty Overview day.
- [x] Cover interrupted setup and existing users; hide guidance when its condition stops applying, while keeping reminder-free medicines valid.
- [x] Settle task 6's presentation: after a successful reminder creation, a non-blocking snackbar points to Overview's upcoming time and offers Getting started; no tutorial-progress state.
- [x] Add JVM tests for state transitions, including a valid weekly schedule with nothing due on the selected day.
- [x] Add and run focused UI tests for menu access, contents/topic navigation, contextual links, state changes, and Back behavior.
- [x] Link the active guide from the product README and index its topics.

**Verification:** `./gradlew :feature:ui:test` passed for full and foss. Focused emulator runs passed for `SetupGuidanceTest` and `HelpReaderTest` on the full flavor. `./gradlew assembleDebug` and `./gradlew :app:assembleFullRelease :app:assembleFossRelease` passed; both release APKs were inspected and contain `index.html`, `getting-started.html`, and the guide image. `./gradlew lint` passed. The English-only notice is asserted on the contents page and direct topic entry; the guide itself remains English in all app locales.

**Acceptance:** A user can open Help offline and follow the complete setup path. Setup hints reflect actual state and never treat an empty day as missing setup. Release-like and both-flavor packaging verification demonstrates Help availability.

## Task 4 — Establish focused documentation screenshots

**Depends on:** Task 3.

- [x] Reuse the existing instrumented harness, robots, locale handling, and Screengrab mechanism rather than creating a second capture framework.
- [x] Add a focused English-only capture scenario for the beginner setup path, with first-use state assertions and a descriptive image name.
- [x] Use synthetic data and stable time/data arrangements compatible with the harness. Assert the intended UI state before capture.
- [x] Exercise setup actions in the scenario that owns the creation flow; do not present seeded fixtures as evidence that creation works.
- [x] Run each added or changed screenshot test locally on an emulator.
- [x] Review and commit approved guide images, reference them from the relevant topics, and verify offline rendering.
- [x] Document image provenance, the exact focused capture command, and when regeneration is necessary.
- [x] Ensure ordinary builds use committed images and do not require an emulator or the all-locale store screenshot run.

**Capture procedure and provenance:** Run `fastlane android capture_help_screenshot` with an emulator connected. The `capture_help_screenshot` lane builds the full-flavor debug app and AndroidTest APK, sets the device to 24-hour time, invokes only `HelpScreenshotTest` under Screengrab with the `en-US` locale, and copies the named capture from `app/build/help-screenshot-output/` to `docs/help/images/`. It does not invoke the all-locale Play Store screenshot lane.

`HelpScreenshotTest` creates synthetic **Medicine A** through the UI. It asserts the no-medicines guidance and the saved medicine's no-reminders guidance, then creates dosage **1** at **08:00** and captures Overview after asserting that the next day's event shows the medicine and formatted time. The committed image `getting-started-upcoming-reminder.png` is embedded in [`Getting started`](../../help/getting-started.md).

Regenerate and review this image when any depicted labels, navigation, layout/theme, or corresponding instructions change. The capture lane pins English and 24-hour time for the intended 08:00 example; verify the capture before replacing the committed file. Ordinary guide builds copy this committed local image and do not need an emulator. `./gradlew :app:generateFullDebugHelpGuideAssets` passed and the generated local HTML references the bundled image.

**Verification:** `fastlane android capture_help_screenshot` passed on a connected API 36 emulator (one instrumented test; one English screenshot captured). `./gradlew :app:generateFullDebugHelpGuideAssets` and `./gradlew lint` passed. Reviewed the screenshot and generated HTML link.

**Acceptance:** Contributors can regenerate just the documentation images. Bundled screenshots match their instructions, resolve offline, and contain no real medication data.

## Task 5 — Guide reminder-type selection

**Depends on:** Task 3.

- [x] Add short fixed-time versus interval guidance at the existing choice point using translatable Android strings.
- [x] Link directly to the related guide topic and verify Back returns to the choice context.
- [x] Keep the choice explicit: do not silently choose a type or recommend a medical schedule.
- [x] Review guide wording and hint wording together against current behavior.
- [x] Add and run focused coverage for guidance visibility, topic navigation, and unchanged selection behavior.

Implementation: the existing reminder-type dialog now has concise translated descriptions for time-based,
continuous-interval, and windowed-interval reminders, plus a single non-selecting link to the bundled
`reminder-types` topic. The topic explains all three scheduling choices and the stock/expiration choices.
The focused `SetupGuidanceTest.setupGuidanceFollowsSavedMedicineAndReminderState` passed on the full-flavor
API 36 emulator. `assembleDebug` and `lint` also passed. The full feature UI JVM suite still contains one
pre-existing failure in `SetupGuidanceTest.should hide selected medicine guidance when its reminder is added`.

**Acceptance:** Users can understand the two choices and open relevant offline help without losing control of their selection.

## Task 6 — Guide checking the upcoming reminder (skipped)

**Depends on:** Task 3.

**Decision:** Skip this task. The existing bundled Getting started topic already tells users to open Overview, check the next scheduled date and time, and treat the entry as a schedule preview rather than confirmation of notification delivery. During review, the proposed Snackbar was not considered sufficiently valuable and was obscured/conflicted by the existing successful-creation Toast. Revert the proposed UI, test, and resource changes; do not add a replacement interruption or tutorial-progress state.

- [ ] Implement the non-blocking post-creation presentation settled in task 3.
- [ ] Point users toward checking the upcoming reminder and comparing its time with their intended setup.
- [ ] Keep the wording clear that a scheduled reminder is not confirmation of notification delivery.
- [ ] Avoid recurring interruptions to normal use and do not introduce tutorial-progress persistence.
- [ ] Test the creation-to-upcoming-reminder path, including return navigation and a reminder not due on the selected Overview day.
- [ ] Update guide wording and regenerate affected screenshots only if they are now inaccurate.

**Acceptance:** Reminder creation has an understandable next step, works for legitimate schedules, and does not imply guaranteed delivery.

## Task 7 — Shorten the intro without changing permissions

**Depends on:** Task 3.

- [x] Replace the six-slide feature tour with orientation covering MedTimer and privacy, medicine then reminder, and notification-permission context.
- [x] Link to Getting started rather than teaching Analysis, tags, or stock before setup.
- [x] Preserve first-launch production presentation and current notification-permission request behavior, including when skipping the intro.
- [x] Keep the existing intro replay menu item debug-only; do not make replay a prerequisite for Help access.
- [x] Review new/changed strings with the existing translation workflow.
- [x] Add and run focused tests for first launch, normal completion, skipping, permission behavior, Help access, and the replay-menu distinction.
- [x] Exercise the release-like strategy chosen in task 2 rather than relying on debug suppression behavior.

**Verification:** The intro now has four orientation slides: MedTimer/privacy, medicine-then-reminder setup, Overview day selection and reminder-state icons, and notification permission. The setup slide opens the bundled Getting started topic. Production permission request and skip handling remain unchanged; the debug-only replay menu remains available only through the existing debug-gated menu action. `IntroReleaseBehaviorTest` passed on the full-flavor API 36 emulator with `@IsDebugBuild = false`, covering first launch, completion, and skip. The updated `BasicUITest.appIntro` passed, including Help access and replay behavior. `./gradlew assembleDebug` and `./gradlew lint` passed.

**Acceptance:** The shorter intro is skippable and permission handling is unchanged. Release users can reread guidance through Help without an intro replay menu.

## Task 8 — Ship notification troubleshooting

**Depends on:** Task 3; use the inventory from task 1.

- [x] Verify and finalize Notifications missing or late against current app behavior, labels, permission gates, and Android settings entry points.
- [x] Explain relevant preferences and permissions/settings, with separate setup/scheduling and delivery checks.
- [x] Describe Overview warnings as helpful prompts, not proof of reliable delivery. The guide notes that the exact-reminders warning checks the app preference, not system permission.
- [x] Add the topic to Help contents, link it from Getting started, and add a direct entry in Notification and reminder settings without broadening diagnostics.
- [x] Keep the permanent Help menu and the settings link available after battery/exact-reminder warnings are dismissed.
- [x] Avoid delivery guarantees and claims of exhaustive manufacturer-specific coverage.
- [x] Add and run focused reader and release-like warning tests for direct navigation, bundled offline content, and access after persistent warning dismissal.
- [x] Review whether screenshots were needed; text guidance is sufficient because Android settings vary and no fixed system screen is required.

**Implementation and verification:** Added [`Notifications missing or late`](../../help/notifications-missing-or-late.md) to the generated Help contents, linked it from Getting started, and added a localized direct link in **Settings → Notification and reminder settings**. The guide separates schedule checks from Android delivery settings and explains persistent warning dismissal accurately. Android permission and settings behavior was cross-checked against current app code and labels; wording avoids promising delivery or covering every manufacturer.

`HelpReaderTest.helpReaderNavigationWorksFromMenuAndNotificationSettings` passed on the connected API 28 emulator, covering contents/topic links, toolbar and system Back, the contextual settings link, and bundled content. `ExactRemindersWarningTest` passed with `@IsDebugBuild = false`, covering exact-warning dismissal, persistent warning absence after leaving and returning to Overview, and continued Help access after dismissing both visible warnings. The focused commands were:

```bash
./gradlew :app:connectedFullDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.futsch1.medtimer.HelpReaderTest
./gradlew :app:connectedFullDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.futsch1.medtimer.ExactRemindersWarningTest
```

Full and FOSS Help asset generation passed; `./gradlew assembleDebug lint` passed. No screenshot was necessary. Android 12+ exact-alarm behavior is source-checked; the connected emulator for this focused run is API 28.

**Acceptance:** Notification help is bundled offline and permanently reachable even when warnings no longer appear.

## Task 9 — Migrate reviewed use cases

**Depends on:** Tasks 3 and 8.

- [x] Review each candidate section of `docs/UseCases.md` against current labels, code, tests, and screenshots.
- [x] Migrate useful material into task-oriented topics under More tasks; keep advanced material outside the beginner path.
- [x] Track replacement coverage so no instructions are retired before their replacement is available.
- [x] Update topic indexes, Help contents, product README references, and other inbound links as coverage becomes ready.
- [x] Once replacement coverage is ready, move the superseded document to `docs/archive/` with a superseded notice, retaining a short replacement pointer at the original path for existing links.
- [x] Check migrated local links and images, and verify bundled topic navigation.
- [x] Leave architectural material such as `docs/reminder_flow.md` separate from user help.

**Replacement coverage review:** Current labels were checked in `core/ui/src/main/res/values/strings.xml` and the reminder/settings preference XML; behavior was cross-checked against `ReminderTest`, scheduler tests, and the feature UI plus Android tests named below. The old document's illustrations were visually reviewed and moved with the archived document. They show the former UI and were not re-captured or asserted to match current screens, so none are reused in active Help. The migrated topics therefore do not depend on them. No user-facing screenshot was needed for these text-only procedures.

| Superseded section                 | Active replacement / review disposition                                                                                                                                                                                            | Evidence reviewed                                                                                                                             |
| ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| Birth control pills                | Generic cycle configuration in [Adjust reminder schedules](../../help/scheduling-reminders.md); removed the medical regimen example and dosage guidance.                                                                           | `cycle_reminder`, `cycle_start_date`, `cycle_consecutive_days`, `cycle_pause_days`; `ReminderTest.cyclicReminderTest`; scheduler cycle cases. |
| Tapering off a medicine            | Date-range configuration in [Adjust reminder schedules](../../help/scheduling-reminders.md); no dose-change recommendation.                                                                                                        | `ReminderTest.activeReminderTest`; Advanced settings → Reminder status labels.                                                                |
| Reminder every n weeks             | Weekdays and cycles in [Adjust reminder schedules](../../help/scheduling-reminders.md); omitted the old arithmetic shortcut in favor of previewing the schedule.                                                                   | `ReminderTest.cyclicReminderTest`; scheduler day/cycle tests; `Remind on weekdays`.                                                           |
| Disable a medicine                 | Deactivate/reactivate all reminders in [Manage medicines and reminders](../../help/manage-medicines.md).                                                                                                                           | `deactivate_all` / `activate_all`; `TagTest.activateAndOverviewVisibility`; `ReminderTest.activeIntervalReminderTest`.                        |
| Validate reminder settings         | Medicine calendar in [Review and record events](../../help/review-events.md), plus beginner upcoming-time check in [Getting started](../../help/getting-started.md). Wording explicitly says schedule preview, not delivery proof. | `calendar`; `CalendarTest`; `CalendarEventsProviderTest`; Overview simulated-event model.                                                     |
| Different notification sounds      | Notification category and priority in [Customize notification behavior](../../help/notification-options.md).                                                                                                                       | `notification_importance`, `high_and_alarm`, notification settings implementation; notification troubleshooting topic.                        |
| Modify events                      | Event status and editing in [Review and record events](../../help/review-events.md).                                                                                                                                               | Overview action/editor robots and `ActionsVisitor`; event editor implementation.                                                              |
| Additional dose with preset amount | Additional dose and inactive-reminder preset in [Review and record events](../../help/review-events.md).                                                                                                                           | `ManualDoseTest.testManualDose` and `testManualDoseOfDisabledReminder`; Manual dose UI.                                                       |
| Export medication history          | CSV/PDF event export and privacy caution in [Review and record events](../../help/review-events.md).                                                                                                                               | `event_data`, `export_events_csv`, `export_events_pdf`; export menu/implementation.                                                           |
| Further customization to snooze    | App snooze duration and Android-owned snoozing in [Customize notification behavior](../../help/notification-options.md); no fixed Android path or device promise.                                                                  | `snooze_duration`; `NotificationTest.customSnooze`; snooze settings XML.                                                                      |
| More nagging, repeating reminders  | Repeat count/delay in [Customize notification behavior](../../help/notification-options.md), with explicit warning that repeats do not fix scheduling or delivery.                                                                 | `repeat_reminders`; `NotificationTest.repeatingReminders`; repeat preferences XML.                                                            |
| Following doses                    | Linked reminders in [Adjust reminder schedules](../../help/scheduling-reminders.md); removed the old sample medication sequence.                                                                                                   | `add_linked_reminder`; `ReminderTest.deleteLinkedReminderTest`; linked scheduler tests.                                                       |
| Interval reminders                 | Continuous/windowed interval behavior in [Reminder types](../../help/reminder-types.md) and [Adjust reminder schedules](../../help/scheduling-reminders.md); removed claims that delivery spacing is guaranteed.                   | Reminder type strings; `ReminderTest.activeIntervalReminderTest` / `reminderTypeTest`; interval/windowed scheduler tests.                     |
| Medicine stock tracking            | Stock, refills, and thresholds in [Manage medicines and reminders](../../help/manage-medicines.md).                                                                                                                                | `MedicineStockTest.medicineStockTest`, `reminderAmountWarningTest`; stock settings XML and `StockSettingsFragment`.                           |
| Stock tracking for another person  | Automatic marking explained as bookkeeping, not proof of intake, in [Manage medicines and reminders](../../help/manage-medicines.md).                                                                                              | `automatically_taken` and summary string; `NotificationTest.automaticallyTakenTest`; reminder settings implementation.                        |
| Using MedTimer for several people  | Tags and filters in [Manage medicines and reminders](../../help/manage-medicines.md); phrased as organization/filtering, not separate-user accounts.                                                                               | `tags`; `TagTest.tagHandling` / `medicineVisibility`; tag management/filter implementation.                                                   |
| Intervals during daytime           | Windowed intervals in [Reminder types](../../help/reminder-types.md) and [Adjust reminder schedules](../../help/scheduling-reminders.md); no detailed start-condition claim for the unresolved creation behavior.                  | Windowed type labels; `ReminderTest.reminderTypeTest`; windowed scheduler tests; task 1 inventory limitation.                                 |
| Alarm reminders                    | Alarm priority, settings, and limits in [Customize notification behavior](../../help/notification-options.md), cross-linked to [Notifications missing or late](../../help/notifications-missing-or-late.md).                       | `high_and_alarm`; alarm settings XML; `NotificationTest.alarmTest`; notification troubleshooting source review.                               |

The former `docs/UseCases.md` now contains only a replacement pointer; its complete historical text and referenced images are in `docs/archive/use-cases.md` and `docs/archive/images/`. The product README and Help contents link only to active Help. `docs/reminder_flow.md` remains architectural documentation and was not moved.

**Verification:** `./gradlew :app:generateFullDebugHelpGuideAssets :app:generateFossDebugHelpGuideAssets --console=plain` passed. A focused Python check confirmed generated internal HTML links and image references resolve in both flavors, all active Help Markdown links/images resolve, and all archived image references resolve. The generated APK asset tree contains `more-tasks.html` and the four linked task topics.

A focused API 36 emulator attempt to exercise the new topic links through `HelpReaderTest` did not provide reliable UI evidence: `UiDevice` could see the native Help toolbar but not text inside the rendered WebView. The existing Help reader test in the same class also failed at its English-notice text selector on this emulator. The page rendered visually. The attempted commands were `./gradlew :app:connectedFullDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.futsch1.medtimer.HelpReaderTest --console=plain` and, while a new test was temporarily present, the same command with `class=com.futsch1.medtimer.HelpReaderTest#helpReaderOpensMigratedTopicsAndReturnsToMoreTasks`; both failed on missing WebView text selectors. No instrumented test change was retained. The project owner manually confirmed that all four More tasks links open and toolbar Back returns to the More tasks page (2026-10-15).

**Acceptance:** There is one actively maintained user manual, useful existing instructions remain covered, and old links lead to their replacements.

## Task 10 — Formalize contributor and agent maintenance

**Depends on:** Tasks 2 and 4. **Release gate:** Complete before the first new-help release.

- [x] Extend `docs/guidelines/documentation-guidelines.md` with the guide source layout, reader/build procedure, and focused screenshot workflow.
- [x] Add an operational checklist: identify affected topics and hints; update them in the behavior PR; review images; regenerate only inaccurate images.
- [x] Require supporting code/tests in the PR or contributor-facing evidence for drafts and substantive topic revisions, without placing implementation references in user instructions.
- [x] Require a PR explanation when no documentation change is needed.
- [x] State that LLM assistance does not replace verifying labels, navigation, defaults, and behavior.
- [x] State that agents edit canonical sources, not generated presentation artifacts, and never use real medication data.
- [x] Link the operational checklist from `AGENTS.md` and update relevant documentation indexes.

**Implementation and verification:** Added the source layout, full/FOSS guide generation and release build commands, reader review path, maintenance checklist, evidence expectations, privacy boundary, and exact focused screenshot lane procedure to [`documentation-guidelines.md`](../../guidelines/documentation-guidelines.md#maintaining-the-bundled-help-guide). Linked the checklist from [`AGENTS.md`](../../../AGENTS.md) and updated the [`guidelines index`](../../guidelines/README.md). Confirmed the capture procedure against `fastlane/Fastfile` and `HelpScreenshotTest`. Both flavor guide-generation tasks passed (up-to-date); edited Markdown links and `git diff --check` passed. No release build or emulator run was needed for these documentation changes.

**Acceptance:** A human or agent can find the exact maintenance and capture procedure, including evidence expectations and privacy boundaries.

## Task 11 — Add mechanical validation to CI

**Depends on:** Tasks 2 and 3. **Release gate:** Complete before the first new-help release.

- [x] Add a reproducible local/CI check for generation and packaging using the chosen reader approach.
- [x] Validate local topic links, fragment targets where used, and image references.
- [x] Validate that app-linked help destinations exist.
- [x] Add checker tests with intentionally broken links, missing images, and missing destinations so failures are actionable.
- [x] Run the checks against migrated topics as task 9 lands.
- [x] Integrate checks into the appropriate existing CI workflow and document the local command.
- [x] Keep checks lightweight: no LLM invocation, screenshot emulator, or all-locale capture requirement for documentation builds/PRs.
- [x] Make clear that mechanical checks do not establish semantic correctness.

**Implementation and verification:** Added `./gradlew :app:validateHelpGuide`, which runs the Python standard-library checker tests, generates both debug-flavor guide assets, and validates generated topic links, fragment targets, local images, literal Help destinations in production Kotlin sources, and every generated guide asset inside both debug APKs. APK assembly remains a separate, explicit `assembleDebug` step in `test.yml`. Regression tests cover valid references and packaging, broken local links, missing fragments, missing images, missing app destinations, and app destination discovery. The local commands and semantic limitations are documented in [`documentation-guidelines.md`](../../guidelines/documentation-guidelines.md#maintaining-the-bundled-help-guide).

`./gradlew assembleDebug --console=plain` followed by `./gradlew :app:validateHelpGuide --console=plain` passed: 8 checker tests; both flavor APKs validated. The migrated Help corpus passes. No emulator or external content download is needed.

**Acceptance:** Broken bundled references and destinations fail mechanically; valid content builds without external content downloads or an emulator. Mechanical success is not evidence of semantic accuracy.

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

_Last reviewed: 2026-10-15._
