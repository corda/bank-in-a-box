package com.r3.refapp.client.auth

import com.r3.refapp.client.response.MessageType

object Constants {

    /**
     * PermitAll spring security method constant, used in tokenKeyAccess
     */
    const val PERMIT_ALL_ACCESS = "permitAll()"

    /**
     * IsAuthenticated spring security method constant, used in checkTokenAccess
     */
    const val AUTHENTICATED_ACCESS = "isAuthenticated()"

    /**
     * OAuth2 grant_type property constant
     */
    const val GRANT_TYPE_PROPERTY = "grant_type"

    /**
     * OAuth2 grant_type property value of refresh_token
     */
    const val REFRESH_TOKEN_GRANT_TYPE = "refresh_token"

    /**
     * BankInABox pre configured role GUEST
     */
    const val ROLE_NAME_GUEST = "GUEST"

    /**
     * BankInABox pre configured role CUSTOMER
     */
    const val ROLE_NAME_CUSTOMER = "CUSTOMER"

    /**
     * BankInABox pre configured role ADMIN
     */
    const val ROLE_NAME_ADMIN = "ADMIN"

    /**
     * Request param / path property name for customer id
     */
    const val CUSTOMER_ID_PROPERTY = "customerId"

    /**
     * Request param / path property name for account id
     */
    const val ACCOUNT_ID_PROPERTY = "accountId"

    /**
     * Request param / path property name for from account id
     */
    const val FROM_ACCOUNT_ID_PROPERTY = "fromAccountId"

    /**
     * Request param / path property name for recurring payment id
     */
    const val RECURRING_PAYMENT_ID_PROPERTY = "recurringPaymentId"

    /**
     * Request param / path property name for customer name
     */
    const val CUSTOMER_NAME_PROPERTY = "customerName"

    /**
     * Request param / path property name for post code
     */
    const val POST_CODE_PROPERTY = "postCode"

    /**
     * OAuth2 user client resource constant
     */
    const val USER_CLIENT_RESOURCE = "USER_CLIENT_RESOURCE"

    /**
     * Username property
     */
    const val USERNAME_PROPERTY = "username"

    /**
     * Production Spring profile constant, used to load OAuth2 authentication beans
     */
    const val PRODUCTION_PROFILE = "prod"

    /**
     * Property name in [MessageType] template
     */
    const val PROPERTY_NAME = "propertyName"

    /**
     * Property value in [MessageType] template
     */
    const val PROPERTY_VALUE = "propertyValue"

    /**
     * Placeholder prefix in [MessageType] template
     */
    const val PLACEHOLDER_PREFIX = "\${"

    /**
     * Placeholder suffix in [MessageType] template
     */
    const val PLACEHOLDER_SUFFIX = "}"
}