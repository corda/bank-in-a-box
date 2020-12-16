package com.r3.refapp.it

import com.r3.refapp.flows.*
import com.r3.refapp.flows.reports.GetTransactionsForCustomerPaginatedFlow
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.schemas.TransactionLogSchemaV1
import com.r3.refapp.schemas.TransactionType
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.issueLoan
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals

class GetTransactionsForCustomerPaginatedFlowTests: AbstractITHelper() {

    @Test
    fun `test GetCustomerTransactionsFlow happy path`() {

        val account1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())
        val account2 = prepareCurrentAccount("PartyA - Customer2", bank, network, emptyList())
        val loanAccount = issueLoan(account1.accountData.accountId, 10 of EUR, bank, network)
        val now = Instant.now()
        val queryParam = RepositoryQueryParams()

        executeFlowWithRunNetwork(IntrabankPaymentFlow(account1.accountData.accountId, loanAccount.accountData.accountId, 2 of EUR), bank, network)
        executeFlowWithRunNetwork(IntrabankPaymentFlow(account1.accountData.accountId, account2.accountData.accountId, 3 of EUR), bank, network)
        executeFlowWithRunNetwork(DepositFiatFlow(loanAccount.accountData.accountId, 4 of EUR), bank, network)

        val transactionLogsCustomer1 = executeFlowWithRunNetwork(GetTransactionsForCustomerPaginatedFlow(queryParam, account1.accountData.customerId,
                now.minusSeconds(20), now.plusSeconds(20)), bank, network).result
        val transactionLogsCustomer2 = executeFlowWithRunNetwork(GetTransactionsForCustomerPaginatedFlow(queryParam, account2.accountData.customerId,
                now.minusSeconds(20), now.plusSeconds(20)), bank, network).result

        assertEquals(3, transactionLogsCustomer1.count())
        assertEquals(1, transactionLogsCustomer2.count())

        val paymentCurrent1ToLoan = transactionLogsCustomer1.single { it.accountFrom == account1.accountData.accountId
                && it.accountTo == loanAccount.accountData.accountId }
        verifyTransactionLog(account1.accountData.accountId, loanAccount.accountData.accountId, 200, TransactionType.TRANSFER,
                paymentCurrent1ToLoan)

        val paymentCurrent1ToCurrent2 = transactionLogsCustomer1.single { it.accountFrom == account1.accountData.accountId
                && it.accountTo == account2.accountData.accountId }
        verifyTransactionLog(account1.accountData.accountId, account2.accountData.accountId,
                300, TransactionType.TRANSFER, paymentCurrent1ToCurrent2)

        val depositToLoan = transactionLogsCustomer1.single { it.accountFrom == null
                && it.accountTo == loanAccount.accountData.accountId }
        verifyTransactionLog(null, loanAccount.accountData.accountId,
                400, TransactionType.DEPOSIT, depositToLoan)

        val paymentToCurrent2 = transactionLogsCustomer2.single()
        verifyTransactionLog(account1.accountData.accountId, account2.accountData.accountId,
                300, TransactionType.TRANSFER, paymentToCurrent2)

    }

    @Test
    fun `test GetCustomerTransactionsFlow empty list happy path`() {

        val now = Instant.now()
        val queryParam = RepositoryQueryParams()
        val transactionLogs = executeFlowWithRunNetwork(GetTransactionsForCustomerPaginatedFlow(queryParam, UUID.randomUUID(),
                now.minusSeconds(20), now.plusSeconds(20)), bank, network).result

        assertEquals(0, transactionLogs.count())

    }

    private fun verifyTransactionLog(expectedAccountFrom: UUID?, expectedAccountTo: UUID?, expectedAmount: Long,
                                     expectedTxType: TransactionType, actual: TransactionLogSchemaV1.TransactionLog) {
        assertEquals(expectedAccountFrom, actual.accountFrom)
        assertEquals(expectedAccountTo, actual.accountTo)
        assertEquals(expectedAmount, actual.amount)
        assertEquals(expectedTxType, actual.txType)
    }
}