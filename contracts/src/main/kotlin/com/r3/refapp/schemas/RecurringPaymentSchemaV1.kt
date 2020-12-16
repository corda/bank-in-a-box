package com.r3.refapp.schemas

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import org.hibernate.annotations.Type
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.persistence.*

/**
 * The family of schemas for RecurringPayment.
 */
object RecurringPaymentSchema


/**
 * RecurringPayment [QueryableState] schema.
 */
object RecurringPaymentSchemaV1 : MappedSchema(
        schemaFamily = RecurringPaymentSchema.javaClass,
        version = 1,
        mappedTypes = listOf(RecurringPayment::class.java)) {

    @Entity
    @Table(name = "recurring_payment", indexes = [
        Index(name = "recurring_payment_account_from_idx", columnList = "account_from"),
        Index(name = "recurring_payment_account_to_idx", columnList = "account_to"),
        Index(name = "recurring_payment_linear_id_idx", columnList = "linear_id")
    ])
    @CordaSerializable
    class RecurringPayment(

            @Column(name = "account_from")
            @Type(type = "uuid-char")
            var accountFrom: UUID,

            @Column(name = "account_to")
            @Type(type = "uuid-char")
            var accountTo: UUID,

            @Column(name = "amount")
            var amount: Long,

            @Column(name = "currency", nullable = false)
            var currency: String,

            @Column(name = "date_start")
            var dateStart: Instant,

            @Column(name = "period")
            var period: Duration,

            @Column(name = "iteration_num", nullable = true)
            var iterationNum: Int?,

            @Column(name = "linear_id")
            @Type(type = "uuid-char")
            var linearId: UUID

    ) : PersistentState() {

        /**
         * Equals and hash code implementations provided in order to give more control over properties included in
         * equals and hash code. Kotlin's data classes are avoided because of well know interoperability issues with
         * JPA. Some of them are forcing lazy initialisation, excluding parent's properties from equals and hash code .etc.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as RecurringPayment

            if (stateRef != other.stateRef) return false
            if (accountFrom != other.accountFrom) return false
            if (accountTo != other.accountTo) return false
            if (amount != other.amount) return false
            if (currency != other.currency) return false
            if (dateStart != other.dateStart) return false
            if (period != other.period) return false
            if (iterationNum != other.iterationNum) return false
            if (linearId != other.linearId) return false

            return true
        }

        override fun hashCode(): Int {
            var result = stateRef?.hashCode() ?: 1
            result = 31 * result + accountFrom.hashCode()
            result = 31 * result + accountTo.hashCode()
            result = 31 * result + amount.hashCode()
            result = 31 * result + currency.hashCode()
            result = 31 * result + dateStart.hashCode()
            result = 31 * result + period.hashCode()
            result = 31 * result + (iterationNum?.hashCode() ?: 1)
            result = 31 * result + linearId.hashCode()
            return result
        }
    }
}