package com.r3.refapp.repositories

import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.states.RecurringPaymentState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.schemas.PersistentStateRef
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.contextLogger
import java.util.*

/**
 * Provide services to query [RecurringPaymentState] data.
 */
@CordaService
class RecurringPaymentRepository(val serviceHub: ServiceHub) : SingletonSerializeAsToken() {

    /**
     * Provides RecurringPayment HQL queries and logger
     * @property recurringPaymentByCustomerId Query to fetch recurringPayments for customer, joins recurring payment and
     * persistent balance tables. Matches both accountFrom and accountTo customerAccounts
     */
    companion object {
        private const val recurringPaymentByCustomerId = "SELECT DISTINCT(rp.stateRef) FROM RecurringPaymentSchemaV1\$RecurringPayment rp " +
                "LEFT JOIN AccountStateSchemaV1\$PersistentBalance pb " +
                "ON rp.accountFrom = pb.account OR rp.accountTo = pb.account " +
                "WHERE pb.customerId = :customerId"

        val logger = contextLogger()
    }

    /**
     * Fetches [RecurringPaymentState] by [recurringPaymentId], throws [RefappException] if unique [RecurringPaymentState]
     * cannot be found
     *
     * @param recurringPaymentId unique identifier of recurring payment
     * @throws [RefappException] if unique [RecurringPaymentState] cannot be found
     */
    fun getRecurringPaymentById(recurringPaymentId: UniqueIdentifier) : StateAndRef<RecurringPaymentState> {
        try {
            return serviceHub.vaultService.queryBy<RecurringPaymentState>(QueryCriteria.LinearStateQueryCriteria(linearId = listOf(recurringPaymentId))).states.single()
        } catch (e: Exception) {
            val message = "Error while fetching RecurringPayment with id: $recurringPaymentId"
            logger.error(message, e)
            throw RefappException(message, e)
        }
    }

    /**
     * Fetches list of [StateAndRef<RecurringPaymentState>] for given [customerId], matches both accountFrom and accountTo
     * properties with customer accounts.
     *
     * @param customerId Id of the customer
     * @return Returns list of [StateAndRef<RecurringPaymentState>] associated with customer accounts.
     */
    fun getRecurringPaymentsForCustomer(customerId: UUID) : List<StateAndRef<RecurringPaymentState>> {

        val recurringPaymentIds =  serviceHub.withEntityManager {
            val query = createQuery(recurringPaymentByCustomerId,
                    PersistentStateRef::class.java)
            query.setParameter("customerId", customerId)
            query.resultList
        }
        return recurringPaymentIds.map { serviceHub.toStateAndRef<RecurringPaymentState>(StateRef(SecureHash.parse(it.txId), it.index)) }
    }

}