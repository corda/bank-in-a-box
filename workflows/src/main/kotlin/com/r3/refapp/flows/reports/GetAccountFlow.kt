package com.r3.refapp.flows.reports

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.states.Account
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.util.*

/**
 * Public API flow used for reporting and views.
 * Retrieve [Account] for given [accountId].
 *
 * @param accountId Id of the account
 * @return Returns [FlowLogic<Account>] object for given [accountId]
 * @throws [RefappException] if account with given [accountId] cannot be found
 */
@StartableByRPC
@InitiatingFlow
class GetAccountFlow(val accountId: UUID) : FlowLogic<Account>() {

    @Suspendable
    override fun call(): Account {
        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        return accountRepository.getAccountStateById(accountId).state.data
    }
}