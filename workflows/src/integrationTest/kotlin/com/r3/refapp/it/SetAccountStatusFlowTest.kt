package com.r3.refapp.it

import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.flows.IntrabankPaymentFlow
import com.r3.refapp.flows.SetAccountStatusFlow
import com.r3.refapp.flows.WithdrawFiatFlow
import com.r3.refapp.states.AccountStatus
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SetAccountStatusFlowTest : AbstractITHelper() {

    @Test
    fun `test SetAccountStatus happy path`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList(),
                AccountStatus.SUSPENDED)

        val accountState = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertEquals(AccountStatus.SUSPENDED, accountState.accountData.status)

        executeFlowWithRunNetwork(SetAccountStatusFlow(customer1.accountData.accountId, AccountStatus.ACTIVE), bank, network)

        val accountStateAfterStatusSet = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertEquals(AccountStatus.ACTIVE, accountStateAfterStatusSet.accountData.status)
    }

    @Test
    fun `test SetAccountStatus happy path with Deposit`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList(),
                AccountStatus.SUSPENDED)

        val accountState = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertEquals(AccountStatus.SUSPENDED, accountState.accountData.status)

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 10 of EUR), bank, network)
        }.message!!
        assertEquals("java.lang.UnsupportedOperationException: Operation not supported on account: " +
                "${customer1.accountData.accountId} in: SUSPENDED status", message)

        executeFlowWithRunNetwork(SetAccountStatusFlow(customer1.accountData.accountId, AccountStatus.ACTIVE), bank, network)
        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 10 of EUR), bank, network)
        val accountStateAfterStatusSet = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertEquals(AccountStatus.ACTIVE, accountStateAfterStatusSet.accountData.status)
        assertEquals(10 of EUR, accountStateAfterStatusSet.accountData.balance)
    }

    @Test
    fun `test SetAccountStatus happy path with Withdrawal`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, attachments,
                AccountStatus.ACTIVE)
        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 10 of EUR), bank, network)
        executeFlowWithRunNetwork(SetAccountStatusFlow(customer1.accountData.accountId, AccountStatus.SUSPENDED), bank, network)

        val accountState = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertEquals(AccountStatus.SUSPENDED, accountState.accountData.status)
        assertEquals(10 of EUR, accountState.accountData.balance)

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(WithdrawFiatFlow(customer1.accountData.accountId, 8 of EUR), bank, network)
        }.message!!
        assertEquals("java.lang.UnsupportedOperationException: Operation not supported on account: ${customer1.accountData.accountId.toString()} in: SUSPENDED status", message)

        executeFlowWithRunNetwork(SetAccountStatusFlow(customer1.accountData.accountId, AccountStatus.ACTIVE), bank, network)
        executeFlowWithRunNetwork(WithdrawFiatFlow(customer1.accountData.accountId, 8 of EUR), bank, network)
        val accountStateAfterStatusSet = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state
                .data
        assertEquals(AccountStatus.ACTIVE, accountStateAfterStatusSet.accountData.status)
        assertEquals(2 of EUR, accountStateAfterStatusSet.accountData.balance)
    }

    @Test
    fun `test SetAccountStatus happy path with Payment`() {

        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network)
        val customer2 = prepareCurrentAccount("PartyA - Customer2", bank, network, status = AccountStatus.SUSPENDED)
        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 10 of EUR), bank, network)

        val message2ndAccPending = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(IntrabankPaymentFlow(customer1.accountData.accountId, customer2.accountData.accountId,8 of EUR),
                    bank, network)
        }.message!!
        assertEquals("java.lang.UnsupportedOperationException: Operation not supported on account: " +
                "${customer2.accountData.accountId} in: SUSPENDED status", message2ndAccPending)

        executeFlowWithRunNetwork(SetAccountStatusFlow(customer2.accountData.accountId, AccountStatus.ACTIVE), bank, network)
        executeFlowWithRunNetwork(SetAccountStatusFlow(customer1.accountData.accountId, AccountStatus.SUSPENDED), bank, network)

        val message1stAccSuspended = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(IntrabankPaymentFlow(customer1.accountData.accountId, customer2.accountData.accountId,8 of EUR),
                    bank, network)
        }.message!!
        assertEquals("java.lang.UnsupportedOperationException: Operation not supported on account: " +
                "${customer1.accountData.accountId} in: SUSPENDED status", message1stAccSuspended)

        executeFlowWithRunNetwork(SetAccountStatusFlow(customer1.accountData.accountId, AccountStatus.ACTIVE), bank, network)
        executeFlowWithRunNetwork(IntrabankPaymentFlow(customer1.accountData.accountId, customer2.accountData.accountId,8 of EUR),
                bank, network)

        val account1State = accountRepository.getCurrentAccountStateById(customer1.accountData.accountId).state.data
        assertEquals(AccountStatus.ACTIVE, account1State.accountData.status)
        assertEquals(2 of EUR, account1State.accountData.balance)

        val account2State = accountRepository.getCurrentAccountStateById(customer2.accountData.accountId).state.data
        assertEquals(AccountStatus.ACTIVE, account2State.accountData.status)
        assertEquals(8 of EUR, account2State.accountData.balance)
    }
}
