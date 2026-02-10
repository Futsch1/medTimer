package com.futsch1.medtimer.reminders

import android.content.Context
import android.os.Bundle
import androidx.work.Data
import com.futsch1.medtimer.ActivityCodes
import java.time.Instant

data class DebugRescheduleData(val delay: Long, val repeats: Int) {
    companion object {
        fun fromData(data: Data): DebugRescheduleData {
            return DebugRescheduleData(data.getLong(ActivityCodes.EXTRA_SCHEDULE_FOR_TESTS, -1), data.getInt(ActivityCodes.EXTRA_REMAINING_REPEATS, -1))
        }

        fun fromData(bundle: Bundle): DebugRescheduleData {
            return DebugRescheduleData(bundle.getLong(ActivityCodes.EXTRA_SCHEDULE_FOR_TESTS, -1), bundle.getInt(ActivityCodes.EXTRA_REMAINING_REPEATS, -1))
        }
    }
}

class DebugReschedule(val context: Context, val data: DebugRescheduleData) {
    fun adjustTimestamp(instant: Instant): Instant {
        return if (data.delay >= 0) {
            Instant.now().plusMillis(data.delay)
        } else {
            instant
        }
    }

    fun scheduleRepeat() {
        if (data.delay >= 0 && data.repeats > 0) {
            ReminderWorkerReceiver.requestScheduleNowForTests(context, data.delay, data.repeats - 1)
        }
    }
}