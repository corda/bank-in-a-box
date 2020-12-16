package com.r3.refapp.client.controllers

import com.r3.refapp.client.NodeRPCConnection
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.flows.reports.GetTransactionsForCustomerPaginatedFlow
import com.r3.refapp.flows.reports.GetTransactionByIdFlow
import com.r3.refapp.flows.reports.GetTransactionsForAccountPaginatedFlow
import com.r3.refapp.flows.reports.GetTransactionsPaginatedFlow
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.repositories.TransactionLogRepository
import com.r3.refapp.schemas.TransactionLogSchemaV1
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*


/**
 * Provides Transactions API endpoints.
 */
@RestController
@RequestMapping("/transactions") // The paths for HTTP requests are relative to this base path.
class TransactionController(rpc: NodeRPCConnection){

    companion object {
        private val logger = LoggerFactory.getLogger(TransactionController::class.java)
    }
    private val proxy = rpc.proxy

    /**
     * Retrieves transactions paginated based on the query parameters
     * @param startPage position of the result page to retrieve
     * @param pageSize specifies the maximum number of transaction in one page
     * @param searchTerm can be matched in LIKE fashion against multiple fields, for full list @see [TransactionLogRepository]
     * @param sortField sort result based on this field
     * @param sortOrder order of the sort (ASC or DESC) [RepositoryQueryParams.SortOrder]
     * @param dateFrom (Optional) parameter to filter transactions with txDate after given date
     * @param dateTo (Optional) parameter to filter transactions with txDate before given date
     * @return [PaginatedResponse<TransactionLogSchemaV1.TransactionLog>]
     */
    @GetMapping(value = [""], produces = ["application/json"])
    private fun getTransactions(@RequestParam startPage: Int = 1, @RequestParam pageSize: Int = 10,
                                @RequestParam sortField: String? = null, @RequestParam sortOrder: RepositoryQueryParams.SortOrder?,
                                @RequestParam searchTerm: String = "", @RequestParam dateFrom: Instant?,
                                @RequestParam dateTo: Instant?
    ): PaginatedResponse<TransactionLogSchemaV1.TransactionLog> {

        val queryParam = RepositoryQueryParams(startPage, pageSize, sortField, sortOrder ?: RepositoryQueryParams.SortOrder.ASC,
                searchTerm)

        return proxy.startFlow (::GetTransactionsPaginatedFlow, queryParam, dateFrom, dateTo)
                    .returnValue.getOrThrow()
    }

    /**
     * Retrieves transactions of a customer paginated based on the query parameters
     * @param startPage position of the result page to retrieve
     * @param pageSize specifies the maximum number of transactions in one page
     * @param searchTerm can be matched in LIKE fashion against multiple fields, for full list @see [TransactionLogRepository]
     * @param sortField sort result based on this field
     * @param sortOrder order of the sort (ASC or DESC) [RepositoryQueryParams.SortOrder]
     * @param customerId transactions of specified customer, passed a url parameter
     * @param dateFrom (Optional) parameter to filter transactions with txDate after given date
     * @param dateTo (Optional) parameter to filter transactions with txDate before given date
     * @return [PaginatedResponse<TransactionLogSchemaV1.TransactionLog>]
     */
    @GetMapping(value = ["/customer/{customerId}"], produces = ["application/json"])
    private fun getCustomerTransactions(@PathVariable(name="customerId") customerId: UUID,
                                        @RequestParam startPage: Int = 1, @RequestParam pageSize: Int = 10,
                                        @RequestParam sortField: String? = null, @RequestParam sortOrder: RepositoryQueryParams.SortOrder?,
                                        @RequestParam searchTerm: String = "",
                                        @RequestParam dateFrom: Instant?, @RequestParam dateTo: Instant?
    ): PaginatedResponse<TransactionLogSchemaV1.TransactionLog> {
        val queryParam = RepositoryQueryParams(startPage, pageSize, sortField, sortOrder ?: RepositoryQueryParams.SortOrder.ASC,
                searchTerm)

        return proxy.startFlow(::GetTransactionsForCustomerPaginatedFlow, queryParam, customerId, dateFrom, dateTo)
                .returnValue.getOrThrow()
    }

    /**
     * Retrieves transactions of an account paginated based on the query parameters
     * @param startPage position of the result page to retrieve
     * @param pageSize specifies the maximum number of transactions in one page
     * @param searchTerm can be matched in LIKE fashion against multiple fields, for full list @see [TransactionLogRepository]
     * @param sortField sort result based on this field
     * @param sortOrder order of the sort (ASC or DESC) [RepositoryQueryParams.SortOrder]
     * @param accountId transactions will contain this account id as to- or fromAccount
     * @param dateFrom (Optional) parameter to filter transactions with txDate after given date
     * @param dateTo (Optional) parameter to filter transactions with txDate before given date
     * @return [PaginatedResponse<TransactionLogSchemaV1.TransactionLog>]
     */
    @GetMapping(value = ["/account/{accountId}"], produces = ["application/json"])
    private fun getAccountTransactions(@PathVariable(name="accountId") accountId: UUID,
                                       @RequestParam startPage: Int = 1, @RequestParam pageSize: Int = 10,
                                       @RequestParam sortField: String? = null, @RequestParam sortOrder: RepositoryQueryParams.SortOrder?,
                                       @RequestParam searchTerm: String = "",
                                       @RequestParam dateFrom: Instant?, @RequestParam dateTo: Instant?
    ): PaginatedResponse<TransactionLogSchemaV1.TransactionLog> {
        val queryParam = RepositoryQueryParams(startPage, pageSize, sortField, sortOrder ?: RepositoryQueryParams.SortOrder.ASC,
                searchTerm)

        return proxy.startFlow (::GetTransactionsForAccountPaginatedFlow, queryParam, accountId, dateFrom, dateTo)
                .returnValue.getOrThrow()
    }

    /**
     * Retrieves transaction based in transaction id
     * @param transactionId id of the transaction
     * @return [TransactionLogSchemaV1.TransactionLog]
     */
    @GetMapping(value = ["/{transactionId}"], produces = ["application/json"])
    private fun getTransactionById(@PathVariable(value = "transactionId")  transactionId: String): TransactionLogSchemaV1.TransactionLog {
        return proxy.startFlow (::GetTransactionByIdFlow, transactionId)
                .returnValue.getOrThrow()
    }
}