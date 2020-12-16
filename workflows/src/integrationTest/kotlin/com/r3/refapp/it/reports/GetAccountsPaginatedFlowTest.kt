package com.r3.refapp.it.reports

import com.r3.refapp.flows.reports.GetAccountsPaginatedFlow
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.states.Account
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.test.utils.TestUtils.prepareOverdraftAccount
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetAccountsPaginatedFlowTest : AbstractITHelper() {

    @Test
    fun `test GetAccountsPaginatedFlow happy path`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                null, RepositoryQueryParams.SortOrder.DESC, "")
        val accounts = prepareAccounts()

        val response = executeFlowWithRunNetwork(GetAccountsPaginatedFlow(repositoryQueryParams,
                accounts[0].accountData.txDate, accounts[4].accountData.txDate), bank, network)

        assertEquals(5, response.pageSize)
        assertEquals(5, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)
        assertEquals(accounts, response.result.map { it.first })

    }

    @Test
    fun `test GetAccountsPaginatedFlow fails with unexistent sort column`() {
        prepareAccounts()
        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "testtest", RepositoryQueryParams.SortOrder.ASC, "")

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(GetAccountsPaginatedFlow(repositoryQueryParams,
                    Instant.now().minusSeconds(10), Instant.now()), bank, network)
        }.message!!
        assertEquals("java.lang.IllegalArgumentException: Invalid sort field testtest", message)
    }

    private fun prepareAccounts() : List<Account> {
        val account1 = prepareCurrentAccount("Customer1", bank, network, emptyList())
        val account2 = prepareOverdraftAccount("Customer2", bank, network, emptyList())
        val account3 = prepareCurrentAccount("Customer3", bank, network, emptyList())
        val account4 = prepareCurrentAccount("Customer4", bank, network, emptyList())
        val account5 = prepareCurrentAccount("Custommer5", bank, network, emptyList())

        return listOf(account1, account2, account3, account4, account5)
    }
}