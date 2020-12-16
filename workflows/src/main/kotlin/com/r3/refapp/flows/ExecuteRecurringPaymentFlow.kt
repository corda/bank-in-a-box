package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.contracts.RecurringPaymentContract
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.repositories.RecurringPaymentLogRepository
import com.r3.refapp.schemas.RecurringPaymentLogSchemaV1
import com.r3.refapp.schemas.RecurringPaymentSchemaV1
import com.r3.refapp.states.Account
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.core.contracts.Command
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.schemas.PersistentStateRef
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.contextLogger
import java.lang.IllegalArgumentException
import java.security.PublicKey
import java.time.Instant
import java.util.*

/**
 * [SchedulableFlow] flow for recurring payments. Flow is initiated by [StateRef] of [RecurringPaymentState] state.
 * Flow consumes and produces one [RecurringPaymentState] state.
 * @param recurringPaymentStateRef state ref of the recurring payment to execute
 */
@InitiatingFlow
@SchedulableFlow
class ExecuteRecurringPaymentFlow(private val recurringPaymentStateRef: StateRef) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call() : SignedTransaction {

        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val recurringPayment = serviceHub.toStateAndRef<RecurringPaymentState>(recurringPaymentStateRef)
        val accountFrom = accountRepository.getCurrentAccountStateById(recurringPayment.state.data.accountFrom)
        val accountTo = accountRepository.getAccountStateById(recurringPayment.state.data.accountTo)
        val accountFromKey = accountRepository.getAccountKey(accountFrom.state.data.accountData.accountInfo)
        val accountToKey = accountRepository.getAccountKey(accountTo.state.data.accountData.accountInfo)

        executePayment(accountFrom.state.data.accountData.accountId, accountTo.state.data.accountData.accountId, recurringPayment)

        val txBuilder = prepareTransaction(accountFromKey, accountFrom.state.data, accountToKey, accountTo.state.data,
                recurringPayment)
        txBuilder.verify(serviceHub)

        val partStx = serviceHub.signInitialTransaction(txBuilder, listOf(accountFromKey, accountToKey))

        return subFlow(FinalityFlow(partStx, emptyList()))
    }

    /**
     * Executes payment via [IntrabankPaymentFlow] and logs result of payment operation to [RecurringPaymentLogSchemaV1.RecurringPaymentLog]
     * @param accountFrom ID of account from which funds will be debited
     * @param accountTo ID of account to which funds will be credited
     * @param recurringPayment the [StateAndRef<RecurringPayment>]
     * @throws [InsufficientBalanceException] from [IntrabankPaymentFlow]
     */
    @Suspendable
    private fun executePayment(accountFrom: UUID,
                               accountTo: UUID,
                               recurringPayment: StateAndRef<RecurringPaymentState>) {

        val recurringPaymentLogRepository = serviceHub.cordaService(RecurringPaymentLogRepository::class.java)
        var exception: Exception? = null
        try {
            subFlow(IntrabankPaymentFlow(accountFrom, accountTo, recurringPayment.state.data.amount))
        } catch (e: Exception) {
            logger.error("Error while executing payment from ExecuteRecurringPaymentFlow", e)
            exception = e
        }
        await(CreateRecurringPaymentLogOperation(recurringPayment, recurringPaymentLogRepository, exception))
    }


    @Suspendable
    private fun prepareTransaction(accountFromKey: PublicKey, accountFrom: CurrentAccountState, accountToKey: PublicKey,
                                   accountTo: Account, recurringPaymentStateAndRef: StateAndRef<RecurringPaymentState>): TransactionBuilder {

        val recurringPayment = recurringPaymentStateAndRef.state.data

        val recurringPaymentOut = recurringPaymentStateAndRef.state.data
                .copy(accountFrom = accountFrom.accountData.accountId, accountTo = accountTo.accountData.accountId,
                        dateStart = recurringPayment.dateStart.plus(recurringPayment.period),
                        iterationNum = recurringPayment.iterationNum?.let { it - 1 } ?: recurringPayment.iterationNum)

        val command = Command(RecurringPaymentContract.Commands.ExecuteRecurringPayment(accountFromKey, accountToKey),
                listOf(accountFromKey, accountToKey))

        val notary = ConfigurationUtils.getConfiguredNotary(serviceHub)
        return TransactionBuilder(notary)
                .addInputState(recurringPaymentStateAndRef)
                .addOutputState(recurringPaymentOut)
                .addCommand(command)
    }

    /**
     * Recurring payment log operation which creates off-ledger records for each [RecurringPaymentState] iteration
     */
    class CreateRecurringPaymentLogOperation(private val recurringPaymentStateAndRef: StateAndRef<RecurringPaymentState>,
                                             private val recurringPaymentLogRepository: RecurringPaymentLogRepository,
                                             private val exception: Exception? = null)
        : FlowExternalOperation<RecurringPaymentLogSchemaV1.RecurringPaymentLog> {


        override fun execute(deduplicationId: String): RecurringPaymentLogSchemaV1.RecurringPaymentLog {
            var recurringPaymentLog: RecurringPaymentLogSchemaV1.RecurringPaymentLog? = null

            try {
                recurringPaymentLog = recurringPaymentLogRepository.getRecurringPaymentLogById(deduplicationId)
            } catch (e: IllegalArgumentException) {
                contextLogger().info("Recurring payment logs not found for deduplicationId: $deduplicationId, " +
                        "new entry will be added.")
            }
            return recurringPaymentLog?: createRecurringPaymentLog(deduplicationId)
        }

        private fun createRecurringPaymentLog(deduplicationId: String) : RecurringPaymentLogSchemaV1.RecurringPaymentLog {
            val txDate = Instant.now()
            val recurringPaymentLog = RecurringPaymentLogSchemaV1.RecurringPaymentLog(deduplicationId,
                    mapRecurringPayment(recurringPaymentStateAndRef),
                    txDate, exception?.message)
            recurringPaymentLogRepository.persistRecurringPaymentLog(recurringPaymentLog)
            return recurringPaymentLog
        }

        private fun mapRecurringPayment(recurringPaymentStateAndRef: StateAndRef<RecurringPaymentState>)
                : RecurringPaymentSchemaV1.RecurringPayment {
            val recurringPaymentState = recurringPaymentStateAndRef.state.data
            val recurringPaymentEntity = RecurringPaymentSchemaV1.RecurringPayment(recurringPaymentState.accountFrom,
                    recurringPaymentState.accountTo, recurringPaymentState.amount.quantity, recurringPaymentState.amount.token.toString(),
                    recurringPaymentState.dateStart, recurringPaymentState.period, recurringPaymentState.iterationNum,
                    recurringPaymentState.linearId.id)
            recurringPaymentEntity.stateRef = PersistentStateRef(recurringPaymentStateAndRef.ref)
            return recurringPaymentEntity
        }
    }
}