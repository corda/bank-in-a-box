package com.r3.refapp.states

import com.r3.refapp.contracts.RecurringPaymentContract
import com.r3.refapp.schemas.RecurringPaymentSchemaV1
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Represents recurring payment [SchedulableState]
 *
 * @property accountFrom account from [UUID] of recurring payment
 * @property accountTo account to [UUID] of recurring payment
 * @property dateStart start date of recurring payment
 * @property amount amount which will be transferred from/to on each period
 * @property period frequency of recurring payment
 * @property iterationNum number of iterations
 * @property owningParty bank which owns both accounts in recurring payment
 *
 */
@BelongsToContract(RecurringPaymentContract::class)
data class RecurringPaymentState(val accountFrom: UUID,
                                 val accountTo: UUID,
                                 val amount: Amount<Currency>,
                                 val dateStart: Instant,
                                 val period: Duration,
                                 val iterationNum: Int?,
                                 val owningParty: AbstractParty,
                                 override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState, SchedulableState {

    override val participants: List<AbstractParty> get() = listOf(owningParty)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is RecurringPaymentSchemaV1 -> RecurringPaymentSchemaV1.RecurringPayment(
                    this.accountFrom,
                    this.accountTo,
                    this.amount.quantity,
                    this.amount.token.toString(),
                    this.dateStart,
                    this.period,
                    this.iterationNum,
                    this.linearId.id)
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(RecurringPaymentSchemaV1)

    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        if(iterationNum != null && iterationNum <= 0)
            return null
        val nextEvent = dateStart.plus(period)

        return ScheduledActivity(flowLogicRefFactory.create("com.r3.refapp.flows.ExecuteRecurringPaymentFlow",
                thisStateRef), nextEvent)
    }
}