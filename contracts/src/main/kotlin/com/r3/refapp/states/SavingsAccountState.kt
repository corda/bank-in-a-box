package com.r3.refapp.states

import com.r3.refapp.contracts.FinancialAccountContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import java.time.Instant
import java.time.Period
import java.util.*


/**
 * Implementation of a savings account state with support for a savings plan.
 * This state will not allow transfers or withdrawals until after [savingsEndDate].
 *
 * @property accountData accounts generic data
 * @property savingsEndDate end date of the savings
 * @property period savings period
 * @property linearId accounts [LinearState] linear id
 */
@BelongsToContract(FinancialAccountContract::class)
data class SavingsAccountState(
        override val accountData: AccountData,
        val savingsEndDate: Instant,
        val period: Period,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : CreditAccount {

    /**
     * Perform a deposit transaction and return a new [SavingsAccountState] with balance credited by [amount].
     * @param amount the deposit amount
     * @return [SavingsAccountState]
     */
    override fun deposit(amount: Amount<Currency>): SavingsAccountState {
        verifyIsActive()
        return copy(accountData = accountData.copy(balance = accountData.balance + amount))
    }

    /**
     * Verify transaction time is after [savingsEndDate] and perform a withdrawal transaction.
     * Return a new [Account] with balance debited by [amount].
     *
     * @param amount the withdrawal amount
     * @return [SavingsAccountState]
     */
    override fun withdraw(amount: Amount<Currency>): SavingsAccountState {
        verifyHasSufficientFunds(amount)
        verifyIsActive()
        verifyWithdrawalAllowed()
        return copy(accountData = accountData.copy(balance = accountData.balance - amount))
    }

    /**
     * Return a new [SavingsAccountState] with updated [accountStatus].
     * @param accountStatus [AccountStatus] of current account
     * @return [SavingsAccountState]
     */
    override fun setStatus(accountStatus: AccountStatus): SavingsAccountState
            = copy(accountData = accountData.copy(status = accountStatus))

    /**
     * Verify that withdrawal is allowed for [SavingsAccountState] and throw an [IllegalArgumentException]
     * if [SavingsAccountState] is still within the savings period.
     * @return true if withdrawal is allowed
     * @throws IllegalStateException if [SavingsAccountState] is in savings period
     */
    private fun verifyWithdrawalAllowed(): Boolean {
        if (Instant.now() < savingsEndDate)
            throw IllegalStateException("Withdrawals are not allowed for Savings account during savings period")
        return true
    }
}