package com.r3.refapp.client.auth

import com.r3.refapp.client.auth.Constants.PRODUCTION_PROFILE
import com.r3.refapp.client.auth.Constants.ROLE_NAME_ADMIN
import com.r3.refapp.client.auth.Constants.ROLE_NAME_CUSTOMER
import com.r3.refapp.client.auth.Constants.USER_CLIENT_RESOURCE
import com.r3.refapp.client.auth.filters.RevokedRoleFilter
import com.r3.refapp.client.auth.handlers.AccessDeniedEntryPoint
import com.r3.refapp.client.auth.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationCodeGrantFilter
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring's OAuth2 resource server configuration
 */
@Configuration
@EnableResourceServer
@Profile(PRODUCTION_PROFILE)
open class ResourceServerConfiguration : ResourceServerConfigurerAdapter() {

    /**
     * Configurable list of paths accessible by users with GUEST role
     */
    @Value("#{'\${auth.server.guest-paths}'.split(',')}")
    lateinit var guestPaths: List<String>

    /**
     * Configurable list of paths accessible by users with ADMIN role
     */
    @Value("#{'\${auth.server.admin-paths}'.split(',')}")
    lateinit var adminPaths: List<String>

    /**
     * Configurable list of paths accessible only by users with CUSTOMER role and matching accountId request param
     */
    @Value("#{'\${auth.server.customer-only-paths}'.split(',')}")
    lateinit var customerOnlyPaths: List<String>

    /**
     * Configurable list of paths accessible by users with CUSTOMER role and matching customerId path variable
     */
    @Value("#{'\${auth.server.customer-matched-paths}'.split(',')}")
    lateinit var customerMatchedPaths: List<String>

    /**
     * Configurable list of paths accessible by all authenticated users
     */
    @Value("#{'\${auth.server.authenticated-paths}'.split(',')}")
    lateinit var authenticatedPaths: List<String>

    /**
     * Configurable list of paths accessible by users with CUSTOMER role and matching account id path variable
     */
    @Value("#{'\${auth.server.account-matched-paths}'.split(',')}")
    lateinit var accountMatchedPaths: List<String>

    /**
     * Configurable list of paths accessible by users with CUSTOMER role and matching account id request param
     */
    @Value("#{'\${auth.server.account-request-param-matched-paths}'.split(',')}")
    lateinit var accountRequestParamMatchedPaths: List<String>

    /**
     * Configurable list of paths accessible by users with CUSTOMER role and matching recurring payment id  request
     * param
     */
    @Value("#{'\${auth.server.recurring-payment-id-matched-paths}'.split(',')}")
    lateinit var recurringPaymentIdMatchedPaths: List<String>

    /**
     * Configurable list of paths accessible by users with CUSTOMER role and matching recurring payment id path variable
     */
    @Value("#{'\${auth.server.recurring-payment-id-path-matched-paths}'.split(',')}")
    lateinit var recurringPaymentIdPathMatchedPaths: List<String>

    /**
     * Configurable list of paths accessible by users with CUSTOMER role and matching transaction id path variable
     */
    @Value("#{'\${auth.server.transaction-id-matched-paths}'.split(',')}")
    lateinit var transactionIdMatchedPaths: List<String>

    /**
     * Configurable list of websocket paths accessible by users with CUSTOMER role
     */
    @Value("#{'\${auth.server.websocket-paths}'.split(',')}")
    lateinit var webSocketPaths: List<String>

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
     * Configurable list of paths accessible by users with ADMIN role and CUSTOMER role with matching customerId path
     * variable and restricted properties matching
     */
    @Value("#{'\${auth.server.customer-update-paths}'.split(',')}")
    lateinit var customerUpdatePaths: List<String>

    /**
     * Configurable list of paths accessible by users with CUSTOMER and ADMIN role
     */
    @Value("#{'\${auth.server.customer-admin-paths}'.split(',')}")
    lateinit var customerAdminPaths: List<String>


    @Autowired
    lateinit var oAuth2WebSecurityExpressionHandler: OAuth2WebSecurityExpressionHandler

    @Autowired
    lateinit var userRepository: UserRepository

    private val accessDeniedEntryPoint = AccessDeniedEntryPoint()

    @Throws(Exception::class)
    override fun configure(resources: ResourceServerSecurityConfigurer) {
        resources.resourceId(USER_CLIENT_RESOURCE)
        resources.expressionHandler(oAuth2WebSecurityExpressionHandler)
    }

    /**
     * CORS configuration for "Bank in a box" endpoints
     */
    @Bean
    open fun corsConfigurationSource(): CorsConfigurationSource? {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = corsAllowedOrigins
        configuration.allowedMethods = corsAllowedMethods
        configuration.allowedHeaders = corsAllowedHeaders
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration(corsAllowedEndpoints, configuration)
        return source
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.addFilterAfter(RevokedRoleFilter(userRepository), OAuth2AuthorizationCodeGrantFilter::class.java)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().cors().and().authorizeRequests()
                // security config for websocket paths accessible to CUSTOMER
                .mvcMatchers(*webSocketPaths.toTypedArray()).anonymous()
                // security config for paths accessible to CUSTOMER and ADMIN users
                .mvcMatchers(*customerAdminPaths.toTypedArray()).hasAnyAuthority(ROLE_NAME_CUSTOMER, ROLE_NAME_ADMIN)
                // security config for paths accessible to everyone (user registration)
                .mvcMatchers(*guestPaths.toTypedArray()).anonymous()
                // security config for paths accessible to ADMIN and CUSTOMER with matching customerId and restricted
                // properties matching
                .mvcMatchers(*customerUpdatePaths.toTypedArray()).access("@idMatcherVerifier" +
                        ".verifyWithCustomerIdAndPropertiesMatching(authentication,#customerId,request)")
                // security config for paths accessible to CUSTOMER only with matching accountId in request params
                .mvcMatchers(*customerOnlyPaths.toTypedArray()).access("@idMatcherVerifier" +
                        ".verifyWithAccountIdInParamMatchingCustomerOnly(authentication,request)")
                // security config for paths accessible to ADMIN and CUSTOMER with matching customerId
                .mvcMatchers(*customerMatchedPaths.toTypedArray()).access("@idMatcherVerifier" +
                        ".verifyWithCustomerIdMatching(authentication,#customerId)")
                // security config for paths accessible to ADMIN and CUSTOMER with matching accountId in request params
                .mvcMatchers(*accountRequestParamMatchedPaths.toTypedArray()).access("@idMatcherVerifier" +
                        ".verifyWithAccountIdInParamMatching(authentication,request)")
                // security config for paths accessible to ADMIN and CUSTOMER with matching accountId in path variable
                .mvcMatchers(*accountMatchedPaths.toTypedArray()).access("@idMatcherVerifier" +
                        ".verifyWithAccountIdMatching(authentication,#accountId)")
                // security config for paths accessible to ADMIN and CUSTOMER with matching recurringPaymentId in path req params
                .mvcMatchers(*recurringPaymentIdMatchedPaths.toTypedArray()).access("@idMatcherVerifier" +
                        ".verifyWithRecurringPaymentIdInParamMatching(authentication,request)")
                // security config for paths accessible to ADMIN and CUSTOMER with matching recurringPaymentId in path variable
                .mvcMatchers(*recurringPaymentIdPathMatchedPaths.toTypedArray()).access("@idMatcherVerifier" +
                        ".verifyWithRecurringPaymentIdInPathMatching(authentication,#recurringPaymentId)")
                // security config for paths accessible to ADMIN and CUSTOMER with matching transactionId in path variable
                .mvcMatchers(*transactionIdMatchedPaths.toTypedArray()).access("@idMatcherVerifier" +
                        ".verifyWithTransactionIdInPathMatching(authentication,#transactionId)")
                // security config for paths accessible to only ADMIN users
                .mvcMatchers(*adminPaths.toTypedArray()).hasAuthority(ROLE_NAME_ADMIN)
                // security config for paths accessible to all authenticated (ADMIN, CUSTOMER, GUEST) users
                .mvcMatchers(*authenticatedPaths.toTypedArray()).authenticated()
                .and().exceptionHandling().authenticationEntryPoint(accessDeniedEntryPoint)
    }
}