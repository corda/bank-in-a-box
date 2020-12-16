package com.r3.refapp.repositories

import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.schemas.RecurringPaymentLogSchemaV1
import com.r3.refapp.schemas.RecurringPaymentSchemaV1
import com.r3.refapp.states.Account
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import org.junit.jupiter.api.*
import java.lang.IllegalArgumentException
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RecurringPaymentLogRepositoryTests : AbstractITHelper() {

    companion object {
        lateinit var accounts: List<Account>
        lateinit var recurringPayments: List<RecurringPaymentState>
        lateinit var recurringPaymentLogs: Map<UUID, List<RecurringPaymentLogSchemaV1.RecurringPaymentLog>>

        @BeforeAll
        @JvmStatic
        fun setUpRepo() {

            val account1 = prepareCurrentAccount("Customer1", bank, network)
            val account2 = prepareCurrentAccount("Customer2", bank, network)
            val account3 = prepareCurrentAccount("Customer3", bank, network)
            executeFlowWithRunNetwork(DepositFiatFlow(account1.accountData.accountId, 100 of EUR), bank, network)
            network.runNetwork()
            val recurringPayment1 = createRecurringPayment(account1.accountData.accountId, account2.accountData.accountId,
                    Instant.now().plusSeconds(1), Duration.ofSeconds(1), 1)
            val recurringPayment2 = createRecurringPayment(account2.accountData.accountId, account3.accountData.accountId,
                    Instant.now().plusSeconds(1), Duration.ofSeconds(1), 2)
            val recurringPayment3 = createRecurringPayment(account3.accountData.accountId, account1.accountData.accountId,
                    Instant.now().plusSeconds(1), Duration.ofSeconds(1), 2)
            network.runNetwork()
            Thread.sleep(3 * 1000)
            network.runNetwork()
            accounts = listOf(account1, account2, account3)
            recurringPayments = listOf(recurringPayment1, recurringPayment2, recurringPayment3)
            recurringPaymentLogs = recurringPayments.map {
                it.linearId.id to recurringPaymentLogRepository.getRecurringPaymentLogsForRecurringPayment(it.linearId.id)
            }.toMap()
        }
    }

    @Test
    @Order(2)
    fun `test persistRecurringPaymentLog happy path`() {

        val logId = "[zzz47b63-c74e-455f-b4cd-c39399003481]:1"
        val recurringPayment = recurringPaymentLogs.values.flatten()[1].recurringPayment
        val recurringPaymentLog = RecurringPaymentLogSchemaV1.RecurringPaymentLog(logId, recurringPayment, Instant.now(), null)
        recurringPaymentLogRepository.persistRecurringPaymentLog(recurringPaymentLog)
        val fetched = recurringPaymentLogRepository.getRecurringPaymentLogById(logId)

        assertEquals(fetched, recurringPaymentLog)
    }

    @Test
    @Order(1)
    fun `test getLastSuccessLogByRecurringPayment happy path`() {

        val recurringPayment = recurringPaymentLogs.map { it.value[0].recurringPayment }[0]
        val fetched = recurringPaymentLogRepository.getLastSuccessLogByRecurringPayment(recurringPayment)

        assertEquals(recurringPaymentLogs[recurringPayment.linearId]?.get(0), fetched)
    }

    @Test
    @Order(1)
    fun `test getLastSuccessLogByRecurringPayment fails with empty list`() {

        val recurringPayment = RecurringPaymentSchemaV1.RecurringPayment(UUID.randomUUID(), UUID.randomUUID(),
                1000L, EUR.currencyCode, Instant.now(), Duration.ofSeconds(1), 2, UUID.randomUUID())
        val message = assertFailsWith<NoSuchElementException> {
            recurringPaymentLogRepository.getLastSuccessLogByRecurringPayment(recurringPayment)
        }.message!!
        assertEquals("List is empty.", message)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentLogsForRecurringPaymentId happy path`() {

        val recurringPayment = recurringPaymentLogs.map { it.value[0].recurringPayment }[0]
        val fetchedRecurringPaymentLogs = recurringPaymentLogRepository.getRecurringPaymentLogsForRecurringPayment(recurringPayment.linearId)

        assertEquals(1, fetchedRecurringPaymentLogs.count())
        assertEquals(recurringPaymentLogs[recurringPayment.linearId], fetchedRecurringPaymentLogs)
    }


    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success page one no search term no date filtering`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "logId", RepositoryQueryParams.SortOrder.ASC, "")
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams, null, null)

        validatePaginationDetails(response, repositoryQueryParams)
        assertEquals(recurringPaymentLogs.values.flatten().sortedBy { it.logId }, response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated happy path unfiltered page two`() {

        val repositoryQueryParams = RepositoryQueryParams(2, 3,
                "logId", RepositoryQueryParams.SortOrder.ASC, "")
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams, null, null)

        validatePaginationDetails(response, repositoryQueryParams, expectedPageCount = 2, expectedTotalPages = 2)
        assertEquals(recurringPaymentLogs.values.flatten().sortedBy { it.logId }.subList(3, 5), response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success empty set for unexistent page`() {
        val repositoryQueryParams = RepositoryQueryParams(3, 10,
                "logId", RepositoryQueryParams.SortOrder.ASC, "")
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams, null, null)

        validatePaginationDetails(response, repositoryQueryParams, expectedPageCount = 0)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success empty set no search term with date filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(3, 10,
                "logId", RepositoryQueryParams.SortOrder.ASC, "")
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams, Instant.now(), Instant.now())

        validatePaginationDetails(response, repositoryQueryParams, expectedTotalCount = 0,
                expectedPageCount = 0, expectedTotalPages = 0)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success full set no search term with date filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "logId", RepositoryQueryParams.SortOrder.ASC, "")
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams,
                Instant.now().minusSeconds(10), Instant.now().plusSeconds(10))

        validatePaginationDetails(response, repositoryQueryParams)
        assertEquals(recurringPaymentLogs.values.flatten().sortedBy { it.logId }, response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success full set no search term with dateFrom filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, "")
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams,
                recurringPaymentLogs.values.flatten().sortedBy { it.txDate }[2].txDate, null)

        validatePaginationDetails(response, repositoryQueryParams, 3, 3)
        assertEquals(recurringPaymentLogs.values.flatten().sortedBy { it.txDate }.subList(2, 5), response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success full set no search term with dateTo filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, "")
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams,
                null, recurringPaymentLogs.values.flatten().sortedBy { it.txDate }[3].txDate)

        validatePaginationDetails(response, repositoryQueryParams, 4, 4)
        assertEquals(recurringPaymentLogs.values.flatten().sortedBy { it.txDate }.subList(0, 4), response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success with full account id filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, accounts[1].accountData.accountId.toString())
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams,
                null, null)

        validatePaginationDetails(response, repositoryQueryParams, 3, 3)
        assertEquals(recurringPaymentLogs.values.flatten().filter {
            it.recurringPayment.accountFrom == accounts[1].accountData.accountId
                    || it.recurringPayment.accountTo == accounts[1].accountData.accountId
        }.sortedBy { it.txDate }, response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success with partial account id filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, accounts[2].accountData.accountId.toString().substring(0, 15))
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams,
                null, null)

        validatePaginationDetails(response, repositoryQueryParams, 4, 4)
        assertEquals(recurringPaymentLogs.values.flatten().filter {
            it.recurringPayment.accountFrom == accounts[2].accountData.accountId
                    || it.recurringPayment.accountTo == accounts[2].accountData.accountId
        }.sortedBy { it.txDate }, response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success with partial recurring payment filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, recurringPayments[2].linearId.id.toString().substring(0, 15))
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams,
                null, null)

        validatePaginationDetails(response, repositoryQueryParams, 2, 2)
        assertEquals(recurringPaymentLogs[recurringPayments[2].linearId.id], response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success with partial log id filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, recurringPaymentLogs.values.flatten()[3].logId!!.substring(0, 15))
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams,
                null, null)

        validatePaginationDetails(response, repositoryQueryParams, 1, 1)
        assertEquals(recurringPaymentLogs.values.flatten().subList(3, 4), response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success with error filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, "Insufficient balance")
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams,
                null, null)

        validatePaginationDetails(response, repositoryQueryParams, 4, 4)
        assertEquals(recurringPaymentLogs.values.flatten().sortedBy { it.txDate }.filter {
            it.recurringPayment.linearId != recurringPayments[0].linearId.id }, response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated success with dateStart sorting`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10, "dateStart", RepositoryQueryParams.SortOrder.ASC,
                "")
        val response = recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams,
                null, null)

        validatePaginationDetails(response, repositoryQueryParams, 5, 5)
        assertEquals(recurringPaymentLogs.values.flatten().sortedBy { it.recurringPayment.dateStart }, response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsPaginated fails with unexistent search column`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5, "testtest", RepositoryQueryParams.SortOrder.ASC,
                "")

        val message = assertFailsWith<IllegalArgumentException> {
            recurringPaymentLogRepository.getRecurringPaymentsPaginated(repositoryQueryParams,
                    null, null)
        }.message!!
        assertEquals("Invalid sort field testtest", message)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsForAccountPaginated happy path unfiltered`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
        "txDate", RepositoryQueryParams.SortOrder.ASC, "")
        val response = recurringPaymentLogRepository.getRecurringPaymentsForAccountPaginated(repositoryQueryParams,
                accounts[0].accountData.accountId, null, null)

        validatePaginationDetails(response, repositoryQueryParams, 3, 3)
        assertEquals(recurringPaymentLogs.values.flatten().filter {
            it.recurringPayment.accountFrom == accounts[0].accountData.accountId
                    || it.recurringPayment.accountTo == accounts[0].accountData.accountId
        }.sortedBy { it.txDate }, response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsForAccountPaginated happy path empty set for error`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, "Insufficient balance, missing 12.00")
        val response = recurringPaymentLogRepository.getRecurringPaymentsForAccountPaginated(repositoryQueryParams,
                accounts[0].accountData.accountId, null, null)

        validatePaginationDetails(response, repositoryQueryParams, 0, 0, 0)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsForAccountPaginated happy path filtered with error`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, "Insufficient balance, missing 10.00")
        val response = recurringPaymentLogRepository.getRecurringPaymentsForAccountPaginated(repositoryQueryParams,
                accounts[0].accountData.accountId, null, null)

        validatePaginationDetails(response, repositoryQueryParams, 2, 2)
        assertEquals(recurringPaymentLogs.values.flatten().filter {
            it.recurringPayment.accountFrom == accounts[0].accountData.accountId
                    || it.recurringPayment.accountTo == accounts[0].accountData.accountId
        }.filter { !it.error.isNullOrBlank() }.sortedBy { it.txDate }, response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsForAccountPaginated happy path empty set unexistent accountId`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 5, "testtest",
                RepositoryQueryParams.SortOrder.ASC, "")

        val message = assertFailsWith<IllegalArgumentException> {
            recurringPaymentLogRepository.getRecurringPaymentsForAccountPaginated(repositoryQueryParams,
                    accounts[0].accountData.accountId, null, null)
        }.message!!
        assertEquals("Invalid sort field testtest", message)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsForCustomerPaginated happy path unfiltered`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, "")
        val customerId = accounts[0].accountData.customerId
        val response = recurringPaymentLogRepository.getRecurringPaymentsForCustomerPaginated(repositoryQueryParams, customerId,
                null, null)

        validatePaginationDetails(response, repositoryQueryParams, 3, 3)
        val accountsFiltered = accounts.filter { it.accountData.customerId == customerId }.map { it.accountData.accountId }
        assertEquals(recurringPaymentLogs.values.flatten().filter { accountsFiltered.contains(it.recurringPayment.accountFrom)
                || accountsFiltered.contains(it.recurringPayment.accountTo)
        }.sortedBy { it.txDate }, response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsForCustomerPaginated happy path empty set for error`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, "Insufficient balance, missing 12.00")
        val response = recurringPaymentLogRepository.getRecurringPaymentsForCustomerPaginated(repositoryQueryParams,
                accounts[0].accountData.customerId, null, null)

        validatePaginationDetails(response, repositoryQueryParams, 0, 0, 0)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsForCustomerPaginated happy path filtered with error`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.ASC, "Insufficient balance, missing 10.00")
        val customerId = accounts[0].accountData.customerId
        val response = recurringPaymentLogRepository.getRecurringPaymentsForCustomerPaginated(repositoryQueryParams, customerId,
                null, null)

        validatePaginationDetails(response, repositoryQueryParams, 2, 2)
        val accountsFiltered = accounts.filter { it.accountData.customerId == customerId }.map { it.accountData.accountId }
        assertEquals(recurringPaymentLogs.values.flatten().filter { accountsFiltered.contains(it.recurringPayment.accountFrom)
                || accountsFiltered.contains(it.recurringPayment.accountTo)
        }.filter { !it.error.isNullOrBlank() }.sortedBy { it.txDate }, response.result)
    }

    @Test
    @Order(1)
    fun `test getRecurringPaymentsForCustomerPaginated happy path empty set unexistent sortField`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 5, "testtest",
                RepositoryQueryParams.SortOrder.ASC, "")

        val message = assertFailsWith<IllegalArgumentException> {
            recurringPaymentLogRepository.getRecurringPaymentsForCustomerPaginated(repositoryQueryParams,
                    accounts[0].accountData.accountId, null, null)
        }.message!!
        assertEquals("Invalid sort field testtest", message)
    }

    private fun validatePaginationDetails(response: PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>,
                                          repositoryQueryParams: RepositoryQueryParams,
                                          expectedTotalCount: Long = 5, expectedPageCount: Int = 5,
                                          expectedTotalPages: Int = 1) {

        assertEquals(expectedTotalCount, response.totalResults)
        assertEquals(repositoryQueryParams.pageSize, response.pageSize)
        assertEquals(expectedTotalPages, response.totalPages)
        assertEquals(repositoryQueryParams.startPage, response.pageNumber)
        assertEquals(expectedPageCount, response.result.count())
    }
}