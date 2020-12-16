package com.r3.refapp.it

import com.r3.refapp.flows.CreateRecurringPaymentFlow
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import net.corda.core.node.services.queryBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateRecurringPaymentFlowTests : AbstractITHelper() {

    @Test
    fun `test CreateRecurringPaymentFlow happy path`() {
        val customer1Acc = prepareCurrentAccount("Bank - Customer1", bank, network, emptyList())
        val customer2Acc = prepareCurrentAccount("Bank - Customer2", bank, network, emptyList())

        val nowPlusTenDays = Instant.now().plus(Duration.ofDays(10))
        val tx = executeFlowWithRunNetwork(CreateRecurringPaymentFlow(customer1Acc.accountData.accountId, customer2Acc.accountData.accountId,
                10 of EUR, nowPlusTenDays, Duration.ofSeconds(10), null), bank, network)

        assertEquals(0, tx.coreTransaction.inputs.size)
        assertEquals(1, tx.tx.commands.size)

        val recurringPaymentOutputs = tx.coreTransaction.outputsOfType<RecurringPaymentState>()
        assertEquals(1, recurringPaymentOutputs.count())

        val recurringPayment = recurringPaymentOutputs.single()
        assertEquals(customer1Acc.accountData.accountId, recurringPayment.accountFrom)
        assertEquals(customer2Acc.accountData.accountId, recurringPayment.accountTo)
        assertEquals(10 of EUR, recurringPayment.amount)
        assertEquals(nowPlusTenDays, recurringPayment.dateStart)
        assertEquals(Duration.ofSeconds(10), recurringPayment.period)

        val vaultRecurringPayment = bank.services.vaultService.queryBy<RecurringPaymentState>().states.single().state.data
        assertEquals(recurringPayment, vaultRecurringPayment)
    }

    @Test
    fun `test CreateRecurringPaymentFlow fails with account doesn't exist`() {
        val customer2Acc = prepareCurrentAccount("Bank - Customer2", bank2, network, attachments2)
        val fakeId = UUID.randomUUID()

        val nowPlusTenSecs = Instant.now().plus(Duration.ofDays(10))
        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(CreateRecurringPaymentFlow(fakeId, customer2Acc.accountData.accountId,
                    10 of EUR, nowPlusTenSecs, Duration.ofSeconds(10), null), bank, network)
        }.message!!
        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Vault query failed. Cannot find interface com.r3.refapp.states.Account with id: $fakeId", message)
    }

}