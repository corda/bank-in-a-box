package com.r3.refapp.flows.reports

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.repositories.TransactionLogRepository
import com.r3.refapp.schemas.TransactionLogSchemaV1
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import java.time.Instant
import java.util.*

/**
 * Public API flow used for reporting and views. Flow retrieves all transactions for given [customerId] with txDate between
 * [dateStart] and [dateEnd].
 * @param queryParams pagination parameters
 * @param customerId Id of the customer
 * @param dateStart txDate from
 * @param dateEnd txDate to
 * @return Returns list of transaction logs [List<TransactionLogSchemaV1.TransactionLog>] for customer and period
 */
@StartableByRPC
class GetTransactionsForCustomerPaginatedFlow(val queryParams: RepositoryQueryParams, val customerId: UUID, val dateStart: Instant?, val dateEnd: Instant?)
    : FlowLogic<PaginatedResponse<TransactionLogSchemaV1.TransactionLog>>() {

    @Suspendable
    override fun call(): PaginatedResponse<TransactionLogSchemaV1.TransactionLog> {

        val transactionLogRepository = serviceHub.cordaService(TransactionLogRepository::class.java)
        return transactionLogRepository.getTransactionLogsForCustomerAndBetweenTime(queryParams, customerId,
                dateStart, dateEnd)
    }
}