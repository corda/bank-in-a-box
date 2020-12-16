package com.r3.refapp.repositories

import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import net.corda.core.contracts.UniqueIdentifier
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecurringPaymentRepositoryTests : AbstractITHelper() {

    @Test
    fun `test getRecurringPaymentById happy path`() {

        val customer1Acc = prepareCurrentAccount("Bank - Customer1", bank, network)
        val customer2Acc = prepareCurrentAccount("Bank - Customer2", bank, network)

        val recurringPayment = createRecurringPayment(customer1Acc.accountData.accountId, customer2Acc.accountData.accountId)
        val fetchedRecurringPayment = recurringPaymentRepository.getRecurringPaymentById(recurringPayment.linearId).state.data

        assertEquals(recurringPayment, fetchedRecurringPayment)
    }

    @Test
    fun `test getRecurringPaymentById fails with no RecurringPayment for id`() {

        val uniqueIdentifier = UniqueIdentifier()
        val message = assertFailsWith<RefappException> {
            recurringPaymentRepository.getRecurringPaymentById(uniqueIdentifier)
        }.message!!
        assertEquals("Refapp exception: Error while fetching RecurringPayment with id: $uniqueIdentifier", message)
    }

    @Test
    fun `test getRecurringPaymentsForCustomer happy path`() {

        val customer1Acc = prepareCurrentAccount("Bank - Customer1", bank, network)
        val customer2Acc = prepareCurrentAccount("Bank - Customer2", bank, network)

        val recurringPayment1 = createRecurringPayment(customer1Acc.accountData.accountId, customer2Acc.accountData.accountId)
        val recurringPayment2 = createRecurringPayment(customer2Acc.accountData.accountId, customer1Acc.accountData.accountId)
        val recurringPayments = recurringPaymentRepository.getRecurringPaymentsForCustomer(customer1Acc.accountData.customerId)
                .map { it.state.data }

        assertEquals(2, recurringPayments.count())
        assertTrue(recurringPayments.contains(recurringPayment1))
        assertTrue(recurringPayments.contains(recurringPayment2))
    }

    @Test
    fun `test getRecurringPaymentsForCustomer empty list happy path`() {

        val recurringPayments = recurringPaymentRepository.getRecurringPaymentsForCustomer(UUID.randomUUID())
        assertEquals(0, recurringPayments.count())
    }
}