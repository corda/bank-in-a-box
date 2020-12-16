package com.r3.refapp.client

import com.r3.refapp.client.controllers.AccountController
import com.r3.refapp.client.utils.ControllerUtils
import com.r3.refapp.flows.reports.GetAccountFlow
import net.corda.core.CordaRuntimeException
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.lang.RuntimeException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@WebMvcTest(controllers = [AccountController::class])
@ExtendWith(SpringExtension::class)
@ActiveProfiles("noauth")
class AccountControllerTest {

    companion object {
        val proxy: CordaRPCOps = Mockito.mock(CordaRPCOps::class.java)
    }

    @TestConfiguration
    open class TestConfig {
        @Bean
        @Primary
        open fun nodeRPCConnection(): NodeRPCConnection {
            val nodeRPCConnection = Mockito.mock(NodeRPCConnection::class.java)
            Mockito.`when`(nodeRPCConnection.proxy).thenReturn(proxy)
            return nodeRPCConnection
        }

    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var accountController: AccountController

    @Test
    fun `test get currency instance success`() {
        val currency = ControllerUtils.getCurrencyInstanceFromString("GBP")
        assertEquals(currency.toString(), "GBP")
    }

    @Test
    fun `test get non fiat currency instance fail`() {
        val message = assertFailsWith<IllegalArgumentException> {
            ControllerUtils.getCurrencyInstanceFromString("ABC")
        }.message!!
        assertEquals("Invalid currency code 'ABC', available currencies are: ${Currency.getAvailableCurrencies().map{ it.toString() }}",
                message)
    }

    @Test
    fun `test getAccountById error handled for CordaRuntimeException happy path`() {
        val uuid = UUID.randomUUID()
        val errorMessage = "Vault query failed. Cannot find Account with id: $uuid"
        val getAccountByIdPath = "/accounts/$uuid"
        Mockito.`when`(proxy.startFlow(::GetAccountFlow, uuid)).thenThrow(CordaRuntimeException(errorMessage))

        mockMvc.perform(get(getAccountByIdPath))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("\$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("\$.statusCode").value("400"))
                .andExpect(jsonPath("\$.message").value(errorMessage))
                .andExpect(jsonPath("\$.path").value(getAccountByIdPath))
                .andExpect(jsonPath("\$.timestamp").isNotEmpty)
    }

    @Test
    fun `test getAccountById error handled for IllegalArgumentException happy path`() {
        val errorMessage = "Invalid currency code 'BLAH', available currencies are: ${Currency.getAvailableCurrencies().map { it.toString() }}"
        val createCurrentAccountPath = "/accounts/create-current-account"

        mockMvc.perform(post(createCurrentAccountPath)
                .param("customerId", UUID.randomUUID().toString())
                .param("tokenType", "BLAH")
                .param("withdrawalDailyLimit", null)
                .param("transferDailyLimit", null))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("\$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("\$.statusCode").value("400"))
                .andExpect(jsonPath("\$.message").value(errorMessage))
                .andExpect(jsonPath("\$.path").value(createCurrentAccountPath))
                .andExpect(jsonPath("\$.timestamp").isNotEmpty)
    }

    @Test
    fun `test getAccountById default error handler invoked happy path`() {
        val uuid = UUID.randomUUID()
        val errorMessage = "Some runtime exception error message"
        val getAccountByIdPath = "/accounts/$uuid"
        Mockito.`when`(proxy.startFlow(::GetAccountFlow, uuid)).thenThrow(RuntimeException(errorMessage))

        mockMvc.perform(get(getAccountByIdPath))
                .andExpect(status().isInternalServerError)
                .andExpect(jsonPath("\$.status").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("\$.statusCode").value("500"))
                .andExpect(jsonPath("\$.message").value("Internal server error occurred, please contact your administrator!"))
                .andExpect(jsonPath("\$.path").value(getAccountByIdPath))
                .andExpect(jsonPath("\$.timestamp").isNotEmpty)
    }

}