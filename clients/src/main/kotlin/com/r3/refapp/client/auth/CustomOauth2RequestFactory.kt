package com.r3.refapp.client.auth

import com.r3.refapp.client.auth.Constants.GRANT_TYPE_PROPERTY
import com.r3.refapp.client.auth.Constants.REFRESH_TOKEN_GRANT_TYPE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.TokenRequest
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory
import org.springframework.security.oauth2.provider.token.TokenStore

/**
 * CustomOauth2RequestFactory provides refresh token functionality, other request token features are delegated to
 * Spring's [DefaultOAuth2RequestFactory]
 *
 * @param clientDetailsService Autowired, configured clientDetailsService
 */
class CustomOauth2RequestFactory(@Autowired private val clientDetailsService: ClientDetailsService): DefaultOAuth2RequestFactory(clientDetailsService) {

    @Autowired
    lateinit var tokenStore: TokenStore

    @Autowired
    lateinit var userDetailsService: UserDetailsService


    /**
     * Provides refresh token functionality, other request token features are delegated to Spring's
     * [DefaultOAuth2RequestFactory]. In the case when grant_type is set to refresh_token [tokenStore] is queried to
     * find authentication for refresh token.
     *
     * @param requestParameters Token request parameters
     * @param authenticatedClient OAuth2 client (e.g. bank_in_a_box)
     * @return [TokenRequest] object
     */
    override fun createTokenRequest(requestParameters: Map<String, String>, authenticatedClient: ClientDetails): TokenRequest {

        if (requestParameters[GRANT_TYPE_PROPERTY] == REFRESH_TOKEN_GRANT_TYPE) {
            val authentication = tokenStore.readAuthenticationForRefreshToken(tokenStore.readRefreshToken(requestParameters[REFRESH_TOKEN_GRANT_TYPE]))
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(authentication.name, null,
                    userDetailsService.loadUserByUsername(authentication.name).authorities)
        }
        return super.createTokenRequest(requestParameters, authenticatedClient)
    }
}