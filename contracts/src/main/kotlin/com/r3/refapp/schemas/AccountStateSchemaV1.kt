package com.r3.refapp.schemas

import com.r3.refapp.states.AccountStatus
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import org.hibernate.annotations.Type
import java.time.Instant
import java.util.*
import javax.persistence.*

/**
 * The family of schemas for AccountState.
 */
object AccountStateSchema


/**
 * An AccountState schema.
 * @property balance the amount of the underlying asset in it's lowest denomination (i.e. cents)
 */
object AccountStateSchemaV1 : MappedSchema(
        schemaFamily = AccountStateSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentBalance::class.java)) {

    @Entity
    @Table(name = "account_schema", indexes = [Index(name = "account_state_account_info_idx", columnList = "account_info"),
        Index(name = "account_state_linear_id_idx", columnList = "linear_id"),
        Index(name = "account_state_customer_id_idx", columnList = "customer_id")])
    class PersistentBalance(
            @Column(name = "account_info")
            @Type(type = "uuid-char")
            var account: UUID,

            @Column(name = "balance")
            var balance: Long,

            @Column(name = "tx_date")
            var txDate: Instant,

            @Enumerated(EnumType.STRING)
            @Column(name = "status", nullable = false, length = 20)
            var status: AccountStatus,

            @Column(name = "customer_id", nullable = false)
            @Type(type = "uuid-char")
            var customerId: UUID,

            @Column(name = "linear_id")
            @Type(type = "uuid-char")
            var linearId: UUID,

            @Column(name = "withdrawal_daily_limit", nullable = true)
            var withdrawalDailyLimit: Long? = null,

            @Column(name = "transfer_daily_limit", nullable = true)
            var transferDailyLimit: Long? = null,

            @Column(name = "overdraft_balance")
            var overdraftBalance: Long = 0L,

            @Column(name = "overdraft_limit")
            var overdraftLimit: Long = 0L

    ) : PersistentState() {

        /**
         * Equals and hash code implementations provided in order to give more control over properties included in
         * equals and hash code. Kotlin's data classes are avoided because of well know interoperability issues with
         * JPA. Some of them are forcing lazy initialisation, excluding parent's properties from equals and hash code .etc.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PersistentBalance

            if (stateRef != other.stateRef) return false
            if (account != other.account) return false
            if (balance != other.balance) return false
            if (txDate != other.txDate) return false
            if (status != other.status) return false
            if (customerId != other.customerId) return false
            if (linearId != other.linearId) return false
            if (withdrawalDailyLimit != other.withdrawalDailyLimit) return false
            if (transferDailyLimit != other.transferDailyLimit) return false
            if (overdraftBalance != other.overdraftBalance) return false
            if (overdraftLimit != other.overdraftLimit) return false

            return true
        }

        override fun hashCode(): Int {
            var result = stateRef?.hashCode() ?: 1
            result = 31 * result + account.hashCode()
            result = 31 * result + balance.hashCode()
            result = 31 * result + txDate.hashCode()
            result = 31 * result + status.hashCode()
            result = 31 * result + customerId.hashCode()
            result = 31 * result + linearId.hashCode()
            result = 31 * result + (withdrawalDailyLimit?.hashCode() ?: 0)
            result = 31 * result + (transferDailyLimit?.hashCode() ?: 0)
            result = 31 * result + overdraftBalance.hashCode()
            result = 31 * result + overdraftLimit.hashCode()
            return result
        }
    }
}