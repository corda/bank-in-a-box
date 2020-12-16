package com.r3.refapp.schemas

import com.r3.refapp.util.GBP
import com.r3.refapp.util.EUR
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RecurringPaymentLogTests : AbstractEntityTest() {

    private val logId = "test123"
    private val txDate = Instant.now()
    private val error = "error"

    private val recurringPayment1 = RecurringPaymentSchemaV1.RecurringPayment(
            accountFrom = UUID.randomUUID(),
            accountTo = UUID.randomUUID(),
            amount = 1000L,
            currency = EUR.toString(),
            dateStart = txDate,
            period = Duration.ofSeconds(1),
            iterationNum = 20,
            linearId = UUID.randomUUID())

    private val recurringPayment2 = RecurringPaymentSchemaV1.RecurringPayment(
            accountFrom = recurringPayment1.accountFrom,
            accountTo = recurringPayment1.accountTo,
            amount = 1000L,
            currency = GBP.toString(),
            dateStart = txDate.plusSeconds(1),
            period = Duration.ofSeconds(1),
            iterationNum = 21,
            linearId = recurringPayment1.linearId)

    private val recurringPaymentLog1 = RecurringPaymentLogSchemaV1.RecurringPaymentLog(
            logId = logId,
            recurringPayment = recurringPayment1,
            txDate = txDate,
            error = error)

    private val recurringPaymentLog2 = RecurringPaymentLogSchemaV1.RecurringPaymentLog(
            logId = logId + 1,
            recurringPayment = recurringPayment2,
            txDate = txDate.plusSeconds(1),
            error = error + 1)

    @Test
    fun `verify all fields in equals method`() {
        val cloneRecurringPaymentLog1 = clone(recurringPaymentLog1)

        assertNotEquals(recurringPaymentLog1, recurringPaymentLog2)
        assertEquals(recurringPaymentLog1, cloneRecurringPaymentLog1)

        verifyWithEachPropertyChanged(recurringPaymentLog1, recurringPaymentLog2) {
            log1: RecurringPaymentLogSchemaV1.RecurringPaymentLog,
            log2: RecurringPaymentLogSchemaV1.RecurringPaymentLog -> assertNotEquals(log1, log2)
        }
    }


    @Test
    fun `verify all fields in hashCode method`() {
        val clonedRecurringPaymentLog1 = clone(recurringPaymentLog1)

        assertNotEquals(recurringPaymentLog1.hashCode(), recurringPaymentLog2.hashCode())
        assertEquals(recurringPaymentLog1.hashCode(), clonedRecurringPaymentLog1.hashCode())
        verifyWithEachPropertyChanged(recurringPaymentLog1, recurringPaymentLog2) {
            log1: RecurringPaymentLogSchemaV1.RecurringPaymentLog,
            log2: RecurringPaymentLogSchemaV1.RecurringPaymentLog -> assertNotEquals(log1.hashCode(), log2.hashCode())
        }

    }
}