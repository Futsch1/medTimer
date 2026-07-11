package com.futsch1.medtimer.wear

import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture

/**
 * A minimal `Futures.immediateFuture` equivalent so the Tile service doesn't need a full Guava
 * dependency just for two call sites - [androidx.wear.tiles.TileService] only needs the tiny
 * `listenablefuture:1.0` shim interface, which comes transitively with `androidx.wear.tiles`.
 */
fun <T> immediateListenableFuture(value: T): ListenableFuture<T> =
    CallbackToFutureAdapter.getFuture { completer ->
        completer.set(value)
        "immediateListenableFuture"
    }
