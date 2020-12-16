package com.r3.refapp.client.auth

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

/** NoAuth Web security configuration */
@Profile("noauth")
@Configuration
open class NoSecWebSecurity: WebSecurityConfigurerAdapter() {
    /**
     * Method used to implement the NO Auth security config.
     *  - permit requests to all urls
     *  - disable csrf defense mechanism (for POST requests)
     * @param http http security object
     */
    override fun configure(http: HttpSecurity) {
        http.authorizeRequests().antMatchers("/**").permitAll().and()
            .csrf().disable()
    }
}