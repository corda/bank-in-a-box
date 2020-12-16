package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.operations.CreditRatingRequestOperation
import com.r3.refapp.states.CreditRatingInfo
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import java.util.*

/**
 * Returns a customer credit information [SignedTransaction]
 * The information is gathered from the credit rating rest webserver
 * @param customerId the customer's id
 * @return [SignedTransaction] created SignedTransaction on the ledger
 */
@StartableByRPC
class GetCustomerCreditRatingFlow(val customerId: UUID) : FlowLogic<CreditRatingInfo>() {

    @Suspendable
    override fun call(): CreditRatingInfo {
        return await(CreditRatingRequestOperation(customerId, serviceHub))
    }
}