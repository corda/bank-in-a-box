package com.r3.refapp.it.reports

import com.r3.refapp.flows.reports.GetTransactionsForAccountPaginatedFlow
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.schemas.TransactionType
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetTransactionsForAccountPaginatedFlowTest : AbstractITHelper() {

    @Test
    fun `test GetTransactionsForAccountPaginatedFlow happy path`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 5, null, RepositoryQueryParams.SortOrder.DESC,
                "")
        val accounts = prepareTransactions()

        val response = executeFlowWithRunNetwork(GetTransactionsForAccountPaginatedFlow(repositoryQueryParams,
                accounts[0].accountData.accountId, null, null), bank, network)

        assertEquals(5, response.pageSize)
        assertEquals(2, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)

        assertEquals(2, response.result.count())
        assertEquals(1, response.result.count { it.txType == TransactionType.DEPOSIT })
        assertEquals(0, response.result.count { it.txType == TransactionType.WITHDRAWAL })
        assertEquals(1, response.result.count { it.txType == TransactionType.TRANSFER })
        assertEquals(1, response.result.count { it.accountFrom == accounts[0].accountData.accountId })
        assertEquals(0, response.result.count { it.accountFrom == accounts[1].accountData.accountId })
        assertEquals(0, response.result.count { it.accountFrom == accounts[2].accountData.accountId })
        assertEquals(1, response.result.count { it.accountTo == accounts[0].accountData.accountId })
        assertEquals(0, response.result.count { it.accountTo == accounts[1].accountData.accountId })
        assertEquals(1, response.result.count { it.accountTo == accounts[2].accountData.accountId })

    }

    @Test
    fun `test GetTransactionsForAccountPaginatedFlow fails with unexistent sort column`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "testtest", RepositoryQueryParams.SortOrder.ASC, "")

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(GetTransactionsForAccountPaginatedFlow(repositoryQueryParams, UUID.randomUUID(),
                    Instant.now().minusSeconds(10), Instant.now()), bank, network)
        }.message!!
        assertEquals("java.lang.IllegalArgumentException: Invalid sort field testtest", message)
    }
}