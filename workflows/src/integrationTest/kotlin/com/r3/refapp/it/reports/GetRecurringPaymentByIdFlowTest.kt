package com.r3.refapp.it.reports

import com.r3.refapp.flows.CreateRecurringPaymentFlow
import com.r3.refapp.flows.reports.GetRecurringPaymentByIdFlow
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetRecurringPaymentByIdFlowTest : AbstractITHelper() {

    @Test
    fun `test GetRecurringPaymentByIdFlow happy path`() {
        val currentAccount1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())
        val currentAccount2 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())

        val recurringPayment = executeFlowWithRunNetwork(CreateRecurringPaymentFlow(currentAccount1.accountData.accountId, currentAccount2.accountData.accountId,
                10 of EUR, Instant.now().plusSeconds(20), Duration.of(30, ChronoUnit.DAYS), null), bank, network)
                .coreTransaction.outputsOfType<RecurringPaymentState>().single()

        val fetchedRecurringPayment = executeFlowWithRunNetwork(GetRecurringPaymentByIdFlow(recurringPayment.linearId.id),
                bank, network)

        assertEquals(recurringPayment, fetchedRecurringPayment)
    }

    @Test
    fun `test GetRecurringPaymentByIdFlow fails with no recurring payment`() {

        val recurringPaymentId = UUID.randomUUID()
        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(GetRecurringPaymentByIdFlow(recurringPaymentId), bank, network)
        }.message!!

        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Error while fetching RecurringPayment with id: $recurringPaymentId", message)
    }
}