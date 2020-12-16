package com.r3.refapp.states

import net.corda.core.contracts.Amount
import java.util.*

/**
 * Implemented by an account that supports withdrawals.
 */
interface CreditAccount: Account {
    /**
     * Perform a withdrawal transaction and return a new [Account] with updated balance.
     * @param amount the withdrawal amount
     * @return [Account]
     */
    fun withdraw(amount: Amount<Currency>): CreditAccount
}