package com.r3.refapp.it

import com.r3.refapp.flows.ApproveOverdraftFlow
import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.flows.IntrabankPaymentFlow
import com.r3.refapp.flows.WithdrawFiatFlow
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.contracts.Amount
import net.corda.core.node.services.Vault
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.Currency
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApproveOverdraftFlowTest : AbstractITHelper() {

    @Test
    fun `test ApproveOverdraftFlow happy path`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())
        val customer2 = prepareCurrentAccount("PartyA - Customer2", bank, network, emptyList())

        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 10 of EUR), bank, network)
        val tx = executeFlowWithRunNetwork(IntrabankPaymentFlow(customer1.accountData.accountId, customer2
                .accountData.accountId, 10 of EUR), bank, network)

        val accountFromState = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        val accountToState = accountRepository.getCurrentAccountStateById(customer2.accountData.accountId).state.data

        assertEquals(0 of EUR, accountFromState.accountData.balance)
        assertEquals(10 of EUR, accountToState.accountData.balance)

        val accountFromOutState = tx.coreTransaction.outRefsOfType(CurrentAccountState::class.java)
                .single { it.state.data.accountData.accountId == customer1.accountData.accountId }.state.data
        val accountToOutState = tx.coreTransaction.outRefsOfType(CurrentAccountState::class.java)
                .single { it.state.data.accountData.accountId == customer2.accountData.accountId }.state.data

        assertEquals(2, tx.coreTransaction.inputs.size)
        assertEquals(0 of EUR, accountFromOutState.accountData.balance)
        assertEquals(10 of EUR, accountToOutState.accountData.balance)

        val accountFromConsumedBalance = getAccountBalanceForAccount(customer1, Vault.StateStatus.CONSUMED)
        val accountToConsumedBalance = getAccountBalanceForAccount(customer2, Vault.StateStatus.CONSUMED)

        assertEquals(10 of EUR, accountFromConsumedBalance.state.data.accountData.balance)
        assertEquals(0 of EUR, accountToConsumedBalance.state.data.accountData.balance)

        val tx2 = executeFlowWithRunNetwork(ApproveOverdraftFlow(customer1.accountData.accountId,2000), bank, network)

        val overdraftAccountState = tx2.coreTransaction.outputsOfType<CurrentAccountState>().single()

        assertEquals(0 of EUR, overdraftAccountState.accountData.balance)
        assertEquals(0, overdraftAccountState.overdraftBalance)
        assertEquals(2000, overdraftAccountState.approvedOverdraftLimit)

        executePaymentAndVerify(customer1, customer2)
        executeWithdrawalAndVerify(customer1)
        executeDepositAndVerify(customer1)
    }

    @Test
    fun `test AprroveOverdraftFlow payment fails with insufficient funds`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())
        val customer2 = prepareCurrentAccount("PartyA - Customer2", bank, network, emptyList())

        executeFlowWithRunNetwork(ApproveOverdraftFlow(customer1.accountData.accountId,2000), bank, network)
        executeDepositAndVerify(customer1, 15 of EUR)

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(IntrabankPaymentFlow(customer1.accountData.accountId, customer2.accountData.accountId, 37 of EUR), bank, network)
        }.message!!

        assertEquals("net.corda.core.contracts.InsufficientBalanceException: Insufficient balance, missing 2.00 EUR", message)
    }

    private fun executeDepositAndVerify(customer: CurrentAccountState, expectedBalance: Amount<Currency> = 2 of EUR) {
        executeFlowWithRunNetwork(DepositFiatFlow(customer.accountData.accountId, 15 of EUR), bank, network)
        val accountFromAfterDeposit = accountRepository.getCurrentAccountStateById(customer.accountData.accountId).state.data

        assertEquals(expectedBalance, accountFromAfterDeposit.accountData.balance)
        assertEquals(0, accountFromAfterDeposit.overdraftBalance)
        assertEquals(2000, accountFromAfterDeposit.approvedOverdraftLimit)
    }

    private fun executeWithdrawalAndVerify(customer: CurrentAccountState) {
        executeFlowWithRunNetwork(WithdrawFiatFlow(customer.accountData.accountId, 3 of EUR), bank, network)
        val accountFromAfterWithdraw = accountRepository.getCurrentAccountStateById(customer.accountData.accountId).state.data

        assertEquals(0 of EUR, accountFromAfterWithdraw.accountData.balance)
        assertEquals(1300, accountFromAfterWithdraw.overdraftBalance)
        assertEquals(2000, accountFromAfterWithdraw.approvedOverdraftLimit)
    }

    private fun executePaymentAndVerify(customer1: CurrentAccountState, customer2: CurrentAccountState) {
        executeFlowWithRunNetwork(IntrabankPaymentFlow(customer1.accountData.accountId, customer2.accountData.accountId, 10 of EUR), bank, network)
        val accountFromAfterPayment = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        val accountToAfterPayment = accountRepository.getCurrentAccountStateById(customer2.accountData.accountId).state.data

        assertEquals(20 of EUR, accountToAfterPayment.accountData.balance)
        assertEquals(0 of EUR, accountFromAfterPayment.accountData.balance)
        assertEquals(1000, accountFromAfterPayment.overdraftBalance)
        assertEquals(2000, accountFromAfterPayment.approvedOverdraftLimit)
    }
}