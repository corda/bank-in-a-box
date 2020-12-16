package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.refapp.contracts.FinancialAccountContract.Commands.AbstractAccountCreationCmd
import com.r3.refapp.contracts.FinancialAccountContract.Commands.CreateSavingsAccount
import com.r3.refapp.flows.internal.AbstractCreateAccountFlow
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.states.*
import com.r3.refapp.util.of
import net.corda.core.contracts.Amount
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import java.security.PublicKey
import java.time.*
import java.util.*


/**
 * Create a new savings account for a customer and return a [SignedTransaction]
 * @param customerId the id of the customer
 * @param tokenType amount token type of the account balance
 * @param currentAccountId id of the associated current account
 * @param savingsAmount monthly amount to be transferred from current to savings account
 * @param savingsStartDate date of the first savings payment
 * @param savingsPeriod savings period in months
 * @return [SignedTransaction]
 */
@StartableByRPC
class CreateSavingsAccountFlow(
        private val customerId: UUID,
        private val tokenType: Currency,
        private val currentAccountId: UUID,
        private val savingsAmount: Amount<Currency>,
        private val savingsStartDate: Instant,
        private val savingsPeriod: Int = 12) : AbstractCreateAccountFlow(customerId) {


    @Suspendable
    override fun call() : SignedTransaction {
        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)

        val signedTransaction = super.call()
        val savingsAccount = signedTransaction.coreTransaction.outputsOfType<SavingsAccountState>().single()
        val currentAccount = accountRepository.getCurrentAccountStateById(currentAccountId)

        val oneMonth = Duration.ofDays(30)
        subFlow(CreateRecurringPaymentFlow(currentAccount.state.data.accountData.accountId, savingsAccount.accountData.accountId,
                savingsAmount, savingsStartDate, oneMonth, savingsPeriod))

        return signedTransaction
    }

    /**
     * Create savings account creation command
     * @param accountKey
     * @returns [SignedTransaction]
     */
    override fun createAccountCreationCommand(accountKey: PublicKey): AbstractAccountCreationCmd {
        return CreateSavingsAccount(accountKey, savingsPeriod, savingsStartDate)
    }

    /**
     * Create savings account object
     * @param cordaAccountInfo corda [AccountInfo]
     * @returns [SignedTransaction]
     */
    override fun createAccountStateObject(cordaAccountInfo: AccountInfo): Account {
        val savingsEndDate = LocalDateTime.ofInstant(savingsStartDate, ZoneId.systemDefault())
                .plusMonths(savingsPeriod.toLong()).atZone(ZoneId.systemDefault()).toInstant()
        return SavingsAccountState(AccountData(cordaAccountInfo.identifier.id, cordaAccountInfo,
                customerId, 0 of tokenType, Instant.now(), AccountStatus.PENDING), savingsEndDate,
                Period.ofMonths(savingsPeriod))
    }
}