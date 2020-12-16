package com.r3.refapp.it.reports

import com.r3.refapp.flows.reports.GetRecurringPaymentsPaginatedFlow
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetRecurringPaymentsPaginatedFlowTest : AbstractITHelper() {

    @Test
    fun `test GetRecurringPaymentsPaginatedFlow happy path`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 5, null, RepositoryQueryParams.SortOrder.DESC, "")

        val account1 = prepareCurrentAccount("Customer1", bank, network)
        val account2 = prepareCurrentAccount("Customer2", bank, network)
        val recurringPayment = createRecurringPayment(account1.accountData.accountId, account2.accountData.accountId, Instant.now().plusSeconds(1),
                Duration.ofSeconds(1), 1)

        Thread.sleep(3 * 1000)
        network.runNetwork()
        val response = executeFlowWithRunNetwork(GetRecurringPaymentsPaginatedFlow(repositoryQueryParams, null, null),
                bank, network)

        assertEquals(5, response.pageSize)
        assertEquals(1, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)

        assertEquals(1, response.result.count())
        assertEquals(1, response.result.count { it.recurringPayment.linearId == recurringPayment.linearId.id })

    }

    @Test
    fun `test GetTransactionsPaginatedFlow fails with unexistent sort column`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5, "testtest", RepositoryQueryParams.SortOrder.ASC, "")

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(GetRecurringPaymentsPaginatedFlow(repositoryQueryParams, Instant.now().minusSeconds(10),
                    Instant.now()), bank, network)
        }.message!!
        assertEquals("java.lang.IllegalArgumentException: Invalid sort field testtest", message)
    }
}