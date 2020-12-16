package com.r3.refapp.flows.reports

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.repositories.RecurringPaymentRepository
import com.r3.refapp.states.RecurringPaymentState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.util.*

/**
 * Public API flow used for reporting and views. Flow retrieves [RecurringPaymentState] for given [linearId].
 * @param linearId Id of the [RecurringPaymentState]
 * @return Returns [FlowLogic<RecurringPaymentState>] object for given [linearId]
 * @throws [RefappException] if recurring payment with given [linearId] cannot be found
 */
@StartableByRPC
@InitiatingFlow
class GetRecurringPaymentByIdFlow(val linearId: UUID) : FlowLogic<RecurringPaymentState>() {

    @Suspendable
    override fun call(): RecurringPaymentState {
        val recurringPaymentRepository = serviceHub.cordaService(RecurringPaymentRepository::class.java)
        return recurringPaymentRepository.getRecurringPaymentById(UniqueIdentifier(null, linearId)).state.data
    }
}