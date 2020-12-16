package com.r3.refapp.it.reports

import com.r3.refapp.flows.CreateCurrentAccountFlow
import com.r3.refapp.flows.reports.GetCustomerNameByAccountFlow
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.util.EUR
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.createCustomer
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetCustomerNameByAccountFlowTest : AbstractITHelper() {

    @Test
    fun `test GetCustomerNameByAccountFlow happy path`() {

        val customerName = "Test Customer1"
        val customerId = createCustomer(customerName = customerName, contactNumber = "123456789", attachments = attachments, node = bank,
                network = network)

        val account = executeFlowWithRunNetwork(CreateCurrentAccountFlow(customerId, EUR, null, null), bank, network)
                .coreTransaction.outputsOfType<CurrentAccountState>().single()

        val customerNameFetched = executeFlowWithRunNetwork(GetCustomerNameByAccountFlow(account.accountData.accountId), bank, network)

        assertEquals(customerName, customerNameFetched)
    }

    @Test
    fun `test GetCustomerNameByAccountFlow fails with no account for accountId`() {

        val accountId = UUID.randomUUID()
        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(GetCustomerNameByAccountFlow(accountId), bank, network)
        }.message!!

        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Vault query failed. Cannot find interface " +
                "com.r3.refapp.states.Account with id: $accountId", message)
    }
}