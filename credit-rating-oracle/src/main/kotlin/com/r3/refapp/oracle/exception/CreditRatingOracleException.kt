package com.r3.refapp.oracle.exception

import net.corda.core.flows.FlowException

/**
 * Exception which can be thrown by CreditRatingOracle
 * @param exceptionMessage reason for the exception
 * @param causeException used for exception chaining
 */
class CreditRatingOracleException(private val exceptionMessage: String,
                                  private val causeException: Throwable? = null) : FlowException("Oracle exception: $exceptionMessage", causeException)