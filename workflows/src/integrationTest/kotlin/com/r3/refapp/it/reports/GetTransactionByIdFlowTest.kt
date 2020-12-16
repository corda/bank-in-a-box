package com.r3.refapp.it.reports

import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.flows.reports.GetTransactionByIdFlow
import com.r3.refapp.schemas.TransactionType
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import org.junit.Assert
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GetTransactionByIdFlowTest : AbstractITHelper() {

    @Test
    fun `test GetTransactionByIdFlow happy path `() {
        val account = prepareCurrentAccount("PartyA - Customer1", bank, network, attachments)
        val tx = executeFlowWithRunNetwork(DepositFiatFlow(account.accountData.accountId, 50 of EUR), bank, network)
        val transactionLog = executeFlowWithRunNetwork(GetTransactionByIdFlow(tx.tx.id.toString()), bank, network)

        assertEquals(tx.tx.id.toString(), transactionLog.txId)
        assertNull(transactionLog.accountFrom)
        assertEquals(account.accountData.accountId, transactionLog.accountTo)
        assertEquals(5000L, transactionLog.amount)
        assertEquals(EUR.currencyCode, transactionLog.currency)
        assertEquals(TransactionType.DEPOSIT, transactionLog.txType)
    }


    @Test
    fun `test GetTransactionByIdFlow fails with transaction log cannot be found`() {

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(GetTransactionByIdFlow("fake_id"), bank, network)
        }.message!!

        Assert.assertEquals("java.lang.IllegalArgumentException: Transactions with id fake_id does not exist", message)
    }
}