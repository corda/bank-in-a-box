package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.schemas.CustomerSchemaV1
import com.r3.refapp.utils.verifyCustomerDetails
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import java.time.Instant
import java.util.*

/**
 * Update Customer with customerId
 * @param customerId Id of the Customer
 * @param customerName the new customer name
 * @param postCode the new customer post code
 * @param contactNumber the new customer contact phone number
 * @param emailAddress the new customer email address
 * @param attachments list of attachments to append, (hash, name) pairs
 */
@StartableByRPC
class UpdateCustomerFlow(
        private val customerId: UUID,
        private val customerName: String? = null,
        private val postCode: String? = null,
        private val contactNumber: String? = null,
        private val emailAddress: String? = null,
        private val attachments: List<Pair<SecureHash, String>>? = null
) : FlowLogic<CustomerSchemaV1.Customer>() {
    @Suspendable
    override fun call(): CustomerSchemaV1.Customer {
        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val customer = accountRepository.getCustomerWithId(customerId)

        // if all parameters are null return unmodified customer
        if (attachments == null && contactNumber == null && emailAddress == null && customerName == null && postCode == null)
            return customer

        if (attachments != null)
            customer.attachments = getMergedAttachments(customer)
        if (contactNumber != null)
            customer.contactNumber = contactNumber
        if (emailAddress != null)
            customer.emailAddress = emailAddress
        if (customerName != null)
            customer.customerName = customerName
        if (postCode != null)
            customer.postCode = postCode

        customer.modifiedOn = Instant.now()

        customer.verifyCustomerDetails(serviceHub)
        serviceHub.withEntityManager {
            merge(customer)
        }
        return customer
    }

    /**
     * Return a unique list of customer attachments, merging [customer.attachments] and [this.attachments]
     * @param customer with existing attachments
     * @return merged attachment list
     */
    private fun getMergedAttachments(customer: CustomerSchemaV1.Customer): List<CustomerSchemaV1.AttachmentReference> {
        if (attachments == null) return customer.attachments
        val mergedAttachments = customer.attachments.toMutableList()
        attachments.forEach {
            mergedAttachments.add(CustomerSchemaV1.AttachmentReference(it.first.toString(), it.second, customer))
        }
        return mergedAttachments.distinctBy { it.attachmentHash }
    }
}