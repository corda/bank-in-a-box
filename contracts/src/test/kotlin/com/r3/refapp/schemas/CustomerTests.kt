package com.r3.refapp.schemas

import org.junit.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CustomerTests : AbstractEntityTest() {

    private val createOnDate = Instant.now()
    private val modifiedOnDate = Instant.now()
    private val customerName = "test customer"
    private val contactNumber = "123456789"
    private val email = "test.customer1@r3.com"
    private val postCode = "QWERTY123"
    private val attachmentHash = "attachmentHash"
    private val attachmentName = "attachmentName"

    @Test
    fun `verify all fields in Customer equals method`() {

        val customers = getCustomers()
        val clonedCustomer = clone(customers.first)

        assertNotEquals(customers.first, customers.second)
        assertEquals(customers.first, clonedCustomer)
        verifyWithEachPropertyChanged(customers.first, customers.second, listOf("attachments")) { cust1: CustomerSchemaV1.Customer,
                                                                                                  cust2: CustomerSchemaV1.Customer ->
            assertNotEquals(cust1, cust2)
        }
    }

    @Test
    fun `verify all fields in Customer hashCode method`() {

        val customers = getCustomers()
        val clonedCustomer = clone(customers.first)

        assertNotEquals(customers.first.hashCode(), customers.second.hashCode())
        assertEquals(customers.first.hashCode(), clonedCustomer.hashCode())
        verifyWithEachPropertyChanged(customers.first, customers.second,
                listOf("attachments")) { cust1: CustomerSchemaV1.Customer,
                                         cust2: CustomerSchemaV1.Customer ->
            assertNotEquals(cust1.hashCode(), cust2.hashCode())
        }
    }

    @Test
    fun `verify all fields in AttachmentReference equals method`() {

        val customers = getCustomers()
        val attachmentReferences = getAttachments(customers)
        val clonedAttachmentReference = clone(attachmentReferences.first)

        assertNotEquals(attachmentReferences.first, attachmentReferences.second)
        assertEquals(attachmentReferences.first, clonedAttachmentReference)
        verifyWithEachPropertyChanged(attachmentReferences.first, attachmentReferences.second) { attachment1: CustomerSchemaV1.AttachmentReference,
                                                                                                 attachment2: CustomerSchemaV1.AttachmentReference
            -> assertNotEquals(attachment1, attachment2)
        }
    }

    @Test
    fun `verify all fields in AttachmentReference hashCode method`() {

        val customers = getCustomers()
        val attachmentReferences = getAttachments(customers)
        val clonedAttachmentReference = clone(attachmentReferences.first)

        assertNotEquals(attachmentReferences.first.hashCode(), attachmentReferences.second.hashCode())
        assertEquals(attachmentReferences.first.hashCode(), clonedAttachmentReference.hashCode())
        verifyWithEachPropertyChanged(attachmentReferences.first, attachmentReferences.second) { attachment1: CustomerSchemaV1.AttachmentReference,
                                                                                                 attachment2: CustomerSchemaV1.AttachmentReference
            -> assertNotEquals(attachment1.hashCode(), attachment2.hashCode())
        }
    }

    private fun getCustomers(): Pair<CustomerSchemaV1.Customer, CustomerSchemaV1.Customer> {
        val customer1 = CustomerSchemaV1.Customer(createOnDate, modifiedOnDate, customerName,
                contactNumber, email, postCode, emptyList())
        val customer2 = CustomerSchemaV1.Customer(createOnDate.plusSeconds(1), modifiedOnDate.plusSeconds(1),
                customerName + "1", contactNumber + "1", email + "1", postCode + "1", emptyList())
        customer2.customerId = UUID.randomUUID()
        return Pair(customer1, customer2)
    }

    private fun getAttachments(customers: Pair<CustomerSchemaV1.Customer, CustomerSchemaV1.Customer>)
            : Pair<CustomerSchemaV1.AttachmentReference, CustomerSchemaV1.AttachmentReference> {
        val attachmentReference1 = CustomerSchemaV1.AttachmentReference(attachmentHash, attachmentName, customers.first)
        val attachmentReference2 = CustomerSchemaV1.AttachmentReference(attachmentHash + 1,
                attachmentName + 1, customers.second)
        attachmentReference2.id = 2
        return Pair(attachmentReference1, attachmentReference2)
    }
}