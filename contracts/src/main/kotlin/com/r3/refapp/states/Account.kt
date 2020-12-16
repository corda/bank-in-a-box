package com.r3.refapp.states

import com.r3.refapp.schemas.AccountStateSchemaV1
import net.corda.core.contracts.Amount
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.contracts.LinearState
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.util.Currency

/**
 * Implemented by a state that contains data related to a financial account. This interface extends
 * the [QueryableState] and [LinearState] interfaces and maps to the
 * [AccountStateSchemaV1.PersistentBalance] schema.
 *
 * @property accountData stores generic account and balance data
 */
@CordaSerializable
interface Account : LinearState, QueryableState {
    val accountData: AccountData

    override val participants: List<AbstractParty> get() = listOf(accountData.accountInfo.host)

    /**
     * Return an instance of [AccountStateSchemaV1.PersistentBalance] containing account data related
     * to this [Account] state.
     * @param schema the [MappedSchema]
     * @return returns mapped [PersistentState]
     * @throws [IllegalArgumentException] if schema is not [AccountStateSchemaV1.PersistentBalance]
     */
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is AccountStateSchemaV1 -> AccountStateSchemaV1.PersistentBalance(
                    this.accountData.accountInfo.identifier.id,
                    this.accountData.balance.quantity,
                    this.accountData.txDate,
                    this.accountData.status,
                    this.accountData.customerId,
                    this.linearId.id)
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    /**
     * Perform a deposit transaction and return a new [Account] with updated balance.
     * @param amount the deposit amount
     * @return [Account]
     */
    fun deposit(amount: Amount<Currency>): Account

    /**
     * Return a new [Account] with updated [accountStatus].
     * @param accountStatus the [AccountStatus] of account
     * @return [Account]
     */
    fun setStatus(accountStatus: AccountStatus): Account

    /**
     * Verify that this [Account] is in active status.
     * @throws [UnsupportedOperationException] if state is not in [AccountStatus.ACTIVE]
     */
    fun verifyIsActive() {
        if (this.accountData.status != AccountStatus.ACTIVE)
            throw UnsupportedOperationException("Operation not supported on account: ${this.accountData.accountId} in: ${this.accountData.status} status")
    }

    /**
     * Verify that this [Account] has sufficient funds for payment/withdrawal.
     * @param amount the [Amount] to be debited
     * @throws [InsufficientBalanceException] if amount is not sufficient
     */
    fun verifyHasSufficientFunds(amount: Amount<Currency>) =
            if (accountData.balance < amount) throw InsufficientBalanceException(amount.minus(accountData.balance)) else true

    /**
     * Return a list of schemas supported by this [Account]
     * @return list of [MappedSchema] this [Account] supports
     */
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(AccountStateSchemaV1)
}