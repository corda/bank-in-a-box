package com.r3.refapp.client.auth.verifier

import com.r3.refapp.client.NodeRPCConnection
import com.r3.refapp.client.auth.Constants.ACCOUNT_ID_PROPERTY
import com.r3.refapp.client.auth.Constants.CUSTOMER_NAME_PROPERTY
import com.r3.refapp.client.auth.Constants.FROM_ACCOUNT_ID_PROPERTY
import com.r3.refapp.client.auth.Constants.POST_CODE_PROPERTY
import com.r3.refapp.client.auth.Constants.PRODUCTION_PROFILE
import com.r3.refapp.client.auth.Constants.RECURRING_PAYMENT_ID_PROPERTY
import com.r3.refapp.client.auth.Constants.ROLE_NAME_ADMIN
import com.r3.refapp.client.auth.Constants.ROLE_NAME_CUSTOMER
import com.r3.refapp.client.utils.extractCustomerId
import com.r3.refapp.flows.reports.GetAccountFlow
import com.r3.refapp.flows.reports.GetRecurringPaymentByIdFlow
import com.r3.refapp.flows.reports.GetTransactionByIdFlow
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.stereotype.Component
import java.util.*
import javax.servlet.http.HttpServletRequest

/**
 * Role based security verifier. Provides various functions for role and customer based access right verification.
 */
@Component
@Profile(PRODUCTION_PROFILE)
class IdMatcherVerifier {

    @Autowired
    lateinit var tokenStore: TokenStore

    @Autowired
    lateinit var rpc: NodeRPCConnection


    companion object {
        private val logger = LoggerFactory.getLogger(IdMatcherVerifier::class.java)
    }

    /**
     * Verifies that endpoint with [customerId] value in request path is accessed by a user with ADMIN role or a user
     * with CUSTOMER role and associated [customerId]
     * @param authentication Spring security [Authentication] object
     * @param customerId customers id
     * @return Boolean value based on security evaluation
     */
    fun verifyWithCustomerIdMatching(authentication: Authentication, customerId: String): Boolean {
        if (authentication.authorities.find { it.authority == ROLE_NAME_ADMIN } != null) return true

        return authentication.extractCustomerId(tokenStore) == customerId
    }

    fun verifyWithCustomerIdAndPropertiesMatching(authentication: Authentication, customerId: String,
                                                  httpServletRequest: HttpServletRequest): Boolean {
        if (authentication.authorities.find { it.authority == ROLE_NAME_ADMIN } != null) return true
        if (verifyWithCustomerIdMatching(authentication, customerId)) {
            val customerName = httpServletRequest.getParameter(CUSTOMER_NAME_PROPERTY)
            val postCode = httpServletRequest.getParameter(POST_CODE_PROPERTY)
            if (customerName == null && postCode == null) {
                return true
            }
        }
        return false
    }

    /**
     * Verifies that endpoint with [accountId] value in request path is accessed by a user with ADMIN role or a user
     * with CUSTOMER role which is an owner of the account with given [accountId]
     * @param authentication Spring security [Authentication] object
     * @param accountId account id
     * @return Boolean value based on security evaluation
     */
    fun verifyWithAccountIdMatching(authentication: Authentication, accountId: String): Boolean {

        if (authentication.authorities.find { it.authority == ROLE_NAME_ADMIN } != null) return true
        return verifyAccountIdMatching(authentication, accountId)
    }

    /**
     * Verifies that endpoint with accountId value in request params is accessed by a user with ADMIN role or a user
     * with CUSTOMER role which is an owner of the account with given accountId
     * @param authentication Spring security [Authentication] object
     * @param httpServletRequest servlet request containing accountId in request params
     * @return Boolean value based on security evaluation
     */
    fun verifyWithAccountIdInParamMatching(authentication: Authentication, httpServletRequest: HttpServletRequest): Boolean {

        if (authentication.authorities.find { it.authority == ROLE_NAME_ADMIN } != null) return true
        val accountId = httpServletRequest.getParameter(ACCOUNT_ID_PROPERTY) ?: httpServletRequest.getParameter(FROM_ACCOUNT_ID_PROPERTY)
        return verifyAccountIdMatching(authentication, accountId)
    }

    /**
     * Verifies that endpoint with accountId value in request params is accessed by a user with CUSTOMER role which is an
     * owner of the account with given accountId. CUSTOMER only endpoints verification.
     * @param authentication Spring security [Authentication] object
     * @param httpServletRequest servlet request containing accountId in request params
     * @return Boolean value based on security evaluation
     */
    fun verifyWithAccountIdInParamMatchingCustomerOnly(authentication: Authentication, httpServletRequest: HttpServletRequest): Boolean {

        if (authentication.authorities.find { it.authority == ROLE_NAME_CUSTOMER } == null) return false
        val accountId = httpServletRequest.getParameter(ACCOUNT_ID_PROPERTY) ?: httpServletRequest.getParameter(FROM_ACCOUNT_ID_PROPERTY)
        return verifyAccountIdMatching(authentication, accountId)
    }

    /**
     * Helper verification function, verifies that provided account for given [accountId] is owned by authenticated
     * user.
     * @param authentication Spring security [Authentication] object
     * @param accountId account id
     * @return Boolean value based on security evaluation
     */
    private fun verifyAccountIdMatching(authentication: Authentication, accountId: String) : Boolean {
        val customerId = authentication.extractCustomerId(tokenStore)
        try {
            val accountCustomerId = rpc.proxy.startFlow(::GetAccountFlow, UUID.fromString(accountId)).returnValue.getOrThrow()
                    .accountData.customerId
            return customerId == accountCustomerId.toString()
        } catch (e: Exception) {
            logger.warn(e.message)
        }
        return false
    }

    /**
     * Verifies that endpoint with recurringPaymentId value in request params is accessed by a user with ADMIN role or a
     * user with CUSTOMER role which is an owner of the account which is in the accountFrom position of the recurring
     * payment
     * @param authentication Spring security [Authentication] object
     * @param httpServletRequest servlet request containing recurringPaymentId in request params
     * @return Boolean value based on security evaluation
     */
    fun verifyWithRecurringPaymentIdInParamMatching(authentication: Authentication, httpServletRequest: HttpServletRequest): Boolean {

        val recurringPaymentId = httpServletRequest.getParameter(RECURRING_PAYMENT_ID_PROPERTY)
        return verifyWithRecurringPaymentIdInPathMatching(authentication, recurringPaymentId)
    }

    /**
     * Verifies that endpoint with [recurringPaymentId] value in request path is accessed by a user with ADMIN role or a
     * user with CUSTOMER role which is an owner of the account which is in the accountFrom position of the recurring
     * payment
     * @param authentication Spring security [Authentication] object
     * @param recurringPaymentId Recurring payment id
     * @return Boolean value based on security evaluation
     */
    fun verifyWithRecurringPaymentIdInPathMatching(authentication: Authentication, recurringPaymentId: String): Boolean {
        if (authentication.authorities.find { it.authority == ROLE_NAME_ADMIN } != null) return true

        val customerId = authentication.extractCustomerId(tokenStore)
        try {
            val recurringPayment = rpc.proxy.startFlow(::GetRecurringPaymentByIdFlow, UUID.fromString(recurringPaymentId)).returnValue.getOrThrow()
            val accountCustomerId = rpc.proxy.startFlow(::GetAccountFlow, recurringPayment.accountFrom).returnValue
                    .getOrThrow().accountData.customerId
            return customerId == accountCustomerId.toString()
        } catch (e: Exception) {
            logger.warn(e.message)
        }
        return false
    }

    /**
     * Verifies that endpoint with [transactionId] value in request path is accessed by a user with ADMIN role or a
     * user with CUSTOMER role which is an owner of the account which is in the accountFrom or accountTo position of
     * the transaction
     * @param authentication Spring security [Authentication] object
     * @param transactionId transaction id
     * @return Boolean value based on security evaluation
     */
    fun verifyWithTransactionIdInPathMatching(authentication: Authentication, transactionId: String): Boolean {
        if (authentication.authorities.find { it.authority == ROLE_NAME_ADMIN } != null) return true

        val customerId = authentication.extractCustomerId(tokenStore)
        try {
            val transaction = rpc.proxy.startFlow(::GetTransactionByIdFlow, transactionId).returnValue.getOrThrow()
            val accountFromCustomerId = transaction.accountFrom?.let {
                rpc.proxy.startFlow(::GetAccountFlow, it).returnValue.getOrThrow().accountData.customerId
            }
            val accountToCustomerId = transaction.accountTo?.let {
                rpc.proxy.startFlow(::GetAccountFlow, it).returnValue.getOrThrow().accountData.customerId
            }
            return customerId == accountFromCustomerId.toString() || customerId == accountToCustomerId.toString()
        } catch (e: Exception) {
            logger.warn(e.message)
        }
        return false
    }

}