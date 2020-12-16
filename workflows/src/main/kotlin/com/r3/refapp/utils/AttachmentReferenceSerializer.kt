package com.r3.refapp.utils

import com.r3.refapp.schemas.CustomerSchemaV1
import net.corda.core.serialization.SerializationCustomSerializer
import java.time.Instant
import java.util.*

/**
 * Custom Corda serializer for [CustomerSchemaV1.AttachmentReference]. Standard Corda serialisation has minor issues with
 * serialisation of objects with circular reference e.g. Parent -> Child -> Parent and custom serializers can be used
 * to resolve mentioned issues. Also custom serializers can be used for complex data structures serialisation, ... .etc.
 */
class AttachmentReferenceSerializer : SerializationCustomSerializer<CustomerSchemaV1.AttachmentReference, AttachmentReferenceSerializer.Proxy> {

    /**
     * Proxy object to which will be sent between nodes over RPC instead of original object [CustomerSchemaV1.AttachmentReference].
     * @param id ID of the attachment reference
     * @param hash attachment's hash value
     * @param name attachment's human readable name
     * @param createdOn created on date of the customer associated with attachment
     * @param modifiedOn modified on date of the customer associated with attachment
     * @param customerName customer name the customer associated with attachment
     * @param contactNumber contact number of the customer associated with attachment
     * @param emailAddress contact number of the customer associated with attachment
     * @param postCode post code of the customer associated with attachment
     * @param customerId ID of the customer associated with attachment
     */
    data class Proxy(val id: Long, val hash: String, val name: String,
                     val createdOn: Instant, val modifiedOn: Instant, val customerName: String, val contactNumber: String,
                     val emailAddress: String, val postCode: String, val customerId: UUID)

    /**
     * ToProxy function called by the sender before object is sent over RPC, function maps original object to proxy.
     * @param obj original object
     * @return Returns proxy [Proxy] object.
     */
    override fun toProxy(obj: CustomerSchemaV1.AttachmentReference) : Proxy {
        return Proxy(obj.id, obj.attachmentHash, obj.name, obj.customer.createdOn, obj.customer.modifiedOn,
                obj.customer.customerName, obj.customer.contactNumber, obj.customer.emailAddress, obj.customer.postCode,
                obj.customer.customerId)
    }

    /**
     * ToProxy function called on the receivers side after object is received over RPC, function maps proxy object to original
     * object type.
     * @param proxy proxy object
     * @return returns original [CustomerSchemaV1.AttachmentReference] object.
     */
    override fun fromProxy(proxy: Proxy) : CustomerSchemaV1.AttachmentReference {
        val customer = CustomerSchemaV1.Customer(proxy.createdOn, proxy.modifiedOn, proxy.customerName, proxy.contactNumber,
                proxy.emailAddress, proxy.postCode, emptyList())
        customer.customerId = proxy.customerId
        val attachmentReference = CustomerSchemaV1.AttachmentReference(proxy.hash, proxy.name, customer)
        attachmentReference.id = proxy.id
        return attachmentReference
    }
}