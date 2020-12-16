package com.r3.refapp.contracts

import com.r3.refapp.states.Account
import com.r3.refapp.states.LoanAccountState
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.states.SavingsAccountState
import com.r3.refapp.util.executeBasicContractVerification
import com.r3.refapp.util.key
import com.r3.refapp.util.ofType
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.security.PublicKey

/**
 * Contract used to verify transactions related to Recurring payments.
 */
class RecurringPaymentContract : Contract {

    companion object {
        const val ID = "com.r3.refapp.contracts.RecurringPaymentContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.CreateRecurringPayment -> {
                verifyCreateRecurringPayment(tx)
            }
            is Commands.ExecuteRecurringPayment -> {
                verifyExecuteRecurringPayment(tx)
            }
            is Commands.CancelRecurringPayment -> {
                verifyCancelRecurringPayment(tx)
            }
            else -> throw IllegalArgumentException("Command not recognized")
        }
    }

    /**
     * Verification method for [Commands.CreateRecurringPayment] command. Throws [IllegalArgumentException]
     * if any of verification checks fail.
     *
     * @param tx the [LedgerTransaction]
     * @throws [IllegalArgumentException] on unsuccessful verification
     */
    private fun verifyCreateRecurringPayment(tx: LedgerTransaction) {
        requireThat {

            executeBasicContractVerification(
                    tx = tx,
                    commands = listOf(1 ofType Commands.CreateRecurringPayment::class.java),
                    inputs = listOf(0 ofType RecurringPaymentState::class.java),
                    outputs = listOf(1 ofType RecurringPaymentState::class.java),
                    signerKeys = listOf("Account from" key { tx.commandsOfType<Commands.CreateRecurringPayment>().single().value.fromAccountPublicKey},
                           "Account to" key { tx.commandsOfType<Commands.CreateRecurringPayment>().single().value.toAccountPublicKey}))

            val recurringPaymentOut = tx.outputsOfType<RecurringPaymentState>().single()

            "Transaction must have time window and from time set" using
                    (tx.timeWindow?.fromTime?.let {return@let true } ?: false)
            "Start date cannot be in past" using (recurringPaymentOut.dateStart.isAfter(tx.timeWindow!!.fromTime!!))
            "Amount should be greater than 0" using (recurringPaymentOut.amount.toDecimal() > BigDecimal.ZERO)

            "From and to accounts should be different" using
                    (recurringPaymentOut.accountFrom != recurringPaymentOut.accountTo)
        }
    }

    /**
     * Verification method for [Commands.ExecuteRecurringPayment] command. Throws [IllegalArgumentException]
     * if any of verification checks fail.
     *
     * @param tx the [LedgerTransaction]
     * @throws [IllegalArgumentException] on unsuccessful verification
     * Require that:
     *   There is one [Commands.ExecuteRecurringPayment] command
     *   The number of inputs is one, and is of type [RecurringPaymentState]
     *   The number of outputs is one, and is of type [RecurringPaymentState]
     *   The [RecurringPaymentState.dateStart] in out state is equal to [[RecurringPaymentState.dateStart]] in in state plus period
     *   If [RecurringPaymentState.iterationNum] in in state is null it should be set to null in out state
     *   If [RecurringPaymentState.iterationNum] is set in in state it should be decremented in out state
     *   Required signers must contain the [RecurringPaymentState.accountFrom] and [RecurringPaymentState.accountTo] keys
     */
    private fun verifyExecuteRecurringPayment(tx: LedgerTransaction) {
        requireThat {

            executeBasicContractVerification(
                    tx = tx,
                    commands = listOf(1 ofType Commands.ExecuteRecurringPayment::class.java),
                    inputs = listOf(1 ofType RecurringPaymentState::class.java),
                    outputs = listOf(1 ofType RecurringPaymentState::class.java),
                    signerKeys = listOf("Account from" key { tx.commandsOfType<Commands.ExecuteRecurringPayment>().single().value.fromAccountPublicKey},
                           "Account to" key { tx.commandsOfType<Commands.ExecuteRecurringPayment>().single().value.toAccountPublicKey}))

            val recurringPaymentIn = tx.inputsOfType<RecurringPaymentState>().single()
            val recurringPaymentOut = tx.outputsOfType<RecurringPaymentState>().single()

            "Start date should be incremented by the period, iteration num if set should be decremented," +
                    " all other fields should be unchanged between Input and Output" using
                    (recurringPaymentOut == recurringPaymentIn
                            .copy(dateStart = recurringPaymentIn.dateStart.plus(recurringPaymentIn.period),
                                    iterationNum = recurringPaymentIn.iterationNum?.let { it - 1 } ?: recurringPaymentIn.iterationNum))
        }
    }

    /**
     * Verification method for [Commands.CancelRecurringPayment] command. Throws [IllegalArgumentException]
     * if any of verification checks fail.
     *
     * @param tx the [LedgerTransaction]
     * @throws [IllegalArgumentException] on unsuccessful verification
     */
    private fun verifyCancelRecurringPayment(tx: LedgerTransaction) {
        requireThat {

            executeBasicContractVerification(
                    tx = tx,
                    commands = listOf(1 ofType Commands.CancelRecurringPayment::class.java),
                    inputs = listOf(1 ofType RecurringPaymentState::class.java),
                    outputs = listOf(0 ofType RecurringPaymentState::class.java),
                    signerKeys = listOf("Account from" key { tx.commandsOfType<Commands.CancelRecurringPayment>().single().value.fromAccountPublicKey},
                            "Account to" key { tx.commandsOfType<Commands.CancelRecurringPayment>().single().value.toAccountPublicKey}),
                    referencedStates = listOf(1 ofType Account::class.java))

            // TODO: Contract checks used to prevent recurring payment cancellation for loan and savings repayments. Functionality
            //   should prevent only bank's customers to cancel re-payments for loans and savings accounts, it will be re-implemented
            //   in Q4 once authentication and authorisation is implemented.
            val referencedToAccount = tx.referenceInputRefsOfType<Account>().single().state.data
            val recurringPayment = tx.inputsOfType<RecurringPaymentState>().single()

            "Recurring payment account to must be equal to referenced account to" using
                    (referencedToAccount.accountData.accountId == recurringPayment.accountTo)

            "Recurring payment cannot be cancelled for loan repayments" using (referencedToAccount !is LoanAccountState)

            if (referencedToAccount is SavingsAccountState) {
                "Recurring payment cannot be cancelled for saving repayments during savings period" using
                        (referencedToAccount.savingsEndDate.isBefore(tx.timeWindow!!.fromTime!!))
            }
        }
    }

    interface Commands : CommandData {
        /**
         * Command used for creation of recurring payments
         */
        class CreateRecurringPayment(val fromAccountPublicKey: PublicKey,
                                     val toAccountPublicKey: PublicKey) : Commands
        /**
         * Command used for execution of recurring payments
         */
        class ExecuteRecurringPayment(val fromAccountPublicKey: PublicKey,
                                      val toAccountPublicKey: PublicKey) : Commands
        /**
         * Command used for cancellation of recurring payments
         */
        class CancelRecurringPayment(val fromAccountPublicKey: PublicKey,
                                     val toAccountPublicKey: PublicKey) : Commands
    }
}