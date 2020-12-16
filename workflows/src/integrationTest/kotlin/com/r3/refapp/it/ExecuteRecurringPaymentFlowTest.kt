package com.r3.refapp.it

import com.r3.refapp.flows.CreateRecurringPaymentFlow
import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.schemas.RecurringPaymentLogSchemaV1
import com.r3.refapp.schemas.RecurringPaymentSchemaV1
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.schemas.PersistentStateRef
import net.corda.testing.node.TestClock
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExecuteRecurringPaymentFlowTests : AbstractITHelper() {

    @Test
    fun `test ExecuteRecurringPaymentFlow happy path`() {
        (bank.services.clock as TestClock).setTo(Instant.now())
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

        val recurringPaymentLog = verifyRecurringPaymentLog(recurringPaymentOutputs.single(),
                mutableListOf())

        val vaultRecurringPayment = bank.services.vaultService.queryBy<RecurringPaymentState>().states.single().state.data
        verifyRecurringPayment(customer1Acc, customer2Acc, vaultRecurringPayment,
                nowPlus2Secs.plus(1, ChronoUnit.DAYS), balanceAccFromBefore = 40 of EUR, balanceAccFromAfter = 30 of EUR,
                balanceAccToBefore = 10 of EUR, balanceAccToAfter = 20 of EUR)

        verifyRecurringPaymentLog(recurringPaymentOutputs.single(),
                mutableListOf(recurringPaymentLog))
    }

    @Test
    fun `test ExecuteRecurringPaymentFlow happy path one iteration`() {
        (bank.services.clock as TestClock).setTo(Instant.now())
        val customer1Acc = prepareCurrentAccount("Bank - Customer1", bank, network, emptyList())
        val customer2Acc = prepareCurrentAccount("Bank - Customer2", bank, network, emptyList())
        executeFlowWithRunNetwork(DepositFiatFlow(customer1Acc.accountData.accountId, 50 of EUR), bank, network)

        val nowPlus2Secs = Instant.now().plusSeconds(2)
        val tx = executeFlowWithRunNetwork(CreateRecurringPaymentFlow(customer1Acc.accountData.accountId, customer2Acc.accountData.accountId,
                10 of EUR, nowPlus2Secs, Duration.of(1, ChronoUnit.DAYS), 1), bank, network)

        assertEquals(0, tx.coreTransaction.inputs.size)
        assertEquals(1, tx.tx.commands.size)

        val recurringPaymentOutputs = tx.coreTransaction.outputsOfType<RecurringPaymentState>()
        assertEquals(1, recurringPaymentOutputs.count())

        verifyRecurringPayment(customer1Acc, customer2Acc, recurringPaymentOutputs.single(), nowPlus2Secs)
        verifyRecurringPaymentLog(recurringPaymentOutputs.single(),
                mutableListOf())

        val consumedRecurringPayments = bank.services.vaultService.queryBy<RecurringPaymentState>(
                QueryCriteria.LinearStateQueryCriteria(linearId = listOf(recurringPaymentOutputs.single().linearId),
                        status = Vault.StateStatus.CONSUMED)).states
        val activeRecurringPayments = bank.services.vaultService.queryBy<RecurringPaymentState>(
                QueryCriteria.LinearStateQueryCriteria(linearId = listOf(recurringPaymentOutputs.single().linearId))).states
        val recurringPaymentStateAndRef = recurringPaymentRepository.getRecurringPaymentById(recurringPaymentOutputs.single().linearId)
        val recurringPayment = recurringPaymentStateAndRef.state.data.generateMappedObject(RecurringPaymentSchemaV1) as RecurringPaymentSchemaV1.RecurringPayment
        recurringPayment.stateRef = PersistentStateRef(recurringPaymentStateAndRef.ref)
        val recurringPaymentLogs = recurringPaymentLogRepository.getRecurringPaymentLogsForRecurringPayment(recurringPayment.linearId)

        assertEquals(1, activeRecurringPayments.count())
        assertEquals(0, activeRecurringPayments.single().state.data.iterationNum)
        assertEquals(1, consumedRecurringPayments.count())
        assertEquals(1, recurringPaymentLogs.count())
    }

    @Test
    fun `test ExecuteRecurringPaymentFlow incufficient funds happy path`() {
        (bank.services.clock as TestClock).setTo(Instant.now())
        val customer1Acc = prepareCurrentAccount("Bank - Customer1", bank, network, emptyList())
        val customer2Acc = prepareCurrentAccount("Bank - Customer2", bank, network, emptyList())
        executeFlowWithRunNetwork(DepositFiatFlow(customer1Acc.accountData.accountId, 5 of EUR), bank, network)

        val nowPlus2Secs = Instant.now().plusSeconds(2)
        val tx = executeFlowWithRunNetwork(CreateRecurringPaymentFlow(customer1Acc.accountData.accountId, customer2Acc.accountData.accountId,
                10 of EUR, nowPlus2Secs, Duration.of(1, ChronoUnit.DAYS), null), bank, network)

        assertEquals(0, tx.coreTransaction.inputs.size)
        assertEquals(1, tx.tx.commands.size)

        val recurringPaymentOutputs = tx.coreTransaction.outputsOfType<RecurringPaymentState>()
        assertEquals(1, recurringPaymentOutputs.count())

        verifyRecurringPayment(customer1Acc, customer2Acc, recurringPaymentOutputs.single(), nowPlus2Secs,
                balanceAccFromBefore = 5 of EUR, balanceAccFromAfter =  5 of EUR, balanceAccToAfter = 0 of EUR)

        verifyRecurringPaymentLogError(recurringPaymentOutputs.single())
    }

    private fun verifyRecurringPaymentLog(recurringPaymentState: RecurringPaymentState, logs: MutableList<RecurringPaymentLogSchemaV1.RecurringPaymentLog>)
            : RecurringPaymentLogSchemaV1.RecurringPaymentLog {

        val recurringPaymentStateAndRef = bank.services.vaultService.queryBy<RecurringPaymentState>(
                QueryCriteria.LinearStateQueryCriteria(linearId = listOf(recurringPaymentState.linearId),
                        status = Vault.StateStatus.CONSUMED)).states.maxBy { it.state.data.dateStart }!!
        val recurringPayment = recurringPaymentStateAndRef.state.data.generateMappedObject(RecurringPaymentSchemaV1) as RecurringPaymentSchemaV1.RecurringPayment
        recurringPayment.stateRef = PersistentStateRef(recurringPaymentStateAndRef.ref)

        val recurringPaymentLog = recurringPaymentLogRepository.getLastSuccessLogByRecurringPayment(recurringPayment)
        val recurringPaymentLogs = recurringPaymentLogRepository.getRecurringPaymentLogsForRecurringPayment(recurringPayment.linearId)

        assertEquals(recurringPayment, recurringPaymentLog.recurringPayment)
        assertNull(recurringPaymentLog.error)
        logs.add(recurringPaymentLog)
        assertTrue(recurringPaymentLogs.containsAll(logs))
        return recurringPaymentLog
    }

    private fun verifyRecurringPaymentLogError(recurringPaymentState: RecurringPaymentState) {
        val recurringPaymentStateAndRef = bank.services.vaultService.queryBy<RecurringPaymentState>(
                QueryCriteria.LinearStateQueryCriteria(linearId = listOf(recurringPaymentState.linearId),
                        status = Vault.StateStatus.CONSUMED)).states.maxBy { it.state.data.dateStart }!!
        val recurringPayment = recurringPaymentStateAndRef.state.data.generateMappedObject(RecurringPaymentSchemaV1) as RecurringPaymentSchemaV1.RecurringPayment
        recurringPayment.stateRef = PersistentStateRef(recurringPaymentStateAndRef.ref)
        val recurringPaymentLogs = recurringPaymentLogRepository.getRecurringPaymentLogsForRecurringPayment(recurringPayment.linearId)

        assertEquals(1, recurringPaymentLogs.count())
        val recurringPaymentLog = recurringPaymentLogs.single()
        assertEquals(recurringPayment, recurringPaymentLog.recurringPayment)
        assertEquals("Insufficient balance, missing 5.00 EUR", recurringPaymentLog.error)
    }
}