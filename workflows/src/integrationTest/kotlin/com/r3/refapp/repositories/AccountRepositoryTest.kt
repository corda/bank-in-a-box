package com.r3.refapp.repositories

import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.flows.ApproveOverdraftFlow
import com.r3.refapp.flows.CreateSavingsAccountFlow
import com.r3.refapp.states.*
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.TestUtils.createAccountInfo
import com.r3.refapp.test.utils.TestUtils.createAttachment
import com.r3.refapp.test.utils.TestUtils.createCurrentAccount
import com.r3.refapp.test.utils.TestUtils.createCustomer
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.issueLoan
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.test.utils.TestUtils.prepareOverdraftAccount
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.*
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AccountRepositoryTests : AbstractITHelper() {

    companion object {
        lateinit var accounts: List<Account>

        @BeforeAll
        @JvmStatic
        fun setUpRepo() {

            val account1 = prepareCurrentAccount("Customer1", bank, network, status = AccountStatus.SUSPENDED)
            val account2 = prepareOverdraftAccount("Customer2", bank, network)
            val account3 = prepareCurrentAccount("Customer3", bank, network)
            val account4 = prepareCurrentAccount("Customer4", bank, network)
            val account5 = prepareCurrentAccount("Custommer5", bank, network)
            accounts = listOf(account1, account2, account3, account4, account5)
        }
    }

    @Test
    @Order(2)
    fun `test getCustomerWithId happy path`() {

        val customerName = "test customer"
        val contactNumber = "123456789"
        val customerEmail = "test-email@r3.com"
        val postCode = "D01 K11"

        val attachment = createAttachment(node = bank, network = network)
        val customerId = createCustomer(customerName, listOf(attachment), bank, network)
        val customer = accountRepository.getCustomerWithId(customerId)

        assertEquals(customerName, customer.customerName)
        assertEquals(contactNumber, customer.contactNumber)
        assertEquals(customerEmail, customer.emailAddress)
        assertEquals(postCode, customer.postCode)

        assert(customer.attachments.size == 1)
        assert(attachment.first.toString() == customer.attachments.single().attachmentHash)
    }

    @Test
    @Order(2)
    fun `test getAccountStateByPublicKey happy path`() {

        val currentAccount = prepareCurrentAccount("test Customer", bank, network, attachments)
        val accountState = accountRepository.getAccountStateById(currentAccount.accountData.accountId)

        assertEquals(AccountStatus.ACTIVE, accountState.state.data.accountData.status)
        assertEquals(bank.info.legalIdentities.single(), accountState.state.data.accountData.accountInfo.host)
    }

    @Test
    @Order(2)
    fun `test getAccountStateById happy path`() {

        val currentAccount = prepareCurrentAccount("test Customer", bank, network)
        val accountState = accountRepository.getAccountStateById(currentAccount.accountData.accountId)

        assertEquals(AccountStatus.ACTIVE, accountState.state.data.accountData.status)
        assertEquals(bank.info.legalIdentities.single(), accountState.state.data.accountData.accountInfo.host)
        assertEquals(currentAccount.accountData.accountId, accountState.state.data.accountData.accountId)
    }

    @Test
    @Order(2)
    fun `test getAccountStateById fails with no account for account id`() {

        val accountId = UUID.randomUUID()
        val message = assertFailsWith<RefappException> {
            accountRepository.getAccountStateById(accountId)
        }.message!!
        assertEquals("Refapp exception: Vault query failed. Cannot find ${Account::class.java} with id: $accountId", message)

    }

    @Test
    @Order(2)
    fun `test getAccountKey happy path`() {

        val accountInfo = createAccountInfo("test account success", bank, network)
        val accountInfoKey = accountRepository.getAccountKey(accountInfo.first)
        assertEquals(accountInfo.second, accountInfoKey)
    }

    @Test
    @Order(2)
    fun `test getAccountKey fails with no account key`() {

        val accountInfo = executeFlowWithRunNetwork(CreateAccount("\"test account\""), bank, network).state.data
        val message = assertFailsWith<RefappException> {
            accountRepository.getAccountKey(accountInfo)
        }.message!!
        assertEquals("Refapp exception: AccountInfo with id: ${accountInfo.identifier} not initialised properly, missing accountKey", message)
    }

    @Test
    @Order(2)
    fun `test getCurrentAccountStateById success`() {

        val currentAccount = prepareCurrentAccount("test Customer", bank, network)
        val accountState = accountRepository.getCurrentAccountStateById(currentAccount.accountData.accountId)

        assertEquals(AccountStatus.ACTIVE, accountState.state.data.accountData.status)
        assertEquals(bank.info.legalIdentities.single(), accountState.state.data.accountData.accountInfo.host)
        assertEquals(currentAccount.accountData.accountId, accountState.state.data.accountData.accountId)
        assertEquals(currentAccount.accountData.accountInfo, accountState.state.data.accountData.accountInfo)
    }

    @Test
    @Order(2)
    fun `test getLoanAccountStateById success`() {

        val currentAccount = prepareCurrentAccount("test Customer", bank, network)
        val loanAccount = issueLoan(currentAccount.accountData.accountId, 10 of EUR, bank, network)
        val loanAccountState = accountRepository.getLoanAccountStateById(loanAccount.accountData.accountId)

        assertEquals(AccountStatus.ACTIVE, loanAccountState.state.data.accountData.status)
        assertEquals(bank.info.legalIdentities.single(), loanAccountState.state.data.accountData.accountInfo.host)
        assertEquals(loanAccount.accountData.accountId, loanAccountState.state.data.accountData.accountId)
        assertEquals(loanAccount.accountData.accountInfo, loanAccountState.state.data.accountData.accountInfo)
    }

    @Test
    @Order(2)
    fun `test getCurrentAccountStateById fails with cannot find current account with id of a loan account`() {

        val currentAccount = prepareCurrentAccount("test Customer", bank, network)
        val loanAccount = issueLoan(currentAccount.accountData.accountId, 10 of EUR, bank, network)
        val message = assertFailsWith<RefappException> {
            accountRepository.getCurrentAccountStateById(loanAccount.accountData.accountId)
        }.message!!
        assertEquals("Refapp exception: Vault query failed. Cannot find ${CurrentAccountState::class.java} with id: ${loanAccount.accountData.accountId}", message)
    }

    @Test
    @Order(2)
    fun `test getLoanAccountStateById fails with cannot find loan account with id of a current account`() {

        val currentAccount = prepareCurrentAccount("test Customer", bank, network)
        val message = assertFailsWith<RefappException> {
            accountRepository.getLoanAccountStateById(currentAccount.accountData.accountId)
        }.message!!
        assertEquals("Refapp exception: Vault query failed. Cannot find ${LoanAccountState::class.java} with id: ${currentAccount.accountData.accountId}", message)
    }

    @Test
    @Order(2)
    fun `test getAccountsForCustomer success`() {

        val currentAccount1 = prepareCurrentAccount("test Customer", bank, network)
        val currentAccount2 = createCurrentAccount(currentAccount1.accountData.customerId, EUR, bank, network)

        val overdraftAccount = executeFlowWithRunNetwork(
                ApproveOverdraftFlow(currentAccount2.accountData.accountId, 1000), bank, network)
                .coreTransaction.outputsOfType<CurrentAccountState>().single()

        val loanAccount = issueLoan(currentAccount1.accountData.accountId, 10 of EUR, bank, network)
        val savingsAccount = executeFlowWithRunNetwork(
                CreateSavingsAccountFlow(
                        currentAccount1.accountData.customerId,
                        EUR,
                        currentAccount1.accountData.accountId,
                        10 of EUR,
                        Instant.now().plusSeconds(10)), bank, network)
                .coreTransaction.outputsOfType<SavingsAccountState>().single()

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                null, RepositoryQueryParams.SortOrder.DESC, "")
        val response = accountRepository.getAccountsForCustomerPaginated(repositoryQueryParams, currentAccount1.accountData.customerId,
                null, null)

        assertEquals(4, response.totalResults)
        assertEquals(4, response.result.size)
        assertTrue(response.result.map { it.first.accountData.accountId }.containsAll(
                listOf(currentAccount1.accountData.accountId,
                        overdraftAccount.accountData.accountId, loanAccount.accountData.accountId,
                        savingsAccount.accountData.accountId)))
    }

    @Test
    @Order(2)
    fun `test getAccountsForCustomer success empty list`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                null, RepositoryQueryParams.SortOrder.DESC, "")
        val response = accountRepository.getAccountsForCustomerPaginated(repositoryQueryParams, UUID.randomUUID(),
                null, null)
        assertEquals(0, response.totalPages)
        assertEquals(0, response.result.size)
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success one page no search term no sort no date filtering`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                null, RepositoryQueryParams.SortOrder.DESC, "")
        val response = accountRepository.getAccountsPaginated(repositoryQueryParams, null, null)

        assertEquals(5, response.pageSize)
        assertEquals(5, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)
        accounts.forEach {
            assertTrue(response.result.map { pair -> pair.first }.contains(it) )
        }
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated returns empty list for unexistent page`() {

        val repositoryQueryParams = RepositoryQueryParams(2, 5,
                null, RepositoryQueryParams.SortOrder.DESC, "")
        val response = accountRepository.getAccountsPaginated(repositoryQueryParams, null, null)

        assertEquals(5, response.pageSize)
        assertEquals(5, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(2, response.pageNumber)
        assertEquals(0, response.result.count())
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success page three no search term no date filtering`() {

        val repositoryQueryParams = RepositoryQueryParams(3, 2,
                "customerName", RepositoryQueryParams.SortOrder.ASC, "")
        val response = accountRepository.getAccountsPaginated(repositoryQueryParams, null, null)

        assertEquals(2, response.pageSize)
        assertEquals(5, response.totalResults)
        assertEquals(3, response.totalPages)
        assertEquals(3, response.pageNumber)
        assertEquals(1, response.result.count())
        assertEquals(accounts[4], response.result[0].first)
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success empty set no search term with date filtering`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "customerName", RepositoryQueryParams.SortOrder.ASC, "")
        val response = accountRepository.getAccountsPaginated(repositoryQueryParams, Instant.now(), Instant.now())

        assertEquals(5, response.pageSize)
        assertEquals(0, response.totalResults)
        assertEquals(0, response.totalPages)
        assertEquals(1, response.pageNumber)
        assertEquals(0, response.result.count())
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success full set no search term with date filtering`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "customerName", RepositoryQueryParams.SortOrder.ASC, "")
        val response = accountRepository.getAccountsPaginated(repositoryQueryParams,
                accounts[0].accountData.txDate, accounts[4].accountData.txDate)

        assertEquals(5, response.pageSize)
        assertEquals(5, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)
        assertEquals(5, response.result.count())
        accounts.forEach {
            assertTrue(response.result.map { pair -> pair.first }.contains(it) )
        }
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success partial set no search term with dateFrom filtering`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "customerName", RepositoryQueryParams.SortOrder.ASC, "")
        val response = accountRepository.getAccountsPaginated(repositoryQueryParams,
                accounts[3].accountData.txDate, null)

        assertEquals(5, response.pageSize)
        assertEquals(2, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)
        assertEquals(2, response.result.count())
        assertEquals(accounts[3], response.result[0].first)
        assertEquals(accounts[4], response.result[1].first)
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success partial set no search term with dateTo filtering`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "customerName", RepositoryQueryParams.SortOrder.ASC, "")
        val response = accountRepository.getAccountsPaginated(repositoryQueryParams,
                null, accounts[2].accountData.txDate)

        assertEquals(5, response.pageSize)
        assertEquals(3, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)
        assertEquals(3, response.result.count())
        assertEquals(accounts[0], response.result[0].first)
        assertEquals(accounts[1], response.result[1].first)
        assertEquals(accounts[2], response.result[2].first)
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success with full account id filtering`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "customerName", RepositoryQueryParams.SortOrder.ASC,
                accounts[0].accountData.accountId.toString())
        val response = accountRepository.getAccountsPaginated(repositoryQueryParams,
                accounts[0].accountData.txDate,  accounts[4].accountData.txDate)

        assertEquals(5, response.pageSize)
        assertEquals(1, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)
        assertEquals(1, response.result.count())
        assertEquals(accounts[0], response.result[0].first)
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success with partial account id filtering`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "customerName", RepositoryQueryParams.SortOrder.ASC,
                accounts[0].accountData.accountId.toString().substring(10, 18))
        val response = accountRepository.getAccountsPaginated(repositoryQueryParams,
                accounts[0].accountData.txDate,  accounts[4].accountData.txDate)

        assertEquals(5, response.pageSize)
        assertEquals(1, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)
        assertEquals(1, response.result.count())
        assertEquals(accounts[0], response.result[0].first)
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success with partial status filtering`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "customerName", RepositoryQueryParams.SortOrder.ASC, "SUSP")

        val response = accountRepository.getAccountsPaginated(repositoryQueryParams,
                null, null)

        assertEquals(5, response.pageSize)
        assertEquals(1, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)
        assertEquals(1, response.result.count())
        assertEquals(accounts[0].accountData.accountId, response.result[0].first.accountData.accountId)
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success with partial customer name filtering`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "customerName", RepositoryQueryParams.SortOrder.ASC, "ommer")

        val response = accountRepository.getAccountsPaginated(repositoryQueryParams,
                null, null)

        assertEquals(5, response.pageSize)
        assertEquals(1, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)
        assertEquals(1, response.result.count())
        assertEquals(accounts[4], response.result[0].first)
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success with account sorting`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "account", RepositoryQueryParams.SortOrder.ASC, "")

        val response = accountRepository.getAccountsPaginated(repositoryQueryParams,
                accounts[0].accountData.txDate,  accounts[4].accountData.txDate)

        assertEquals(5, response.pageSize)
        assertEquals(5, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)
        assertEquals(5, response.result.count())
        assertEquals(accounts.sortedBy { it.accountData.accountId.toString() }, response.result.map { it.first })
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated success with customer name sorting`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "customerName", RepositoryQueryParams.SortOrder.ASC, "")

        val response = accountRepository.getAccountsPaginated(repositoryQueryParams,
                accounts[0].accountData.txDate,  accounts[4].accountData.txDate)

        assertEquals(5, response.pageSize)
        assertEquals(5, response.totalResults)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.pageNumber)
        assertEquals(5, response.result.count())
        assertEquals(accounts, response.result.map { it.first })
    }

    @Test
    @Order(1)
    fun `test getAccountsPaginated fails with unexistent search column`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                "testtest", RepositoryQueryParams.SortOrder.ASC, "")

        val message = assertFailsWith<IllegalArgumentException> {
            accountRepository.getAccountsPaginated(repositoryQueryParams,
                    null, null)
        }.message!!
        assertEquals("Invalid sort field testtest", message)
    }

    @Test
    fun `test attachmentExists false happy path`() {
        val exists = accountRepository.attachmentExists(listOf("fakeAttachment1", "fakeAttachment2"), UUID.randomUUID())
        assertFalse(exists)
    }

    @Test
    fun `test attachmentExists false same customer happy path`() {
        val attachment = createAttachment(node = bank, network = network)
        val customerId = createCustomer("test customer name", listOf(attachment), bank, network)
        val exists = accountRepository.attachmentExists(listOf("fakeAttachment1", "fakeAttachment2", attachment.first
                .toString()), customerId)
        assertFalse(exists)
    }

    @Test
    fun `test attachmentExists true different customer happy path`() {
        val attachment = createAttachment(node = bank, network = network)
        createCustomer("test customer name", listOf(attachment), bank, network)
        val exists = accountRepository.attachmentExists(listOf("fakeAttachment1", "fakeAttachment2", attachment.first
                .toString()), UUID.randomUUID())
        assertTrue(exists)
    }
}