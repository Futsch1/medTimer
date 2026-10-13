#!/usr/bin/env bash

set -uo pipefail

if [[ -z ${GRADLE_TASK:-} ]]; then
	echo "GRADLE_TASK must be set" >&2
	exit 2
fi
if [[ $# -gt 1 ]]; then
	echo "Usage: $0 [instrumentation-test-class[#method]]" >&2
	exit 2
fi

gradle_args=()
if [[ -n ${1:-} ]]; then
	gradle_args+=("-Pandroid.testInstrumentationRunnerArguments.class=$1")
	printf 'Filtering instrumented tests to %s.\n' "$1"
fi

logcat_pid=''
stop_logcat() {
	if [[ -n $logcat_pid ]]; then
		kill "$logcat_pid" 2>/dev/null || true
		wait "$logcat_pid" 2>/dev/null || true
		logcat_pid=''
	fi
}
trap stop_logcat EXIT

# Build before pinning the emulator clock: Gradle setup/assembly can take long enough for the
# emulator to resynchronize its clock before instrumentation starts. Pin it after that work.
if ! ./gradlew :app:assembleFullDebugAndroidTest; then
	exit 1
fi

adb shell settings put global auto_time 0
adb shell su 0 toybox date 0801160025
emulator_time=$(adb shell date '+%Y-%m-%d %H:%M' | tr -d '\r')
if [[ $emulator_time != '2025-08-01 16:00' ]]; then
	echo "Could not pin emulator clock to the instrumented-test time (got '$emulator_time')." >&2
	exit 1
fi
printf 'Pinned emulator clock to %s before running tests.\n' "$emulator_time"

adb logcat -c
logcat_filters=(
	TestRunner:V
	AndroidJUnitRunner:V
	AndroidRuntime:E
)
case "${INCLUDE_APP_LOGS:-false}" in
true)
	printf 'MedTimer app debug log streaming is enabled.\n'
	logcat_filters+=(
		SchedulerDebug:V
		ReminderDebug:V
		BackupDebug:V
		StockHandlingDebug:V
		AlarmDebug:V
		AutostartDebug:V
		MedTimerMain:V
		Biometrics:V
		Location:V
		Simulation:V
		Database:V
		SimpleIdlingResource:V
		WidgetImpl:V
	)
	;;
false | '')
	printf 'MedTimer app debug log streaming is disabled.\n'
	;;
*)
	printf 'INCLUDE_APP_LOGS must be true or false; got %s.\n' "$INCLUDE_APP_LOGS" >&2
	exit 2
	;;
esac
printf 'Streaming Android test-runner and runtime-error logcat while %s runs.\n' "$GRADLE_TASK"
adb logcat -v time -s "${logcat_filters[@]}" &
logcat_pid=$!

# Keep the Gradle status while continuing to collect diagnostics and device output.
gradle_status=0
./gradlew "$GRADLE_TASK" "${gradle_args[@]}" || gradle_status=$?

stop_logcat
python3 .github/scripts/summarize-android-test-results.py app/build/outputs/androidTest-results || true
adb pull /sdcard/googletest/test_outputfiles app/build/outputs/androidTest-results/ || true
exit "$gradle_status"
