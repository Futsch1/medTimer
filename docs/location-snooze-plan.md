# Plan: Location-Based Snooze ("Snooze Until Home") — Support Classes

## Context
Users want to snooze a medication reminder and have it re-fire automatically when they arrive home. This plan covers all support infrastructure (location classes, DI module, manifest, Gradle) so the user can wire in the UI themselves.

The app has no existing location code. Reminders flow through `AlarmProcessor` → `ReminderProcessorBroadcastReceiver`. The standard snooze path lives in `SnoozeProcessor`. Crucially, `AlarmProcessor.setAlarmForReminderNotification()` already short-circuits and fires a broadcast immediately if the scheduled time is <= now, which the new processor exploits.

---

## Approach: Android Geofencing API

Use `GeofencingClient` (from `play-services-location`) to monitor a home geofence. Geofencing is battery-efficient (OS-managed, no polling), fires exactly on the `ENTER` transition, and has a simple setup/teardown API. The trade-off is a dependency on Google Play Services, which is acceptable given the app already uses Google Material and flexbox.

Pending snoozed reminders are persisted in `SharedPreferences("medTimer")` as JSON (using Gson). This avoids a Room schema migration and matches the existing pattern of writing transient state to the `@MedTimerPreferencess` preferences file.

---

## Files to Create

All new files go in `app/src/main/java/com/futsch1/medtimer/location/`.

### `HomeLocation.kt`
```kotlin
data class HomeLocation(val latitude: Double, val longitude: Double, val radiusMeters: Float = 150f)
```
Pure data carrier, no dependencies.

### `HomeLocationStore.kt`
Persists home location and the list of pending location-snoozed reminders.

```kotlin
class HomeLocationStore @Inject constructor(
    @MedTimerPreferencess private val prefs: SharedPreferences,
    private val gson: Gson
) {
    fun saveHomeLocation(location: HomeLocation)
    fun getHomeLocation(): HomeLocation?
    fun clearHomeLocation()

    fun addPendingLocationSnooze(data: ReminderNotificationData)
    fun getPendingLocationSnoozes(): List<ReminderNotificationData>
    fun clearAllPendingLocationSnoozes()
}
```
- Keys: `"home_location"` (JSON string), `"pending_location_snoozes"` (JSON array)
- `ReminderNotificationData` is serialized via a private wrapper `SerializablePendingSnooze(reminderIds: IntArray, reminderEventIds: IntArray, notificationId: Int, remindInstantEpochSecond: Long)` to avoid `Instant` serialization issues
- Uses `@MedTimerPreferencess` qualifier (note: intentionally double-`s` to match existing code in `DatastoreModule.kt`)

### `GeofenceRegistrar.kt`
Wraps all `GeofencingClient` calls.

```kotlin
class GeofenceRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geofencingClient: GeofencingClient,
    private val homeLocationStore: HomeLocationStore
) {
    companion object { const val GEOFENCE_ID = "medtimer_home" }

    fun isLocationServiceAvailable(): Boolean  // checks GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    fun registerHomeGeofence(): Boolean  // false if no home saved, no permission, Play Services unavailable, or API error
    fun unregisterHomeGeofence()
    private fun buildGeofencePendingIntent(): PendingIntent  // targets GeofenceBroadcastReceiver
    private fun hasRequiredPermissions(): Boolean  // checks ACCESS_FINE_LOCATION + ACCESS_BACKGROUND_LOCATION on API 29+
}
```
- `isLocationServiceAvailable()` uses `GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)` as an upfront check. Returns `false` on devices without Google Play Services (some Huawei, Amazon Fire, custom ROMs). The UI integration should call this first and hide/disable the "Snooze until home" option if it returns `false`.
- `registerHomeGeofence()` calls `isLocationServiceAvailable()` first and returns `false` immediately if Play Services are absent. Even without this check, `addGeofences` is wrapped in try/catch on `ApiException` as a safety net. The feature degrades gracefully — no crash, the snooze simply isn't registered.
- Transition type: `GEOFENCE_TRANSITION_ENTER` only (avoids duplicate DWELL/EXIT firings)
- Expiration: `Geofence.NEVER_EXPIRE`
- `PendingIntent` flags: `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`, request code 0 (one home geofence)

### `LocationSnoozeProcessor.kt`
Fires all pending location-snoozed reminders when the geofence triggers.

```kotlin
class LocationSnoozeProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val homeLocationStore: HomeLocationStore,
    private val geofenceRegistrar: GeofenceRegistrar
) {
    fun processLocationSnooze()
}
```
- Reads `homeLocationStore.getPendingLocationSnoozes()`
- For each `ReminderNotificationData`: sets `remindInstant = Instant.now()`, then calls `alarmProcessor.setAlarmForReminderNotification(data)` — this exploits the existing short-circuit in `AlarmProcessor.kt:44` that fires a broadcast immediately when time <= now, triggering the standard `Reminder` → `ReminderNotificationProcessor` path with no new code paths
- Clears pending store
- Calls `geofenceRegistrar.unregisterHomeGeofence()` to save battery
- No coroutine needed: `setAlarmForReminderNotification` only posts a broadcast synchronously

### `GeofenceBroadcastReceiver.kt`
Receives the OS geofence transition event.

```kotlin
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    @Inject lateinit var locationSnoozeProcessor: LocationSnoozeProcessor

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) { Log.e(LogTags.REMINDER, "Geofence error: ${event.errorCode}"); return }
        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            locationSnoozeProcessor.processLocationSnooze()
        }
    }
}
```
- `@AndroidEntryPoint` enables Hilt injection in `BroadcastReceiver`
- `onReceive` is main-thread; `processLocationSnooze()` only sends broadcasts (no I/O), so no `goAsync()` needed

---

## DI Module to Create

**`app/src/main/java/com/futsch1/medtimer/di/LocationModule.kt`**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides @Singleton
    fun provideGeofencingClient(@ApplicationContext context: Context): GeofencingClient =
        LocationServices.getGeofencingClient(context)

    @Provides @Singleton
    fun provideGson(): Gson = GsonBuilder().create()
}
```
`GeofencingClient` must be singleton (add/remove must use same instance). `Gson` singleton is safe — no other module provides it; existing usages create local instances.

---

## `ActivityCodes.kt` Changes

No new `ProcessorCode` enum entry is needed — `GeofenceBroadcastReceiver` receives its own `PendingIntent`-delivered intent from the OS and calls `LocationSnoozeProcessor` directly. The processor then re-uses the existing `Reminder` code path.

---

## AndroidManifest.xml Changes

**Add permissions** (after existing `<uses-permission>` block):
```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission
    android:name="android.permission.ACCESS_BACKGROUND_LOCATION"
    android:minSdkVersion="29" />
```
Background location is API 29+ only (`android:minSdkVersion="29"` suppresses lint on API 28 devices). On API 29+ users must grant coarse/fine first, then background in a separate flow — the UI implementation must handle this two-step request.

**Register receiver** (inside `<application>`, alongside other receivers):
```xml
<receiver
    android:name=".location.GeofenceBroadcastReceiver"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="com.futsch1.medtimer.GEOFENCE_TRANSITION" />
    </intent-filter>
</receiver>
```
`exported="false"` is correct — geofencing delivers via `PendingIntent`, not public broadcast.

---

## Gradle Changes

**`gradle/libs.versions.toml`** — add:
```toml
[versions]
playServicesLocation = "21.3.0"

[libraries]
play-services-location = { module = "com.google.android.gms:play-services-location", version.ref = "playServicesLocation" }
```

**`app/build.gradle.kts`** — add to `dependencies`:
```kotlin
implementation(libs.play.services.location)
```

---

## Boot Handling Note
Geofences are cleared by the OS after a device reboot. The existing `Autostart.kt` / `AutostartService.kt` handles `BOOT_COMPLETED` and restores pending notifications. The user should also add a call to `geofenceRegistrar.registerHomeGeofence()` there when `homeLocationStore.getPendingLocationSnoozes().isNotEmpty()`. This is part of the user's integration work.

---

## User Integration Points (Not in this plan's scope)

1. **"Snooze until home" notification action**: Call `homeLocationStore.addPendingLocationSnooze(data)` then `geofenceRegistrar.registerHomeGeofence()`; cancel the notification
2. **Home location preferences UI**: Show a button to set home = current location via `FusedLocationProviderClient.lastLocation`, then call `homeLocationStore.saveHomeLocation(HomeLocation(lat, lng))`
3. **Permission request flow**: Two-step — request `ACCESS_FINE_LOCATION` first, then `ACCESS_BACKGROUND_LOCATION` (API 29+)
4. **Boot re-registration**: In `AutostartService`, call `geofenceRegistrar.registerHomeGeofence()` if pending snoozes exist

---

## Verification

After implementation, test end-to-end by:
1. Set home location via the preferences UI
2. Grant all location permissions
3. Trigger a reminder and press "Snooze until home"
4. Verify pending snooze is saved (check `homeLocationStore.getPendingLocationSnoozes()`)
5. Use Android Studio's Location tool (Extended Controls) to simulate location entering the home geofence
6. Verify `GeofenceBroadcastReceiver.onReceive()` is called and the notification re-appears
7. Verify geofence is removed after the transition
