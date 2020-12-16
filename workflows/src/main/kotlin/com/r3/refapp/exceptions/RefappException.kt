package com.r3.refapp.exceptions

import net.corda.core.flows.FlowException

/**
 * Exception thrown if any issues are encountered within this app's domain.
 * @property exceptionMessage description of the failure
 * @property causeException the originating exception instance
 */
class RefappException(private val exceptionMessage: String,
                      private val causeException: Throwable? = null) : FlowException("Refapp exception: $exceptionMessage", causeException)