package com.futsch1.medtimer.helpers

import android.util.Log
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.Volatile

class SimpleIdlingResource(private val resourceName: String) : IdlingResource {
    init {
        IdlingRegistry.getInstance().register(this)
        Log.d("SimpleIdlingResource", "Registered resource: $resourceName")
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

    fun setBusy() {
        Log.d("SimpleIdlingResource", "Set busy: $resourceName")
        mIsIdleNow.set(false)
    }

    fun setIdle() {
        if (!mIsIdleNow.get()) {
            Log.d("SimpleIdlingResource", "Set idle: $resourceName")
            mIsIdleNow.set(true)
            mCallback?.onTransitionToIdle()
        }
    }

    fun destroy() {
        Log.d("SimpleIdlingResource", "Unregister: $resourceName")
        IdlingRegistry.getInstance().unregister(this)
    }
}