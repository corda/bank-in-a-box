package com.r3.refapp.schemas

import com.r3.refapp.util.GBP
import com.r3.refapp.util.EUR
import net.corda.core.schemas.PersistentStateRef
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RecurringPaymentTest : AbstractEntityTest() {

    @Test
    fun `verify all fields in equals method`() {

        val recurringPayment1 = getRecurringPayment(10L, EUR,10L, 1)
        val recurringPayment2 = getRecurringPayment(11L, GBP, 11L, 2)
        val clonedRecurringPayment = clone(recurringPayment1)

        assertNotEquals(recurringPayment1, recurringPayment2)
        assertEquals(recurringPayment1, clonedRecurringPayment)

        verifyWithEachPropertyChanged(recurringPayment1, recurringPayment2) {
            payment1: RecurringPaymentSchemaV1.RecurringPayment,
            payment2: RecurringPaymentSchemaV1.RecurringPayment -> assertNotEquals(payment1, payment2)
        }
    }

    @Test
    fun `verify all fields in hashCode method`() {

        val recurringPayment1 = getRecurringPayment(10L, EUR, 10L, 1)
        val recurringPayment2 = getRecurringPayment(11L, GBP, 11L, 2)
        val clonedRecurringPayment = clone(recurringPayment1)

        assertNotEquals(recurringPayment1.hashCode(), recurringPayment2.hashCode())
        assertEquals(recurringPayment1.hashCode(), clonedRecurringPayment.hashCode())

        verifyWithEachPropertyChanged(recurringPayment1, recurringPayment2) {
            payment1: RecurringPaymentSchemaV1.RecurringPayment,
            payment2: RecurringPaymentSchemaV1.RecurringPayment -> assertNotEquals(payment1.hashCode(), payment2.hashCode())
        }
    }

    private fun getRecurringPayment(amount: Long, currency: Currency, periodSeconds: Long, index: Int)
            : RecurringPaymentSchemaV1.RecurringPayment {

        val recurringPayment = RecurringPaymentSchemaV1.RecurringPayment(
                accountFrom = UUID.randomUUID(),
                accountTo = UUID.randomUUID(),
                amount = amount,
                currency = currency.toString(),
                dateStart = Instant.now().plusSeconds(periodSeconds),
                period = Duration.ofSeconds(periodSeconds),
                iterationNum = index,
                linearId = UUID.randomUUID()
        )
        recurringPayment.stateRef = PersistentStateRef("txId", index)
        return recurringPayment
    }
}