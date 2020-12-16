package com.r3.refapp.client.controllers

import net.corda.core.CordaException
import net.corda.core.CordaRuntimeException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.Instant
import javax.servlet.http.HttpServletRequest

/**
 * ApiError data class which is used to map Bank in a box exceptions to a standard error format which will be returned
 * to
 * clients.
 * @param status Spring's [HttpStatus] object
 * @param statusCode Standard HTTP status code
 * @param message Human readable error message
 * @param path Endpoint's path
 * @param timestamp Timestamp when error has occurred
 */
data class ApiError(val status: HttpStatus, val statusCode: Int, val message: String,
                    val path: String, val timestamp: Instant = Instant.now())

/**
 * Spring's [ResponseEntityExceptionHandler] controller advice. All errors returned from controllers will be
 * intercepted by [ApiErrorHandler] implementation and handled by handler functions.
 */
@ControllerAdvice
class ApiErrorHandler: ResponseEntityExceptionHandler() {

    /**
     * Default handler which handles all exception types except the ones handled by more specific handlers.
     * @param ex Exception thrown by the controller
     * @param httpRequest Standard servlet [HttpServletRequest] object
     * @return Spring's [ResponseEntity] with bank in a box specific [ApiError] object
     */
    @ExceptionHandler(value = [Exception::class])
    fun defaultHandle(ex: Exception, httpRequest: HttpServletRequest): ResponseEntity<ApiError> {
        val apiError = ApiError(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error occurred, please contact your administrator!", httpRequest.requestURI)
        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }

    /**
     * Bad request handler which handles [CordaException], [CordaRuntimeException] and [IllegalArgumentException] and all of their
     * subclasses.
     * @param ex Exception thrown by the controller
     * @param httpRequest Standard servlet [HttpServletRequest] object
     * @return Spring's [ResponseEntity] with bank in a box specific [ApiError] object
     */
    @ExceptionHandler(value = [CordaException::class, CordaRuntimeException::class, IllegalArgumentException::class])
    fun handleBadRequest(ex: Exception, httpRequest: HttpServletRequest):
            ResponseEntity<ApiError> {
        return when (ex) {
            is CordaException -> getBadRequestResponseEntity(ex.originalMessage!!, httpRequest)
            is CordaRuntimeException -> getBadRequestResponseEntity(ex.originalMessage!!, httpRequest)
            is IllegalArgumentException -> getBadRequestResponseEntity(ex.message!!, httpRequest)
            else -> defaultHandle(ex, httpRequest)
        }
    }

    /**
     * Helper function used to create BAD_REQUEST [ResponseEntity] with [ApiError] object.
     * @param message Error message
     * @param httpRequest Standard servlet [HttpServletRequest] object
     * @return Spring's [ResponseEntity] with bank in a box specific [ApiError] object
     */
    private fun getBadRequestResponseEntity(message: String, httpRequest: HttpServletRequest) :
            ResponseEntity<ApiError> {
        val apiError = ApiError(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value(),
                message, httpRequest.requestURI)
        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }
}