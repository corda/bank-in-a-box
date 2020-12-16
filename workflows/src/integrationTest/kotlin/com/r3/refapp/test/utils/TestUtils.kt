package com.r3.refapp.test.utils

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.refapp.flows.*
import com.r3.refapp.states.*
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.InputStreamAndHash
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.*
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import java.security.PublicKey
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object TestUtils {

    fun <T> executeFlowWithRunNetwork(flow: FlowLogic<T>, node: StartedMockNode, network: MockNetwork): T {
        val flowFuture = node.startFlow(flow)
        // sometimes a retry is necessary as some messages can arrive after runNetwork and the future gets stuck
        var retries = 5
        while (retries > 0) {
            try {
                network.runNetwork()
                return flowFuture.get(100, TimeUnit.MILLISECONDS)
            } catch(e: TimeoutException) {
                --retries
            }
        }
        throw TimeoutException()
    }

    fun createCustomer(
            customerName: String,
            attachments: List<Pair<SecureHash, String>>,
            node: StartedMockNode,
            network: MockNetwork,
            contactNumber: String = "123456789",
            emailAddress: String = "test-email@r3.com",
            postCode: String = "D01 K11"): UUID {

        val customerAttachments = if (attachments.isEmpty()) listOf(createAttachment(node = node, network = network)) else attachments
        return executeFlowWithRunNetwork(CreateCustomerFlow(customerName, contactNumber, emailAddress, postCode, customerAttachments), node, network)
    }

    fun prepareCurrentAccount(
            customerPartyName: String,
            node: StartedMockNode,
            network: MockNetwork,
            attachments: List<Pair<SecureHash, String>> = listOf(),
            status: AccountStatus = AccountStatus.ACTIVE,
            withdrawalDailyLimit: Long? = null,
            transferDailyLimit: Long? = null
    ) : CurrentAccountState {

        val customerAttachments = if (attachments.isEmpty()) listOf(createAttachment(node = node, network = network)) else attachments
        val customerId = createCustomer(customerPartyName, customerAttachments, node, network)

        val account = executeFlowWithRunNetwork(
                CreateCurrentAccountFlow(customerId, EUR, withdrawalDailyLimit, transferDailyLimit), node, network)
                .coreTransaction.outputsOfType<CurrentAccountState>().single()
        return executeFlowWithRunNetwork(SetAccountStatusFlow(account.accountData.accountId, status), node, network)
                .coreTransaction.outputsOfType<CurrentAccountState>().single()
    }

    fun createCurrentAccount(customerId: UUID, tokenType: Currency = EUR, node: StartedMockNode, network: MockNetwork) : CurrentAccountState {
        return executeFlowWithRunNetwork(CreateCurrentAccountFlow(customerId, tokenType), node, network)
                .coreTransaction.outputsOfType<CurrentAccountState>().single()
    }

    fun prepareOverdraftAccount(
            customerPartyName: String,
            node: StartedMockNode,
            network: MockNetwork,
            attachments: List<Pair<SecureHash, String>> = emptyList(), status: AccountStatus = AccountStatus.ACTIVE) :
            CurrentAccountState {
        val currentAccount = prepareCurrentAccount(customerPartyName, node, network, attachments)
        return executeFlowWithRunNetwork(ApproveOverdraftFlow(currentAccount.accountData.accountId, 2000), node, network)
                .coreTransaction.outputsOfType<CurrentAccountState>().single()
    }

    fun prepareSavingsAccount(
            customerPartyName: String,
            node: StartedMockNode,
            network: MockNetwork,
            attachments: List<Pair<SecureHash, String>>) : SavingsAccountState {

        val customerAttachments = if (attachments.isEmpty()) listOf(createAttachment(node = node, network = network)) else attachments
        val customerId = createCustomer(customerPartyName, customerAttachments, node, network)

        val account = createSavingsAccount(customerId, EUR, node, network)
        return executeFlowWithRunNetwork(SetAccountStatusFlow(account.accountData.accountId, AccountStatus.ACTIVE), node, network)
                .coreTransaction.outputsOfType<SavingsAccountState>().single()
    }

    fun createAttachment(uploader: String = "test", node: StartedMockNode, network: MockNetwork) : Pair<SecureHash, String> {
        val attachment = node.services.attachments.importAttachment(
                InputStreamAndHash.createInMemoryTestZip(RandomUtils.nextInt(1, 1024),
                        RandomUtils.nextInt(0, Byte.MAX_VALUE.toInt()).toByte(), RandomStringUtils.randomAlphanumeric(10)).inputStream,
                uploader, "${RandomStringUtils.randomAlphanumeric(10)}$uploader.zip")

        network.runNetwork()
        return Pair(attachment, "Test attachment")
    }

    fun createSavingsAccount(customerId: UUID, tokenType: Currency = EUR, node: StartedMockNode, network: MockNetwork) : SavingsAccountState {
        val currentAccount = createCurrentAccount(customerId, tokenType, node, network)
        return executeFlowWithRunNetwork(CreateSavingsAccountFlow(customerId, tokenType, currentAccount.accountData.accountId,
                10 of EUR, Instant.now().plusSeconds(10)), node, network)
                .coreTransaction.outputsOfType<SavingsAccountState>().single()
    }

    fun createAccountInfo(accountName: String, node: StartedMockNode, network: MockNetwork) : Pair<AccountInfo, PublicKey> {
        val accountInfo = executeFlowWithRunNetwork(CreateAccount(accountName), node, network).state.data
        val accountKey = executeFlowWithRunNetwork(RequestKeyForAccount(accountInfo), node, network).owningKey
        return Pair(accountInfo, accountKey)
    }

    fun issueLoan(accountId: UUID, loan: Amount<Currency>, node: StartedMockNode, network: MockNetwork) : LoanAccountState {
        return executeFlowWithRunNetwork(IssueLoanFlow(accountId, loan, 5 * 12), node, network)
                .coreTransaction.outputsOfType<LoanAccountState>().single()
    }

    fun getRefappMockNetworkWithNotaryAndOracle(notaryName: String, creditRatingPort: Int, creditLimit: Int = 600,
                                                execThreadNum: Int = 2, ratingHostPortMissing: Boolean = false): MockNetwork {
        val notaryLegalName = CordaX500Name.parse(notaryName)
        val oracleLegalName = CordaX500Name(organisation = "Oracle", locality = "London", country = "GB")

        val mockNetworkParameters = MockNetworkParameters(
                networkParameters = testNetworkParameters(minimumPlatformVersion = 4),
                cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                        TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"),
                        TestCordapp.findCordapp("com.r3.refapp.contracts"),
                        TestCordapp.findCordapp("com.r3.refapp.states"),
                        TestCordapp.findCordapp("com.r3.refapp.flows")
                                .withConfig(listOfNotNull(ConfigurationUtils.NOTARY_NAME_PROPERTY to notaryLegalName.toString(),
                                        if (!ratingHostPortMissing) ConfigurationUtils.CREDIT_RATING_HOST_PORT to
                                                "localhost:$creditRatingPort" else null,
                                        ConfigurationUtils.CREDIT_RATING_THRESHOLD to creditLimit,
                                        ConfigurationUtils.CREDIT_RATING_VALIDITY_DURATION_HOURS to 1,
                                        ConfigurationUtils.EXECPROV_THREAD_NUM to execThreadNum,
                                        ConfigurationUtils.ORACLE_NAME to oracleLegalName.toString(),
                                        ConfigurationUtils.LOAN_REPAYMENT_PERIOD to "P30D"
                                        ).toMap()),
                        TestCordapp.findCordapp("com.r3.refapp.oracle")

                ),
                notarySpecs = listOf(MockNetworkNotarySpec(notaryLegalName))
        )
        val network =  MockNetwork(mockNetworkParameters)
        network.createPartyNode(oracleLegalName)
        return network
    }
}