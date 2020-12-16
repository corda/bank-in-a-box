package com.r3.refapp.states

import com.r3.refapp.contracts.FinancialAccountContract
import net.corda.core.contracts.*
import java.util.*

/**
 * Implementation of a loan account state.
 *
 * @property accountData accounts generic data
 * @property linearId accounts [LinearState] linear id
 */
@BelongsToContract(FinancialAccountContract::class)
data class LoanAccountState(
        override val accountData: AccountData,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
): Account {
    /**
     * Perform a deposit transaction and return a new [LoanAccountState] with balance debited by [amount].
     * @param amount the deposit amount
     * @return [CurrentAccountState]
     */
    override fun deposit(amount: Amount<Currency>): LoanAccountState {
        verifyHasSufficientFunds(amount)
        verifyIsActive()
        return copy(accountData = accountData.copy(balance = accountData.balance - amount))
    }

    /**
     * Return a new [LoanAccountState] with updated [accountStatus].
     * @param accountStatus [AccountStatus] of current account
     * @return [LoanAccountState]
     */
    override fun setStatus(accountStatus: AccountStatus): LoanAccountState {
        return copy(accountData = accountData.copy(status = accountStatus))
    }
}