package com.r3.refapp.schemas

import net.corda.core.schemas.MappedSchema
import net.corda.core.serialization.CordaSerializable
import java.io.Serializable
import java.time.Instant
import javax.persistence.*

/**
 * The family of schemas for RecurringPaymentLog.
 */
object RecurringPaymentLogSchema


/**
 * A RecurringPaymentLog schema.
 */
class RecurringPaymentLogSchemaV1 : MappedSchema(
        schemaFamily = RecurringPaymentLogSchema::class.java,
        version = 1,
        mappedTypes = listOf(RecurringPaymentLog::class.java)) {

    @Entity
    @Table(name = "recurring_payment_log", indexes = [
            Index(name = "recurring_payment_log_rp_idx", columnList = "rp_tx_id, rp_idx")
    ])
    @CordaSerializable
    class RecurringPaymentLog(

            @Id
            @Column(name = "log_id", nullable = false)
            var logId: String?,

            @OneToOne
            @JoinColumns(value = [
                    JoinColumn(name = "rp_tx_id", referencedColumnName = "transaction_id", nullable = false),
                    JoinColumn(name = "rp_idx", referencedColumnName = "output_index", nullable = false)
            ], foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
            var recurringPayment: RecurringPaymentSchemaV1.RecurringPayment,

            @Column(name = "tx_date", nullable = false)
            var txDate: Instant,

            @Column(name = "error")
            var error: String?

    ) : Serializable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RecurringPaymentLog

            if (logId != other.logId) return false
            if (recurringPayment != other.recurringPayment) return false
            if (txDate != other.txDate) return false
            if (error != other.error) return false

            return true
        }

        override fun hashCode(): Int {
            var result = logId?.hashCode() ?: 0
            result = 31 * result + recurringPayment.hashCode()
            result = 31 * result + txDate.hashCode()
            result = 31 * result + (error?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "RecurringPaymentLog(logId=$logId, recurringPayment=$recurringPayment, txDate=$txDate, error=$error)"
        }
    }
}
