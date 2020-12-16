package com.r3.refapp.client.auth

import com.r3.refapp.client.auth.Constants.PRODUCTION_PROFILE
import com.r3.refapp.client.auth.service.DaoUserDetailsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler
import org.springframework.security.web.csrf.CookieCsrfTokenRepository


/** Web security configuration */
@Profile(PRODUCTION_PROFILE)
@Configuration
@EnableAuthorizationServer
@EnableResourceServer
open class EndpointWebSecurity : WebSecurityConfigurerAdapter() {

    @Autowired
    lateinit var userDetailsService: UserDetailsService

    /**
     * DaoAuthenticationProvider bean configuration.
     */
    @Bean
    open fun authenticationProvider(): DaoAuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setPasswordEncoder(passwordEncoder())
        provider.setUserDetailsService(userDetailsService)
        return provider
    }

    /**
     * BCryptPasswordEncoder bean configuration.
     */
    @Bean
    open fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    /**
     * AuthenticationManager bean configuration.
     */
    @Bean
    @Throws(Exception::class)
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    /**
     * AuthenticationManagerBuilder bean configuration with [DaoUserDetailsService] and [BCryptPasswordEncoder].
     */
    @Throws(Exception::class)
    @Autowired
    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder())
    }

    /**
     * HttpSecurity configuration.
     */
    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.exceptionHandling()
                .and().csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    }

    /**
     * OAuth2WebSecurityExpressionHandler bean configuration.
     */
    @Bean
    open fun oAuth2WebSecurityExpressionHandler(applicationContext: ApplicationContext): OAuth2WebSecurityExpressionHandler {
        val expressionHandler = OAuth2WebSecurityExpressionHandler()
        expressionHandler.setApplicationContext(applicationContext)
        return expressionHandler
    }
}