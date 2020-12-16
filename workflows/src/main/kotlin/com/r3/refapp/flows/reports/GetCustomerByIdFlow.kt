package com.r3.refapp.flows.reports

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.schemas.CustomerSchemaV1
import net.corda.core.flows.*
import java.util.*

/**
 * Public API flow used for reporting and views. Flow retrieves [CustomerSchemaV1.Customer] for given [customerId].
 * @param customerId Id of the customer
 * @return Returns [FlowLogic<CustomerSchemaV1.Customer>] object for given customerId
 * @throws IllegalArgumentException if customer with given [customerId] cannot be found
 */
@StartableByRPC
@InitiatingFlow
class GetCustomerByIdFlow(val customerId: UUID) : FlowLogic<CustomerSchemaV1.Customer>() {

    @Suspendable
    override fun call(): CustomerSchemaV1.Customer {
        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        return accountRepository.getCustomerWithId(customerId)
    }
}
