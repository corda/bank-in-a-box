package com.r3.refapp.it

import com.r3.refapp.flows.ApproveOverdraftFlow
import com.r3.refapp.flows.CreateSavingsAccountFlow
import com.r3.refapp.flows.SetAccountStatusFlow
import com.r3.refapp.flows.WithdrawFiatFlow
import com.r3.refapp.flows.reports.GetAccountsForCustomerPaginatedFlow
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.states.*
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.createCurrentAccount
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.issueLoan
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals

class GetAccountsForCustomerPaginatedFlowTest : AbstractITHelper() {

    @Test
    fun `test GetAccountsForCustomerPaginatedFlow happy path`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, attachments)
        val customer2 = createCurrentAccount(customer1.accountData.customerId, EUR, bank, network)
        val overdraftAccount = executeFlowWithRunNetwork(ApproveOverdraftFlow(customer2.accountData.accountId,
                1000), bank, network).coreTransaction.outputsOfType<CurrentAccountState>().single()
        executeFlowWithRunNetwork(SetAccountStatusFlow(overdraftAccount.accountData.accountId, AccountStatus.ACTIVE), bank, network)
        val loanAccount = issueLoan(customer1.accountData.accountId, 10 of EUR, bank, network)
        val savingsAccount = executeFlowWithRunNetwork(CreateSavingsAccountFlow(customer1.accountData.customerId, EUR, customer1.accountData.accountId,
                10 of EUR, Instant.now().plusSeconds(10)), bank, network).coreTransaction.outputsOfType<SavingsAccountState>().single()

        executeFlowWithRunNetwork(WithdrawFiatFlow(customer2.accountData.accountId, 7 of EUR), bank, network)

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                null, RepositoryQueryParams.SortOrder.DESC, "")
        val accounts = executeFlowWithRunNetwork(GetAccountsForCustomerPaginatedFlow(repositoryQueryParams, customer1
                .accountData.customerId, null, null), bank, network)

        assertEquals(4, accounts.totalResults)
        assertEquals(4, accounts.result.size)

        val currentAccountBalance = accounts.result.single { it.first.accountData.accountId == customer1.accountData.accountId }.first
        val overdraftAccountBalance = accounts.result.single { it.first.accountData.accountId == overdraftAccount.accountData.accountId }.first
        val loanAccountBalance = accounts.result.single { it.first.accountData.accountId == loanAccount.accountData.accountId }.first
        val savingsAccountBalance = accounts.result.single { it.first.accountData.accountId == savingsAccount.accountData.accountId }.first

        verifyAccountBalance(currentAccountBalance, customer1.accountData.accountId)
        verifyAccountBalance(overdraftAccountBalance, customer2.accountData.accountId)
        verifyAccountBalance(loanAccountBalance, loanAccount.accountData.accountId)
        verifyAccountBalance(savingsAccountBalance, savingsAccount.accountData.accountId)
    }

    @Test
    fun `test GetAccountsForCustomerPaginatedFlow happy path empty list`() {

        val repositoryQueryParams = RepositoryQueryParams(1, 5,
                null, RepositoryQueryParams.SortOrder.DESC, "")
        val accounts = executeFlowWithRunNetwork(GetAccountsForCustomerPaginatedFlow(repositoryQueryParams, UUID.randomUUID(), null,
                null), bank, network)
        assertEquals(0, accounts.totalPages)
        assertEquals(0, accounts.result.size)
    }

    private fun verifyAccountBalance(account: Account, accountId: UUID) {

        val fetchedAccount = accountRepository.getAccountStateById(accountId).state.data
        assertEquals(fetchedAccount, account)
    }
}