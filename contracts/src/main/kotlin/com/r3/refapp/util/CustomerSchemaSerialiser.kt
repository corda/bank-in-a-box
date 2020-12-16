package com.r3.refapp.util

import co.paralleluniverse.fibers.Suspendable
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.r3.refapp.schemas.CustomerSchemaV1
import java.time.Instant
import java.util.*

/**
 * Custom Kryo serializer/deserializer for Customer object. Problem with Hibernate entities and Kryo serialisation used
 * in Quasar framework in Corda is that Kryo is unable to serialize entities with collection properties since
 * serializer / deserializer doesn't have active session at the point of serialization / deserialization which is causing
 * errors in Corda log and flow cannot be checkpointed and saved to disk. One approach outlined in Corda documentation
 * is not to pass or return Hibernate entities to [Suspendable] methods in order to prevent entities to be on stack and
 * hence not serialised. Such solution would cause additional POJO's and mappers on every interaction with repositories.
 * Kryo allows creation of custom serializers, [CustomerSchemaSerializer] is an example of custom serializer in which entities
 * are detached from hibernate proxy object and then serialized / deserialized without causing any issues.
 */
class CustomerSchemaSerializer : Serializer<CustomerSchemaV1.Customer>() {

    /**
     * Override of Kryo's [Serializer] write function. Write function will be used in serialization process to write
     * objects content to disk when Quasar framework is tyring to checkpoint flow.
     * @param kryo the [Kryo] serializer / deserializer object
     * @param output the Kryo's output stream
     * @param `object` the object to be serialized
     */
    override fun write(kryo: Kryo, output: Output, `object`: CustomerSchemaV1.Customer) {
        output.writeString(`object`.createdOn.toString())
        output.writeString(`object`.modifiedOn.toString())
        output.writeString(`object`.customerName)
        output.writeString(`object`.contactNumber)
        output.writeString(`object`.emailAddress)
        output.writeString(`object`.postCode)
        output.writeString(`object`.customerId.toString())
        val attachments = `object`.attachments.map {
            val attachment = CustomerSchemaV1.AttachmentReference(it.attachmentHash, it.name, `object`)
            attachment.id = it.id
            attachment
        }
        kryo.writeClassAndObject(output, attachments)
    }

    /**
     * Override of Kryo's [Serializer] read function. Read function will be used in deserialization process to read
     * objects content from disk when Quasar framework is tyring to re-start checkpointed flow.
     * @param kryo the [Kryo] serializer / deserializer object
     * @param input the Kryo's input stream
     * @param type the type of object to be deserialized
     * @return Returns deserialized [CustomerSchemaV1.Customer] object.
     */
    override fun read(kryo: Kryo, input: Input, type: Class<CustomerSchemaV1.Customer>?): CustomerSchemaV1.Customer? {
        val createdOn = Instant.parse(input.readString())
        val modifiedOn = Instant.parse(input.readString())
        val customerName = input.readString()
        val contactNumber = input.readString()
        val emailAddress = input.readString()
        val postCode = input.readString()
        val customerId = UUID.fromString(input.readString())
        val attachments = kryo.readClassAndObject(input)
        attachments as List<CustomerSchemaV1.AttachmentReference>
        val customer = CustomerSchemaV1.Customer(createdOn, modifiedOn, customerName, contactNumber, emailAddress, postCode, attachments)
        customer.customerId = customerId
        attachments.forEach { it.customer = customer }
        return customer
    }

}