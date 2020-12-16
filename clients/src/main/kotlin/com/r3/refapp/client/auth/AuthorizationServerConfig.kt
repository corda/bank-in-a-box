package com.r3.refapp.client.auth

import com.r3.refapp.client.auth.Constants.AUTHENTICATED_ACCESS
import com.r3.refapp.client.auth.Constants.PERMIT_ALL_ACCESS
import com.r3.refapp.client.auth.Constants.PRODUCTION_PROFILE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.OAuth2RequestFactory
import org.springframework.security.oauth2.provider.endpoint.TokenEndpointAuthenticationFilter
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import javax.sql.DataSource


/**
 * Authorization server configuration, configures all Spring's OAuth2Server beans.
 *
 */
@Configuration
@Profile(PRODUCTION_PROFILE)
open class AuthorizationServerConfig : AuthorizationServerConfigurerAdapter() {

    /**
     * Configurable parameter used for OAuth2 token scope verification
     */
    @Value("\${auth.server.check-user-scopes}")
    private var checkUserScopes: Boolean = false

    /**
     * Configurable jks keystore path used for JWT token signing
     */
    @Value("\${auth.server.key-store-path}")
    lateinit var keyStorePath: String

    /**
     * Configurable jks keystore pass
     */
    @Value("\${auth.server.key-store-pass}")
    lateinit var keyStorePass: String

    /**
     * Configurable jks keystore alias
     */
    @Value("\${auth.server.key-store-alias}")
    lateinit var keyStoreAlias: String

    /**
     * Configurable CORS allowed endpoints
     */
    @Value("\${auth.server.cors.allowed-endpoints}")
    lateinit var corsAllowedEndpoints: String

    /**
     * Configurable CORS allowed origins
     */
    @Value("\${auth.server.cors.allowed-origins}")
    lateinit var corsAllowedOrigins: List<String>

    /**
     * Configurable CORS allowed methods
     */
    @Value("\${auth.server.cors.allowed-methods}")
    lateinit var corsAllowedMethods: List<String>

    /**
     * Configurable CORS allowed headers
     */
    @Value("\${auth.server.cors.allowed-headers}")
    lateinit var corsAllowedHeaders: List<String>

    /**
     * Configured [PasswordEncoder] which will be used in [ClientDetailsService] configuration
     * ([BCryptPasswordEncoder] in this case)
     */
    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    /**
     * Configured [DataSource] which will be used in [ClientDetailsService] configuration
     */
    @Autowired
    lateinit var dataSource: DataSource

    /**
     * Configured [UserDetailsService] which will be used in [AuthenticationManager] configuration
     */
    @Autowired
    lateinit var  userDetailsService: UserDetailsService

    /**
     * Configured [ClientDetailsService] which will be used in [OAuth2RequestFactory] configuration
     */
    @Autowired
    lateinit var clientDetailsService: ClientDetailsService

    /**
     * Configured [AuthenticationManager] which will be used in [AuthorizationServerEndpointsConfigurer] configuration
     */
    @Autowired
    lateinit var authenticationManager: AuthenticationManager

    /**
     * OAuth2RequestFactory bean configuration, provides [CustomOauth2RequestFactory] with token store configuration.
     */
    @Bean
    open fun requestFactory(): OAuth2RequestFactory {
        val requestFactory = CustomOauth2RequestFactory(clientDetailsService)
        requestFactory.setCheckUserScopes(checkUserScopes)
        return requestFactory
    }

    /**
     * TokenStore bean configuration, provides [JwtTokenStore] with [JwtAccessTokenConverter] in order to enable JWT
     * tokens.
     */
    @Bean
    open fun tokenStore(): TokenStore {
        return JwtTokenStore(jwtAccessTokenConverter())
    }

    /**
     * JwtAccessTokenConverter bean configuration, uses configured jks to sign JWT tokens. Configured [CustomTokenEnhancer]
     * is used to add customerId information to default token.
     */
    @Bean
    open fun jwtAccessTokenConverter(): JwtAccessTokenConverter {
        val converter = CustomTokenEnhancer()
        converter.setKeyPair(KeyStoreKeyFactory(ClassPathResource(keyStorePath), keyStorePass.toCharArray()).getKeyPair(keyStoreAlias))
        return converter
    }

    /**
     * [ClientDetailsServiceConfigurer] configuration, uses configured JDBC datasource (h2 or postgres based on the
     * application.yml config) as client details storage, [BCryptPasswordEncoder] is configured for password hashing.
     */
    @Throws(Exception::class)
    override fun configure(clients: ClientDetailsServiceConfigurer) {
        clients.jdbc(dataSource).passwordEncoder(passwordEncoder)
    }

    /**
     * [TokenEndpointAuthenticationFilter] configuration, [authenticationManager] and
     * [CustomOauth2RequestFactory] are configured as authentication manager and request factory.
     */
    @Bean
    open fun tokenEndpointAuthenticationFilter(): TokenEndpointAuthenticationFilter {
        return TokenEndpointAuthenticationFilter(authenticationManager, requestFactory())
    }

    /**
     * OAuth2Server access configuration, OAuth2Server CORS configuration.
     */
    @Throws(Exception::class)
    override fun configure(oauthServer: AuthorizationServerSecurityConfigurer) {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowedOrigins = corsAllowedOrigins
        config.allowedMethods = corsAllowedMethods
        config.allowedHeaders = corsAllowedHeaders
        source.registerCorsConfiguration(corsAllowedEndpoints, config)
        val filter = CorsFilter(source)
        oauthServer.addTokenEndpointAuthenticationFilter(filter)
        oauthServer.tokenKeyAccess(PERMIT_ALL_ACCESS).checkTokenAccess(AUTHENTICATED_ACCESS)
    }

    /**
     * AuthorizationServerEndpoints configuration, JWT token enhancer, DB [UserDetailsService] and [AuthenticationManager]
     * are configured
     */
    @Throws(Exception::class)
    override fun configure(endpoints: AuthorizationServerEndpointsConfigurer) {
        endpoints.tokenStore(tokenStore()).tokenEnhancer(jwtAccessTokenConverter()).authenticationManager(authenticationManager).userDetailsService(userDetailsService)
        if (checkUserScopes)
            endpoints.requestFactory(requestFactory())
    }

}