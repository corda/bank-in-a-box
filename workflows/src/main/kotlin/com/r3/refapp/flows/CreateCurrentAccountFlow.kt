package com.r3.refapp.flows

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.transactions.SignedTransaction
import net.corda.core.flows.StartableByRPC
import com.r3.refapp.contracts.FinancialAccountContract.Commands.CreateCurrentAccount
import com.r3.refapp.contracts.FinancialAccountContract.Commands.AbstractAccountCreationCmd
import com.r3.refapp.flows.internal.AbstractCreateAccountFlow
import com.r3.refapp.states.*
import com.r3.refapp.util.of
import java.security.PublicKey
import java.time.Instant
import java.util.*


/**
 * Create a new current account for customer with zero balance and return a [SignedTransaction]
 * @param customerId the id of the customer
 * @param tokenType amount token type of the account balance
 * @param withdrawalDailyLimit max daily withdrawal limit [optional]
 * @param transferDailyLimit max daily withdrawal limit [optional]
 * @return [SignedTransaction]
 */
@StartableByRPC
class CreateCurrentAccountFlow(
        private val customerId: UUID,
        private val tokenType: Currency,
        private val withdrawalDailyLimit: Long? = null,
        private val transferDailyLimit: Long? = null) : AbstractCreateAccountFlow(customerId) {
    /**
     * Create savings account creation command
     * @param accountKey
     * @returns [SignedTransaction]
     */
    override fun createAccountCreationCommand(accountKey: PublicKey): AbstractAccountCreationCmd {
        return CreateCurrentAccount(accountKey)
    }

    /**
     * Create current account state object
     * @param cordaAccountInfo is assigned to the account
     * @returns [SignedTransaction]
     */
    override fun createAccountStateObject(cordaAccountInfo: AccountInfo): Account {
        val accountData = AccountData(
                accountId = cordaAccountInfo.identifier.id,
                accountInfo = cordaAccountInfo,
                customerId = customerId,
                balance = 0 of tokenType,
                txDate = Instant.now(),
                status = AccountStatus.PENDING)

        return CurrentAccountState(accountData, withdrawalDailyLimit, transferDailyLimit,
                linearId = cordaAccountInfo.linearId)
    }

}