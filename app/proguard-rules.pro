# Keep source file names and line numbers for readable crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic type signatures (required by Gson TypeToken)
-keepattributes Signature

# Keep annotations (required by Hilt, Room, etc.)
-keepattributes *Annotation*

# ---- Gson ----------------------------------------------------------------
# Keep all backup model classes serialized/deserialized by field name
-keep class com.futsch1.medtimer.core.domain.backup.** { *; }

# Keep inner wrapper class used by JSONBackup for versioned parsing
-keep class com.futsch1.medtimer.database.backup.JSONBackup$DatabaseContentWithVersion { *; }

# Keep anonymous TypeToken subclasses created in Room Converters
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }