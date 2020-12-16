package com.r3.refapp.client.auth.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import com.r3.refapp.client.response.ErrorMessageResponse
import com.r3.refapp.client.response.MessageResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Successful logout handler
 */
class EndpointLogoutSuccessHandler : LogoutSuccessHandler {
    /**
     * Creates a simple logout response
     */
    override fun onLogoutSuccess(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication) {
        response.contentType = "application/json";
        response.status = HttpServletResponse.SC_OK
        val mapper = ObjectMapper()
        response.outputStream.println(mapper.writeValueAsString(MessageResponse("Logout successful")))
    }

}
