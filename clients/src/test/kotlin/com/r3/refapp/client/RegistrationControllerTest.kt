package com.r3.refapp.client

import com.r3.refapp.client.auth.service.DaoUserDetailsService
import com.r3.refapp.client.utils.generateAttachmentHash
import com.r3.refapp.flows.reports.GetCustomerByIdFlow
import com.r3.refapp.schemas.CustomerSchemaV1
import com.r3.refapp.test.utils.getFlowHandle
import net.corda.core.internal.InputStreamAndHash
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.annotation.EnableTransactionManagement
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DataJpaTest
class RegistrationControllerTest {

    companion object {
        val proxy: CordaRPCOps = Mockito.mock(CordaRPCOps::class.java)
    }

    @Autowired
    lateinit var userDetailsService: DaoUserDetailsService

    private val multipartFileAttachment = MockMultipartFile(
            "supportingDocumentation.zip",
            "supportingDocumentation.zip",
            "application/zip",
            InputStreamAndHash.createInMemoryTestZip(1024, 0).inputStream)

    @TestConfiguration
    @EnableJpaRepositories(basePackages = ["com.r3.refapp.client.auth.repository"])
    @EnableTransactionManagement
    open class TestConfig {

        @Bean
        @Primary
        open fun nodeRPCConnection(): NodeRPCConnection {
            val nodeRPCConnection = Mockito.mock(NodeRPCConnection::class.java)
            Mockito.`when`(nodeRPCConnection.proxy).thenReturn(proxy)
            return nodeRPCConnection
        }

        @Bean
        @Primary
        open fun userDetailsService() : DaoUserDetailsService {
            return DaoUserDetailsService()
        }

    }


    @Test
    fun `test customer ID exists fail`() {
        val customer = CustomerSchemaV1.Customer.from(
                customerName = "customer1",
                contactNumber = "5551234",
                emailAddress = "customer1@r3.com",
                postCode = "ZIP1234",
                attachments = emptyList()
        )

        val hash = multipartFileAttachment.generateAttachmentHash()
        customer.attachments = listOf(CustomerSchemaV1.AttachmentReference(hash, "supportingDocumentation.zip", customer))

        Mockito.`when`(proxy.startFlow(::GetCustomerByIdFlow, customer.customerId))
                .thenReturn(getFlowHandle<CustomerSchemaV1.Customer>(customer))

        userDetailsService.saveNewUser(
                customer.customerName,
                "12345",
                customer.emailAddress,
                customer.customerId,
                multipartFileAttachment)

        val message = assertFailsWith<IllegalArgumentException> {
            userDetailsService.saveNewUser(
                    "customer2",
                    "12345",
                    customer.emailAddress,
                    customer.customerId,
                    multipartFileAttachment)
        }.message!!

        assertEquals("User with customerId already exists", message)
    }
}