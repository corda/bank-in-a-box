package com.r3.refapp.client.auth

import com.r3.refapp.client.auth.Constants.CUSTOMER_ID_PROPERTY
import com.r3.refapp.client.auth.entity.User
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter

/**
 * Adds additional properties to standard [OAuth2AccessToken].
 */
class CustomTokenEnhancer: JwtAccessTokenConverter() {

    /**
     * Adds customerId property (if present) to [OAuth2AccessToken].
     * @param accessToken OAuth2AccessToken object
     * @param authentication OAuth2Authentication object
     * @return Returns enhanced [OAuth2AccessToken]
     */
    override fun enhance(accessToken: OAuth2AccessToken, authentication: OAuth2Authentication): OAuth2AccessToken {

        val customAccessToken = DefaultOAuth2AccessToken(accessToken)
        val principal = authentication.userAuthentication.principal
        principal as User
        customAccessToken.additionalInformation[CUSTOMER_ID_PROPERTY] = principal.customerId
        return super.enhance(customAccessToken, authentication)
    }
}