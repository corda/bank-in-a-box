package com.r3.refapp.states

import com.r3.refapp.contracts.FinancialAccountContract
import com.r3.refapp.schemas.AccountStateSchemaV1
import com.r3.refapp.util.of
import net.corda.core.contracts.*
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.Currency

/**
 * Implementation of a current account state with optional limits [withdrawalDailyLimit], [transferDailyLimit] and
 * [approvedOverdraftLimit].
 *
 * @property accountData accounts generic data
 * @property withdrawalDailyLimit daily limit for withdrawals
 * @property transferDailyLimit daily limit for transfers
 * @property overdraftBalance
 * @property approvedOverdraftLimit max [overdraftBalance] limit
 * @property linearId accounts [LinearState] linear id
 * @return [CurrentAccountState]
 */
@BelongsToContract(FinancialAccountContract::class)
@CordaSerializable
data class CurrentAccountState(
        override val accountData: AccountData,
        val withdrawalDailyLimit: Long? = null,
        val transferDailyLimit: Long? = null,
        val overdraftBalance: Long? = null,
        val approvedOverdraftLimit: Long? = null,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : CreditAccount {

    /**
     * Perform a deposit transaction and return a new [CurrentAccountState] with balance credited by [amount].
     * @param amount the deposit amount
     * @return [CurrentAccountState]
     */
    override fun deposit(amount: Amount<Currency>): CurrentAccountState {
        verifyIsActive()

        return when {
            (overdraftBalance ?: 0L == 0L) -> {
                val updatedAccountData = accountData.copy(
                        balance = accountData.balance + amount,
                        txDate = Instant.now())
                copy(accountData = updatedAccountData)
            }
            ((overdraftBalance ?: 0L) < amount.quantity) -> {
                val updatedAccountData = accountData.copy(
                        balance = Amount((amount.quantity - (overdraftBalance ?: 0)), amount.token),
                        txDate = Instant.now())
                copy(overdraftBalance = 0L, accountData = updatedAccountData)
            }
            else -> {
                val updatedAccountData = accountData.copy(txDate = Instant.now())
                copy(overdraftBalance = (overdraftBalance ?: 0L) - amount.quantity, accountData = updatedAccountData)
            }
        }
    }

    /**
     * Perform a withdrawal transaction and return a new [Account] with balance debited by [amount].
     * @param amount the withdrawal amount
     * @return [CurrentAccountState]
     */
    override fun withdraw(amount: Amount<Currency>): CurrentAccountState {
        verifyHasSufficientFunds(amount)
        verifyIsActive()

        return if(accountData.balance >= amount) {
            val updatedAccountData = accountData.copy(
                    balance = accountData.balance - amount,
                    txDate = Instant.now())
            copy(accountData = updatedAccountData)
        } else {
            val updatedOverdraftBalance = (overdraftBalance ?: 0L) + amount.quantity - accountData.balance.quantity
            val updatedAccountData = accountData.copy(
                    balance = 0 of accountData.balance.token,
                    txDate = Instant.now())
            copy(overdraftBalance = updatedOverdraftBalance, accountData = updatedAccountData)
        }
    }

    /**
     * Return a new [CurrentAccountState] with updated [accountStatus].
     * @param accountStatus [AccountStatus] of current account
     * @return [CurrentAccountState]
     */
    override fun setStatus(accountStatus: AccountStatus): CurrentAccountState {
        val updatedAccountData = accountData.copy(status = accountStatus)
        return copy(accountData = updatedAccountData)
    }

    /**
     * Overrides function from [Account] to include withdrawal and transfer daily limit fields, overdraft balance
     * and approved overdraft limit.
     * @param schema the [MappedSchema]
     * @return returns mapped [PersistentState]
     * @throws [IllegalArgumentException] if schema is not [AccountStateSchemaV1.PersistentBalance]
     */
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        val persistentState = super.generateMappedObject(schema)
        if (persistentState !is AccountStateSchemaV1.PersistentBalance) {
            throw IllegalArgumentException("Schema $schema is not a AccountStateSchemaV1.PersistentBalance schema type")
        }
        persistentState.withdrawalDailyLimit = withdrawalDailyLimit
        persistentState.transferDailyLimit = transferDailyLimit
        persistentState.overdraftBalance = overdraftBalance ?: 0
        persistentState.overdraftLimit = approvedOverdraftLimit ?: 0

        return persistentState
    }

    /**
     * Return a new [CurrentAccountState] with [CurrentAccountState.withdrawalDailyLimit] and
     * [CurrentAccountState.transferDailyLimit] set to given [withdrawalDailyLimit] and [transferDailyLimit]. A value of
     * -1 will reset either limit to null.
     * @param withdrawalDailyLimit daily limit for withdrawals
     * @param transferDailyLimit daily limit for transfers
     * @return [CurrentAccountState]
     */
    fun withLimits(withdrawalDailyLimit: Long?, transferDailyLimit: Long?): CurrentAccountState {
        val newWithdrawalDailyLimit = when(withdrawalDailyLimit) {
            null -> this.withdrawalDailyLimit
            -1L -> null
            else -> withdrawalDailyLimit
        }

        val newTransferDailyLimit = when(transferDailyLimit) {
            null -> this.transferDailyLimit
            -1L -> null
            else -> transferDailyLimit
        }

        return copy(withdrawalDailyLimit = newWithdrawalDailyLimit, transferDailyLimit = newTransferDailyLimit)
    }

    /**
     * Verify that account has sufficient funds for payment/withdrawal.
     * @param amount the [Amount] amount
     * @throws [InsufficientBalanceException] if amount is not sufficient
     */
    override fun verifyHasSufficientFunds(amount: Amount<Currency>): Boolean {
        val availableOverdraftBalance = Amount(((approvedOverdraftLimit ?: 0L) - (overdraftBalance ?: 0L)), amount.token)

        return if (accountData.balance + availableOverdraftBalance < amount) {
            throw InsufficientBalanceException(amount.minus(accountData.balance + availableOverdraftBalance))
        } else true
    }
}