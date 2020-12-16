package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.contracts.FinancialAccountContract
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.repositories.TransactionLogRepository
import com.r3.refapp.schemas.TransactionType
import com.r3.refapp.states.Account
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

/**
 * Deposit amount of fiat currency into account's balance [SignedTransaction]
 * @param accountId the account's ID
 * @param amount the amount of fiat currency to deposit
 * @return [SignedTransaction] created SignedTransaction on the ledger
 */
@StartableByRPC
class DepositFiatFlow(val accountId: UUID, val amount: Amount<Currency>) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val transactionLogRepository = serviceHub.cordaService(TransactionLogRepository::class.java)

        val accountBalanceStateAndRef = accountRepository.getAccountStateById(accountId)

        val partStx = createDepositTransaction(accountBalanceStateAndRef)
        val outAccountBalance = partStx.coreTransaction.outputsOfType<Account>().single()

        val finalStx = subFlow(FinalityFlow(partStx, emptyList()))
        transactionLogRepository.logTransaction(
                txId = finalStx.id,
                amount = amount,
                txDate = outAccountBalance.accountData.txDate,
                txType = TransactionType.DEPOSIT,
                accountFrom = null,
                accountTo = outAccountBalance.accountData.accountId
        )

        return finalStx
    }

    /**
     * Return a signed deposit transaction crediting the input [StateAndRef<Account>] by [this.amount]
     * @param accountBalanceStateAndRef input [StateAndRef<Account>]
     * @returns [SignedTransaction]
     */
    @Suspendable
    private fun createDepositTransaction(accountBalanceStateAndRef: StateAndRef<Account>)
            : SignedTransaction {

        val inAccountBalance = accountBalanceStateAndRef.state.data
        val outAccountBalance = inAccountBalance.deposit(amount)

        val command = Command(FinancialAccountContract.Commands.DepositFunds(
                inAccountBalance.accountData.accountInfo.identifier, amount),
                ourIdentity.owningKey
        )
        val notary = ConfigurationUtils.getConfiguredNotary(serviceHub)

        val txBuilder = TransactionBuilder(notary)
                .addInputState(accountBalanceStateAndRef)
                .addOutputState(outAccountBalance)
                .addCommand(command)

        txBuilder.verify(serviceHub)

        return serviceHub.signInitialTransaction(txBuilder)
    }
}