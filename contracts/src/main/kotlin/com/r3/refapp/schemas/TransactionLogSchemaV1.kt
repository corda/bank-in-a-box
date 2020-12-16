package com.r3.refapp.schemas

import net.corda.core.schemas.MappedSchema
import net.corda.core.serialization.CordaSerializable
import org.hibernate.annotations.Type
import java.io.Serializable
import java.time.Instant
import java.util.*
import javax.persistence.*

/**
 * The family of schemas for TransactionLog.
 */
object TransactionLogSchema


@CordaSerializable
enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}


/**
 * A Transaction Log schema.
 */
class TransactionLogSchemaV1 : MappedSchema(
        schemaFamily = TransactionLogSchema::class.java,
        version = 1,
        mappedTypes = listOf(TransactionLog::class.java)) {

    @Entity
    @Table(name = "transaction_log")
    @CordaSerializable
    class TransactionLog(
            @Id
            @Column(name = "tx_id", unique = true, nullable = false)
            var txId: String,

            @Column(name = "account_from", nullable = true)
            @Type(type = "uuid-char")
            var accountFrom: UUID?,

            @Column(name = "account_to", nullable = true)
            @Type(type = "uuid-char")
            var accountTo: UUID?,

            @Column(name = "amount", nullable = false)
            var amount: Long,

            @Column(name = "currency", nullable = false)
            var currency: String,

            @Column(name = "transaction_date", nullable = false)
            var txDate: Instant,

            @Enumerated(EnumType.STRING)
            @Column(name = "transaction_type", length = 20, nullable = false)
            var txType: TransactionType

    ) : Serializable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TransactionLog

            if (txId != other.txId) return false
            if (accountFrom != other.accountFrom) return false
            if (accountTo != other.accountTo) return false
            if (amount != other.amount) return false
            if (currency != other.currency) return false
            if (txDate != other.txDate) return false
            if (txType != other.txType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = txId.hashCode()
            result = 31 * result + (accountFrom?.hashCode() ?: 0)
            result = 31 * result + (accountTo?.hashCode() ?: 0)
            result = 31 * result + amount.hashCode()
            result = 31 * result + currency.hashCode()
            result = 31 * result + txDate.hashCode()
            result = 31 * result + txType.hashCode()
            return result
        }

        override fun toString(): String {
            return "TransactionLog(txId='$txId', accountFrom='$accountFrom', accountTo='$accountTo', amount=$amount, currency=$currency, txDate=$txDate, txType='$txType')"
        }

    }
}