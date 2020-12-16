package com.r3.refapp.flows.reports

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.schemas.CustomerSchemaV1
import com.r3.refapp.states.Account
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.time.Instant

/**
 * Public API flow used for reporting and views. Flow retrieves accounts and associated customers paginated,
 * for given [repositoryQueryParams], [dateFrom] and [dateTo]. [RepositoryQueryParams.searchTerm] can be matched
 * in LIKE fashion against multiple fields, for full list @see [AccountRepository]. Result set can be sorted
 * based on [RepositoryQueryParams.sortField] and [RepositoryQueryParams.sortOrder] values against all fields
 * in [Account] and [CustomerSchemaV1.Customer].
 * @param repositoryQueryParams [RepositoryQueryParams] object that holds searchTerm, pagination and sorting data
 * @param dateFrom (Optional) parameter to filter accounts with txDate after given date
 * @param dateTo (Optional) parameter to filter accounts with txDate before given date
 */
@StartableByRPC
@InitiatingFlow
class GetAccountsPaginatedFlow(
        val repositoryQueryParams: RepositoryQueryParams,
        val dateFrom: Instant?,
        val dateTo: Instant?) : FlowLogic<PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>>>() {

    @Suspendable
    override fun call(): PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>> {

        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        return accountRepository.getAccountsPaginated(repositoryQueryParams, dateFrom, dateTo)
    }
}