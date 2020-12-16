package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.schemas.CustomerSchemaV1
import com.r3.refapp.utils.verifyCustomerDetails
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import java.util.*


/**
 * Create a new customer and return the id [Long]
 * @param customerName the customer's name
 * @param contactNumber the customer's contact phone number
 * @param emailAddress the customer's email address
 * @param postCode the post code of the customer's address
 * @param attachments list of customer attachments (attachment hash, attachment name pairs)
 * @return [Long] of the created customer
 */
@StartableByRPC
class CreateCustomerFlow(
        private val customerName: String,
        private val contactNumber: String,
        private val emailAddress: String,
        private val postCode: String,
        private val attachments: List<Pair<SecureHash, String>>
) : FlowLogic<UUID>() {

    @Suspendable
    override fun call(): UUID {

        if (attachments.isEmpty()) {
            throw IllegalArgumentException("One or more customer attachments must be provided")
        }

        val customer = CustomerSchemaV1.Customer.from(
                customerName,
                contactNumber,
                emailAddress,
                postCode,
                emptyList())

        val attachmentReferences = attachments.distinct().map {
            CustomerSchemaV1.AttachmentReference(it.first.toString(), it.second, customer)
        }
        customer.attachments = attachmentReferences

        customer.verifyCustomerDetails(serviceHub)
        createOffLedgerCustomer(customer)

        return customer.customerId
    }

    /**
     * Persists [CustomerSchemaV1.Customer] to DB
     * @param customer instance to persist
     * @return returns persisted [CustomerSchemaV1.Customer]
     * @throws [RefappException] if customer cannot be persisted.
     */
    fun createOffLedgerCustomer(customer: CustomerSchemaV1.Customer): CustomerSchemaV1.Customer {
        try {
            return serviceHub.withEntityManager {
                persist(customer)
                customer
            }
        } catch (e: Exception) {
            logger.error("Error while persisting customer: ${e.message}")
            throw RefappException("Customer cannot be persisted, please check with administrator!")
        }
    }
}