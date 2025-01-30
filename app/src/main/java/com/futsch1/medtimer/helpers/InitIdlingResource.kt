package com.futsch1.medtimer.helpers

import android.util.Log
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.Volatile

class InitIdlingResource(private val resourceName: String) : IdlingResource {
    init {
        IdlingRegistry.getInstance().register(this)
        Log.d("InitIdlingResource", "Registered resource: $resourceName")
    }

    @Volatile
    private var mCallback: ResourceCallback? = null

    // Idleness is controlled with this boolean.
    private val mIsIdleNow = AtomicBoolean(false)

    override fun getName(): String {
        return resourceName
    }

    override fun isIdleNow(): Boolean {
        return mIsIdleNow.get()
    }

    override fun registerIdleTransitionCallback(callback: ResourceCallback) {
        mCallback = callback
    }

    fun resetInitialized() {
        Log.d("InitIdlingResource", "Reset initialized: $resourceName")
        mIsIdleNow.set(false)
    }

    fun setInitialized() {
        if (!mIsIdleNow.get()) {
            Log.d("InitIdlingResource", "Set initialized: $resourceName")
            mIsIdleNow.set(true)
            mCallback?.onTransitionToIdle()
        }
    }

    fun destroy() {
        Log.d("InitIdlingResource", "Unregister: $resourceName")
        IdlingRegistry.getInstance().unregister(this)
    }
}