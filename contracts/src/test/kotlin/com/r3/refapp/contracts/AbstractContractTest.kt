package com.r3.refapp.contracts

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.refapp.states.*
import com.r3.refapp.util.of
import com.r3.refapp.util.EUR
import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.EnforceVerifyOrFail
import net.corda.testing.dsl.TransactionDSL
import net.corda.testing.dsl.TransactionDSLInterpreter
import net.corda.testing.node.MockServices
import net.corda.testing.node.transaction
import java.time.Instant
import java.time.Period
import java.util.*

abstract class AbstractContractTest {

    protected val nodeOperator = TestIdentity(CordaX500Name("opeartor", "New York", "US"))
    protected val ledgerServices = MockServices(listOf("com.r3.refapp", "com.r3.corda.lib.accounts"), nodeOperator,
            testNetworkParameters(minimumPlatformVersion = 4))
    protected val alice = TestIdentity(CordaX500Name("alice", "New York", "US"))
    protected val bob = TestIdentity(CordaX500Name("bob", "Tokyo", "JP"))
    protected val bank = TestIdentity(CordaX500Name("bank", "Dublin", "IE"))
    protected val bank2 = TestIdentity(CordaX500Name("bank2", "Dublin", "IE"))
    protected val oracle = TestIdentity(CordaX500Name("Oracle", "Dublin", "IE"))
    protected val notary = TestIdentity(DUMMY_NOTARY_NAME)

    protected val aliceId = UniqueIdentifier.fromString(UUID.randomUUID().toString())
    protected val bobId = UniqueIdentifier.fromString(UUID.randomUUID().toString())

    protected val aliceCustomerId = UUID.randomUUID()
    protected val bobCustomerId = UUID.randomUUID()

    protected class DummyCommand : FinancialAccountContract.Commands
    protected class DummyState(override val linearId: UniqueIdentifier = UniqueIdentifier(),
                               override val participants: List<AbstractParty> = listOf()) : LinearState

    protected fun transaction(script: TransactionDSL<TransactionDSLInterpreter>.() -> EnforceVerifyOrFail) {
        ledgerServices.transaction(notary.party, script)
    }

    protected fun getCurrentAccount(
            accountId: UUID,
            accBank: TestIdentity = bank,
            id: UniqueIdentifier = UniqueIdentifier(id = accountId),
            balance: Amount<Currency> = 0 of EUR,
            accountStatus: AccountStatus = AccountStatus.ACTIVE,
            customerId: UUID = UUID.randomUUID(),
            txDate: Instant = Instant.now(),
            withdrawalDailyLimit: Long? = null,
            transferDailyLimit: Long? = null,
            linearId: UniqueIdentifier = UniqueIdentifier()
    ): CurrentAccountState {

        val accountInfo = AccountInfo(accountId.toString(), accBank.party, id)
        return CurrentAccountState(getAccountData(accountId, accountInfo, balance, txDate, accountStatus, customerId),
                withdrawalDailyLimit, transferDailyLimit, linearId = linearId)
    }

    protected fun getOverdraftAccount(
            currentAccount: CurrentAccountState,
            approvedLimit: Long = 500,
            accountInfo: AccountInfo = currentAccount.accountData.accountInfo,
            balance: Amount<Currency> = currentAccount.accountData.balance,
            customerId: UUID = currentAccount.accountData.customerId,
            overdraftBalance: Long = 0,
            linearId: UniqueIdentifier = currentAccount.linearId
    ): CurrentAccountState {

        return CurrentAccountState(accountData = getAccountData(currentAccount.accountData.accountId, accountInfo,
                balance, currentAccount.accountData.txDate, currentAccount.accountData.status, customerId),
                overdraftBalance = overdraftBalance,
                approvedOverdraftLimit = approvedLimit,
                withdrawalDailyLimit = currentAccount.withdrawalDailyLimit,
                transferDailyLimit = currentAccount.transferDailyLimit,
                linearId = linearId)
    }

    protected fun getLoanAccountState(
            accountName: String,
            accountIdentifier: UniqueIdentifier = UniqueIdentifier(),
            accBank: TestIdentity = bank,
            amount: Amount<Currency>,
            status: AccountStatus = AccountStatus.ACTIVE
    ): LoanAccountState {

        val accountInfo = AccountInfo(accountName, accBank.party, accountIdentifier)
        return LoanAccountState(getAccountData(accountIdentifier.id, accountInfo, amount, Instant.now(), status))
    }

    protected fun getSavingAccountState(
            accountName: String,
            accountIdentifier: UniqueIdentifier = UniqueIdentifier(),
            accBank: TestIdentity = bank,
            amount: Amount<Currency> = 0 of EUR,
            status: AccountStatus = AccountStatus.PENDING,
            savingsEndDate: Instant = Instant.now()
    ): SavingsAccountState {

        val accountInfo = AccountInfo(accountName, accBank.party, accountIdentifier)
        val accountData = getAccountData(accountIdentifier.id, accountInfo, amount, Instant.now(), status)

        return SavingsAccountState(accountData, savingsEndDate, Period.ofMonths(12))
    }

    private fun getAccountData(
            accountId: UUID,
            accountInfo: AccountInfo,
            balance: Amount<Currency> = 0 of EUR,
            txDate: Instant = Instant.now(),
            accountStatus: AccountStatus = AccountStatus.ACTIVE,
            customerId: UUID = UUID.randomUUID()
    ): AccountData {
        return AccountData(accountId, accountInfo, customerId, balance, txDate, accountStatus)
    }
}