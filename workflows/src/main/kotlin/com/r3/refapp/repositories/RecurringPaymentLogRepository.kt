package com.r3.refapp.repositories

import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.schemas.*
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.serialization.SingletonSerializeAsToken
import java.time.Instant
import java.util.*
import javax.persistence.Column

/**
 * Provides services to query [RecurringPaymentLogSchemaV1.RecurringPaymentLog].
 */
@CordaService
class RecurringPaymentLogRepository(val serviceHub: ServiceHub) : SingletonSerializeAsToken() {

    companion object {

        val allowedRecurringPaymentSortFields = getRecurringPaymentSortFields()
        val allowedRecurringPaymentLogSortFields = getRecurringPaymentLogSortFields()

        /**
         * Return a list of RecurringPayment sort fields generated from [RecurringPaymentSchemaV1.RecurringPayment] member names.
         * These members are annotated with @Column and have a column in the database
         * @return [List<String>] available sort fields
         */
        private fun getRecurringPaymentSortFields(): List<String> {
            return RecurringPaymentSchemaV1.RecurringPayment::class.java.declaredFields
                    .filter{ it.isAnnotationPresent(Column::class.java) }
                    .map { it.name }
        }

        /**
         * Return a list of RecurringPaymentLog sort fields generated from [RecurringPaymentSchemaV1.RecurringPaymentLog]
         * member names. These members are annotated with @Column and have a column in the database
         * @return [List<String>] available sort fields
         */
        private fun getRecurringPaymentLogSortFields(): List<String> {
            return RecurringPaymentLogSchemaV1.RecurringPaymentLog::class.java.declaredFields
                    .filter{ it.isAnnotationPresent(Column::class.java) } // can only sort based on columns
                    .map { it.name }
        }

        /**
         * Recurring payment log paginated query:
         *
         * SELECT DISTINCT rpl, rpl.recurringPayment
         * FROM RecurringPaymentLogSchemaV1\$RecurringPaymentLog as rpl,
         *      AccountStateSchemaV1\$PersistentBalance AS pb,
         *      CustomerSchemaV1\$Customer AS c,
         *      VaultSchemaV1\$VaultStates AS vs
         * WHERE (pb.stateRef = vs.stateRef) AND (pb.customerId = c.customerId)
         *      AND ((pb.account = rpl.recurringPayment.accountFrom)
         *          OR (pb.account = rpl.recurringPayment.accountTo))
         *
         *      [AND (rpl.recurringPayment.accountFrom = :accountId)
         *          OR (rpl.recurringPayment.accountTo = :accountId)]
         *      [AND (vs.stateStatus = :vaultStateStatus)]
         *      [AND cast(c.customerId AS string) = :customerId]
         *      [AND rpl.txDate >= :dateFrom]
         *      [AND rpl.txDate <= :dateTo]
         *
         *      [AND (cast(pb.account AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(pb.status AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(pb.customerId AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.customerName LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.contactNumber LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.emailAddress LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.postCode LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(rpl.recurringPayment.linearId AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(rpl.recurringPayment.accountFrom AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(rpl.recurringPayment.accountTo AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR rpl.logId LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR rpl.error LIKE CONCAT('%%',:searchTerm,'%%')]
         */

        /**
         * SELECT String for Account, Customer and Recurring Payment Log joined queries
         */
        const val recurringPaymentAndAccountQueryStr = "SELECT %s " +
                "FROM RecurringPaymentLogSchemaV1\$RecurringPaymentLog as rpl, " +
                    "AccountStateSchemaV1\$PersistentBalance AS pb, " +
                    "CustomerSchemaV1\$Customer AS c, " +
                    "VaultSchemaV1\$VaultStates AS vs " +
                "WHERE (pb.stateRef = vs.stateRef) " +
                "AND (pb.customerId = c.customerId) " +
                "AND ((pb.account = rpl.recurringPayment.accountFrom) " +
                "OR (pb.account = rpl.recurringPayment.accountTo)) "

        /**
         * LIKE predicates for Recurring Payment Log with named searchTerm param
         */
        const val recurringPaymentLogLikePredicateStr =
                "cast(rpl.recurringPayment.linearId AS string) LIKE CONCAT('%%',:searchTerm,'%%')" +
                "OR cast(rpl.recurringPayment.accountFrom AS string) LIKE CONCAT('%%',:searchTerm,'%%')" +
                "OR cast(rpl.recurringPayment.accountTo AS string) LIKE CONCAT('%%',:searchTerm,'%%')" +
                "OR rpl.logId LIKE CONCAT('%%',:searchTerm,'%%')" +
                "OR rpl.error LIKE CONCAT('%%',:searchTerm,'%%')"

        const val recurringPaymentLogAndAccountPredicateStr = AccountRepository.customerAndAccountLikePredicateStr +
                " OR $recurringPaymentLogLikePredicateStr"

        /**
         * Predicate for filtering by accountId
         */
        const val recurrentPaymentLogAccountIdPredicate =
                "(rpl.recurringPayment.accountFrom = :accountId) " +
                "OR (rpl.recurringPayment.accountTo = :accountId)"

        /**
         * Predicates for named query params
         */
        val recurringPaymentLogAndAccountParamToPredicateMap = mapOf(
                "vaultStateStatus" to "AND (vs.stateStatus = :vaultStateStatus)",
                "customerId" to "AND cast(c.customerId AS string) = :customerId",
                "accountId" to "AND ($recurrentPaymentLogAccountIdPredicate)",
                "dateFrom" to "AND rpl.txDate >= :dateFrom",
                "dateTo" to "AND rpl.txDate <= :dateTo",
                "searchTerm" to "AND ($recurringPaymentLogAndAccountPredicateStr)")
    }

    /**
     * Persists [RecurringPaymentLogSchemaV1.RecurringPaymentLog] to DB
     * @param recurringPaymentLog [RecurringPaymentLogSchemaV1.RecurringPaymentLog] object
     */
    fun persistRecurringPaymentLog(recurringPaymentLog: RecurringPaymentLogSchemaV1.RecurringPaymentLog) {
        serviceHub.withEntityManager {
            persist(recurringPaymentLog)
        }
    }

    /**
     * Fetches [RecurringPaymentLogSchemaV1.RecurringPaymentLog] from DB for given [logId]
     * @param logId recurring payment log id
     * @return returns [RecurringPaymentLogSchemaV1.RecurringPaymentLog] from DB
     * @throws [IllegalArgumentException] if [RecurringPaymentLogSchemaV1.RecurringPaymentLog] is not found for given [logId]
     */
    fun getRecurringPaymentLogById(logId: String): RecurringPaymentLogSchemaV1.RecurringPaymentLog {
        return serviceHub.withEntityManager {
            find(RecurringPaymentLogSchemaV1.RecurringPaymentLog::class.java, logId)
                    ?: throw IllegalArgumentException("Cannot find RecurringPaymentLog for id: $logId")
        }
    }

    /**
     * Fetches last successful [RecurringPaymentLogSchemaV1.RecurringPaymentLog] from DB for given [recurringPayment]
     * @param recurringPayment recurring payment
     * @return returns [RecurringPaymentLogSchemaV1.RecurringPaymentLog] from DB
     * @throws [NoSuchElementException] if successful [RecurringPaymentLogSchemaV1.RecurringPaymentLog] is not found for given [recurringPayment]
     */
    fun getLastSuccessLogByRecurringPayment(recurringPayment: RecurringPaymentSchemaV1.RecurringPayment): RecurringPaymentLogSchemaV1.RecurringPaymentLog {
        return serviceHub.withEntityManager {
            val query = createQuery("SELECT rpl FROM RecurringPaymentLogSchemaV1\$RecurringPaymentLog rpl " +
                    "WHERE rpl.recurringPayment.linearId = :recurringPayment AND rpl.error IS NULL " +
                    "ORDER BY rpl.txDate DESC ",
                    RecurringPaymentLogSchemaV1.RecurringPaymentLog::class.java)
            query.setParameter("recurringPayment", recurringPayment.linearId)
            query.maxResults = 1
            query.resultList.first()
        }
    }

    /**
     * Fetches list of [RecurringPaymentLogSchemaV1.RecurringPaymentLog] from DB for given [recurringPaymentId]
     * @param recurringPaymentId recurring payment linear id
     * @return returns [List<RecurringPaymentLog>] from DB
     */
    fun getRecurringPaymentLogsForRecurringPayment(recurringPaymentId: UUID): List<RecurringPaymentLogSchemaV1.RecurringPaymentLog> {
        return serviceHub.withEntityManager {
            val query = createQuery("SELECT rpl FROM RecurringPaymentLogSchemaV1\$RecurringPaymentLog rpl " +
                    "WHERE rpl.recurringPayment.linearId = :recurringPayment",
                    RecurringPaymentLogSchemaV1.RecurringPaymentLog::class.java)
            query.setParameter("recurringPayment", recurringPaymentId)
            query.resultList
        }
    }

    /**
     * Returns [PaginatedResponse] which contains pagination information and list of
     * [RecurringPaymentLogSchemaV1.RecurringPaymentLog] objects which are composed. Result set can be filtered by optional
     * [dateFrom] and [dateTo] fields and optional search term which will be (LIKE) matched against fields:
     * accountFrom, accountTo, linearId, logId, error, customerId, customerName, contactNumber, emailAddress, postCode and
     * status (account status).
     * Result set can be sorted by any of the fields from [RecurringPaymentLogSchemaV1.RecurringPaymentLog] and
     * [RecurringPaymentSchemaV1.RecurringPayment].
     *
     * @param repositoryQueryParams [RepositoryQueryParams] object that holds searchTerm, pagination and sorting data
     * @param dateFrom (Optional) date from which accounts with txDate will be included
     * @param dateTo (Optional) date till which accounts with txDate will be included
     * @return [PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>] containing recurring payments which
     * meets criteria
     */
    fun getRecurringPaymentsPaginated(repositoryQueryParams: RepositoryQueryParams, dateFrom: Instant?, dateTo: Instant?)
            : PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog> {

        val searchTerm = if (repositoryQueryParams.searchTerm.isNotEmpty()) repositoryQueryParams.searchTerm else null
        val queryParams = mapOf(
                "vaultStateStatus" to Vault.StateStatus.UNCONSUMED,
                "searchTerm" to searchTerm,
                "dateFrom" to dateFrom,
                "dateTo" to dateTo
        )

        return getRecurringPaymentsPaginatedFromQueryParams(repositoryQueryParams, queryParams)
    }

    /**
     * Returns [PaginatedResponse] which contains pagination information and list of [RecurringPaymentLogSchemaV1.RecurringPaymentLog]
     * objects. Result set will be filtered with given [accountId] and can also be filtered by optional [dateFrom]
     * and [dateTo] fields and optional search term which will be (LIKE) matched against fields:
     * accountFrom, accountTo, linearId, logId, error, customerId, customerName, contactNumber, emailAddress, postCode and
     * status (account status).
     * Result set can be sorted by any of the fields from [RecurringPaymentLogSchemaV1.RecurringPaymentLog] and
     * [RecurringPaymentSchemaV1.RecurringPayment].
     *
     * @param repositoryQueryParams [RepositoryQueryParams] object that holds searchTerm, pagination and sorting data
     * @param accountId ID of the account
     * @param dateFrom (Optional) date from which accounts with txDate will be included
     * @param dateTo (Optional) date till which accounts with txDate will be included
     * @return [PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>] containing recurring payments which
     * meets criteria
     */
    fun getRecurringPaymentsForAccountPaginated(
            repositoryQueryParams: RepositoryQueryParams,
            accountId: UUID,
            dateFrom: Instant?,
            dateTo: Instant?): PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog> {

        val searchTerm = if (repositoryQueryParams.searchTerm.isNotEmpty()) repositoryQueryParams.searchTerm else null
        val queryParams = mapOf(
                "vaultStateStatus" to Vault.StateStatus.UNCONSUMED,
                "accountId" to accountId,
                "searchTerm" to searchTerm,
                "dateFrom" to dateFrom,
                "dateTo" to dateTo
        )

        return getRecurringPaymentsPaginatedFromQueryParams(repositoryQueryParams, queryParams)
    }

    /**
     * Returns [PaginatedResponse] which contains pagination information and list of [RecurringPaymentLogSchemaV1.RecurringPaymentLog]
     * objects Result set will be filtered with given [customerId] and can also be filtered by optional [dateFrom] and
     * [dateTo] fields and optional search term which will be (LIKE) matched against fields:
     * accountFrom, accountTo, linearId, logId, error, customerId, customerName, contactNumber, emailAddress, postCode and
     * status (account status).
     * Result set can be sorted by any of the fields from [RecurringPaymentLogSchemaV1.RecurringPaymentLog] and
     * [RecurringPaymentSchemaV1.RecurringPayment].
     *
     * @param repositoryQueryParams [RepositoryQueryParams] object that holds searchTerm, pagination and sorting data
     * @param customerId ID of the customer
     * @param dateFrom (Optional) date from which accounts with txDate will be included
     * @param dateTo (Optional) date till which accounts with txDate will be included
     * @return [PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>] containing recurring payments which
     * meets criteria
     */
    fun getRecurringPaymentsForCustomerPaginated(
            repositoryQueryParams: RepositoryQueryParams,
            customerId: UUID,
            dateFrom: Instant?,
            dateTo: Instant?): PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog> {

        val searchTerm = if (repositoryQueryParams.searchTerm.isNotEmpty()) repositoryQueryParams.searchTerm else null
        val queryParams = mapOf(
                "vaultStateStatus" to Vault.StateStatus.UNCONSUMED,
                "customerId" to customerId.toString(),
                "searchTerm" to searchTerm,
                "dateFrom" to dateFrom,
                "dateTo" to dateTo
        )

        return getRecurringPaymentsPaginatedFromQueryParams(repositoryQueryParams, queryParams)
    }

    /**
     * Return [PaginatedResponse] of [RecurringPaymentLogSchemaV1.RecurringPaymentLog] and pagination information.
     * Execute a count and result list parametrized query for the given [queryParams] and construct and return a
     * [PaginatedResponse].
     * @param repositoryQueryParams query search term, pagination and sorting data
     * @param queryParams a map of query parameters and values present in [recurringPaymentLogParamToPredicateMap]
     * @return [PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>] matching search criteria
     */
    private fun getRecurringPaymentsPaginatedFromQueryParams(
            repositoryQueryParams: RepositoryQueryParams, queryParams: Map<String, Any?>)
            : PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog> {

        val sortColumnAndOrder = getSortColumnAndOrder(repositoryQueryParams)

        return serviceHub.withEntityManager {
            val countQuery = this.getParametrizedQuery(
                    queryParams = queryParams,
                    paramToPredicateMap = recurringPaymentLogAndAccountParamToPredicateMap,
                    baseQueryString = recurringPaymentAndAccountQueryStr.format("COUNT(DISTINCT rpl)"))

            val resultCount = countQuery.resultList.first() as Long

            val query = this.getParametrizedQuery(
                    queryParams = queryParams,
                    paramToPredicateMap = recurringPaymentLogAndAccountParamToPredicateMap,
                    baseQueryString = recurringPaymentAndAccountQueryStr.format("DISTINCT rpl, rpl.recurringPayment"),
                    sortColumnAndOrder = sortColumnAndOrder)

            query.firstResult = (repositoryQueryParams.startPage - 1) * repositoryQueryParams.pageSize
            query.maxResults = repositoryQueryParams.pageSize

            val recurringPaymentLogs = query.resultList.map {
                val row = it as Array<Any>
                row[0] as RecurringPaymentLogSchemaV1.RecurringPaymentLog
            }

            PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>(
                    result = recurringPaymentLogs,
                    totalResults = resultCount,
                    pageSize = repositoryQueryParams.pageSize,
                    pageNumber = repositoryQueryParams.startPage)
        }
    }

    /**
     * Return a String of the form "<table_name>.[repositoryQueryParams.sortField] [repositoryQueryParams.sortOrder]".
     * [repositoryQueryParams.sortField] must be present in [allowedRecurringPaymentSortFields] or
     * [allowedRecurringPaymentLogSortFields], or an [IllegalArgumentException] is thrown.
     * @param repositoryQueryParams query search term, pagination and sorting data
     * @return [String] full column path and sort order
     * @throws IllegalArgumentException if [repositoryQueryParams.sortField] is not a valid column
     */
    private fun getSortColumnAndOrder(repositoryQueryParams: RepositoryQueryParams): String {
        return when {
            repositoryQueryParams.sortField == null -> ""
            allowedRecurringPaymentSortFields.contains(repositoryQueryParams.sortField) -> {
                "rpl.recurringPayment.${repositoryQueryParams.sortField} ${repositoryQueryParams.sortOrder}"
            }
            allowedRecurringPaymentLogSortFields.contains(repositoryQueryParams.sortField) -> {
                "rpl.${repositoryQueryParams.sortField} ${repositoryQueryParams.sortOrder}"
            }
            else -> throw IllegalArgumentException("Invalid sort field ${repositoryQueryParams.sortField}")
        }
    }
}