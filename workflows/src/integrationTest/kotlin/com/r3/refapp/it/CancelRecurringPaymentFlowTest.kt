package com.r3.refapp.it

import com.r3.refapp.flows.CancelRecurringPaymentFlow
import com.r3.refapp.flows.CreateRecurringPaymentFlow
import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.flows.IssueLoanFlow
import com.r3.refapp.states.LoanAccountState
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.test.utils.TestUtils.prepareSavingsAccount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CancelRecurringPaymentFlowTests : AbstractITHelper() {

    @Test
    fun `test CancelRecurringPaymentFlow happy path`() {
        val customer1Acc = prepareCurrentAccount("Bank - Customer1", bank, network, emptyList())
        val customer2Acc = prepareCurrentAccount("Bank - Customer2", bank, network, emptyList())
        executeFlowWithRunNetwork(DepositFiatFlow(customer1Acc.accountData.accountId, 50 of EUR), bank, network)

        val nowPlus2Secs = Instant.now().plusSeconds(2)
        val tx = executeFlowWithRunNetwork(CreateRecurringPaymentFlow(customer1Acc.accountData.accountId, customer2Acc.accountData.accountId,
                10 of EUR, nowPlus2Secs, Duration.of(1, ChronoUnit.DAYS), null), bank, network)

        assertEquals(0, tx.coreTransaction.inputs.size)
        assertEquals(1, tx.tx.commands.size)

        val recurringPaymentOutputs = tx.coreTransaction.outputsOfType<RecurringPaymentState>()
        assertEquals(1, recurringPaymentOutputs.count())

        verifyRecurringPayment(customer1Acc, customer2Acc, recurringPaymentOutputs.single(), nowPlus2Secs)

        val vaultRecurringPayment = bank.services.vaultService.queryBy<RecurringPaymentState>().states.single().state.data
        verifyRecurringPayment(customer1Acc, customer2Acc, vaultRecurringPayment, nowPlus2Secs.plus(1, ChronoUnit.DAYS),
                balanceAccFromBefore = 40 of EUR, balanceAccFromAfter = 30 of EUR, balanceAccToBefore = 10 of EUR,
                balanceAccToAfter = 20 of EUR)

        val vaultRecurringPaymentAfter = bank.services.vaultService.queryBy<RecurringPaymentState>().states.single().state.data
        executeFlowWithRunNetwork(CancelRecurringPaymentFlow(vaultRecurringPaymentAfter.linearId), bank, network)

        assertEquals(0, bank.services.vaultService.queryBy<RecurringPaymentState>().states.count())
    }

    @Test
    fun `test CancelRecurringPaymentFlow fails with savings account in savings period`() {
        val customer1Acc = prepareCurrentAccount("Bank - Customer1", bank, network, emptyList())
        val customer2Acc = prepareSavingsAccount("Bank - Customer2", bank, network, emptyList())
        executeFlowWithRunNetwork(DepositFiatFlow(customer1Acc.accountData.accountId, 50 of EUR), bank, network)

        val nowPlus2Secs = Instant.now().plusSeconds(2)
        val tx = executeFlowWithRunNetwork(CreateRecurringPaymentFlow(customer1Acc.accountData.accountId, customer2Acc.accountData.accountId,
                10 of EUR, nowPlus2Secs, Duration.ofSeconds(3), null), bank, network)

        assertEquals(0, tx.coreTransaction.inputs.size)
        assertEquals(1, tx.tx.commands.size)

        val recurringPaymentOutputs = tx.coreTransaction.outputsOfType<RecurringPaymentState>()
        assertEquals(1, recurringPaymentOutputs.count())

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(CancelRecurringPaymentFlow(recurringPaymentOutputs.single().linearId), bank, network)
        }.message!!

        assertTrue(message.contains("Contract verification failed: Failed requirement: Recurring payment cannot be cancelled for saving repayments during savings period"))
    }

    @Test
    fun `test CancelRecurringPaymentFlow fails with loan account`() {
        val customer1Acc = prepareCurrentAccount("Bank - Customer1", bank, network, emptyList())
        val loanTx = executeFlowWithRunNetwork(IssueLoanFlow(customer1Acc.accountData.accountId, 1000 of EUR, 12), bank, network)
        val loanAccount = loanTx.coreTransaction.outputsOfType<LoanAccountState>().single()

        val recurringPayment = recurringPaymentRepository.getRecurringPaymentsForCustomer(customer1Acc.accountData.customerId)
                .single { it.state.data.accountTo == loanAccount.accountData.accountId }

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(CancelRecurringPaymentFlow(recurringPayment.state.data.linearId), bank, network)
        }.message!!

        assertTrue(message.contains("Contract verification failed: Failed requirement: Recurring payment cannot be cancelled for loan repayments"))
    }

    @Test
    fun `test CancelRecurringPaymentFlow fails on recurring payment not found`() {

        val linearId = UniqueIdentifier()
        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(CancelRecurringPaymentFlow(linearId), bank, network)
        }.message!!
        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Error while fetching RecurringPayment with id: ${linearId}", message)
    }
}

