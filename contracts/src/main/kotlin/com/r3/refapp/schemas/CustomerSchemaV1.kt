package com.r3.refapp.schemas

import com.esotericsoftware.kryo.DefaultSerializer
import com.r3.refapp.util.CustomerSchemaSerializer
import net.corda.core.schemas.MappedSchema
import net.corda.core.serialization.CordaSerializable
import org.hibernate.annotations.Type
import java.io.Serializable
import java.time.Instant
import java.util.*
import javax.persistence.*

/**
 * The family of schemas for Customer.
 */
object CustomerSchema


/**
 * A Customer schema.
 */
class CustomerSchemaV1 : MappedSchema(
        schemaFamily = CustomerSchema::class.java,
        version = 1,
        mappedTypes = listOf(Customer::class.java, AttachmentReference::class.java)) {

    /**
     * JPA entity [Customer] mapping class. Because of [OneToMany] relationship Kryo serializer is unable to properly
     * serialize / deserialize object, in order to resolve this issue, custom serializer is provided via [DefaultSerializer]
     * annotation, for more information @see [CustomerSchemaSerializer]
     */
    @Entity
    @Table(name = "customer")
    @DefaultSerializer(CustomerSchemaSerializer::class)
    @CordaSerializable
    class Customer (
            @Column(name = "created_on", nullable = false)
            var createdOn: Instant,

            @Column(name = "modified_on", nullable = false)
            var modifiedOn: Instant,

            @Column(name = "customer_name", nullable = false)
            var customerName: String,

            @Column(name = "contact_number", nullable = false)
            var contactNumber: String,

            @Column(name = "email", nullable = false)
            var emailAddress: String,

            @Column(name = "postcode", nullable = false)
            var postCode: String,

            @OneToMany(cascade = [(CascadeType.ALL)], fetch = FetchType.EAGER, mappedBy = "customer")
            var attachments: List<AttachmentReference> = emptyList(),

            @Id
            @Column(name = "customer_id", nullable = false)
            @Type(type = "uuid-char")
            var customerId: UUID = UUID.randomUUID()
    ) : Serializable {

        companion object {
            fun from(
                    customerName: String,
                    contactNumber: String,
                    emailAddress: String,
                    postCode: String,
                    attachments: List<AttachmentReference>): Customer {

                val now = Instant.now()
                return Customer(
                        createdOn = now,
                        modifiedOn = now,
                        customerName = customerName,
                        contactNumber = contactNumber,
                        emailAddress = emailAddress,
                        postCode = postCode,
                        attachments = attachments
                )
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Customer

            if (createdOn != other.createdOn) return false
            if (modifiedOn != other.modifiedOn) return false
            if (customerName != other.customerName) return false
            if (contactNumber != other.contactNumber) return false
            if (emailAddress != other.emailAddress) return false
            if (postCode != other.postCode) return false
            if (customerId != other.customerId) return false
            if (attachments != other.attachments) return false

            return true
        }

        override fun hashCode(): Int {
            var result = createdOn.hashCode()
            result = 31 * result + modifiedOn.hashCode()
            result = 31 * result + customerName.hashCode()
            result = 31 * result + contactNumber.hashCode()
            result = 31 * result + emailAddress.hashCode()
            result = 31 * result + postCode.hashCode()
            result = 31 * result + customerId.hashCode()
            result = 31 * result + attachments.hashCode()
            return result
        }
    }


    @Entity
    @Table(name = "attachment_reference", indexes = [Index(name = "customer_id_fk_idx", columnList = "customer_id")])
    class AttachmentReference(

            @Column(name = "attachment_hash", unique = true)
            var attachmentHash: String,

            @Column(name = "attachment_name")
            var name: String,

            @ManyToOne(fetch = FetchType.LAZY)
            @JoinColumn(name = "customer_id", nullable = false)
            @Type(type = "uuid-char")
            var customer: Customer

    ) : Serializable {

        @Id
        @GeneratedValue
        @Column(name = "attachment_id")
        var id: Long = 0

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AttachmentReference

            if (attachmentHash != other.attachmentHash) return false
            if (name != other.name) return false
            if (id != other.id) return false
            if (customer.customerId != other.customer.customerId) return false

            return true
        }

        override fun hashCode(): Int {
            var result = attachmentHash.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + id.hashCode()
            result = 31 * result + customer.customerId.hashCode()
            return result
        }

    }
}