#!/usr/bin/env bash

set -uo pipefail

if [[ -z "${GRADLE_TASK:-}" ]]; then
  echo "GRADLE_TASK must be set" >&2
  exit 2
fi
if [[ "$#" -gt 1 ]]; then
  echo "Usage: $0 [instrumentation-test-class[#method]]" >&2
  exit 2
fi

gradle_args=()
if [[ -n "${1:-}" ]]; then
  gradle_args+=("-Pandroid.testInstrumentationRunnerArguments.class=$1")
  printf 'Filtering instrumented tests to %s.\n' "$1"
fi

logcat_pid=''
stop_logcat() {
  if [[ -n "$logcat_pid" ]]; then
    kill "$logcat_pid" 2>/dev/null || true
    wait "$logcat_pid" 2>/dev/null || true
    logcat_pid=''
  fi
}
trap stop_logcat EXIT

adb logcat -c
printf 'Streaming Android test-runner and runtime-error logcat while %s runs.\n' "$GRADLE_TASK"
adb logcat -v time -s \
  TestRunner:V \
  AndroidJUnitRunner:V \
  AndroidRuntime:E \
  SchedulerDebug:V \
  ReminderDebug:V \
  BackupDebug:V \
  StockHandlingDebug:V \
  AlarmDebug:V \
  AutostartDebug:V \
  MedTimerMain:V \
  Biometrics:V \
  Location:V \
  Simulation:V \
  Database:V \
  SimpleIdlingResource:V \
  WidgetImpl:V &
logcat_pid=$!

# Keep the Gradle status while continuing to collect diagnostics and device output.
gradle_status=0
./gradlew "$GRADLE_TASK" "${gradle_args[@]}" || gradle_status=$?

stop_logcat
python3 .github/scripts/summarize-android-test-results.py app/build/outputs/androidTest-results || true
adb pull /sdcard/googletest/test_outputfiles app/build/outputs/androidTest-results/ || true
exit "$gradle_status"
