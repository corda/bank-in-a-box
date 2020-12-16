package com.r3.refapp.flows.reports

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.schemas.CustomerSchemaV1
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

/**
 * Return a paginated list of all [Customer]
 * @param queryParams [RepositoryQueryParams] object that holds a repository query's possible parameters.
 * @return returns List of [Customer]
 */
@StartableByRPC
class GetCustomersPaginatedFlow(private val queryParams: RepositoryQueryParams) : FlowLogic<PaginatedResponse<CustomerSchemaV1.Customer>>() {
    @Suspendable
    override fun call(): PaginatedResponse<CustomerSchemaV1.Customer> {
        val accountRepository: AccountRepository = serviceHub.cordaService(AccountRepository::class.java)
        return accountRepository.getCustomersPaginated(queryParams)
    }
}