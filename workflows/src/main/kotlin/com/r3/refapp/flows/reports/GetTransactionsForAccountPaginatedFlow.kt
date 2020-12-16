package com.r3.refapp.flows.reports

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.repositories.TransactionLogRepository
import com.r3.refapp.schemas.TransactionLogSchemaV1
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.time.Instant
import java.util.*

/**
 * Public API flow used for reporting and views. Flow retrieves transactions paginated for given [repositoryQueryParams],
 * [accountId], [dateFrom] and [dateTo]. [RepositoryQueryParams.searchTerm] can be matched in LIKE fashion against multiple fields,
 * for full list @see [TransactionLogRepository]. Result set can be sorted based on [RepositoryQueryParams.sortField] and
 * [RepositoryQueryParams.sortOrder] values against all fields in [TransactionLogSchemaV1.TransactionLog].
 * @param repositoryQueryParams [RepositoryQueryParams] object that holds searchTerm, pagination and sorting data
 * @param accountId Id of the account which will be matched against accountFrom and accountTo fields of the
 * [TransactionLogSchemaV1.TransactionLog]
 * @param dateFrom (Optional) parameter to filter accounts with txDate after given date
 * @param dateTo (Optional) parameter to filter accounts with txDate before given date
 */
@StartableByRPC
@InitiatingFlow
class GetTransactionsForAccountPaginatedFlow(val repositoryQueryParams: RepositoryQueryParams,
                                             val accountId: UUID,
                                             val dateFrom: Instant?, val dateTo: Instant?)
    : FlowLogic<PaginatedResponse<TransactionLogSchemaV1.TransactionLog>>() {

    @Suspendable
    override fun call(): PaginatedResponse<TransactionLogSchemaV1.TransactionLog> {

        val transactionLogRepository = serviceHub.cordaService(TransactionLogRepository::class.java)
        return transactionLogRepository.getTransactionsForAccountPaginated(repositoryQueryParams, accountId, dateFrom, dateTo)
    }
}