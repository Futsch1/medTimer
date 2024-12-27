package com.futsch1.medtimer.helpers

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.Volatile

class InitIdlingResource(private val resourceName: String) : IdlingResource {
    class DummyIdlingResource(private val resourceName: String) : IdlingResource {
        override fun getName(): String {
            return resourceName
        }

        override fun isIdleNow(): Boolean {
            return true
        }

        override fun registerIdleTransitionCallback(callback: ResourceCallback?) {
            // Intentionally empty
        }

    }

    init {
        IdlingRegistry.getInstance().unregister(DummyIdlingResource(name))
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

    fun resetInitialized() {
        mIsIdleNow.set(false)
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