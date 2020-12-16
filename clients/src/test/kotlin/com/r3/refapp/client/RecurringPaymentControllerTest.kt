package com.r3.refapp.client

import com.r3.refapp.client.controllers.RecurringPaymentController
import com.r3.refapp.client.response.RecurringPaymentResponse
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.flows.reports.GetRecurringPaymentByIdFlow
import com.r3.refapp.flows.reports.GetRecurringPaymentsForAccountPaginatedFlow
import com.r3.refapp.flows.reports.GetRecurringPaymentsForCustomerPaginatedFlow
import com.r3.refapp.flows.reports.GetRecurringPaymentsPaginatedFlow
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.schemas.RecurringPaymentLogSchemaV1
import com.r3.refapp.schemas.RecurringPaymentSchemaV1
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.test.utils.getFlowHandle
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.FlowHandle
import net.corda.core.messaging.startFlow
import net.corda.testing.core.TestIdentity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@RunWith(MockitoJUnitRunner::class)
class RecurringPaymentControllerTest {

    @Mock
    lateinit var nodeRPCConnection: NodeRPCConnection

    @Mock
    lateinit var proxy: CordaRPCOps

    lateinit var recurringPaymentController: RecurringPaymentController

    private val accountFromId = UUID.randomUUID()
    private val accountToId = UUID.randomUUID()
    private val customerId = UUID.randomUUID()
    private val testIdentity = TestIdentity(CordaX500Name("alice", "New York", "US")).party
    private val startDate = Instant.now()
    private val recurringPaymentId = UniqueIdentifier(null, UUID.randomUUID())
    private val recurringPaymentLogId = "[ee55409e-8500-43a4-8572-d2c0d705eb93]:"

    @Before
    fun setUp() {
        Mockito.`when`(nodeRPCConnection.proxy).thenReturn(proxy)
        recurringPaymentController = RecurringPaymentController(nodeRPCConnection)
    }

    @Test
    fun `test getRecurringPaymentById happy path`() {
        Mockito.`when`(proxy.startFlow(::GetRecurringPaymentByIdFlow, recurringPaymentId.id)).thenReturn(prepareState())
        val response = recurringPaymentController.getRecurringPaymentById(recurringPaymentId.id)

        validateResponseObject(response, null, null, null)
    }

    @Test
    fun `test getRecurringPaymentsPaginated happy path`() {
        Mockito.`when`(proxy.startFlow(::GetRecurringPaymentsPaginatedFlow, prepareQueryParameters(), null, null))
                .thenReturn(preparePaginatedResponse())
        val response = recurringPaymentController.getRecurringPaymentsPaginated(1, 10, null,
                RepositoryQueryParams.SortOrder.ASC, "",null, null)

        assertEquals(1, response.pageNumber)
        assertEquals(10, response.pageSize)
        assertEquals(1, response.totalPages)
        assertEquals(5, response.totalResults)
        assertEquals(5, response.result.count())
        response.result.forEach {
            val index = response.result.indexOf(it)
            validateResponseObject(it, index, "Exception$index","$recurringPaymentLogId$index")
        }
    }

    @Test
    fun `test getRecurringPaymentsForAccountPaginated happy path`() {
        Mockito.`when`(proxy.startFlow(::GetRecurringPaymentsForAccountPaginatedFlow, prepareQueryParameters(), accountFromId, null,
                null)).thenReturn(preparePaginatedResponse())
        val response = recurringPaymentController.getRecurringPaymentsForAccountPaginated(1, 10, null,
                RepositoryQueryParams.SortOrder.ASC, "",null, null, accountFromId)

        assertEquals(1, response.pageNumber)
        assertEquals(10, response.pageSize)
        assertEquals(1, response.totalPages)
        assertEquals(5, response.totalResults)
        assertEquals(5, response.result.count())
        response.result.forEach {
            val index = response.result.indexOf(it)
            validateResponseObject(it, index, "Exception$index","$recurringPaymentLogId$index")
        }
    }

    @Test
    fun `test getRecurringPaymentsForCustomerPaginated happy path`() {
        Mockito.`when`(proxy.startFlow(::GetRecurringPaymentsForCustomerPaginatedFlow, prepareQueryParameters(),
                customerId, null, null)).thenReturn(preparePaginatedResponse())
        val response = recurringPaymentController.getRecurringPaymentsForCustomerPaginated(1, 10, null,
                RepositoryQueryParams.SortOrder.ASC, "",null, null, customerId)

        assertEquals(1, response.pageNumber)
        assertEquals(10, response.pageSize)
        assertEquals(1, response.totalPages)
        assertEquals(5, response.totalResults)
        assertEquals(5, response.result.count())
        response.result.forEach {
            val index = response.result.indexOf(it)
            validateResponseObject(it, index, "Exception$index","$recurringPaymentLogId$index")
        }
    }

    private fun validateResponseObject(response: RecurringPaymentResponse, iterationNum: Int?, error: String?, logId: String?) {
        assertEquals(accountFromId, response.accountFrom)
        assertEquals(accountToId, response.accountTo)
        assertEquals(10000, response.amount)
        assertEquals("EUR", response.currencyCode)
        assertNotNull(response.dateStart)
        assertEquals("10 days", response.period)
        assertEquals(iterationNum, response.iterationNum)
        assertEquals(recurringPaymentId.id, response.recurringPaymentId)
        assertEquals(error, response.error)
        assertEquals(logId, response.logId)
    }

    private fun prepareState() : FlowHandle<RecurringPaymentState> {
        val state = RecurringPaymentState(
                accountFrom = accountFromId,
                accountTo = accountToId,
                amount = 100 of EUR,
                dateStart = startDate,
                period = Duration.ofDays(10),
                iterationNum = null,
                owningParty = testIdentity,
                linearId = recurringPaymentId)

        return getFlowHandle<RecurringPaymentState>(state)
    }

    private fun preparePaginatedResponse() : FlowHandle<PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>> {
        val responseList = (0..4).toList().reversed().map {
            val txDate = Instant.now()
            val recurringPayment = RecurringPaymentSchemaV1.RecurringPayment(accountFromId, accountToId, 10000,
                    "EUR", txDate, Duration.ofDays(10), it, recurringPaymentId.id)
            RecurringPaymentLogSchemaV1.RecurringPaymentLog("$recurringPaymentLogId$it",
                    recurringPayment, txDate, "Exception$it")
        }
        val paginatedResponse = PaginatedResponse(responseList.reversed(), 5, 10, 1, 1)

        return getFlowHandle<PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>>(paginatedResponse)
    }

    private fun prepareQueryParameters() =
            RepositoryQueryParams(1, 10, null, RepositoryQueryParams.SortOrder.ASC, "")
}