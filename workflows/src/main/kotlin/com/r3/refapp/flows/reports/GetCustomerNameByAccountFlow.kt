package com.r3.refapp.flows.reports

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.schemas.CustomerSchemaV1
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.util.*

/**
 * Public API flow used for views. Flow retrieves [CustomerSchemaV1.Customer.customerName] of the account owner for
 * given [accountId].
 * @param accountId Id of the account
 * @return Returns [CustomerSchemaV1.Customer.customerName] for given [accountId]
 * @throws NoSuchElementException if account with given [accountId] cannot be found
 */
@StartableByRPC
@InitiatingFlow
class GetCustomerNameByAccountFlow(val accountId: UUID) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val account = accountRepository.getAccountStateById(accountId)
        return accountRepository.getCustomerWithId(account.state.data.accountData.customerId).customerName
    }
}