package com.r3.refapp.it

import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.flows.IntrabankPaymentFlow
import com.r3.refapp.flows.WithdrawFiatFlow
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.schemas.TransactionLogSchemaV1
import com.r3.refapp.schemas.TransactionType
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.transactions.SignedTransaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TransactionLogTest : AbstractITHelper() {

    lateinit var accountId: UUID
    lateinit var accountTransferToId: UUID
    lateinit var account2Id: UUID
    lateinit var transferTx: SignedTransaction

    @BeforeAll
    private fun generateTransactions() {
        accountId = prepareCurrentAccount("AN Other", bank, network).accountData.accountId
        account2Id = prepareCurrentAccount("AN Other", bank, network).accountData.accountId
        accountTransferToId = prepareCurrentAccount("AN Other", bank, network).accountData.accountId

        executeFlowWithRunNetwork(DepositFiatFlow(accountId, 1000 of EUR), bank, network)
        executeFlowWithRunNetwork(DepositFiatFlow(account2Id, 1000 of EUR), bank, network)
        executeFlowWithRunNetwork(DepositFiatFlow(accountTransferToId, 1000 of EUR), bank, network)

        transferTx = executeFlowWithRunNetwork(
                IntrabankPaymentFlow(accountId, accountTransferToId, 100 of EUR),
                bank, network)

        executeFlowWithRunNetwork(IntrabankPaymentFlow(accountTransferToId, accountId, 100 of EUR), bank, network)

        executeFlowWithRunNetwork(IntrabankPaymentFlow(accountId, account2Id, 100 of EUR), bank, network)
        executeFlowWithRunNetwork(IntrabankPaymentFlow(account2Id, accountId, 100 of EUR), bank, network)

        executeFlowWithRunNetwork(IntrabankPaymentFlow(account2Id, accountTransferToId, 100 of EUR), bank, network)
        executeFlowWithRunNetwork(IntrabankPaymentFlow(accountTransferToId, account2Id, 100 of EUR), bank, network)

        executeFlowWithRunNetwork(WithdrawFiatFlow(accountId, 50 of EUR), bank, network)
    }

    @Test
    fun `test deposit transaction log success`() {
        val txLogs = transactionLogRepository.getTransactionLogByTransactionType(accountId, TransactionType.DEPOSIT)
        assert(txLogs.size == 1)

        val txLog = txLogs.first()
        assertEquals(txLog.amount, 100000)
        assertEquals(txLog.accountFrom, null)
        assertEquals(txLog.accountTo, accountId)
        assertEquals(txLog.txType, TransactionType.DEPOSIT)
    }

    @Test
    fun `test withdraw transaction log success`() {
        val txLogs = transactionLogRepository.getTransactionLogByTransactionType(accountId, TransactionType.WITHDRAWAL)
        assert(txLogs.size == 1)

        val txLog = txLogs.first()
        assertEquals(txLog.amount, 5000)
        assertEquals(txLog.accountFrom, accountId)
        assertEquals(txLog.accountTo, null)
        assertEquals(txLog.txType, TransactionType.WITHDRAWAL)
    }

    @Test
    fun `test transfer transaction log success`() {
        val txLogs = transactionLogRepository.getTransactionLogByTransactionType(accountId, TransactionType.TRANSFER)
        val txLog = txLogs.single { it.accountFrom == accountId && it.accountTo == accountTransferToId }

        assertEquals(txLog.amount, 10000)
        assertEquals(txLog.accountFrom, accountId)
        assertEquals(txLog.accountTo, accountTransferToId)
        assertEquals(txLog.txType, TransactionType.TRANSFER)
    }

    @Test
    fun `test getTransactionLogsForCustomerAndBetweenTime success`() {

        val now = Instant.now()
        val queryParam = RepositoryQueryParams()
        val customerId = accountRepository.getAccountStateById(accountId).state.data.accountData.customerId

        val txLogs = transactionLogRepository.getTransactionLogsForCustomerAndBetweenTime(queryParam, customerId,
                now.minusSeconds(20), now.plusSeconds(20)).result

        val txLogDeposit = txLogs.single { it.txType == TransactionType.DEPOSIT }
        assertEquals(100000, txLogDeposit.amount)
        assertEquals(txLogDeposit.accountFrom, null)
        assertEquals(accountId, txLogDeposit.accountTo)
        assertEquals(TransactionType.DEPOSIT, txLogDeposit.txType)

        val txLogTransfer = txLogs.single {
            it.accountFrom == accountId && it.accountTo == accountTransferToId && it.txType == TransactionType.TRANSFER
        }

        assertEquals(10000, txLogTransfer.amount)
        assertEquals(accountId, txLogTransfer.accountFrom)
        assertEquals(accountTransferToId, txLogTransfer.accountTo)
        assertEquals(TransactionType.TRANSFER, txLogTransfer.txType)
    }

    @Test
    fun `test getTransactionLogsForCustomerAndBetweenTime empty list customer id not found success`() {
        val now = Instant.now()
        val queryParam = RepositoryQueryParams()

        val txLogs = transactionLogRepository.getTransactionLogsForCustomerAndBetweenTime(queryParam, UUID.randomUUID(),
                now.minusSeconds(20), now.plusSeconds(20)).result

        assertEquals(0, txLogs.count())
    }

    @Test
    fun `test getTransactionLogsForCustomerAndBetweenTime empty list date start not in limits success`() {
        val now = Instant.now()
        val queryParam = RepositoryQueryParams()
        val customerId = accountRepository.getAccountStateById(accountId).state.data.accountData.customerId

        val txLogs = transactionLogRepository.getTransactionLogsForCustomerAndBetweenTime(queryParam, customerId,
                now.plusSeconds(10), now.plusSeconds(20)).result

        assertEquals(0, txLogs.count())
    }

    @Test
    fun `test getTransactionLogsForCustomerAndBetweenTime empty list date end not in limits success`() {
        val now = Instant.now()
        val queryParam = RepositoryQueryParams()

        val txLogs = transactionLogRepository.getTransactionLogsForCustomerAndBetweenTime(queryParam, UUID.randomUUID(),
                now.minusSeconds(20), now.minusSeconds(15)).result

        assertEquals(0, txLogs.count())
    }

    @Test
    fun `test getTransactionById success`() {
        val txLog = transactionLogRepository.getTransactionLogById(transferTx.tx.id.toString())

        assertEquals(transferTx.tx.id.toString(), txLog.txId)
        assertEquals(accountId, txLog.accountFrom)
        assertEquals(accountTransferToId, txLog.accountTo)
        assertEquals(10000L, txLog.amount)
        assertEquals(EUR.toString(), txLog.currency)
        assertEquals(TransactionType.TRANSFER, txLog.txType)
    }

    @Test
    fun `test getTransactionById fails with no transaction for id`() {

        val message = assertFailsWith<IllegalArgumentException> {
            transactionLogRepository.getTransactionLogById("fake_id")
        }.message!!

        assertEquals("Transactions with id fake_id does not exist", message)
    }

    @Test
    fun `test getTransactionsPaginated happy path unfiltered unsorted`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                null, RepositoryQueryParams.SortOrder.DESC, "")
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams)
        val expectedAccountsMap = mapOf(accountId to Pair(3, 3), account2Id to Pair(2, 3), accountTransferToId to Pair(2, 3))
        validateTransactionDetails(expectedAccountsMap, txLogs)
    }

    @Test
    fun `test getTransactionsPaginated happy path unfiltered page two`() {
        val repositoryQueryParams = RepositoryQueryParams(2, 4,
                "txDate", RepositoryQueryParams.SortOrder.ASC, "")
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams, expectedPageCount = 4, expectedTotalPages = 3)
        val expectedAccountsMap = mapOf(accountId to Pair(1, 2), account2Id to Pair(2, 1), accountTransferToId to Pair(1, 1))
        validateTransactionDetails(expectedAccountsMap, txLogs, 0, 0, 4)
    }

    @Test
    fun `test getTransactionsPaginated success empty set for unexistent page`() {
        val repositoryQueryParams = RepositoryQueryParams(2, 10,
                null, RepositoryQueryParams.SortOrder.ASC, "")
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, null)
        validatePaginationDetails(txLogs, repositoryQueryParams, expectedPageCount = 0, expectedTotalPages = 1)
    }

    @Test
    fun `test getTransactionsPaginated success empty set no search term with date filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(2, 10,
                null, RepositoryQueryParams.SortOrder.ASC, "")
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, Instant.now(), Instant.now())
        validatePaginationDetails(txLogs, repositoryQueryParams, expectedTotalCount = 0, expectedPageCount = 0, expectedTotalPages = 0)
    }

    @Test
    fun `test getTransactionsPaginated success full set no search term with date filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                null, RepositoryQueryParams.SortOrder.DESC, "")
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams)
        val expectedAccountsMap = mapOf(accountId to Pair(3, 3), account2Id to Pair(2, 3), accountTransferToId to Pair(2, 3))
        validateTransactionDetails(expectedAccountsMap, txLogs)
    }

    @Test
    fun `test getTransactionsPaginated success partial set no search term with dateFrom filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.DESC, "")
        val allTxLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, null)
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, allTxLogs.result[5].txDate, null)

        validatePaginationDetails(txLogs, repositoryQueryParams, expectedTotalCount = 6, expectedPageCount = 6)
        val expectedAccountsMap = mapOf(accountId to Pair(2, 2), account2Id to Pair(2, 2), accountTransferToId to Pair(2, 1))
        validateTransactionDetails(expectedAccountsMap, txLogs, expectedWithdrawals = 1, expectedTransfers = 5, expectedDeposits = 0)
    }

    @Test
    fun `test getTransactionsPaginated success partial set no search term with dateTo filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.DESC, "")
        val allTxLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, null)
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, allTxLogs.result[5].txDate)

        validatePaginationDetails(txLogs, repositoryQueryParams, expectedTotalCount = 5, expectedPageCount = 5)
        val expectedAccountsMap = mapOf(accountId to Pair(1, 2), account2Id to Pair(0, 1), accountTransferToId to Pair(1, 2))
        validateTransactionDetails(expectedAccountsMap, txLogs, expectedWithdrawals = 0, expectedTransfers = 2, expectedDeposits = 3)
    }

    @Test
    fun `test getTransactionsPaginated success with full account id filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.DESC, accountId.toString())
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams, expectedTotalCount = 6, expectedPageCount = 6)
        val expectedAccountsMap = mapOf(accountId to Pair(3, 3), account2Id to Pair(1, 1), accountTransferToId to Pair(1, 1))
        validateTransactionDetails(expectedAccountsMap, txLogs, expectedWithdrawals = 1, expectedTransfers = 4, expectedDeposits = 1)
    }

    @Test
    fun `test getTransactionsPaginated success with partial account id filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.DESC, account2Id.toString().substring(4, 15))
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams, expectedTotalCount = 5, expectedPageCount = 5)
        val expectedAccountsMap = mapOf(accountId to Pair(1, 1), account2Id to Pair(2, 3), accountTransferToId to Pair(1, 1))
        validateTransactionDetails(expectedAccountsMap, txLogs, expectedWithdrawals = 0, expectedTransfers = 4, expectedDeposits = 1)
    }

    @Test
    fun `test getTransactionsPaginated success with partial tx type filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.DESC, "DEPO")
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams, expectedTotalCount = 3, expectedPageCount = 3)
        val expectedAccountsMap = mapOf(accountId to Pair(0, 1), account2Id to Pair(0, 1), accountTransferToId to Pair(0, 1))
        validateTransactionDetails(expectedAccountsMap, txLogs, expectedWithdrawals = 0, expectedTransfers = 0, expectedDeposits = 3)
    }

    @Test
    fun `test getTransactionsPaginated success with partial customer name filtering`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.DESC, "AN Ot")
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams)
        val expectedAccountsMap = mapOf(accountId to Pair(3, 3), account2Id to Pair(2, 3), accountTransferToId to Pair(2, 3))
        validateTransactionDetails(expectedAccountsMap, txLogs)
    }

    @Test
    fun `test getTransactionsPaginated success with accountFrom sorting`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "accountFrom", RepositoryQueryParams.SortOrder.DESC, "")
        val txLogs = transactionLogRepository.getTransactionsPaginated(repositoryQueryParams, null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams)
        val expectedAccountsMap = mapOf(accountId to Pair(3, 3), account2Id to Pair(2, 3), accountTransferToId to Pair(2, 3))
        validateTransactionDetails(expectedAccountsMap, txLogs)

        val accountsSortedList: List<String> = listOf(accountId.toString(), account2Id.toString(), accountTransferToId.toString())
                .sorted().reversed()

        assertEquals(accountsSortedList, txLogs.result.filter { it.accountFrom != null }.map { it.accountFrom.toString() }.distinct())
    }

    @Test
    fun `test getTransactionsPaginated fails with unexistent search column`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "testtest", RepositoryQueryParams.SortOrder.ASC, "")

        val message = assertFailsWith<IllegalArgumentException> {
            transactionLogRepository.getTransactionsPaginated(repositoryQueryParams,
                    null, null)
        }.message!!
        assertEquals("Invalid sort field testtest", message)
    }

    @Test
    fun `test getTransactionsForAccountPaginated happy path unfiltered unsorted`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                null, RepositoryQueryParams.SortOrder.DESC, "")
        val txLogs = transactionLogRepository.getTransactionsForAccountPaginated(repositoryQueryParams, accountId,null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams, 6, 6)
        val expectedAccountsMap = mapOf(accountId to Pair(3, 3), account2Id to Pair(1, 1), accountTransferToId to Pair(1, 1))
        validateTransactionDetails(expectedAccountsMap, txLogs,1, 1, 4)
    }

    @Test
    fun `test getTransactionsForAccountPaginated happy path empty set for currency`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                null, RepositoryQueryParams.SortOrder.DESC, "GBP")
        val txLogs = transactionLogRepository.getTransactionsForAccountPaginated(repositoryQueryParams, accountId,null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams, 0, 0, 0)
        val expectedAccountsMap = mapOf(accountId to Pair(0, 0), account2Id to Pair(0, 0), accountTransferToId to Pair(0, 0))
        validateTransactionDetails(expectedAccountsMap, txLogs,0, 0, 0)
    }

    @Test
    fun `test getTransactionsForAccountPaginated happy path filtered with currency`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                "txDate", RepositoryQueryParams.SortOrder.DESC, "EUR")
        val txLogs = transactionLogRepository.getTransactionsForAccountPaginated(repositoryQueryParams, accountId,null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams, 6, 6)
        val expectedAccountsMap = mapOf(accountId to Pair(3, 3), account2Id to Pair(1, 1), accountTransferToId to Pair(1, 1))
        validateTransactionDetails(expectedAccountsMap, txLogs,1, 1, 4)
    }

    @Test
    fun `test getTransactionsForAccountPaginated happy path empty set unexistent accountId`() {
        val repositoryQueryParams = RepositoryQueryParams(1, 10,
                null, RepositoryQueryParams.SortOrder.DESC, "EUR")
        val txLogs = transactionLogRepository.getTransactionsForAccountPaginated(repositoryQueryParams, UUID.randomUUID(),null, null)

        validatePaginationDetails(txLogs, repositoryQueryParams, 0, 0, 0)
        val expectedAccountsMap = mapOf(accountId to Pair(0, 0), account2Id to Pair(0, 0), accountTransferToId to Pair(0, 0))
        validateTransactionDetails(expectedAccountsMap, txLogs,0, 0, 0)
    }

    private fun validatePaginationDetails(response: PaginatedResponse<TransactionLogSchemaV1.TransactionLog>,
                                          repositoryQueryParams: RepositoryQueryParams,
                                          expectedTotalCount: Long = 10, expectedPageCount: Int = 10,
                                          expectedTotalPages: Int = 1) {

        assertEquals(expectedTotalCount, response.totalResults)
        assertEquals(repositoryQueryParams.pageSize, response.pageSize)
        assertEquals(expectedTotalPages, response.totalPages)
        assertEquals(repositoryQueryParams.startPage, response.pageNumber)
        assertEquals(expectedPageCount, response.result.count())
    }

    private fun validateTransactionDetails(expectedAccountsMap: Map<UUID, Pair<Int, Int>>,
                                           txLogs: PaginatedResponse<TransactionLogSchemaV1.TransactionLog>,
                                           expectedWithdrawals: Int = 1, expectedDeposits: Int = 3,
                                           expectedTransfers: Int = 6) {

        assertEquals(expectedWithdrawals, txLogs.result.count { it.txType == TransactionType.WITHDRAWAL })
        assertEquals(expectedDeposits, txLogs.result.count { it.txType == TransactionType.DEPOSIT })
        assertEquals(expectedTransfers, txLogs.result.count { it.txType == TransactionType.TRANSFER })
        expectedAccountsMap.entries.forEach { entry ->
            assertEquals(entry.value.first, txLogs.result.count { it.accountFrom == entry.key })
            assertEquals(entry.value.second, txLogs.result.count { it.accountTo == entry.key })
        }
    }
}