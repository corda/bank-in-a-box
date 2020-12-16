package com.r3.refapp.test.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.r3.refapp.flows.CreateRecurringPaymentFlow
import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.flows.IntrabankPaymentFlow
import com.r3.refapp.flows.WithdrawFiatFlow
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.repositories.RecurringPaymentLogRepository
import com.r3.refapp.repositories.RecurringPaymentRepository
import com.r3.refapp.repositories.TransactionLogRepository
import com.r3.refapp.states.Account
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.node.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractITHelper {

    companion object {
        lateinit var network: MockNetwork
        lateinit var bank: StartedMockNode
        lateinit var bank2: StartedMockNode
        lateinit var attachments: List<Pair<SecureHash, String>>
        lateinit var attachments2: List<Pair<SecureHash, String>>
        lateinit var recurringPaymentLogRepository: RecurringPaymentLogRepository
        lateinit var recurringPaymentRepository: RecurringPaymentRepository
        lateinit var accountRepository: AccountRepository
        lateinit var transactionLogRepository: TransactionLogRepository
        lateinit var wireMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun setup() {
            wireMockServer = WireMockServer(0)
            createCreditRatingStubForAll(650)
            wireMockServer.start()
            network = TestUtils.getRefappMockNetworkWithNotaryAndOracle("O=Notary Service, L=Zurich, C=CH", wireMockServer.port())//wireMockServer.port())
            bank = network.createPartyNode()
            bank2 = network.createPartyNode()
            attachments = listOf(TestUtils.createAttachment("test", bank, network))
            attachments2 = listOf(TestUtils.createAttachment("test", bank2, network))
            recurringPaymentLogRepository = bank.services.cordaService(RecurringPaymentLogRepository::class.java)
            recurringPaymentRepository = bank.services.cordaService(RecurringPaymentRepository::class.java)
            accountRepository = bank.services.cordaService(AccountRepository::class.java)
            transactionLogRepository = bank.services.cordaService(TransactionLogRepository::class.java)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            network.stopNodes()
            if(wireMockServer.isRunning()) {
                wireMockServer.stop()
            }
        }

        fun createCreditRatingStubForAll(rating: Int) {
            wireMockServer.stubFor(WireMock.get(WireMock.urlMatching("/creditRating/customer/.*"))
                    .willReturn(WireMock.aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\n" +
                                    "    \"customerName\": \"Customer Name\",\n" +
                                    "    \"customerId\": \"697f8245-4e04-42ea-a2ef-3043d5d62db2\",\n" +
                                    "    \"rating\": ${rating},\n" +
                                    "    \"time\": \"${Instant.now()}\"\n" +
                                    "}")))
        }

        fun createRecurringPayment(customerFromId: UUID, customerToId: UUID,
                                   dateStart: Instant = Instant.now().plus(Duration.ofDays(10)),
                                   period: Duration = Duration.ofSeconds(10), iterationNum: Int = 2): RecurringPaymentState {
            val tx = executeFlowWithRunNetwork(CreateRecurringPaymentFlow(customerFromId, customerToId, 10 of EUR, dateStart,
                    period, iterationNum), bank, network)

            return tx.coreTransaction.outputsOfType<RecurringPaymentState>().single()
        }
    }

    protected fun verifyRecurringPayment(customer1Acc: CurrentAccountState, customer2Acc: CurrentAccountState, recurringPayment: RecurringPaymentState,
                                         now: Instant, balanceAccFromBefore: Amount<Currency> = 50 of EUR,
                                         balanceAccFromAfter: Amount<Currency> = 40 of EUR,
                                         balanceAccToBefore: Amount<Currency> = 0 of EUR,
                                         balanceAccToAfter: Amount<Currency> = 10 of EUR) {

        val accountStatesBefore = bank.services.vaultService.queryBy<CurrentAccountState>().states
        val vaultRecurringPayment = bank.services.vaultService.queryBy<RecurringPaymentState>(
                QueryCriteria.LinearStateQueryCriteria(linearId = listOf(recurringPayment.linearId))).states.single().state.data

        assertEquals(customer1Acc.accountData.accountId, recurringPayment.accountFrom)
        assertEquals(customer2Acc.accountData.accountId, recurringPayment.accountTo)
        assertEquals(balanceAccFromBefore,
                accountStatesBefore.single { it.state.data.accountData.accountId == customer1Acc.accountData.accountId }
                        .state.data.accountData.balance)
        assertEquals(balanceAccToBefore,
                accountStatesBefore.single { it.state.data.accountData.accountId == customer2Acc.accountData.accountId }
                        .state.data.accountData.balance)
        assertEquals(10 of EUR, recurringPayment.amount)
        assertEquals(now, recurringPayment.dateStart)
        assertEquals(Duration.of(1, ChronoUnit.DAYS), recurringPayment.period)
        assertEquals(recurringPayment, vaultRecurringPayment)

        (bank.services.clock as TestClock).advanceBy(Duration.of(1, ChronoUnit.DAYS))
        Thread.sleep(1000 * 2)
        network.runNetwork()

        val vaultRecurringPaymentAfter = bank.services.vaultService.queryBy<RecurringPaymentState>(
                QueryCriteria.LinearStateQueryCriteria(linearId = listOf(recurringPayment.linearId))).states.single().state.data
        val accountStatesAfter = bank.services.vaultService.queryBy<CurrentAccountState>().states

        assertEquals(customer1Acc.accountData.accountId, vaultRecurringPaymentAfter.accountFrom)
        assertEquals(customer2Acc.accountData.accountId, vaultRecurringPaymentAfter.accountTo)
        assertTrue(balanceAccFromAfter >=
                accountStatesAfter.single { it.state.data.accountData.accountId == customer1Acc.accountData.accountId }
                        .state.data.accountData.balance)
        assertTrue(balanceAccToAfter <=
                accountStatesAfter.single { it.state.data.accountData.accountId == customer2Acc.accountData.accountId }
                        .state.data.accountData.balance)
        assertEquals(10 of EUR, vaultRecurringPaymentAfter.amount)
        assertTrue(now.plusSeconds(3) <= vaultRecurringPaymentAfter.dateStart)
        assertEquals(Duration.of(1, ChronoUnit.DAYS), recurringPayment.period)
    }

    fun prepareTransactions(): List<Account> {
        val account1 = prepareCurrentAccount("Customer1", bank, network)
        val account2 = prepareCurrentAccount("Customer2", bank, network)
        val account3 = prepareCurrentAccount("Customer3", bank, network)

        executeFlowWithRunNetwork(DepositFiatFlow(account1.accountData.accountId, 100 of EUR), bank, network)
        executeFlowWithRunNetwork(DepositFiatFlow(account2.accountData.accountId, 100 of EUR), bank, network)
        executeFlowWithRunNetwork(IntrabankPaymentFlow(account1.accountData.accountId, account3.accountData.accountId, 20 of EUR),
                bank, network)
        executeFlowWithRunNetwork(WithdrawFiatFlow(account2.accountData.accountId, 100 of EUR), bank, network)

        return listOf(account1, account2, account3)
    }

    inline fun <reified T: Account> getAccountBalanceForAccount(account: T, status: Vault.StateStatus = Vault.StateStatus.UNCONSUMED)
            : StateAndRef<T> = queryAccountBalancesForAccount(account, status).maxBy { it.state.data.accountData.balance }!!

    inline fun <reified T: Account> queryAccountBalancesForAccount(account: T, status: Vault.StateStatus = Vault.StateStatus.UNCONSUMED)
            : List<StateAndRef<T>> {

        val accountFromCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(account.linearId.id), status = status)

        return bank.services.vaultService.queryBy<T>(accountFromCriteria).states
    }
}