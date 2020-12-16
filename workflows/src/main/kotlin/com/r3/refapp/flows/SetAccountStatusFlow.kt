package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.contracts.FinancialAccountContract
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.states.AccountStatus
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.core.contracts.Command
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

/**
 * Public API flow for manual account approval/suspend/un-suspend functionality.
 * This flow will set the status of provided account to given [accountStatus].
 *
 * @param accountId id of the account to update
 * @param accountStatus new account status
 */
@InitiatingFlow
@StartableByRPC
class SetAccountStatusFlow(private val accountId: UUID, private val accountStatus: AccountStatus) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val accountIn = accountRepository.getAccountStateById(accountId)
        val accountOut = accountIn.state.data.setStatus(accountStatus)

        val command = Command(FinancialAccountContract.Commands.SetAccountStatus(accountId, accountStatus),
                listOf(accountIn.state.data.accountData.accountInfo.host.owningKey))

        val notary = ConfigurationUtils.getConfiguredNotary(serviceHub)
        val txBuilder = TransactionBuilder(notary)
                .addInputState(accountIn)
                .addOutputState(accountOut)
                .addCommand(command)

        txBuilder.verify(serviceHub)
        val partStx = serviceHub.signInitialTransaction(txBuilder, listOf(accountIn.state.data.accountData.accountInfo.host.owningKey))

        return subFlow(FinalityFlow(partStx, emptyList()))
    }
}