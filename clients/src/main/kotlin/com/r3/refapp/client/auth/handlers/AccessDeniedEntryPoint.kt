package com.r3.refapp.client.auth.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import com.r3.refapp.client.response.ErrorMessageResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Access denied/Unauthorized entry point
 */
class AccessDeniedEntryPoint: AuthenticationEntryPoint {
    /**
     * Constructs access denied response consisting of error message and 401 Unauthorized status
     * @param request that triggered the unauthorized access
     * @param response http response object
     * @param authException exception that triggered the unauthorized access
     */
    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        response.contentType = "application/json";
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        if (authException.message != null) {
            val mapper = ObjectMapper()
            response.outputStream.println(mapper.writeValueAsString(ErrorMessageResponse(authException.message!!)))
        }
    }
}