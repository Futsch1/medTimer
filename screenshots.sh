#!/usr/bin/env bash

function start_clean_status_bar {
    # Start demo mode
    adb shell settings put global sysui_demo_allowed 1

    # Display time 12:00
    adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
    # Display full mobile data without type
    adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e level 4 -e datatype false
    adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true
    # Hide notifications
    adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
    # Show full battery but not in charging state
    adb shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false -e level 100
}

function stop_clean_status_bar {
    adb shell am broadcast -a com.android.systemui.demo -e command exit
}

locales=('en-US' 'es-ES' 'de-DE' 'fr-FR' 'it-IT' 'zh-rCN' 'nl-NL' 'ru-RU' 'tr-TR')
tests_apk_path="app/build/outputs/apk/debug/MedTimer-debug.apk"
app_apk_path="app/build/outputs/apk/androidTest/debug/MedTimer-debug-androidTest.apk"

./gradlew assembleDebug assembleAndroidTest

for i in "${locales[@]}"
do
    start_clean_status_bar
    if [ "$i" == "en-US" ]
    then
      adb shell settings put system time_12_24 12
    else
      adb shell settings put system time_12_24 24
    fi
    fastlane screengrab \
        --locales="$i" \
        --tests_apk_path="$tests_apk_path" \
        --app_apk_path="$app_apk_path" \
        --exit_on_test_failure=false \
        --use_timestamp_suffix=false \
        --use_adb_root=true \
        --use_tests_in_classes=com.futsch1.medtimer.ScreenshotsTest
    stop_clean_status_bar
done

./copyIntroDrawables.sh
