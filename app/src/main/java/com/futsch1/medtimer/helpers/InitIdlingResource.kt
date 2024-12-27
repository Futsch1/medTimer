package com.futsch1.medtimer.helpers

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.Volatile

class InitIdlingResource(val resourceName: String) : IdlingResource {
    init {
        IdlingRegistry.getInstance().register(this)
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

    fun setInitialized() {
        if (!mIsIdleNow.get()) {
            mIsIdleNow.set(true)
            mCallback?.onTransitionToIdle()
        }
    }

    fun destroy() {
        IdlingRegistry.getInstance().unregister(this)
    }
}