# MedTimer Manual Testing Plan — Pre-Release

## How to use this plan

Work through sections in order. Items marked **[GAP]** have no automated coverage and are the highest-risk scenarios given the recent refactoring (Kotlin migration, DI, entity/model separation). Items marked **[TIME]** require waiting for a real alarm to fire — set them up first, then work through other sections while you wait.

Use a **physical device** for all [TIME] and [GAP] sections. An emulator is acceptable for UI-only sections.

---

## Priority order if time is limited

| Priority | Sections |
|---|---|
| Must do | 3, 10, 4, 8, 14 |
| Should do | 1, 2, 6, 9, 11 |
| Nice to have | 5, 7, 12, 13, 15, Regression checklist |

---

## Section 1 — Medicine & Reminder CRUD

- [ ] Create a new medicine with all fields filled: name, amount, unit, colour, icon, production date, expiration date
- [ ] Verify the medicine appears in the list with the correct colour and icon
- [ ] Add a time-based reminder — confirm it appears with the correct summary string
- [ ] Edit the medicine's name and amount — verify overview and reminder table reflect the change immediately
- [ ] Duplicate the medicine — verify the duplicate has an independent copy of all reminders
- [ ] Delete the duplicate — verify it disappears, no orphaned reminders appear, and the original is unaffected
- [ ] Create a second medicine, add a reminder, then delete only the **reminder** — verify the medicine remains with zero reminders

---

## Section 2 — Reminder Types: UI Configuration

*For each type: create, save, re-open the edit screen, and verify every field round-trips correctly.*

### 2a. Time-based
- [ ] Set a specific time, select Mon/Wed/Fri only, active period (start +2 days, end +30 days), 3-consecutive / 2-pause-day cycle — re-open and verify all fields

### 2b. Continuous interval
- [ ] Set 4-hour interval, start "from first processed" — re-open and verify
- [ ] Set 90-minute interval, start from fixed time 08:00 — re-open and verify

### 2c. Windowed interval **[GAP]**
- [ ] Set 2-hour interval, window 08:00–20:00 — re-open and verify both interval and window times persist

### 2d. Linked reminder **[GAP]**
- [ ] Create reminders on two medicines; add a linked reminder on medicine B pointing to medicine A's reminder with +60 min offset — re-open and verify link target and offset are correct

### 2e. Out-of-stock
- [ ] Set stock to 5, threshold 3, type ONCE — re-open and verify values persist

### 2f. Variable amount
- [ ] Enable "variable amount" on a reminder — verify the flag persists after save

### 2g. Instructions
- [ ] Add dosing instructions text — verify it appears on the reminder card and persists after re-open

---

## Section 3 — Notification Delivery **[GAP] [TIME]**

*Highest-risk section. Set up before starting other sections, then return after the scheduled times.*

**Setup:**
- [ ] Create medicine "Test-Now" with a time-based reminder **3 minutes from now**, notification importance HIGH
- [ ] Create medicine "Test-Interval" with a continuous-interval reminder of 5 minutes starting 2 minutes from now
- [ ] Send app to background (press Home — do **not** force-stop)

**After scheduled times pass:**
- [ ] Notification appears for "Test-Now" within ~10 seconds of scheduled time
- [ ] Tapping **Taken**: notification dismissed, TAKEN event appears in overview and calendar, stock decrements if configured, next occurrence scheduled correctly in overview
- [ ] Tapping **Skip**: SKIPPED event created, next occurrence scheduled correctly
- [ ] Tapping **Snooze** (if configured): notification re-fires after the snooze interval
- [ ] Interval reminder fires at 5-minute intervals
- [ ] Mark first interval dose TAKEN — verify second fires from the correct reference point (processed time vs. fixed start time)

**Notification appearance:**
- [ ] Correct medicine name and dose amount in notification text
- [ ] Correct colour accent matching medicine colour setting
- [ ] Correct icon
- [ ] Notification appears in the correct channel in system notification settings

---

## Section 4 — Linked Reminder End-to-End **[GAP] [TIME]**

- [ ] Medicine A: time-based reminder in 4 minutes
- [ ] Medicine B: linked reminder, source = medicine A, offset +5 minutes
- [ ] Wait for medicine A's notification — mark it **Taken**
- [ ] Verify medicine B's notification fires ~5 minutes after the TAKEN timestamp (not after the scheduled time)
- [ ] Repeat, but mark medicine A as **Skipped** — verify medicine B's reminder does NOT fire (or behaves as expected per scheduler logic)

---

## Section 5 — Weekend Mode **[TIME]**

- [ ] Enable Weekend Mode in Settings; set a weekend time distinct from the standard reminder time
- [ ] On a **weekday**: verify the reminder fires at the standard time, not the weekend override
- [ ] On a **weekend day**: verify the reminder fires at the weekend-overridden time
- [ ] Disable Weekend Mode — verify the reminder reverts to the standard schedule

---

## Section 6 — Active Period & Day-of-Week Filtering **[TIME]**

- [ ] Set a reminder's active period to start **tomorrow** — verify it does NOT appear as pending today
- [ ] Set a reminder's active period to end **yesterday** — verify it shows as inactive and no notification fires
- [ ] Restrict a reminder to today's day of week only — verify it is listed as pending for today
- [ ] Remove today's day from the restriction — verify it disappears from today's pending list immediately (no restart required)

---

## Section 7 — Cyclic Reminders (Consecutive + Pause Days) **[GAP] [TIME]**

- [ ] Configure a reminder: 1 consecutive day, 1 pause day (alternating on/off)
- [ ] Set the cycle start date to yesterday — verify whether today is "on" or "off" matches the expected cycle position
- [ ] Observe over two real days: reminder fires on day 1, absent on day 2, fires on day 3

---

## Section 8 — Out-of-Stock & Expiration Reminders **[GAP]**

### 8a. Out-of-stock (ONCE)
- [ ] Set stock to 2, threshold 3, type **ONCE**
- [ ] Mark two doses TAKEN to bring stock below threshold — verify out-of-stock notification fires exactly once
- [ ] Take another dose — verify no second out-of-stock notification fires
- [ ] Verify the stock count in the UI decrements correctly on each TAKEN action

### 8b. Out-of-stock (ALWAYS)
- [ ] Change type to **ALWAYS** — take another dose — verify the notification fires again

### 8c. Refill
- [ ] Trigger the refill action from the notification or overview — verify stock quantity increases by the configured refill size

### 8d. Expiration **[TIME]**
- [ ] Set a medicine's expiration date to tomorrow — verify an expiration notification fires on that date (or confirm the scheduled alarm is visible in the reminder table)

---

## Section 9 — Manual Dose Logging

- [ ] Log a manual dose for a medicine from the overview
- [ ] Set a custom past timestamp (e.g. 2 hours ago) — verify the event appears in history with the correct time, not the current time
- [ ] Verify stock decrements if stock tracking is enabled
- [ ] Log a manual dose for a medicine with variable amount — verify the custom amount is stored in the event

---

## Section 10 — Background Reliability **[GAP]**

### 10a. App backgrounded normally
- [ ] Schedule a reminder 5 minutes out — press Home, lock the screen, wait
- [ ] Verify the notification fires while the screen is locked

### 10b. Force-stop recovery **[GAP]**
- [ ] Schedule a reminder 10 minutes out — force-stop the app via Settings → Apps → Force Stop
- [ ] Relaunch the app manually
- [ ] Verify the reminder is rescheduled (AutostartService should restore RAISED events from the last 24 hours)
- [ ] Verify no duplicate notifications for reminders already TAKEN before the force-stop

### 10c. Device reboot **[GAP]**
- [ ] Schedule a reminder 10 minutes out — reboot the device
- [ ] Verify the notification fires after reboot (boot receiver must reschedule alarms cleared by reboot)

### 10d. Battery optimisation
- [ ] With battery optimisation **not** exempted, schedule a reminder, leave the phone idle for >30 minutes — verify the notification fires (tests Doze mode)
- [ ] Revoke SCHEDULE_EXACT_ALARM permission — verify the app does not crash and falls back to inexact alarm scheduling

---

## Section 11 — Notifications: Permissions & Edge Cases **[GAP]**

- [ ] Revoke notification permission at runtime (Android 13+) — verify the app shows an appropriate warning rather than crashing
- [ ] Re-grant notification permission — verify notifications resume
- [ ] Dismiss a notification without acting on it — verify it is NOT marked taken/skipped; it remains as a pending RAISED event
- [ ] With repeat-reminder configured: dismiss without acting — verify the repeat fires after the configured interval

---

## Section 12 — Statistics & Calendar

- [ ] After generating TAKEN and SKIPPED events in earlier sections, open Statistics
- [ ] Verify the calendar shows correct icons/colours for each day with events
- [ ] Verify charts reflect correct counts
- [ ] Verify the reminder table shows all medicines and their next scheduled times
- [ ] Switch between available time-range views — verify no crashes

---

## Section 13 — Settings Round-Trip

- [ ] Change theme (light/dark/system) — verify all screens render correctly
- [ ] Change language if supported — verify UI strings and reminder summaries update
- [ ] Enable biometric lock — lock the app — verify biometric prompt appears on next open and the app is unusable without authentication
- [ ] Enable secure window flag — verify the app screen is blank in the recent-apps switcher
- [ ] Configure a global notification repeat interval — verify a triggered reminder repeats at that interval

---

## Section 14 — Backup & Restore **[GAP]**

- [ ] Create several medicines with varied reminder types, events, and stock levels
- [ ] Export a JSON backup
- [ ] Open the backup file and verify the structure contains medicines, reminders, and events
- [ ] Delete all medicines in the app
- [ ] Import the backup — verify all medicines, reminders, stock levels, and historical events are restored exactly
- [ ] Export a CSV — open it and verify the event history is correct and complete

---

## Section 15 — Tags

- [ ] Create two tags (e.g. "Morning", "Evening")
- [ ] Assign tags to different medicines
- [ ] Filter the medicine list by tag — verify only tagged medicines appear
- [ ] Delete a tag — verify it is removed from all medicines that had it, with no crash

---

## Regression Checklist

*Run last — quick checks that nothing regressed from the Kotlin/DI/entity-model separation.*

- [ ] App launches without crash on fresh install (no existing database)
- [ ] App upgrades from the previous release without crash (database migration)
- [ ] No crash navigating through every tab and every settings screen
- [ ] Overview correctly shows today's pending reminders sorted by time
- [ ] Reminder summary strings (e.g. "Every 4h · Mon Wed Fri") display correctly for all reminder types
- [ ] Dark theme has no unreadable text or invisible icons
- [ ] All dialogs (delete confirmation, time picker, colour picker) open and close without crash
- [ ] Rotating the screen mid-dialog or mid-edit does not lose data
