package com.r3.refapp.repositories

import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.schemas.TransactionLogSchemaV1
import com.r3.refapp.schemas.TransactionType
import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.serialization.SingletonSerializeAsToken
import java.time.Instant
import java.util.*
import javax.persistence.Column

/**
 * Provide services to query and persist [TransactionLogSchemaV1.TransactionLog]
 */
@CordaService
class TransactionLogRepository(val serviceHub: ServiceHub) : SingletonSerializeAsToken() {

    /**
     * Provide TransactionLog HQL queries
     * @property TransactionLogByTransactionTypeHQL Query transaction logs by transaction type and by accountId (present
     * in either accountFrom or accountTo columns)
     * @property TransactionLogByTransactionTypeAndBetweenTimeHQL Extend [TransactionLogByTransactionTypeHQL] to
     * additionally filter by transaction time (between start and end time)
     */
    companion object {

        val allowedTransactionLogSortFields = getTransactionLogSortFields()

        private const val TransactionLogByTransactionTypeHQL = "SELECT tl FROM TransactionLogSchemaV1\$TransactionLog tl " +
                "WHERE (tl.accountFrom = :accountId OR tl.accountTo = :accountId) " +
                "AND tl.txType = :txType "

        private const val TransactionLogByTransactionTypeAndBetweenTimeHQL = TransactionLogByTransactionTypeHQL +
                "AND tl.txDate >= :startTime " +
                "AND tl.txDate <= :endTime"

        /**
         * Return a list of TransactionLog sort fields generated from [TransactionLogSchemaV1.TransactionLog] member names.
         * These members are annotated with @Column and have a column in the database
         * @return [List<String>] available sort fields
         */
        private fun getTransactionLogSortFields(): List<String> {
            return TransactionLogSchemaV1.TransactionLog::class.java.declaredFields
                    .filter{ it.isAnnotationPresent(Column::class.java) }
                    .map { it.name }
        }

        /**
         * Transaction log paginated query:
         *
         * SELECT DISTINCT tl
         * FROM TransactionLogSchemaV1\$TransactionLog as tl,
         *      AccountStateSchemaV1\$PersistentBalance AS pb,
         *      CustomerSchemaV1\$Customer AS c,
         *      VaultSchemaV1\$VaultStates AS vs
         * WHERE (pb.stateRef = vs.stateRef) AND (pb.customerId = c.customerId)
         *      AND ((pb.account = tl.accountFrom) OR (pb.account = tl.accountTo))
         *
         *      [AND (tl.accountFrom = :accountId) OR (tl.accountTo = :accountId)]
         *      [AND (vs.stateStatus = :vaultStateStatus)]
         *      [AND cast(c.customerId AS string) = :customerId]
         *      [AND tl.txDate >= :dateFrom]
         *      [AND tl.txDate <= :dateTo]
         *
         *      [AND (cast(pb.account AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(pb.status AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(pb.customerId AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.customerName LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.contactNumber LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.emailAddress LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR c.postCode LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(tl.accountFrom AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(tl.accountTo AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR cast(tl.txType AS string) LIKE CONCAT('%%',:searchTerm,'%%')
         *          OR tl.currency LIKE CONCAT('%%',:searchTerm,'%%')]
         */

        /**
         * SELECT String for Account, Customer and Transaction Log joined queries
         */
        const val transactionLogAndAccountQueryStr = "SELECT %s " +
                "FROM TransactionLogSchemaV1\$TransactionLog as tl, " +
                    "AccountStateSchemaV1\$PersistentBalance AS pb, " +
                    "CustomerSchemaV1\$Customer AS c, " +
                    "VaultSchemaV1\$VaultStates AS vs " +
                "WHERE (pb.stateRef = vs.stateRef) " +
                "AND (pb.customerId = c.customerId) " +
                "AND ((pb.account = tl.accountFrom) OR (pb.account = tl.accountTo)) "

        /**
         * LIKE predicates for Recurring Payment Log with named searchTerm param
         */
        const val transactionLogLikePredicateStr =
                "cast(tl.accountFrom AS string) LIKE CONCAT('%%',:searchTerm,'%%')" +
                "OR cast(tl.accountTo AS string) LIKE CONCAT('%%',:searchTerm,'%%')" +
                "OR cast(tl.txType AS string) LIKE CONCAT('%%',:searchTerm,'%%')" +
                "OR tl.currency LIKE CONCAT('%%',:searchTerm,'%%')"

        const val transactionLogAndAccountLikePredicateStr = AccountRepository.customerAndAccountLikePredicateStr +
                " OR $transactionLogLikePredicateStr"

        /**
         * Predicates for filtering by accountId
         */
        const val transactionLogAccountIdPredicate = "(tl.accountFrom = :accountId) OR (tl.accountTo = :accountId)"

        /**
         * Predicates for named query params
         */
        val transactionLogAndAccountParamToPredicateMap = mapOf(
                "vaultStateStatus" to "AND (vs.stateStatus = :vaultStateStatus)",
                "customerId" to "AND cast(c.customerId AS string) = :customerId",
                "accountId" to "AND (${transactionLogAccountIdPredicate})",
                "dateFrom" to "AND tl.txDate >= :dateFrom",
                "dateTo" to "AND tl.txDate <= :dateTo",
                "searchTerm" to "AND (${transactionLogAndAccountLikePredicateStr})")
    }

    /**
     * Query and return a list of [TransactionLogSchemaV1.TransactionLog] for accountId and txType
     * @param accountId id of account to query
     * @param txType type of transaction
     * @return List<[TransactionLogSchemaV1.TransactionLog]>
     */
    fun getTransactionLogByTransactionType(accountId: UUID, txType: TransactionType): List<TransactionLogSchemaV1.TransactionLog> {
        return serviceHub.withEntityManager {
            val query = createQuery(
                    TransactionLogByTransactionTypeHQL,
                    TransactionLogSchemaV1.TransactionLog::class.java
            )
            query.setParameter("accountId", accountId)
            query.setParameter("txType", txType)
            query.resultList
        }
    }

    /**
     * Query and return a list of [TransactionLogSchemaV1.TransactionLog] for accountId and txType,
     * with txDate between startTime and endTime
     * @param accountId id of account to query
     * @param txType type of transaction
     * @param startTime txDate from
     * @param endTime txDate to
     * @return List<[TransactionLogSchemaV1.TransactionLog]>
     */
    fun getTransactionLogByTransactionTypeAndBetweenTime(
            accountId: UUID, txType: TransactionType, startTime: Instant, endTime: Instant)
            : List<TransactionLogSchemaV1.TransactionLog> {

        return serviceHub.withEntityManager {
            val query = createQuery(
                    TransactionLogByTransactionTypeAndBetweenTimeHQL,
                    TransactionLogSchemaV1.TransactionLog::class.java
            )

            query.setParameter("accountId", accountId)
            query.setParameter("txType", txType)
            query.setParameter("startTime", startTime)
            query.setParameter("endTime", endTime)

            query.resultList
        }
    }

    /**
     * Create a new [TransactionLogSchemaV1.TransactionLog] and persist to DB
     * @param txId transaction id
     * @param amount delta balance amount
     * @param txDate time of transaction
     * @param txType type of transaction
     * @param accountFrom id of account that was debited, optional
     * @param accountTo id of account that was credited, optional
     */
    fun logTransaction(txId: SecureHash, amount: Amount<Currency>, txDate: Instant, txType: TransactionType,
                       accountFrom: UUID? = null, accountTo: UUID? = null) {

        val transactionLog = TransactionLogSchemaV1.TransactionLog(
                txId = txId.toString(),
                accountFrom = accountFrom,
                accountTo = accountTo,
                amount = amount.quantity,
                currency = amount.token.toString(),
                txDate = txDate,
                txType = txType
        )
        persistTransactionLog(transactionLog)
    }

    /**
     * Persists [TransactionLogSchemaV1.TransactionLog] to DB
     * @param transactionLog [TransactionLogSchemaV1.TransactionLog] object
     */
    fun persistTransactionLog(transactionLog: TransactionLogSchemaV1.TransactionLog) {
        serviceHub.withEntityManager {
            persist(transactionLog)
        }
    }

    /**
     * Query and return a list of [TransactionLogSchemaV1.TransactionLog] for [customerId]
     * with txDate between [startTime] and [endTime]
     * @param repositoryQueryParams pagination parameters
     * @param customerId id of customer to query
     * @param startTime txDate from
     * @param endTime txDate to
     * @return List<[TransactionLogSchemaV1.TransactionLog]>
     */
    fun getTransactionLogsForCustomerAndBetweenTime(
            repositoryQueryParams: RepositoryQueryParams,
            customerId: UUID,
            startTime: Instant?,
            endTime: Instant?): PaginatedResponse<TransactionLogSchemaV1.TransactionLog> {

        val searchTerm = if (repositoryQueryParams.searchTerm.isNotEmpty()) repositoryQueryParams.searchTerm else null
        val queryParams = mapOf(
                "vaultStateStatus" to Vault.StateStatus.UNCONSUMED,
                "customerId" to customerId.toString(),
                "searchTerm" to searchTerm,
                "dateFrom" to startTime,
                "dateTo" to endTime
        )

        return getTransactionLogPaginatedFromQueryParams(repositoryQueryParams, queryParams)
    }

    /**
     * Query and return a [TransactionLogSchemaV1.TransactionLog] for [txId].
     * @param txId id of transaction log
     * @return [TransactionLogSchemaV1.TransactionLog]
     * @throws [IllegalArgumentException] if transaction log cannot be found for give [txId]
     */
    fun getTransactionLogById(txId: String) : TransactionLogSchemaV1.TransactionLog {
        return serviceHub.withEntityManager {
            find(TransactionLogSchemaV1.TransactionLog::class.java, txId)
        } ?: throw java.lang.IllegalArgumentException("Transactions with id $txId does not exist")
    }

    /**
     * Returns [PaginatedResponse] which contains pagination information and list of [TransactionLogSchemaV1.TransactionLog]
     * objects. Result set can be filtered by optional
     * [dateFrom] and [dateTo] fields and optional search term which will be (LIKE) matched against fields:
     * accountFrom, accountTo, currency, transactionType, customerId, customerName, contactNumber, emailAddress,
     * postCode and status (account status)
     * Result set can be sorted by any of the fields from [TransactionLogSchemaV1.TransactionLog].
     *
     * @param repositoryQueryParams [RepositoryQueryParams] object that holds searchTerm, pagination and sorting data
     * @param dateFrom (Optional) date from which will be matched against transactions txDate
     * @param dateTo (Optional) date till which will be matched against transactions txDate
     * @return [PaginatedResponse<TransactionLogSchemaV1.TransactionLog>] containing transactions which meets
     * criteria
     */
    fun getTransactionsPaginated(repositoryQueryParams: RepositoryQueryParams, dateFrom: Instant?, dateTo: Instant?)
            : PaginatedResponse<TransactionLogSchemaV1.TransactionLog> {

        val searchTerm = if (repositoryQueryParams.searchTerm.isNotEmpty()) repositoryQueryParams.searchTerm else null
        val queryParams = mapOf(
                "vaultStateStatus" to Vault.StateStatus.UNCONSUMED,
                "searchTerm" to searchTerm,
                "dateFrom" to dateFrom,
                "dateTo" to dateTo
        )

        return getTransactionLogPaginatedFromQueryParams(repositoryQueryParams, queryParams)
    }

    /**
     * Returns [PaginatedResponse] which contains pagination information and list of [TransactionLogSchemaV1.TransactionLog]
     * objects. Result set will be filtered with given [accountId] and can also be filtered by optional
     * [dateFrom] and [dateTo] fields and optional search term which will be (LIKE) matched against fields:
     * accountFrom, accountTo, currency, transactionType, customerId, customerName, contactNumber, emailAddress,
     * postCode and status (account status)
     * Result set can be sorted by any of the fields from [TransactionLogSchemaV1.TransactionLog].
     *
     * @param repositoryQueryParams [RepositoryQueryParams] object that holds searchTerm, pagination and sorting data
     * @param accountId Id of the account which will be matched against accountFrom and accountTo fields of the
     * [TransactionLogSchemaV1.TransactionLog]
     * @param dateFrom (Optional) date from which will be matched against transactions txDate
     * @param dateTo (Optional) date till which will be matched against transactions txDate
     * @return [PaginatedResponse<TransactionLogSchemaV1.TransactionLog>] containing transactions which meets
     * criteria
     */
    fun getTransactionsForAccountPaginated(
            repositoryQueryParams: RepositoryQueryParams,
            accountId: UUID,
            dateFrom: Instant?,
            dateTo: Instant?): PaginatedResponse<TransactionLogSchemaV1.TransactionLog> {

        val searchTerm = if (repositoryQueryParams.searchTerm.isNotEmpty()) repositoryQueryParams.searchTerm else null
        val queryParams = mapOf(
                "vaultStateStatus" to Vault.StateStatus.UNCONSUMED,
                "accountId" to accountId,
                "searchTerm" to searchTerm,
                "dateFrom" to dateFrom,
                "dateTo" to dateTo
        )

        return getTransactionLogPaginatedFromQueryParams(repositoryQueryParams, queryParams)
    }

    /**
     * Return [PaginatedResponse] of [TransactionLogSchemaV1.TransactionLog] and pagination information.
     * Execute a count and result list parametrized query for the given [queryParams] and construct and return a
     * [PaginatedResponse].
     * @param repositoryQueryParams query search term, pagination and sorting data
     * @param queryParams a map of query parameters and values present in [transactionLogParamToPredicateMap]
     * @return [PaginatedResponse<TransactionLogSchemaV1.TransactionLog>] matching search criteria
     */
    private fun getTransactionLogPaginatedFromQueryParams(
            repositoryQueryParams: RepositoryQueryParams, queryParams: Map<String, Any?>)
            : PaginatedResponse<TransactionLogSchemaV1.TransactionLog> {

        val sortColumnAndOrder = getSortColumnAndOrder(repositoryQueryParams)

        return serviceHub.withEntityManager {
            val countQuery = this.getParametrizedQuery(
                    queryParams = queryParams,
                    paramToPredicateMap = transactionLogAndAccountParamToPredicateMap,
                    baseQueryString = transactionLogAndAccountQueryStr.format("COUNT(DISTINCT tl)"))

            val resultCount = countQuery.resultList.first() as Long

            val query = this.getParametrizedQuery(
                    queryParams = queryParams,
                    paramToPredicateMap = transactionLogAndAccountParamToPredicateMap,
                    baseQueryString = transactionLogAndAccountQueryStr.format("DISTINCT tl"),
                    sortColumnAndOrder = sortColumnAndOrder)

            query.firstResult = (repositoryQueryParams.startPage - 1) * repositoryQueryParams.pageSize
            query.maxResults = repositoryQueryParams.pageSize

            val transactionLogs = query.resultList.map { it as TransactionLogSchemaV1.TransactionLog }

            PaginatedResponse<TransactionLogSchemaV1.TransactionLog>(
                    result = transactionLogs,
                    totalResults = resultCount,
                    pageSize = repositoryQueryParams.pageSize,
                    pageNumber = repositoryQueryParams.startPage)
        }
    }

    /**
     * Return a String of the form "<table_name>.[repositoryQueryParams.sortField] [repositoryQueryParams.sortOrder]".
     * [repositoryQueryParams.sortField] must be present in [allowedTransactionLogSortFields] or an
     * [IllegalArgumentException] is thrown.
     * @param repositoryQueryParams query search term, pagination and sorting data
     * @return [String] full column path and sort order
     * @throws IllegalArgumentException if [repositoryQueryParams.sortField] is not a valid column
     */
    private fun getSortColumnAndOrder(repositoryQueryParams: RepositoryQueryParams): String {
        return when {
            repositoryQueryParams.sortField == null -> ""
            allowedTransactionLogSortFields.contains(repositoryQueryParams.sortField) -> {
                "tl.${repositoryQueryParams.sortField} ${repositoryQueryParams.sortOrder}"
            }
            else -> throw IllegalArgumentException("Invalid sort field ${repositoryQueryParams.sortField}")
        }
    }
}