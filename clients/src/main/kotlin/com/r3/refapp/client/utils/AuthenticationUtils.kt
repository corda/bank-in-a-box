package com.r3.refapp.client.utils

import com.r3.refapp.client.auth.Constants
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails
import org.springframework.security.oauth2.provider.token.TokenStore

/**
 * Extracts customer id from the [Authentication] with the CUSTOMER role.
 * @param tokenStore Spring security [TokenStore] object
 * @return Customer id from the OAuth2 token
 */
fun Authentication.extractCustomerId(tokenStore: TokenStore): String? {
    val authenticationDetails = this.details
    return if (authenticationDetails is OAuth2AuthenticationDetails && this.authorities.find { it.authority == Constants
                    .ROLE_NAME_CUSTOMER } != null ) {
        val token = tokenStore.readAccessToken(authenticationDetails.tokenValue)
        return token.additionalInformation[Constants.CUSTOMER_ID_PROPERTY].toString()
    } else null
}