package com.r3.refapp.flows.reports

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.repositories.RecurringPaymentLogRepository
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.schemas.RecurringPaymentLogSchemaV1
import com.r3.refapp.schemas.RecurringPaymentSchemaV1
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.time.Instant
import java.util.*

/**
 * Public API flow used for reporting and views. Flow retrieves recurring payments paginated for given
 * [repositoryQueryParams], [customerId], [dateFrom] and [dateTo]. [RepositoryQueryParams.searchTerm] can be matched in
 * LIKE fashion against multiple fields, for full list @see [RecurringPaymentLogRepository]. Result set can be sorted
 * based on [RepositoryQueryParams.sortField] and [RepositoryQueryParams.sortOrder] values against all fields in
 * [RecurringPaymentLogSchemaV1.RecurringPaymentLog] and [RecurringPaymentSchemaV1.RecurringPayment].
 * @param repositoryQueryParams [RepositoryQueryParams] object that holds searchTerm, pagination and sorting data
 * @param customerId Id of the customer which will be matched against accountFrom and accountTo owner fields of the
 * [RecurringPaymentSchemaV1.RecurringPayment]
 * @param dateFrom (Optional) parameter to filter accounts with txDate after given date
 * @param dateTo (Optional) parameter to filter accounts with txDate before given date
 */
@InitiatingFlow
@StartableByRPC
class GetRecurringPaymentsForCustomerPaginatedFlow(val repositoryQueryParams: RepositoryQueryParams, val customerId: UUID,
                                                   val dateFrom: Instant?, val dateTo: Instant?)
    : FlowLogic<PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>>() {

    @Suspendable
    override fun call(): PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog> {
        val recurringPaymentLogRepository = serviceHub.cordaService(RecurringPaymentLogRepository::class.java)
        return recurringPaymentLogRepository.getRecurringPaymentsForCustomerPaginated(repositoryQueryParams, customerId,
                dateFrom, dateTo)
    }
}