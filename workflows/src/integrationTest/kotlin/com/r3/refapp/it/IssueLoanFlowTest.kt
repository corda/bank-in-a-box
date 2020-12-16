package com.r3.refapp.it

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.flows.IssueLoanFlow
import com.r3.refapp.repositories.RecurringPaymentRepository
import com.r3.refapp.states.Account
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.states.LoanAccountState
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.schemas.PersistentStateRef
import net.corda.testing.node.MockNetwork
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue


class IssueLoanFlowTest : AbstractITHelper() {

    private val loanTerm = 5 * 12 // 5 years in months
    private val loanAmount = 10000

    @Test
    fun `test issue loan`() {

        val currentAccount = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())
        executeFlowWithRunNetwork(DepositFiatFlow(currentAccount.accountData.accountId, 500 of EUR), bank, network)

        val loanAccount = executeFlowWithRunNetwork(
                IssueLoanFlow(currentAccount.accountData.accountId, loanAmount of EUR, loanTerm),
                bank, network).coreTransaction.outputsOfType<LoanAccountState>().single()

        val currentAccountBalance = getAccountBalanceForAccount(currentAccount)
        val loanAccountBalance = getAccountBalanceForAccount(loanAccount)

        assertEquals(10500 of EUR, currentAccountBalance.state.data.accountData.balance)
        assertEquals(loanAmount of EUR, loanAccountBalance.state.data.accountData.balance)

        val recurringPayments = recurringPaymentRepository.getRecurringPaymentsForCustomer(loanAccount.accountData.customerId)
        assertEquals(1, recurringPayments.count())

        val recurringPayment = recurringPayments.single().state.data
        assertEquals(currentAccount.accountData.accountId, recurringPayment.accountFrom)
        assertEquals(loanAccount.accountData.accountId, recurringPayment.accountTo)

        val monthlyPayment = 166.67 of EUR
        assertEquals(monthlyPayment, recurringPayment.amount)
        assertTrue(Instant.now().plus(Duration.ofDays(29)) <= recurringPayment.dateStart,
                "Recuring payment should star 30 days from now"
        )
        assertEquals(Duration.ofDays(30), recurringPayment.period)
    }

    @Test
    fun `test fail issue loan on not enough credit rating`() {
        val currentAccount = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())

        createCreditRatingStubForClient(currentAccount.accountData.customerId, 550)

        val exception = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(
                    IssueLoanFlow(currentAccount.accountData.accountId, loanAmount of EUR, loanTerm),
                    bank, network)
        }
        val expectedMsg = "Refapp exception: Credit rating for ${currentAccount.accountData.customerId} customer not enough for receiving a loan"
        exception.message?.contains(expectedMsg)?.let { assertTrue(it,"Expected \n $expectedMsg \n but got \n${exception.message}") }
    }

    fun createCreditRatingStubForClient(customerID: UUID, rating: Int) {
        wireMockServer.stubFor(get(urlEqualTo("/creditRating/customer/$customerID"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "    \"customerName\": \"Customer Name\",\n" +
                                "    \"customerId\": \"$customerID\",\n" +
                                "    \"rating\": ${rating},\n" +
                                "    \"time\": \"${Instant.now()}\"\n" +
                                "}")))
    }
}