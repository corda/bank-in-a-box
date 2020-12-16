package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.contracts.FinancialAccountContract
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.states.CurrentAccountState
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
 * Public API flow for setting [withdrawalDailyLimit] and [transferDailyLimit].
 * Flow will set provided fields to account for given [accountId]. Flow consumes [CurrentAccountState] (any subclass)
 * and produces [CurrentAccountState]. A value limit of -1 indicates that the limit should be reset.
 * @param accountId Id of the account
 * @param withdrawalDailyLimit Daily limit for withdrawals
 * @param transferDailyLimit Daily limit for transfers
 */
@InitiatingFlow
@StartableByRPC
class SetAccountLimitsFlow(val accountId: UUID, val withdrawalDailyLimit: Long?, val transferDailyLimit: Long?) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val accountIn = accountRepository.getCurrentAccountStateById(accountId)
        val accountOut = accountIn.state.data.withLimits(withdrawalDailyLimit, transferDailyLimit)

        val command = Command(FinancialAccountContract.Commands.SetLimits(accountId, withdrawalDailyLimit, transferDailyLimit),
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