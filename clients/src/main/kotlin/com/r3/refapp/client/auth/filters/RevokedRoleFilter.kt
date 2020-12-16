package com.r3.refapp.client.auth.filters

import com.r3.refapp.client.auth.repository.UserRepository
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.web.filter.GenericFilterBean
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

/**
 * Custom Spring Security filter used to filter incoming requests of the users which had roles revoked recently. OAuth2
 * tokens are issued with certain pre-configured expiration time, in order to protect endpoints from users accessing
 * them in period after role has been revoked but before token has expired [RevokedRoleFilter] will be used. Standard
 * OAuth2 authentication is performed in filters before but [RevokedRoleFilter] further filters access for users with
 * revoked roles.
 */
class RevokedRoleFilter(private val userRepository: UserRepository): GenericFilterBean() {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {

        val authentication = SecurityContextHolder.getContext().authentication
        if(authentication is OAuth2Authentication) {
            val user = userRepository.findByUsername(authentication.principal.toString())
            val roleNames = user.roles.map { it.name }
            val updatedAuthorities = authentication.authorities.filter { roleNames.contains(it.authority) }

            val updatedAuth = UsernamePasswordAuthenticationToken(authentication.principal, authentication.credentials,
                    updatedAuthorities)
            val oAuth2Auth = OAuth2Authentication(authentication.oAuth2Request, updatedAuth)
            oAuth2Auth.details = authentication.details
            SecurityContextHolder.getContext().authentication = oAuth2Auth
        }

        chain.doFilter(request, response)
    }
}