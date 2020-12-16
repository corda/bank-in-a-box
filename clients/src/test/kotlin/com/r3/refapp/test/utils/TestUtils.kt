package com.r3.refapp.test.utils

import com.r3.refapp.test.util.CordaTestingFutureImpl
import net.corda.core.flows.StateMachineRunId
import net.corda.core.messaging.FlowHandle
import net.corda.core.messaging.FlowHandleImpl
import java.util.*
import java.util.concurrent.CompletableFuture

fun <T> getFlowHandle(returnValue: T) : FlowHandle<T> {
    val completableFuture = CompletableFuture<T>()
    completableFuture.complete(returnValue)

    val testingFutureImpl = CordaTestingFutureImpl(completableFuture)

    return FlowHandleImpl(StateMachineRunId(UUID.randomUUID()), testingFutureImpl)
}