package com.r3.refapp.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import com.r3.refapp.contracts.FinancialAccountContract
import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.repositories.TransactionLogRepository
import com.r3.refapp.schemas.TransactionType
import com.r3.refapp.states.Account
import com.r3.refapp.states.CreditAccount
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.utils.ConfigurationUtils
import com.r3.refapp.utils.checkTransferDailyLimit
import net.corda.core.contracts.*
import java.util.Currency


/**
 * Extension function used to verify that accounts have same [accountInfo.host]
 * property.
 *
 * @param account the [AccountInfo] account
 * @throws [RefappException] if hosts are not equal
 */
fun AccountInfo.verifySameBank(account: AccountInfo) =
        if (host != account.host)
            throw RefappException("For intrabank payment both accounts must be in same bank, account1 bank: $host, account2 bank: ${account.host}") else Unit

/**
 * Internal Refapp flow used for Intrabank payments.
 *
 * Transfer funds from [CreditAccount] [accountFrom] to [Account] [accountTo].
 * @param accountFrom transfer funds from this account
 * @param accountTo transfer funds to this account
 * @param amount the balance to transfer
 */
@InitiatingFlow
@StartableByRPC
class IntrabankCashPaymentFlow(private val accountFrom: StateAndRef<CreditAccount>,
                               private val accountTo: StateAndRef<Account>,
                               private val amount: Amount<Currency>) : FlowLogic<SignedTransaction>() {


    companion object {
        object FETCHING_ACCOUNT_DATA : ProgressTracker.Step("Fetching account data.")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying transaction.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction.")
        object FINALISING_TRANSACTION : ProgressTracker.Step("Finalising transaction.")


        fun tracker() = ProgressTracker(
                FETCHING_ACCOUNT_DATA,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val transactionLogRepository = serviceHub.cordaService(TransactionLogRepository::class.java)

        progressTracker.currentStep = FETCHING_ACCOUNT_DATA

        val accountFromOut = accountFrom.state.data.withdraw(amount)
        val accountToOut = accountTo.state.data.deposit(amount)

        val partStx = createIntrabankPaymentTransaction(accountFromOut, accountToOut)
        accountFromOut.checkTransferDailyLimit(amount, transactionLogRepository)

        progressTracker.currentStep = FINALISING_TRANSACTION
        val finalStx = subFlow(FinalityFlow(partStx, emptyList()))

        transactionLogRepository.logTransaction(
                txId = finalStx.id,
                amount = amount,
                txDate = accountFromOut.accountData.txDate,
                txType = TransactionType.TRANSFER,
                accountFrom = accountFromOut.accountData.accountId,
                accountTo = accountToOut.accountData.accountId
        )

        return finalStx
    }

    /**
     * Return a signed Intrabank payment transaction transferring [this.amount] from [accountFrom] to [accountTo]
     * @param accountFromOut from account output state
     * @param accountToOut to account output state
     * @returns [SignedTransaction]
     */
    @Suspendable
    private fun createIntrabankPaymentTransaction(accountFromOut: CreditAccount, accountToOut: Account)
            : SignedTransaction {

        progressTracker.currentStep = FETCHING_ACCOUNT_DATA

        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val accountFromKey = accountRepository.getAccountKey(accountFromOut.accountData.accountInfo)
        val accountToKey = accountRepository.getAccountKey(accountToOut.accountData.accountInfo)

        val command = Command(FinancialAccountContract.Commands.CreateIntrabankPayment(
                amount,
                accountFromOut.accountData.accountId,
                accountToOut.accountData.accountId,
                accountFromKey, accountToKey),
                listOf(accountFromKey, accountToKey))

        val notary = ConfigurationUtils.getConfiguredNotary(serviceHub)
        val txBuilder = TransactionBuilder(notary)
                .addInputState(accountFrom)
                .addInputState(accountTo)
                .addOutputState(accountFromOut)
                .addOutputState(accountToOut)
                .addCommand(command)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        return serviceHub.signInitialTransaction(txBuilder, listOf(accountFromKey, accountToKey))
    }
}