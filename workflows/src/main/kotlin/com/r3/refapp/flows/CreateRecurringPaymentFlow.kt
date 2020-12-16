package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.contracts.RecurringPaymentContract
import com.r3.refapp.flows.internal.verifySameBank
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.SchedulableState
import net.corda.core.contracts.TimeWindow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Public API flow for initialisation of recurring payments. Flow initialises recurring payment by producing
 * [RecurringPaymentState] [SchedulableState].
 * @param accountFrom transfer funds from this account
 * @param accountTo transfer funds to this account
 * @param amount the balance to transfer
 * @param dateStart start date of the recurring payment
 * @param period payment is executed every interval specified by this duration
 * @param iterationNum specifies the total number of payments to execute
 */
@InitiatingFlow
@StartableByRPC
class CreateRecurringPaymentFlow(private val accountFrom: UUID,
                                 private val accountTo: UUID,
                                 private val amount: Amount<Currency>,
                                 private val dateStart: Instant,
                                 private val period: Duration,
                                 private val iterationNum: Int?) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val accountFromState = accountRepository.getAccountStateById(accountFrom).state.data
        val accountToState = accountRepository.getAccountStateById(accountTo).state.data
        val accountFromKey = accountRepository.getAccountKey(accountFromState.accountData.accountInfo)
        val accountToKey = accountRepository.getAccountKey(accountToState.accountData.accountInfo)
        accountFromState.accountData.accountInfo.verifySameBank(accountToState.accountData.accountInfo)

        val recurringPayment = RecurringPaymentState(accountFromState.accountData.accountId,
                accountToState.accountData.accountId, amount, dateStart,
                period, iterationNum, accountFromState.accountData.accountInfo.host)

        val command = Command(RecurringPaymentContract.Commands.CreateRecurringPayment(accountFromKey, accountToKey),
                listOf(accountFromKey, accountToKey))

        val notary = ConfigurationUtils.getConfiguredNotary(serviceHub)
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(recurringPayment)
                .addCommand(command)
                .setTimeWindow(TimeWindow.fromOnly(Instant.now()))

        txBuilder.verify(serviceHub)

        val partStx = serviceHub.signInitialTransaction(txBuilder, listOf(accountFromKey, accountToKey))

        return subFlow(FinalityFlow(partStx, emptyList()))
    }
}