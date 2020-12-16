package com.r3.refapp.test.util

import net.corda.core.concurrent.CordaFuture
import net.corda.core.internal.concurrent.OpenFuture
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class CordaTestingFutureImpl<V>(private val impl: CompletableFuture<V> = CompletableFuture()) : OpenFuture<V> {
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return true
    }

    override fun get(): V {
        return impl.get()
    }

    override fun get(timeout: Long, unit: TimeUnit): V {
        return impl.get(timeout, unit)
    }

    override fun isCancelled(): Boolean {
        return impl.isCancelled
    }

    override fun isDone(): Boolean {
        return impl.isDone
    }

    override fun set(value: V): Boolean {
        return impl.complete(value)
    }

    override fun setException(t: Throwable): Boolean {
        return impl.completeExceptionally(t)
    }

    override fun <W> then(callback: (CordaFuture<V>) -> W) {
    }

    override fun toCompletableFuture(): CompletableFuture<V> {
        return impl.toCompletableFuture()
    }
}