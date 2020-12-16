package com.r3.refapp.it.reports

import com.r3.refapp.flows.reports.GetAccountFlow
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.issueLoan
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.test.utils.TestUtils.prepareOverdraftAccount
import com.r3.refapp.test.utils.TestUtils.prepareSavingsAccount
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.assertFailsWith

class GetAccountFlowTest : AbstractITHelper() {

    @Test
    fun `test GetAccountFlow happy path for current account`() {
        val currentAccount = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())
        val fetchedAccount = executeFlowWithRunNetwork(GetAccountFlow(currentAccount.accountData.accountId), bank, network)

        assertEquals(currentAccount, fetchedAccount)
    }

    @Test
    fun `test GetAccountFlow happy path for overdraft account`() {
        val overdraftAccount = prepareOverdraftAccount("PartyA - Customer1", bank, network, emptyList())
        val fetchedAccount = executeFlowWithRunNetwork(GetAccountFlow(overdraftAccount.accountData.accountId), bank, network)

        assertEquals(overdraftAccount, fetchedAccount)
    }

    @Test
    fun `test GetAccountFlow happy path for savings account`() {
        val savingsAccount = prepareSavingsAccount("PartyA - Customer1", bank, network, emptyList())
        val fetchedAccount = executeFlowWithRunNetwork(GetAccountFlow(savingsAccount.accountData.accountId), bank, network)

        assertEquals(savingsAccount, fetchedAccount)
    }

    @Test
    fun `test GetAccountFlow happy path for loan account`() {
        val currentAccount = prepareCurrentAccount("PartyA - Customer1",  bank, network, emptyList())
        val loanAccount = issueLoan(currentAccount.accountData.accountId, 200 of EUR, bank, network)
        val fetchedAccount = executeFlowWithRunNetwork(GetAccountFlow(loanAccount.accountData.accountId), bank, network)

        assertEquals(loanAccount, fetchedAccount)
    }

    @Test
    fun `test GetAccountFlow fails with account cannot be found`() {

        val uuid = UUID.randomUUID()
        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(GetAccountFlow(uuid), bank, network)
        }.message!!

        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Vault query failed. Cannot find interface com.r3.refapp.states.Account " +
                "with id: $uuid", message)
    }

}