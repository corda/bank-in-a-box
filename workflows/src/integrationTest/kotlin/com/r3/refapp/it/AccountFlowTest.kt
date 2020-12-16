package com.r3.refapp.it

import com.r3.refapp.flows.CreateCurrentAccountFlow
import com.r3.refapp.flows.CreateCustomerFlow
import com.r3.refapp.flows.CreateSavingsAccountFlow
import com.r3.refapp.flows.UpdateCustomerFlow
import com.r3.refapp.schemas.CustomerSchemaV1
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.states.SavingsAccountState
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.createAttachment
import com.r3.refapp.test.utils.TestUtils.createCustomer
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.InputStreamAndHash
import net.corda.core.node.services.queryBy
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFails

import kotlin.test.assertFailsWith


class AccountFlowTests : AbstractITHelper() {

    @Test
    fun `test create customer`() {
        val attachment = createAttachment(node = bank, network = network)
        val customerId = createCustomer("AN Other", listOf(attachment), bank, network)

        val customer: CustomerSchemaV1.Customer = accountRepository.getCustomerWithId(customerId)

        // assert that attachment has been correctly added to the customer
        assert(customer.attachments.size == 1)
        assert(customer.attachments.single().attachmentHash == attachment.first.toString())
    }

    @Test
    fun `test create customer fails with post code too long`() {
        val attachment = createAttachment(node = bank, network = network)

        val message = assertFailsWith<ExecutionException> {
        val customerId = createCustomer("AN Other", listOf(attachment), bank, network, postCode = "too long test post" +
                " code!")
        }.message

        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Customer cannot be persisted, please check " +
                "with administrator!", message)
    }

    @Test
    fun `test create customer fails with attachment already used`() {
        val attachment = createAttachment(node = bank, network = network)
        createCustomer("Customer1", listOf(attachment), bank, network)

        val message = assertFailsWith<ExecutionException> {
            createCustomer("Customer2", listOf(attachment), bank, network)
        }.message

        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Some attachments from the customer's attachment list are associated with different " +
                "customer, Please provide customer specific attachments.", message)
    }

    @Test
    fun `test empty attachment list fail`() {
        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(CreateCustomerFlow("AN Other", "123456789", "test-email@r3.com", "D01 K11",
                    emptyList()), bank, network)
        }.message!!

        assertEquals("java.lang.IllegalArgumentException: One or more customer attachments must be provided", message)
    }

    @Test
    fun `test attachment does not exist fail`() {
        assertFailsWith<ExecutionException> {
            val emptyAttachments = listOf(Pair(SecureHash.randomSHA256(), ""))
            createCustomer("AN Other", emptyAttachments, bank, network)
        }
    }

    @Test
    fun `test create current account with zero balance`() {
        val customerId = createCustomer("AN Other", attachments, bank, network)

        val signedTx = executeFlowWithRunNetwork(CreateCurrentAccountFlow(customerId, EUR), bank, network)

        val accountState = signedTx.tx.outputStates.single() as CurrentAccountState
        assert(accountState.accountData.balance == 0 of EUR)
    }

    @Test
    fun `test create savings account with zero balance`() {
        val customerId = createCustomer("AN Other", emptyList(), bank, network)

        val currentAccountTx = executeFlowWithRunNetwork(CreateCurrentAccountFlow(customerId, EUR), bank, network)
        val currentAccount = currentAccountTx.coreTransaction.outputsOfType<CurrentAccountState>().single()

        val savingsAccountTx = executeFlowWithRunNetwork(
                CreateSavingsAccountFlow(customerId, EUR, currentAccount.accountData.accountId, 10 of EUR,
                        Instant.now().plusSeconds(60)), bank, network)

        val savingsAccountState = savingsAccountTx.tx.outputStates.single() as SavingsAccountState
        val recurringPayment = bank.services.vaultService.queryBy<RecurringPaymentState>().states.single {
            it.state.data.accountTo == savingsAccountState.accountData.accountId }

        assert(savingsAccountState.accountData.balance == 0 of EUR)
        assert(currentAccount.accountData.accountId == recurringPayment.state.data.accountFrom)
    }

    @Test
    fun `test create current account customer does not exist fail`() {
        assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(CreateCurrentAccountFlow(UUID.randomUUID(), EUR), bank, network)
        }
    }

    @Test
    fun `test create savings account customer does not exist fail`() {
        assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(CreateSavingsAccountFlow(
                    UUID.randomUUID(), EUR, UUID.randomUUID(), 10 of EUR,
                    Instant.now().plusSeconds(60)),
                    bank, network)
        }
    }

    @Test
    fun `test create savings account current account does not exist fail`() {
        val customerId = createCustomer("AN Other", emptyList(), bank, network)

        assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(CreateSavingsAccountFlow(customerId, EUR, UUID.randomUUID(), 10 of EUR,
                    Instant.now().plusSeconds(60)),
                    bank, network)
        }
    }

    @Test
    fun `test update customer attachments`() {
        val customerId = createCustomer("AN Other", listOf(createAttachment(node = bank, network = network)), bank, network)

        val newAttachment = bank.services.attachments.importAttachment(
                InputStreamAndHash.createInMemoryTestZip(RandomUtils.nextInt(1, 1024),
                        RandomUtils.nextInt(0, Byte.MAX_VALUE.toInt()).toByte()).inputStream,
                "BankA",
                "additionalSupportingDocumentation.zip")

        val attachments = listOf(Pair(newAttachment, "Appended Attachment"))
        executeFlowWithRunNetwork(UpdateCustomerFlow(customerId, attachments = attachments), bank, network)

        val customer: CustomerSchemaV1.Customer = accountRepository.getCustomerWithId(customerId)

        assertEquals(2, customer.attachments.size)
    }

    @Test
    fun `test update customer attachments fails with attachment already used`() {
        val assignedAttachment = createAttachment(node = bank, network = network)
        createCustomer("Customer1", listOf(assignedAttachment), bank, network)
        val customerId = createCustomer("Customer1", emptyList(), bank, network)

        val attachments = listOf(Pair(assignedAttachment.first, "Appended Attachment"))

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(UpdateCustomerFlow(customerId, attachments = attachments), bank, network)
        }.message

        assertEquals("com.r3.refapp.exceptions.RefappException: Refapp exception: Some attachments from the customer's attachment list are associated with different " +
                "customer, Please provide customer specific attachments.", message)
    }

    @Test
    fun `test update customer data`() {
        val attachment = createAttachment(node = bank, network = network)
        val customerId = createCustomer("AN Other", listOf(attachment), bank, network)

        val updateCustomerName = "new name"
        val updatePostCode = "PC2"
        val updateContactNumber = "updatedNumber"
        val updateEmailAddress = "updatedemail@emal.com"

        executeFlowWithRunNetwork(UpdateCustomerFlow(
                customerId, updateCustomerName, updatePostCode, updateContactNumber, updateEmailAddress),
                bank, network)

        val customer: CustomerSchemaV1.Customer = accountRepository.getCustomerWithId(customerId)

        assertEquals(1, customer.attachments.size)
        // check if attachment is unchanged
        assertEquals(attachment.first.toString(), customer.attachments[0].attachmentHash)
        assertEquals(updateContactNumber, customer.contactNumber)
        assertEquals(updateEmailAddress, customer.emailAddress)
        assertEquals(updateCustomerName, customer.customerName)
        assertEquals(updatePostCode, customer.postCode)
    }
}