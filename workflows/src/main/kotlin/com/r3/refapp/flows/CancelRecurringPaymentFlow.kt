package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.contracts.RecurringPaymentContract
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.repositories.RecurringPaymentRepository
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.core.contracts.*
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant


/**
 * Public API flow for cancellation of recurring payments. Flow stops recurring payments by consuming
 * [RecurringPaymentState] [SchedulableState].
 * @param recurringPaymentId id of the recurring payment to cancel
 */
@InitiatingFlow
@StartableByRPC
class CancelRecurringPaymentFlow(private val recurringPaymentId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val recurringPaymentRepository = serviceHub.cordaService(RecurringPaymentRepository::class.java)

        val recurringPayment = recurringPaymentRepository.getRecurringPaymentById(recurringPaymentId)
        val accountFrom = accountRepository.getCurrentAccountStateById(recurringPayment.state.data.accountFrom)
        val accountTo = accountRepository.getAccountStateById(recurringPayment.state.data.accountTo)
        val accountFromKey = accountRepository.getAccountKey(accountFrom.state.data.accountData.accountInfo)
        val accountToKey = accountRepository.getAccountKey(accountTo.state.data.accountData.accountInfo)

        val command = Command(RecurringPaymentContract.Commands.CancelRecurringPayment(accountFromKey, accountToKey),
                listOf(accountFromKey, accountToKey))

        val notary = ConfigurationUtils.getConfiguredNotary(serviceHub)
        val txBuilder = TransactionBuilder(notary)
                .addInputState(recurringPayment)
                .addCommand(command)
                .addReferenceState(ReferencedStateAndRef(accountTo))
                .setTimeWindow(TimeWindow.fromOnly(Instant.now()))

        txBuilder.verify(serviceHub)

        val partStx = serviceHub.signInitialTransaction(txBuilder, listOf(accountFromKey, accountToKey))

        return subFlow(FinalityFlow(partStx, emptyList()))
    }
}