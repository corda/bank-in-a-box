package com.r3.refapp.it

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.r3.refapp.contracts.FinancialAccountContract
import com.r3.refapp.flows.CreateCordaAccountFlow
import com.r3.refapp.flows.internal.OracleSignCreditRatingRequestFlow
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.states.AccountData
import com.r3.refapp.states.AccountStatus
import com.r3.refapp.states.CreditRatingInfo
import com.r3.refapp.states.LoanAccountState
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.TestUtils.createAttachment
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.getRefappMockNetworkWithNotaryAndOracle
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.core.contracts.Command
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.security.PublicKey
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.function.Predicate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@Execution(ExecutionMode.SAME_THREAD)
class OracleSignCreditRatingRequestFlowTest {

    lateinit var bank: StartedMockNode
    lateinit var oracle: StartedMockNode
    lateinit var network: MockNetwork
    lateinit var wireMockServer: WireMockServer
    lateinit var attachments: List<Pair<SecureHash, String>>
    lateinit var accountRepository: AccountRepository

    fun setup(ratingHostPortMissing: Boolean) {
        wireMockServer = WireMockServer(0)
        wireMockServer.start()
        network = getRefappMockNetworkWithNotaryAndOracle("O=Notary Service, L=Zurich, C=CH", wireMockServer
                .port(), 600, 2, ratingHostPortMissing)
        bank = network.createPartyNode()
        oracle = network.createPartyNode()
        attachments = listOf(createAttachment("test", bank, network))
        accountRepository = bank.services.cordaService(AccountRepository::class.java)
    }

    fun tearDown() {
        network.stopNodes()
        if(wireMockServer.isRunning()) {
            wireMockServer.stop()
        }
    }


    @Test
    fun `test OracleSignCreditRatingRequestFlow happy path`() {

        setup(false)
        val customerName = "Customer 1"
        val oracleKey = oracle.services.ourIdentity.owningKey
        val notary = ConfigurationUtils.getConfiguredNotary(bank.services)
        val repaymentAccountIn = prepareCurrentAccount(customerName, bank, network, attachments)
        val repaymentAccountOut = repaymentAccountIn.deposit(1000 of EUR)
        val loanAccountState = getLoanAccountState(repaymentAccountOut.accountData.customerId)

        createCreditRatingStub(600, customerName, repaymentAccountIn.accountData.customerId.toString())

        val verifyCreditRatingCommand = getVerifyCreditRatingCommand(customerName, repaymentAccountIn.accountData.customerId)
        val commandIssueLoan = getIssueLoanCommand(repaymentAccountIn.accountData.accountInfo.identifier)

        val txBuilder = TransactionBuilder(notary)
                .addInputState(accountRepository.getAccountStateById(repaymentAccountIn.linearId.id))
                .addOutputState(repaymentAccountOut)
                .addOutputState(loanAccountState)
                .addCommand(commandIssueLoan)
                .addCommand(verifyCreditRatingCommand)
                .setTimeWindow(TimeWindow.between(verifyCreditRatingCommand.value.dateStart, verifyCreditRatingCommand.value.dateStart.plus(Duration.ofDays(1))))
        txBuilder.verify(bank.services)

        val partStx = bank.services.signInitialTransaction(txBuilder)
        val filteredTx = getFilteredTx(partStx, oracleKey)

        val oracleSignature = executeFlowWithRunNetwork(OracleSignCreditRatingRequestFlow(txBuilder, oracle.services.ourIdentity,
                filteredTx), oracle, network)

        assertNotNull(oracleSignature)
        assertEquals(oracleKey, oracleSignature.by)
        tearDown()
    }

    @Test
    fun `test OracleSignCreditRatingRequestFlow fails with uncofigured rating server`() {

        setup(true)
        val customerName = "Customer 1"
        val oracleKey = oracle.services.ourIdentity.owningKey
        val notary = ConfigurationUtils.getConfiguredNotary(bank.services)
        val repaymentAccountIn = prepareCurrentAccount(customerName, bank, network, attachments)
        val repaymentAccountOut = repaymentAccountIn.deposit(1000 of EUR)
        val loanAccountState = getLoanAccountState(repaymentAccountOut.accountData.customerId, bank, network)

        createCreditRatingStub(600, customerName, repaymentAccountIn.accountData.customerId.toString())

        val verifyCreditRatingCommand = getVerifyCreditRatingCommand(customerName, repaymentAccountIn.accountData.customerId)
        val commandIssueLoan = getIssueLoanCommand(repaymentAccountIn.accountData.accountInfo.identifier, bank.services.ourIdentity.owningKey)

        val txBuilder = TransactionBuilder(notary)
                .addInputState(accountRepository.getAccountStateById(repaymentAccountIn.linearId.id))
                .addOutputState(repaymentAccountOut)
                .addOutputState(loanAccountState)
                .addCommand(commandIssueLoan)
                .addCommand(verifyCreditRatingCommand)
                .setTimeWindow(TimeWindow.between(verifyCreditRatingCommand.value.dateStart, verifyCreditRatingCommand.value.dateStart.plus(Duration.ofDays(1))))
        txBuilder.verify(bank.services)

        val partStx = bank.services.signInitialTransaction(txBuilder)
        val filteredTx = getFilteredTx(partStx, oracleKey)

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(OracleSignCreditRatingRequestFlow(txBuilder, oracle.services.ourIdentity,
                    filteredTx), oracle, network)
        }.message

        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Missing required configuration " +
                "property: credit_rating_host_port. Please check your configuration!", message)
        tearDown()
    }

    private fun getFilteredTx(partStx: SignedTransaction, oracleKey: PublicKey): FilteredTransaction {
        val mtx = partStx.buildFilteredTransaction(Predicate {
            when (it) {
                is Command<*> -> oracleKey in it.signers && it.value is FinancialAccountContract.Commands.VerifyCreditRating
                else -> false
            }
        })
        return mtx
    }

    private fun getVerifyCreditRatingCommand(customerName: String, customerId: UUID):
            Command<FinancialAccountContract.Commands.VerifyCreditRating> {
        val creditRatingInfo = CreditRatingInfo(customerName, customerId, 600, Instant.now())

        return Command(FinancialAccountContract.Commands.VerifyCreditRating(creditRatingInfo,
                500, oracle.services.ourIdentity.owningKey, Instant.now(), Duration.ofDays(1)), listOf(bank.services
                .ourIdentity.owningKey, oracle.services.ourIdentity.owningKey))
    }

    private fun getIssueLoanCommand(repaymentAccountId: UniqueIdentifier, bankKey: PublicKey = bank.services.ourIdentity.owningKey) =
            Command(FinancialAccountContract.Commands.IssueLoan(repaymentAccountId, 1000 of EUR), bankKey)

    private fun getLoanAccountState(customerId: UUID, bankNode: StartedMockNode = bank, mockNetwork: MockNetwork = network)
            : LoanAccountState {
        val cordaLoanAccount = executeFlowWithRunNetwork(CreateCordaAccountFlow(), bankNode, mockNetwork).state.data
        return LoanAccountState(AccountData(cordaLoanAccount.identifier.id, cordaLoanAccount, customerId,
                1000 of EUR, Instant.now(), AccountStatus.ACTIVE))
    }

    fun createCreditRatingStub(rating: Int, customerName: String, customerId: String) {
        wireMockServer.stubFor(WireMock.get(WireMock.urlMatching("/creditRating/customer/.*"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "    \"customerName\": \"${customerName}\",\n" +
                                "    \"customerId\": \"${customerId}\",\n" +
                                "    \"rating\": ${rating},\n" +
                                "    \"time\": \"${Instant.now()}\"\n" +
                                "}")))
    }
}