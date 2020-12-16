package com.r3.refapp.client.controllers

import com.r3.refapp.client.NodeRPCConnection
import com.r3.refapp.client.response.CustomerNameResponse
import com.r3.refapp.client.utils.ControllerUtils
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.flows.reports.GetCustomersPaginatedFlow
import com.r3.refapp.flows.CreateCustomerFlow
import com.r3.refapp.flows.UpdateCustomerFlow
import com.r3.refapp.flows.reports.GetCustomerByIdFlow
import com.r3.refapp.flows.reports.GetCustomerNameByAccountFlow
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.schemas.CustomerSchemaV1
import net.corda.client.jackson.JacksonSupport
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

/**
 * Provides Customer API endpoints.
 */
@RestController
@RequestMapping("/customers") // The paths for HTTP requests are relative to this base path.
class CustomerController(rpc: NodeRPCConnection){

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy
    private val objectMapper = JacksonSupport.createNonRpcMapper()


    /**
     * Invokes the create customer flows which creates new customer and returns the newly created customer's id
     * @param customerName the customer's name
     * @param contactNumber the customer's contact phone number
     * @param emailAddress the customer's email address
     * @param postCode the post code of the customer's address
     * @param attachments list of customer attachments (attachment hash, attachment name pairs)
     * @return id of the created customer in {"customerId": "$customerId"} json format
     */
    @PostMapping(value = ["/create"], produces = ["application/json"])
    private fun createCustomer(@RequestParam(value = "customerName") customerName: String,
                               @RequestParam(value = "contactNumber") contactNumber: String,
                               @RequestParam(value = "emailAddress") emailAddress: String,
                               @RequestParam(value = "postCode") postCode: String,
                               @RequestParam(value = "attachments") attachments: List<String>
    ): String {
        val processedAttch = ControllerUtils.processStringAttachments(attachments)
        val customerId = proxy.startFlow(::CreateCustomerFlow, customerName, contactNumber, emailAddress, postCode, processedAttch).returnValue.getOrThrow()
        return objectMapper.writeValueAsString(mapOf("customerId" to customerId))
    }

    /**
     * Upload an attachment to the node. ZIP and Jar file types are supported
     * @param file ZIP or Jar file attachment
     * @param uploader name of the uploader
     * @return secureHash of the attachment in {"secureHash": "$hash"} json format
     */
    @PostMapping(value = ["/upload-attachment"], produces = ["application/json"])
    private fun uploadAttachment(@RequestParam(value = "file") file: MultipartFile, @RequestParam(value = "uploader") uploader: String): String {
        val filename = file.originalFilename
        require(filename != null) { "File name must be set" }

        val hasZipJarExtension = filename.endsWith("zip", true) || filename.endsWith("jar", true)
        require (hasZipJarExtension) { "Only ZIP or Jar attachments are supported" }

        val hash = proxy.uploadAttachmentWithMetadata(
                jar = file.inputStream,
                uploader = uploader,
                filename = filename!!
        )
        return objectMapper.writeValueAsString(mapOf("secureHash" to hash.toString()))
    }

    /**
     * Queries paginated list of all the customers
     * @param startPage position of the result page to retrieve
     * @param pageSize specifies the maximum number of customers in one page
     * @param searchTerm can be matched in LIKE fashion against the column fields of [CustomerSchemaV1.Customer].
     * @param sortField sort result based on this field
     * @param sortOrder order of the sort (ASC or DESC) [RepositoryQueryParams.SortOrder]
     * @return List of [CustomerSchemaV1.Customer]
     */
    @GetMapping(value = [""], produces = ["application/json"])
    private fun getCustomersPaginated(@RequestParam startPage: Int?, @RequestParam pageSize: Int?,
                                      @RequestParam sortField: String?, @RequestParam sortOrder: RepositoryQueryParams.SortOrder?,
                                      @RequestParam searchTerm: String?
                                      ): PaginatedResponse<CustomerSchemaV1.Customer> {
        val tmpStartPage = startPage ?: 1
        val tmpPageSize = pageSize ?: 100
        val tmpSearchTerm = searchTerm ?: ""
        val queryParam = RepositoryQueryParams(tmpStartPage, tmpPageSize, sortField, sortOrder ?: RepositoryQueryParams.SortOrder.ASC,
                tmpSearchTerm)
        return proxy.startFlow(::GetCustomersPaginatedFlow, queryParam).returnValue.getOrThrow()
    }

    /**
     * Queries a customer with given customerId
     * @param customerId of [CustomerSchemaV1.Customer] to query
     * @return [CustomerSchemaV1.Customer] with customerId
     */
    @GetMapping(value = ["/{customerId}"], produces = ["application/json"])
    private fun getCustomer(@PathVariable customerId: UUID): CustomerSchemaV1.Customer {
        return proxy.startFlow(::GetCustomerByIdFlow, customerId).returnValue.getOrThrow()
    }

    /**
     * Update a customer with given customerId
     * @param customerId of [CustomerSchemaV1.Customer] to update
     * @param customerName the new customer name
     * @param postCode the new customer post code
     * @param contactNumber the new customer contact phone number
     * @param emailAddress the new customer email address
     * @param attachments list of attachments to add
     * @return [CustomerSchemaV1.Customer] updated customer
     */
    @PutMapping(value = ["/update/{customerId}"], produces = ["application/json"])
    private fun updateCustomer(@PathVariable customerId: UUID,
                               @RequestParam(value = "customerName") customerName: String?,
                               @RequestParam(value = "postCode") postCode: String?,
                               @RequestParam(value = "contactNumber") contactNumber: String?,
                               @RequestParam(value = "emailAddress") emailAddress: String?,
                               @RequestParam(value = "attachments") attachments: List<String>?): CustomerSchemaV1.Customer {

        val processedAttch = if (attachments != null ) ControllerUtils.processStringAttachments(attachments) else null
        return proxy.startFlow(::UpdateCustomerFlow, customerId, customerName, postCode, contactNumber, emailAddress, processedAttch)
                .returnValue.getOrThrow()
    }

    /**
     * Queries for a accounts owner customer name with given [accountId]
     * @param accountId Id of the account
     * @return [CustomerNameResponse] of the account owner
     */
    @GetMapping(value = ["/name/{accountId}"], produces = ["application/json"])
    private fun getCustomerNameForAccountId(@PathVariable accountId: UUID): CustomerNameResponse {
        return CustomerNameResponse(proxy.startFlow(::GetCustomerNameByAccountFlow, accountId).returnValue.getOrThrow())
    }
}