package com.r3.refapp.it

import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.flows.internal.IntrabankCashPaymentFlow
import com.r3.refapp.flows.IntrabankPaymentFlow
import com.r3.refapp.states.Account
import com.r3.refapp.states.CreditAccount
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.issueLoan
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.test.utils.TestUtils.prepareOverdraftAccount
import com.r3.refapp.test.utils.TestUtils.prepareSavingsAccount
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.Test

import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

enum class AccountTypeTest {
    CURRENT,
    OVERDRAFT,
    SAVINGS,
    LOAN
}

class IntrabankPaymentTest : AbstractITHelper() {

    @RepeatedTest(4)
    fun `test IntrabankPaymentFlow happy path`(repetitionInfo: RepetitionInfo) {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())
        val customer2 = prepareAccount(repetitionInfo,"PartyA - Customer2", bank, network, emptyList())
        val expectedToBalance = customer2.deposit(10 of EUR).accountData.balance


        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 50 of EUR), bank, network)
        val tx = executeFlowWithRunNetwork(IntrabankPaymentFlow(customer1.accountData.accountId, customer2.accountData.accountId, 10 of EUR),
                bank, network)

        val accountFromState = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        val accountToState = accountRepository.getAccountStateById(customer2.accountData.accountId).state.data

        assertEquals(40 of EUR, accountFromState.accountData.balance)
        assertEquals(expectedToBalance, accountToState.accountData.balance)

        val accountFromOutState = tx.coreTransaction.outRefsOfType(CurrentAccountState::class.java)
                .single { it.state.data.accountData.accountId == customer1.accountData.accountId }.state.data
        val accountToOutState = tx.coreTransaction.outRefsOfType(Account::class.java)
                .single { it.state.data.accountData.accountId == customer2.accountData.accountId }.state.data

        assertEquals(2, tx.coreTransaction.inputs.size)
        assertEquals(40 of EUR, accountFromOutState.accountData.balance)
        assertEquals(expectedToBalance, accountToOutState.accountData.balance)

        val accountFromConsumedBalance = getAccountBalanceForAccount(customer1, Vault.StateStatus.CONSUMED)
        val accountToConsumedBalance = getAccountBalanceForAccount(customer2, Vault.StateStatus.CONSUMED)

        assertEquals(50 of EUR, accountFromConsumedBalance.state.data.accountData.balance)
        assertEquals(customer2.accountData.balance, accountToConsumedBalance.state.data.accountData.balance)
    }

    @Test
    fun `test IntrabankPaymentFlow fail on fromAccount not CreditAccount`() {
        val repaymentAccount = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())
        val fromAccount = issueLoan(repaymentAccount.accountData.accountId, 2000 of EUR, bank, network)

        val toAccount = prepareCurrentAccount("PartyA - Customer2", bank, network, emptyList())

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(IntrabankPaymentFlow(fromAccount.accountData.accountId, toAccount.accountData.accountId, 10 of EUR),
                    bank, network)
        }.message!!
        assertEquals("${RefappException::class.qualifiedName}: Refapp exception: Vault query failed. Cannot find " +
                "interface ${CreditAccount::class.qualifiedName} with id: ${fromAccount.accountData.accountId}", message)
    }

    @RepeatedTest(4)
    fun `test IntrabankPaymentFlow fails on insuficient funds`(repetitionInfo: RepetitionInfo) {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())
        val customer2 = prepareAccount(repetitionInfo, "PartyA - Customer2", bank, network, emptyList())

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(IntrabankPaymentFlow(customer1.accountData.accountId, customer2.accountData.accountId, 10 of EUR),
                    node = bank, network = network)
        }.message!!
        assertEquals("net.corda.core.contracts.InsufficientBalanceException: Insufficient balance, missing 10.00 EUR", message)
    }

    @RepeatedTest(4)
    fun `test IntrabankPaymentFlow fails on no accountBalance`(repetitionInfo: RepetitionInfo) {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())
        val customer2 = prepareAccount(repetitionInfo,"PartyA - Customer2", bank2, network, emptyList())

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(IntrabankPaymentFlow(customer1.accountData.accountId, customer2.accountData.accountId, 10 of EUR),
                    node = bank, network = network)
        }.message!!
        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Vault query failed. Cannot find " +
                "interface com.r3.refapp.states.Account with id: ${customer2.accountData.accountId}", message)
    }

    @RepeatedTest(4)
    fun `test IntrabankCashPaymentFlow happy path`(repetitionInfo: RepetitionInfo) {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())
        val customer2 = prepareAccount(repetitionInfo, "PartyA - Customer2", bank, network, emptyList())
        val expectedToBalance = customer2.deposit(10 of EUR).accountData.balance
        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 50 of EUR), bank, network)
        val account1State = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId)
        val account2State = accountRepository.getAccountStateById(customer2.accountData.accountId)

        val tx = executeFlowWithRunNetwork(IntrabankCashPaymentFlow(account1State, account2State, 10 of EUR), bank, network)

        val accountFromState = getAccountBalanceForAccount(customer1)
        val accountToState = getAccountBalanceForAccount(customer2)

        assertEquals(40 of EUR, accountFromState.state.data.accountData.balance)
        assertEquals(expectedToBalance, accountToState.state.data.accountData.balance)

        val accountFromOutState = tx.coreTransaction.outRefsOfType(CurrentAccountState::class.java)
                .single { it.state.data.accountData.accountId == customer1.accountData.accountId }.state.data
        val accountToOutState = tx.coreTransaction.outRefsOfType(Account::class.java)
                .single { it.state.data.accountData.accountId == customer2.accountData.accountId }.state.data


        assertEquals(2, tx.coreTransaction.inputs.size)
        assertEquals(40 of EUR, accountFromOutState.accountData.balance)
        assertEquals(expectedToBalance, accountToOutState.accountData.balance)

        val accountFromConsumedBalance = getAccountBalanceForAccount(customer1, Vault.StateStatus.CONSUMED)
        val accountToConsumedBalance = getAccountBalanceForAccount(customer2, Vault.StateStatus.CONSUMED)

        assertEquals(50 of EUR, accountFromConsumedBalance.state.data.accountData.balance)
        assertEquals(customer2.accountData.balance, accountToConsumedBalance.state.data.accountData.balance)
    }

    @RepeatedTest(4)
    fun `test transfer daily limit fail`(repetitionInfo: RepetitionInfo) {
        val customer1 = prepareCurrentAccount(
                customerPartyName = "PartyA - Customer1",
                node = bank,
                network = network,
                attachments = emptyList(),
                transferDailyLimit = 100000)

        val customer2 = prepareAccount(repetitionInfo,"PartyA - Customer2", bank, network, emptyList())

        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 1200 of EUR), bank, network)
        val account1State = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId)
        val account2State = accountRepository.getAccountStateById(customer2.accountData.accountId)

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(
                    IntrabankCashPaymentFlow(account1State, account2State, 1001 of EUR), bank, network)
        }.message!!
        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Daily transfer limit exceeded (amount transferred from: 0, amount to transfer: 1001.00 EUR, limit: 100000)", message)
    }

    private fun prepareAccount(repetitionInfo: RepetitionInfo, customerPartyName: String, bank: StartedMockNode,
                               network: MockNetwork, attachments: List<Pair<SecureHash, String>>): Account {
        val accountType = AccountTypeTest.values()[repetitionInfo.currentRepetition - 1]
        return when (accountType) {
            AccountTypeTest.CURRENT -> prepareCurrentAccount(customerPartyName, bank, network, attachments)
            AccountTypeTest.OVERDRAFT -> prepareOverdraftAccount(customerPartyName, bank, network, attachments)
            AccountTypeTest.SAVINGS -> prepareSavingsAccount(customerPartyName, bank, network, attachments)
            AccountTypeTest.LOAN -> {
                val account = prepareCurrentAccount(customerPartyName, bank, network, attachments)
                issueLoan(account.accountData.accountId, 2000 of EUR, bank, network)
            }
        }
    }
}
