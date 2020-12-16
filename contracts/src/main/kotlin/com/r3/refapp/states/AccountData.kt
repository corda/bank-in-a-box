package com.r3.refapp.states

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.contracts.Amount
import net.corda.core.serialization.CordaSerializable
import java.time.Instant
import java.util.*

/**
 * Account data class which holds all generic account information.
 * @param accountId [UUID] of account
 * @param accountInfo [AccountInfo] object from Account's SDK
 * @param customerId id of customer
 * @param balance account's current balance
 * @param txDate date of the last transaction on account
 * @param status current [AccountStatus]
 */
@CordaSerializable
data class AccountData(val accountId: UUID,
                       val accountInfo: AccountInfo,
                       val customerId: UUID,
                       val balance: Amount<Currency>,
                       val txDate: Instant,
                       val status: AccountStatus)