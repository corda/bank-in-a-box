package com.r3.refapp.it

import com.r3.refapp.flows.IssueLoanFlow
import com.r3.refapp.flows.SetAccountLimitsFlow
import com.r3.refapp.states.LoanAccountState
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.test.utils.TestUtils.prepareOverdraftAccount
import com.r3.refapp.test.utils.TestUtils.prepareSavingsAccount
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SetAccountLimitsFlowTest : AbstractITHelper() {

    @Test
    fun `test SetAccountLimits happy path with current account`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network)

        val accountState = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertNull(accountState.withdrawalDailyLimit)
        assertNull(accountState.transferDailyLimit)

        executeFlowWithRunNetwork(SetAccountLimitsFlow(customer1.accountData.accountId, 1000L,
                1500L), bank, network)

        val accountStateAfterLimitsSet = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertEquals(1000L, accountStateAfterLimitsSet.withdrawalDailyLimit)
        assertEquals(1500L, accountStateAfterLimitsSet.transferDailyLimit)
    }

    @Test
    fun `test SetAccountLimits single limit`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, withdrawalDailyLimit = 500, transferDailyLimit = 500)
        val accountState = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data

        executeFlowWithRunNetwork(SetAccountLimitsFlow(customer1.accountData.accountId, 1000L, null), bank, network)

        val accountStateAfterLimitsSet = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertEquals(1000L, accountStateAfterLimitsSet.withdrawalDailyLimit)
        assertEquals(500L, accountStateAfterLimitsSet.transferDailyLimit)
    }

    @Test
    fun `test SetAccountLimits limit reset`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, withdrawalDailyLimit = 500, transferDailyLimit = 500)
        val accountState = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data

        executeFlowWithRunNetwork(SetAccountLimitsFlow(customer1.accountData.accountId, -1, -1), bank, network)

        val accountStateAfterLimitsSet = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertEquals(null, accountStateAfterLimitsSet.withdrawalDailyLimit)
        assertEquals(null, accountStateAfterLimitsSet.transferDailyLimit)
    }

    @Test
    fun `test SetAccountLimits happy path with overdraft account`() {

        val customer1 = prepareOverdraftAccount("PartyA - Customer1", bank, network)

        val accountState = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertNull(accountState.withdrawalDailyLimit)
        assertNull(accountState.transferDailyLimit)

        executeFlowWithRunNetwork(SetAccountLimitsFlow(customer1.accountData.accountId, 1000L,
                1500L), bank, network)

        val accountStateAfterLimitsSet = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertEquals(1000L, accountStateAfterLimitsSet.withdrawalDailyLimit)
        assertEquals(1500L, accountStateAfterLimitsSet.transferDailyLimit)
    }

    @Test
    fun `test SetAccountLimits fails with savings account`() {

        val customer1 = prepareSavingsAccount("PartyA - Customer1", bank, network, emptyList())

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(SetAccountLimitsFlow(customer1.accountData.accountId, 1000L,
                    1500L), bank, network)
        }.message!!
        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Vault query failed. Cannot find " +
                "class com.r3.refapp.states.CurrentAccountState with id: ${customer1.accountData.accountId}", message)
    }

    @Test
    fun `test SetAccountLimits fails with loan account`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network)
        val loanAccount = executeFlowWithRunNetwork(IssueLoanFlow(customer1.accountData.accountId, 100 of EUR, 12), bank, network)
                .coreTransaction.outputsOfType<LoanAccountState>().single()

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(SetAccountLimitsFlow(loanAccount.accountData.accountId, 1000L,
                    1500L), bank, network)
        }.message!!
        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Vault query failed. Cannot find " +
                "class com.r3.refapp.states.CurrentAccountState with id: ${loanAccount.accountData.accountId}", message)
    }

    @Test
    fun `test SetAccountLimits fails with account does not exist`() {

        val accountId = UUID.randomUUID()
        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(SetAccountLimitsFlow(accountId, 1000L,
                    1500L), bank, network)
        }.message!!
        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Vault query failed. Cannot find " +
                "class com.r3.refapp.states.CurrentAccountState with id: $accountId", message)
    }
}