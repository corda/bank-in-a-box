package com.r3.refapp.it

import com.r3.refapp.flows.ApproveOverdraftFlow
import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.flows.WithdrawFiatFlow
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.test.utils.TestUtils.prepareSavingsAccount
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WithdrawFiatFlowTest : AbstractITHelper() {

    @Test
    fun `test make successful withdraws`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network)
        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 1000 of EUR), bank, network)

        //withdraw some funds
        executeFlowWithRunNetwork(WithdrawFiatFlow(customer1.accountData.accountId, 30 of EUR), bank, network)

        var balanceAcc = getAccountBalanceForAccount(customer1)
        assertEquals(970, balanceAcc.state.data.accountData.balance.toDecimal().toInt())

        executeFlowWithRunNetwork(WithdrawFiatFlow(customer1.accountData.accountId, 200 of EUR), bank, network)
        executeFlowWithRunNetwork(WithdrawFiatFlow(customer1.accountData.accountId, 250 of EUR), bank, network)

        balanceAcc = getAccountBalanceForAccount(customer1)
        assertEquals(520, balanceAcc.state.data.accountData.balance.toDecimal().toInt())
    }

    @Test
    fun `test error 0 withdraw`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network)
        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 1000 of EUR), bank, network)

        val exception = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(WithdrawFiatFlow(customer1.accountData.accountId, 0 of EUR), bank, network)
        }
        exception.message?.contains("Amount should be greater than 0")?.let { assertTrue(it) }
    }

    @Test
    fun `test insufficient balance withdraw`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network)
        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 1000 of EUR), bank, network)

        val exception = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(WithdrawFiatFlow(customer1.accountData.accountId, 1200 of EUR), bank, network)
        }
        exception.message?.contains("Insufficient balance, missing 200.00")?.let { assertTrue(it) }
    }

    @Test
    fun `test savings account withdrawal fails in savings period`() {

        val currentAccount = prepareCurrentAccount("PartyA - Customer1", bank, network)
        executeFlowWithRunNetwork(DepositFiatFlow(currentAccount.accountData.accountId, 1000 of EUR), bank, network)

        val savingsAccount = prepareSavingsAccount("PartyA - Customer1", bank, network, emptyList())
        executeFlowWithRunNetwork(DepositFiatFlow(savingsAccount.accountData.accountId, 100 of EUR), bank, network)

        val exception = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(WithdrawFiatFlow(savingsAccount.accountData.accountId, 30 of EUR), bank, network)
        }
        exception.message?.contains("Withdrawals are not allowed for Savings account during savings period")
    }

    @Test
    fun `test overdraft account withdrawal success`() {

        val customer1 = prepareCurrentAccount(
                "PartyA - Customer1", bank, network, attachments = emptyList(), withdrawalDailyLimit = 50000)

        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 100 of EUR), bank, network)
        executeFlowWithRunNetwork(ApproveOverdraftFlow(customer1.accountData.accountId, 10000), bank, network)

        executeFlowWithRunNetwork(WithdrawFiatFlow(customer1.accountData.accountId, 30 of EUR), bank, network)

        val nonOverdraftBalanceState = queryAccountBalancesForAccount(customer1)
                .single { it.state.data.approvedOverdraftLimit != null }

        assertEquals(70 of EUR, nonOverdraftBalanceState.state.data.accountData.balance)
        assertEquals(0, nonOverdraftBalanceState.state.data.overdraftBalance)

        executeFlowWithRunNetwork(WithdrawFiatFlow(customer1.accountData.accountId, 90 of EUR), bank, network)

        val overdraftBalanceState = queryAccountBalancesForAccount(customer1)
                .single { it.state.data.approvedOverdraftLimit != null }

        assertEquals(0 of EUR, overdraftBalanceState.state.data.accountData.balance)
        assertEquals(2000, overdraftBalanceState.state.data.overdraftBalance)
    }

    @Test
    fun `test withdrawal daily limit fail`() {
        val customer1 = prepareCurrentAccount(
                "PartyA - Customer1", bank, network, attachments = emptyList(), withdrawalDailyLimit = 50000)

        assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(WithdrawFiatFlow(customer1.accountData.accountId, 550 of EUR), bank, network)
        }
    }
}