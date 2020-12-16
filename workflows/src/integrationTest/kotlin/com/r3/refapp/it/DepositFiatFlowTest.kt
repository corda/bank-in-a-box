package com.r3.refapp.it

import com.r3.refapp.flows.DepositFiatFlow
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.test.utils.TestUtils.prepareCurrentAccount
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DepositFiatFlowTest : AbstractITHelper() {

    @Test
    fun `test make deposit`() {
        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())

        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 30 of EUR), bank, network)

        var balanceAcc = getAccountBalanceForAccount(customer1)
        assertEquals(30, balanceAcc.state.data.accountData.balance.toDecimal().toInt())

        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 20 of EUR), bank, network)
        executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 25 of EUR), bank, network)

        balanceAcc = getAccountBalanceForAccount(customer1)
        assertEquals(75, balanceAcc.state.data.accountData.balance.toDecimal().toInt())
    }

    @Test
    fun `test error 0 deposit`() {
        val customer1 = prepareCurrentAccount("PartyA - Customer1", bank, network, emptyList())

        val exception = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(DepositFiatFlow(customer1.accountData.accountId, 0 of EUR), bank, network)
        }
        exception.message?.contains("Amount should be greater than 0")?.let { assertTrue(it) }
    }
}