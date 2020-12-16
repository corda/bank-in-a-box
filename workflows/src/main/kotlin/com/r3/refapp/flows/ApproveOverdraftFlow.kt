package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.contracts.FinancialAccountContract
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

/**
 * Public API flow for overdraft approval.
 * Approve overdraft for account with [accountId] for overdraft limit [amount].
 *
 * @param accountId the id of the [CurrentAccountState] to approve overdraft for
 * @param amount the [approvedOverdraftLimit] amount in currency units
 */
@InitiatingFlow
@StartableByRPC
class ApproveOverdraftFlow(val accountId: UUID, val amount: Long) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val accountRepository: AccountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val currentAccountState = accountRepository.getCurrentAccountStateById(accountId)
        val currentAccount = currentAccountState.state.data

        val overdraftAccount = CurrentAccountState(
                currentAccount.accountData,
                withdrawalDailyLimit = currentAccount.withdrawalDailyLimit,
                transferDailyLimit = currentAccount.transferDailyLimit,
                overdraftBalance = 0L,
                approvedOverdraftLimit = amount,
                linearId = currentAccount.linearId
        )

        val command = Command(FinancialAccountContract.Commands.ApproveOverdraft(accountId, amount),
                listOf(currentAccount.accountData.accountInfo.host.owningKey))

        val notary = ConfigurationUtils.getConfiguredNotary(serviceHub)
        val txBuilder = TransactionBuilder(notary)
                .addInputState(currentAccountState)
                .addOutputState(overdraftAccount)
                .addCommand(command)

        txBuilder.verify(serviceHub)
        val partStx = serviceHub.signInitialTransaction(txBuilder,
                listOf(currentAccountState.state.data.accountData.accountInfo.host.owningKey))

        return subFlow(FinalityFlow(partStx, emptyList()))
    }

}