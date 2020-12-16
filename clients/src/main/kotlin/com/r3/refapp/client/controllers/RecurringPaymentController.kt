package com.r3.refapp.client.controllers

import com.r3.refapp.client.NodeRPCConnection
import com.r3.refapp.client.response.RecurringPaymentResponse
import com.r3.refapp.client.utils.mapToRecurringPaymentResponse
import com.r3.refapp.client.utils.toResponse
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.flows.reports.GetRecurringPaymentByIdFlow
import com.r3.refapp.flows.reports.GetRecurringPaymentsForCustomerPaginatedFlow
import com.r3.refapp.flows.reports.GetRecurringPaymentsForAccountPaginatedFlow
import com.r3.refapp.flows.reports.GetRecurringPaymentsPaginatedFlow
import com.r3.refapp.repositories.RecurringPaymentLogRepository
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.schemas.RecurringPaymentSchemaV1
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/recurring-payments") // The paths for HTTP requests are relative to this base path.
class RecurringPaymentController(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RecurringPaymentController::class.java)
    }

    private val proxy = rpc.proxy

    /**
     * Queries recurring payments with given [recurringPaymentId].
     * @param recurringPaymentId ID of the recurring payment
     * @return [RecurringPaymentResponse] object with given [recurringPaymentId]
     */
    @GetMapping(value = ["/{recurringPaymentId}"], produces = ["application/json"])
    fun getRecurringPaymentById(@PathVariable recurringPaymentId: UUID): RecurringPaymentResponse {
        return proxy.startFlow(::GetRecurringPaymentByIdFlow, recurringPaymentId).returnValue.getOrThrow().toResponse()
    }

    /**
     * Retrieves filtered recurring payments in paginated form.
     * @param startPage position of the result page to retrieve
     * @param pageSize specifies the maximum number of customers in one page
     * @param searchTerm can be matched in LIKE fashion against multiple fields, for full list @see [RecurringPaymentLogRepository]
     * @param sortField sort result based on this field
     * @param sortOrder order of the sort (ASC or DESC) [RepositoryQueryParams.SortOrder]
     * @param dateFrom (Optional) parameter to filter recurring payments with txDate after given date
     * @param dateTo (Optional) parameter to filter recurring payments with txDate before given date
     * @return [PaginatedResponse] which contains list of [RecurringPaymentResponse] objects
     */
    @GetMapping(value = [""], produces = ["application/json"])
    fun getRecurringPaymentsPaginated(@RequestParam startPage: Int = 1, @RequestParam pageSize: Int = 10,
                                      @RequestParam sortField: String? = null, @RequestParam sortOrder: RepositoryQueryParams.SortOrder?,
                                      @RequestParam searchTerm: String = "", @RequestParam dateFrom: Instant?,
                                      @RequestParam dateTo: Instant?):
            PaginatedResponse<RecurringPaymentResponse> {

        val queryParam = RepositoryQueryParams(startPage, pageSize, sortField, sortOrder ?: RepositoryQueryParams.SortOrder.ASC,
                searchTerm)
        return proxy.startFlow(::GetRecurringPaymentsPaginatedFlow, queryParam, dateFrom, dateTo).returnValue.getOrThrow()
                .mapToRecurringPaymentResponse()
    }

    /**
     * Retrieves filtered recurring payments for given [accountId] in paginated form.
     * @param startPage position of the result page to retrieve
     * @param pageSize specifies the maximum number of customers in one page
     * @param searchTerm can be matched in LIKE fashion against multiple fields, for full list @see [RecurringPaymentLogRepository]
     * @param sortField sort result based on this field
     * @param sortOrder order of the sort (ASC or DESC) [RepositoryQueryParams.SortOrder]
     * @param dateFrom (Optional) parameter to filter recurring payments with txDate after given date
     * @param dateTo (Optional) parameter to filter recurring payments with txDate before given date
     * @param accountId ID of the account which will be matched against accountFrom and accountTo fields of
     * [RecurringPaymentSchemaV1.RecurringPayment]
     * @return [PaginatedResponse] which contains list of [RecurringPaymentResponse] objects
     */
    @GetMapping(value = ["account/{accountId}"], produces = ["application/json"])
    fun getRecurringPaymentsForAccountPaginated(@RequestParam startPage: Int = 1, @RequestParam pageSize: Int = 10,
                                                @RequestParam sortField: String? = null, @RequestParam sortOrder: RepositoryQueryParams.SortOrder?,
                                                @RequestParam searchTerm: String = "", @RequestParam dateFrom: Instant?,
                                                @RequestParam dateTo: Instant?, @PathVariable accountId: UUID):
            PaginatedResponse<RecurringPaymentResponse> {

        val queryParam = RepositoryQueryParams(startPage, pageSize, sortField, sortOrder ?: RepositoryQueryParams.SortOrder.ASC,
                searchTerm)
        return proxy.startFlow(::GetRecurringPaymentsForAccountPaginatedFlow, queryParam,  accountId, dateFrom, dateTo).returnValue.getOrThrow()
                .mapToRecurringPaymentResponse()
    }

    /**
     * Retrieves filtered recurring payments for given [customerId] in paginated form.
     * @param startPage position of the result page to retrieve
     * @param pageSize specifies the maximum number of customers in one page
     * @param searchTerm can be matched in LIKE fashion against multiple fields, for full list @see [RecurringPaymentLogRepository]
     * @param sortField sort result based on this field
     * @param sortOrder order of the sort (ASC or DESC) [RepositoryQueryParams.SortOrder]
     * @param dateFrom (Optional) parameter to filter recurring payments with txDate after given date
     * @param dateTo (Optional) parameter to filter recurring payments with txDate before given date
     * @param customerId ID of the customer which will be matched against accountFrom and accountTo owners of
     * [RecurringPaymentSchemaV1.RecurringPayment]
     * @return [PaginatedResponse] which contains list of [RecurringPaymentResponse] objects
     */
    @GetMapping(value = ["customer/{customerId}"], produces = ["application/json"])
    fun getRecurringPaymentsForCustomerPaginated(@RequestParam startPage: Int = 1, @RequestParam pageSize: Int = 10,
                                                 @RequestParam sortField: String? = null, @RequestParam sortOrder: RepositoryQueryParams.SortOrder?,
                                                 @RequestParam searchTerm: String = "", @RequestParam dateFrom: Instant?,
                                                 @RequestParam dateTo: Instant?, @PathVariable customerId: UUID):
            PaginatedResponse<RecurringPaymentResponse> {

        val queryParam = RepositoryQueryParams(startPage, pageSize, sortField, sortOrder ?: RepositoryQueryParams.SortOrder.ASC,
                searchTerm)
        return proxy.startFlow(::GetRecurringPaymentsForCustomerPaginatedFlow, queryParam, customerId, dateFrom, dateTo).returnValue.getOrThrow()
                .mapToRecurringPaymentResponse()
    }
}