package com.r3.refapp.repositories

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.schemas.AccountStateSchemaV1
import com.r3.refapp.schemas.CustomerSchemaV1
import com.r3.refapp.schemas.CustomerSchemaV1.Customer
import com.r3.refapp.states.Account
import com.r3.refapp.states.CreditAccount
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.states.LoanAccountState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.schemas.PersistentStateRef
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.contextLogger
import java.security.PublicKey
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.NoResultException

/**
 * Provide services to query [AccountStateSchemaV1.PersistentBalance].
 */
@CordaService
class AccountRepository(val serviceHub: ServiceHub) : SingletonSerializeAsToken() {

    companion object {
        val logger = contextLogger()
        val allowedAccountSortFields = getAccountSortFields()
        val allowedCustomerSortFields = getCustomerSortFields()

        /**
         * Return a list of Account sort fields generated from [AccountStateSchemaV1.PersistentBalance] member names.
         * These members are annotated with @Column and have a column in the database
         * @return [List<String>] available sort fields
         */
        private fun getAccountSortFields(): List<String> {
            return AccountStateSchemaV1.PersistentBalance::class.java.declaredFields
                    .filter{ it.isAnnotationPresent(Column::class.java) }
                    .map { it.name }
        }

        /**
         * Return a list of Customer sort fields generated from [Customer] member names.
         * These members are annotated with @Column and have a column in the database
         * @return [List<String>] available sort fields
         */
        private fun getCustomerSortFields(): List<String> {
            return Customer::class.java.declaredFields
                    .filter{ it.isAnnotationPresent(Column::class.java) } // can only sort based on columns
                    .map { it.name }
        }

        /**
         * Account and customer paginated query:
         *
         * SELECT pb.stateRef, c
         * FROM AccountStateSchemaV1\$PersistentBalance AS pb,
         *      CustomerSchemaV1\$Customer AS c,
         *      VaultSchemaV1\$VaultStates AS vs
         * WHERE (pb.stateRef = vs.stateRef) AND (pb.customerId = c.customerId)
         *
         *      [AND (vs.stateStatus = :vaultStateStatus)]
         *      [AND cast(c.customerId AS string) = :customerId]
         *      [AND pb.txDate >= :dateFrom]
         *      [AND pb.txDate <= :dateTo]
         *
         *      [AND (cast(pb.account AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(pb.status AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(pb.customerId AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.customerName LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.contactNumber LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.emailAddress LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.postCode LIKE CONCAT('%%',:searchTerm,'%%')]
         */

        /**
         * SELECT String for Account and CustomerSchemaV1.Customer joined queries
         */
        const val customerAndAccountQueryStr = "SELECT %s " +
                "FROM AccountStateSchemaV1\$PersistentBalance AS pb, " +
                    "CustomerSchemaV1\$Customer AS c, " +
                    "VaultSchemaV1\$VaultStates AS vs " +
                "WHERE (pb.stateRef = vs.stateRef) " +
                "AND (pb.customerId = c.customerId) "

        /**
         * LIKE predicates for Account with named searchTerm param
         */
        const val accountLikePredicateStr =
                "cast(pb.account AS string) LIKE CONCAT('%%',:searchTerm,'%%') " +
                "OR cast(pb.status AS string) LIKE CONCAT('%%',:searchTerm,'%%') " +
                "OR cast(pb.customerId AS string) LIKE CONCAT('%%',:searchTerm,'%%') "

        /**
         * LIKE predicates for CustomerSchemaV1.Customer with named searchTerm param
         */
        const val customerLikePredicateStr =
                "c.customerName LIKE CONCAT('%%',:searchTerm,'%%') " +
                "OR c.contactNumber LIKE CONCAT('%%',:searchTerm,'%%') " +
                "OR c.emailAddress LIKE CONCAT('%%',:searchTerm,'%%') " +
                "OR c.postCode LIKE CONCAT('%%',:searchTerm,'%%')"

        const val customerAndAccountLikePredicateStr = "$accountLikePredicateStr OR $customerLikePredicateStr"

        /**
         * Predicates for named query params
         */
        val customerAndAccountParamToPredicateMap = mapOf(
                "vaultStateStatus" to "AND (vs.stateStatus = :vaultStateStatus)",
                "customerId" to "AND cast(c.customerId AS string) = :customerId",
                "dateFrom" to "AND pb.txDate >= :dateFrom",
                "dateTo" to "AND pb.txDate <= :dateTo",
                "searchTerm" to "AND ($customerAndAccountLikePredicateStr)")

        /**
         * HQL query used to verify that unique attachment has been uploaded for customer
         */
        const val ATTACHMENTS_EXIST_HQL = "SELECT 1 FROM CustomerSchemaV1\$AttachmentReference ar " +
                                          "WHERE EXISTS (" +
                                            "SELECT 1 FROM CustomerSchemaV1\$AttachmentReference ari " +
                                            "WHERE ari.attachmentHash IN (:attachmentHashList) " +
                                            "AND ari.customer.customerId != :customerId)"
    }

    /**
     * Return [Customer] with id customerId
     * @param customerId of [Customer] to query
     * @return [Customer] with customerId
     * @throws [IllegalArgumentException] if [Customer] cannot be found for customerId
     */
    fun getCustomerWithId(customerId: UUID): Customer {
        return serviceHub.withEntityManager {
            find(Customer::class.java, customerId)
        } ?: throw java.lang.IllegalArgumentException("Customer with id $customerId does not exist")
    }

    /**
     * Verifies that provided [attachmentHashList] is unique.
     * @param attachmentHashList List of attachment hashes
     * @param customerId of [Customer] to query
     * @return True if provided [attachmentHashList] is unique, false otherwise
     */
    fun attachmentExists(attachmentHashList: List<String>, customerId: UUID): Boolean {
        return serviceHub.withEntityManager {
            val query =  createQuery(ATTACHMENTS_EXIST_HQL, Integer::class.java)
            query.setParameter("attachmentHashList", attachmentHashList)
            query.setParameter("customerId", customerId)
            try {
                query.singleResult.toInt()
                true
            } catch (e: NoResultException) {
                false
            }
        }
    }

    /**
     * Return a paginated list of all [Customer]
     * @param queryParams [RepositoryQueryParams] object that holds a repository query's possible parameters.
     * @return [PaginatedResponse<Customer>] containing customers that meet the search criteria
     */
    fun getCustomersPaginated(repositoryQueryParams: RepositoryQueryParams): PaginatedResponse<Customer> {
        val queryStr = "SELECT %s FROM CustomerSchemaV1\$Customer c"

        val sortColumnAndOrder = getSortColumnAndOrder(repositoryQueryParams)

        val searchTerm = if (repositoryQueryParams.searchTerm.isNotEmpty()) repositoryQueryParams.searchTerm else null
        val queryParams = mapOf("searchTerm" to searchTerm)

        val queryPredicates = mapOf("searchTerm" to " WHERE $customerLikePredicateStr")

        return serviceHub.withEntityManager {
            val countQuery = this.getParametrizedQuery(
                    queryParams = queryParams,
                    paramToPredicateMap = queryPredicates,
                    baseQueryString = queryStr.format("COUNT(c)"),
                    sortColumnAndOrder = "")

            val resultCount = countQuery.resultList.first() as Long

            val query = this.getParametrizedQuery(
                    queryParams = queryParams,
                    paramToPredicateMap = queryPredicates,
                    baseQueryString = queryStr.format("c"),
                    sortColumnAndOrder = sortColumnAndOrder)

            query.firstResult = (repositoryQueryParams.startPage - 1) * repositoryQueryParams.pageSize
            query.maxResults = repositoryQueryParams.pageSize

            val customers = query.resultList.map { it as CustomerSchemaV1.Customer }

            PaginatedResponse(
                    result = customers,
                    totalResults = resultCount,
                    pageSize = repositoryQueryParams.pageSize,
                    pageNumber = repositoryQueryParams.startPage)
        }
    }

    /**
     * Return queried Account state based on id and type
     * @param accountId of [Account.accountId] to query
     * @return [StateAndRef<T>]
     * @throws [RefappException] if [StateAndRef<T>] is not found for [accountId]
     */
    inline fun <reified T : Account> queryAccountStateById(accountId: UUID): StateAndRef<T> {
        val expression = builder{ AccountStateSchemaV1.PersistentBalance::account.equal(accountId)}
        val accountsStateCriteria = QueryCriteria.VaultCustomQueryCriteria(expression)
        try {
            return serviceHub.vaultService.queryBy<T>(accountsStateCriteria).states.single()
        } catch (e: NoSuchElementException) {
            val message = "Vault query failed. Cannot find ${T::class.java} with id: $accountId"
            logger.error(message, e)
            throw RefappException(message, e)
        }
    }

    fun getAccountStateById(accountId: UUID): StateAndRef<Account> {
        return queryAccountStateById(accountId)
    }

    /**
     * Return queried CreditAccount based on id
     * @param accountId of [CreditAccount.accountId] to query
     * @return [StateAndRef<T>]
     * @throws [RefappException] if [StateAndRef<CurrentAccountState>] is not found for [accountId]
     */
    fun  getCreditAccountStateById(accountId: UUID): StateAndRef<CreditAccount> {
        return queryAccountStateById(accountId)
    }

    /**
     * Return queried CurrentAccountState state based on id
     * @param accountId of [CurrentAccountState.accountId] to query
     * @return [StateAndRef<T>]
     * @throws [RefappException] if [StateAndRef<CurrentAccountState>] is not found for [accountId]
     */
    fun  getCurrentAccountStateById(accountId: UUID): StateAndRef<CurrentAccountState> {
        return queryAccountStateById(accountId)
    }

    /**
     * Return queried LoanAccount state based on id
     * @param accountId of [LoanAccountState.accountId] to query
     * @return [StateAndRef<T>]
     * @throws [RefappException] if [StateAndRef<LoanAccountState>] is not found for [accountId]
     */
    fun  getLoanAccountStateById(accountId: UUID): StateAndRef<LoanAccountState> {
        return queryAccountStateById(accountId)
    }

    /**
     * Returns first key for [AccountInfo]
     * @param accountInfo [AccountInfo]
     * @return [PublicKey]
     * @throws [RefappException] if [PublicKey] is not found for [AccountInfo]
     */
    fun getAccountKey(accountInfo: AccountInfo) : PublicKey {
        try {
            return serviceHub.accountService.accountKeys(accountInfo.identifier.id).first()
        } catch (e: NoSuchElementException) {
            val message = "AccountInfo with id: ${accountInfo.identifier} not initialised properly, missing accountKey"
            logger.error(message, e)
            throw RefappException(message, e)
        }
    }

    /**
     * Returns [PaginatedResponse] which contains pagination information and list of [Pair] objects which are composed
     * of account [Account] and related [CustomerSchemaV1.Customer]. Result set will be filtered with
     * given [customerId] and can also be filtered by optional [dateFrom] and [dateTo] fields and optional search
     * term which will be (LIKE) matched against fields: account, status, customerId, customerName, contactNumber,
     * emailAddress, postCode.
     * Result set can be sorted by any of the fields from [Account] and [CustomerSchemaV1.Customer]
     *
     * @param repositoryQueryParams [RepositoryQueryParams] object that holds searchTerm, pagination and sorting data
     * @param dateFrom (Optional) date from which accounts with txDate will be included
     * @param dateTo (Optional) date till which accounts with txDate will be included
     * @return [PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>>] containing accounts which meets
     * criteria
     */
    fun getAccountsForCustomerPaginated(
            repositoryQueryParams: RepositoryQueryParams,
            customerId: UUID,
            dateFrom: Instant?,
            dateTo: Instant?): PaginatedResponse<Pair<Account, Customer>> {

        val searchTerm = if (repositoryQueryParams.searchTerm.isNotEmpty()) repositoryQueryParams.searchTerm else null
        val queryParams = mapOf(
                "vaultStateStatus" to Vault.StateStatus.UNCONSUMED,
                "customerId" to customerId.toString(),
                "searchTerm" to searchTerm,
                "dateFrom" to dateFrom,
                "dateTo" to dateTo
        )

        return getAccountsPaginatedFromQueryParams(repositoryQueryParams, queryParams)
    }

    /**
     * Returns [PaginatedResponse] which contains pagination information and list of [Pair] objects which are composed
     * of account [Account] and related [CustomerSchemaV1.Customer]. Result set can be filtered by optional
     * [dateFrom] and [dateTo] fields and optional search term which will be (LIKE) matched against fields:
     * account, status, customerId, customerName, contactNumber, emailAddress and postCode
     * Result set can be sorted by any of the fields from [Account] and [CustomerSchemaV1.Customer]
     *
     * @param repositoryQueryParams [RepositoryQueryParams] object that holds searchTerm, pagination and sorting data
     * @param dateFrom (Optional) date from which accounts with txDate will be included
     * @param dateTo (Optional) date till which accounts with txDate will be included
     * @return [PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>>] containing accounts which meets
     * criteria
     */
    fun getAccountsPaginated(repositoryQueryParams: RepositoryQueryParams, dateFrom: Instant?, dateTo: Instant?)
            : PaginatedResponse<Pair<Account, Customer>> {

        val searchTerm = if (repositoryQueryParams.searchTerm.isNotEmpty()) repositoryQueryParams.searchTerm else null
        val queryParams = mapOf(
                "vaultStateStatus" to Vault.StateStatus.UNCONSUMED,
                "searchTerm" to searchTerm,
                "dateFrom" to dateFrom,
                "dateTo" to dateTo
        )

        return getAccountsPaginatedFromQueryParams(repositoryQueryParams, queryParams)
    }

    /**
     * Return [PaginatedResponse] of [Account] and associated [CustomerSchemaV1.Customer] pairs and pagination information.
     * Execute a count and result list parametrized query for the given [queryParams] and construct and return a
     * [PaginatedResponse].
     * @param repositoryQueryParams query search term, pagination and sorting data
     * @param queryParams a map of query parameters and values present in [customerParamToPredicateMap]
     * @return [PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>>] containing account and customer pairs
     * matching search criteria
     */
    private fun getAccountsPaginatedFromQueryParams(repositoryQueryParams: RepositoryQueryParams, queryParams: Map<String, Any?>)
            : PaginatedResponse<Pair<Account, Customer>> {

        val sortColumnAndOrder = getSortColumnAndOrder(repositoryQueryParams)

        return serviceHub.withEntityManager {
            val countQuery = this.getParametrizedQuery(
                    queryParams = queryParams,
                    paramToPredicateMap = customerAndAccountParamToPredicateMap,
                    baseQueryString = customerAndAccountQueryStr.format("COUNT(*)"))

            val resultCount = countQuery.resultList.first() as Long

            val query = this.getParametrizedQuery(
                    queryParams = queryParams,
                    paramToPredicateMap = customerAndAccountParamToPredicateMap,
                    baseQueryString = customerAndAccountQueryStr.format("pb.stateRef, c"),
                    sortColumnAndOrder = sortColumnAndOrder)

            query.firstResult = (repositoryQueryParams.startPage - 1) * repositoryQueryParams.pageSize
            query.maxResults = repositoryQueryParams.pageSize

            val accountAndCustomerPairs = query.resultList
                    .map { mapResultToAccountAndCustomerPair(it) }
                    .map { Pair<Account, CustomerSchemaV1.Customer>(it.first, it.second) }

            PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>>(
                    result = accountAndCustomerPairs,
                    totalResults = resultCount,
                    pageSize = repositoryQueryParams.pageSize,
                    pageNumber = repositoryQueryParams.startPage)
        }
    }

    /**
     * Return a String of the form "<table_name>.[repositoryQueryParams.sortField] [repositoryQueryParams.sortOrder]".
     * [repositoryQueryParams.sortField] must be present in [allowedAccountSortFields] or [allowedCustomerSortFields],
     * or an [IllegalArgumentException] is thrown.
     * @param repositoryQueryParams query search term, pagination and sorting data
     * @return [String] full column path and sort order
     * @throws IllegalArgumentException if [repositoryQueryParams.sortField] is not a valid column
     */
    private fun getSortColumnAndOrder(repositoryQueryParams: RepositoryQueryParams): String {
        return when {
            repositoryQueryParams.sortField == null -> ""
            allowedAccountSortFields.contains(repositoryQueryParams.sortField) -> {
                "pb.${repositoryQueryParams.sortField} ${repositoryQueryParams.sortOrder}"
            }
            allowedCustomerSortFields.contains(repositoryQueryParams.sortField) -> {
                "c.${repositoryQueryParams.sortField} ${repositoryQueryParams.sortOrder}"
            }
            else -> throw IllegalArgumentException("Invalid sort field ${repositoryQueryParams.sortField}")
        }
    }

    /**
     * Map a result row from a [customerAndAccountQueryStr] query to Pair<Account, CustomerSchemaV1.Customer>.
     * @param resultRow Array<Object> with values to be cast as [PersistentStateRef], [CustomerSchemaV1.Customer]
     * @return [Pair<Account, CustomerSchemaV1.Customer>] containing a single [Account], [CustomerSchemaV1]
     * query result.
     */
    private fun mapResultToAccountAndCustomerPair(resultRow: Any?): Pair<Account, CustomerSchemaV1.Customer> {
        val row = resultRow as Array<Any>

        val persistentStateRef = row[0] as PersistentStateRef
        val stateRef = StateRef(SecureHash.parse(persistentStateRef.txId), persistentStateRef.index)

        return Pair<Account, CustomerSchemaV1.Customer>(
                serviceHub.toStateAndRef<Account>(stateRef).state.data,
                row[1] as CustomerSchemaV1.Customer)
    }
}