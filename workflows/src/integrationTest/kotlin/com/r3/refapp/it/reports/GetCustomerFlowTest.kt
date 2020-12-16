package com.r3.refapp.it.reports

import com.r3.refapp.flows.reports.GetCustomersPaginatedFlow
import com.r3.refapp.flows.reports.GetCustomerByIdFlow
import com.r3.refapp.repositories.*
import com.r3.refapp.test.utils.AbstractITHelper
import com.r3.refapp.test.utils.TestUtils.createAttachment
import com.r3.refapp.test.utils.TestUtils.createCustomer
import com.r3.refapp.test.utils.TestUtils.executeFlowWithRunNetwork
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetCustomerFlowTest : AbstractITHelper() {

    companion object {
        val searchTerm = "XYZ"
        val customersWithSearchTerm = mutableListOf<UUID>()
        val customerIdNotExit = UUID.randomUUID()
        lateinit var customerId: UUID
        lateinit var attachment: Pair<SecureHash, String>

        @BeforeAll
        @JvmStatic
        fun setupClients() {
            attachment = createAttachment(node = bank, network = network)
            customerId = createCustomer("Test Customer", listOf(attachment), bank, network)
            // adding some extra elements that do not match the search criteria
            for (i in 0..6 step 2)  {
                createCustomer("Test Customer${i}", emptyList(), bank, network)
            }
            customersWithSearchTerm.add(createCustomer(customerName = "Test Customer1", contactNumber = "123${searchTerm}", attachments = emptyList(),  node = bank, network = network))
            customersWithSearchTerm.add(createCustomer(customerName = "Test Customer3", emailAddress = "customer@${searchTerm}.com", attachments =  emptyList(), node = bank, network = network))
            customersWithSearchTerm.add(createCustomer(customerName = "Test Customer5", contactNumber = "123${searchTerm}456", attachments =  emptyList(), node = bank, network = network))
            customersWithSearchTerm.add(createCustomer(customerName = "Test Customer7", postCode = "${searchTerm}D09 B17", attachments =  emptyList(), node = bank, network = network))
            customersWithSearchTerm.add(createCustomer(customerName = "Test Customer9${searchTerm}", attachments = emptyList(), node = bank, network = network))
        }
    }

    @Test
    fun `test GetCustomerByIdFlow happy path`() {
        val customer = executeFlowWithRunNetwork(GetCustomerByIdFlow(customerId), bank, network)

        assertEquals(customerId, customer.customerId)
        assertEquals("Test Customer", customer.customerName)
        assertEquals("123456789", customer.contactNumber)
        assertEquals("test-email@r3.com", customer.emailAddress)
        assertEquals("D01 K11", customer.postCode)
        assertEquals(1, customer.attachments.count())
        assertEquals(attachment.first.toString(), customer.attachments.single().attachmentHash)
        assertEquals(attachment.second, customer.attachments.single().name)
        assertEquals(customerId, customer.attachments.single().customer.customerId)
    }

    @Test
    fun `test GetCustomerByIdFlow fails with not customer`() {

        val message = assertFailsWith<ExecutionException> {
            executeFlowWithRunNetwork(GetCustomerByIdFlow(customerIdNotExit), bank, network)
        }.message!!

        assertEquals("java.lang.IllegalArgumentException: Customer with id $customerIdNotExit does not exist", message)
    }

    @Test
    fun `test GetCustomerPaginatedFlow with query search term`() {
        val queryParams = RepositoryQueryParams(sortField = "customerName", sortOrder = RepositoryQueryParams.SortOrder.ASC, searchTerm = searchTerm)
        val queriedCustomers = executeFlowWithRunNetwork(GetCustomersPaginatedFlow(queryParams), bank, network).result
        assertEquals(customersWithSearchTerm.size, queriedCustomers.size)

        for (i in 0 until customersWithSearchTerm.size) {
            assertEquals(customersWithSearchTerm[i], queriedCustomers[i].customerId)
        }
    }
    @Test
    fun `test GetCustomerPaginatedFlow order asc is reverse of desc`() {
        // check if desc query is revers of asc query
        val queryParamsAsc = RepositoryQueryParams(sortField = "customerName", sortOrder = RepositoryQueryParams.SortOrder.ASC, searchTerm = searchTerm)
        val queriedCustomersAsc = executeFlowWithRunNetwork(GetCustomersPaginatedFlow(queryParamsAsc), bank, network)
        val queryParamsDesc = RepositoryQueryParams(sortField = "customerName", sortOrder = RepositoryQueryParams.SortOrder.DESC, searchTerm = searchTerm)
        val queriedCustomersDesc = executeFlowWithRunNetwork(GetCustomersPaginatedFlow(queryParamsDesc), bank, network)

        assertEquals(customersWithSearchTerm.size, queriedCustomersAsc.result.size)
        assertEquals(customersWithSearchTerm.size, queriedCustomersDesc.result.size)
        assertEquals(customersWithSearchTerm.size.toLong(), queriedCustomersDesc.totalResults)
        assertEquals(customersWithSearchTerm.size.toLong(), queriedCustomersDesc.totalResults)
        for (i in 0 until customersWithSearchTerm.size) {
            assertEquals(queriedCustomersAsc.result[customersWithSearchTerm.size - i - 1].customerId, queriedCustomersDesc.result[i].customerId)
        }
    }

    @Test
    fun `test GetCustomerPaginatedFlow getting middle page`() {
        // test pagination by getting middle elements
        val queryParamsPaginated = RepositoryQueryParams(sortField = "customerName", startPage = 2, pageSize = 2, sortOrder = RepositoryQueryParams.SortOrder.ASC, searchTerm = searchTerm)
        val queriedCustomersPaginated = executeFlowWithRunNetwork(GetCustomersPaginatedFlow(queryParamsPaginated), bank, network)
        assertEquals(2, queriedCustomersPaginated.result.size)
        assertEquals(5, queriedCustomersPaginated.totalResults)
        assertEquals(customersWithSearchTerm[2], queriedCustomersPaginated.result[0].customerId)
        assertEquals(customersWithSearchTerm[3], queriedCustomersPaginated.result[1].customerId)
    }

    @Test
    fun `test GetCustomerPaginatedFlow search criteria not found`() {
        val searchTerm = "ZYX"
        val queryParams = RepositoryQueryParams(sortField = "customerName", sortOrder = RepositoryQueryParams.SortOrder.ASC, searchTerm = searchTerm)
        val queriedCustomers = executeFlowWithRunNetwork(GetCustomersPaginatedFlow(queryParams), bank, network)
        assertEquals(queriedCustomers.totalResults, 0)
        assertEquals(queriedCustomers.result.size, 0)
    }

}