package com.futsch1.medtimer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

interface CoroutineCallback<RESULT> {

    companion object {

        @JvmOverloads
        fun <R> call(
            callback: CoroutineCallback<R>,
            dispatcher: CoroutineDispatcher = Dispatchers.Default
        ): Continuation<R> {
            return object : Continuation<R> {
                override val context: CoroutineContext
                    get() = dispatcher

                override fun resumeWith(result: Result<R>) {
                    callback.onComplete(result.getOrNull(), result.exceptionOrNull())
                }
            }
        }
    }

    fun onComplete(result: RESULT?, error: Throwable?)
}

