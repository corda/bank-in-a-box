package com.r3.refapp.utils

import com.r3.refapp.exceptions.RefappException
import net.corda.core.cordapp.CordappConfigException
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import java.time.Duration

/**
 * Centralised configuration utilities for this app.
 */
object ConfigurationUtils {

    const val NOTARY_NAME_PROPERTY = "refapp_notary"
    const val CREDIT_RATING_HOST_PORT = "credit_rating_host_port"
    const val CREDIT_RATING_THRESHOLD = "credit_rating_threshold"
    const val LOAN_REPAYMENT_PERIOD = "loan_repayment_period"
    const val EXECPROV_THREAD_NUM = "execprov_thread_num"
    const val ORACLE_NAME = "oracle_party"
    const val CREDIT_RATING_VALIDITY_DURATION_HOURS = "credit_rating_validity_duration_hours"

    /**
     * Retrieves configured Notary [Party] for refapp from [ServiceHub].
     *
     * @param serviceHub the [ServiceHub]
     * @return returns configured Notary [Party]
     * @throws [RefappException] if configured Notary cannot be found in [ServiceHub],
     * if property cannot be found in config or if property value is malformed [CordaX500Name]
     */
    fun getConfiguredNotary(serviceHub: ServiceHub): Party {
        val notaryName =  getConfigProperty(serviceHub, NOTARY_NAME_PROPERTY)
        try {
            serviceHub.networkMapCache.getNotary(CordaX500Name.parse(notaryName))?.let {
                return it
            }
        } catch(e: Exception) {
            when(e) {
                is IllegalArgumentException ->
                    throw RefappException("Malformed CordaX500Name: $notaryName in config property: $NOTARY_NAME_PROPERTY. Please check your configuration!")
            }
        }
        throw RefappException("Notary not found for config property: $NOTARY_NAME_PROPERTY with value $notaryName. Please check your configuration!")
    }

    /**
     * Retrieves configured Oracle [Party] for refapp from [ServiceHub].
     *
     * @param serviceHub the [ServiceHub]
     * @return returns configured Oracle [Party]
     * @throws [RefappException] if configured Oracle cannot be found in [ServiceHub],
     * if property cannot be found in config or if property value is malformed [CordaX500Name]
     */
    fun getConfiguredOracle(serviceHub: ServiceHub): Party {
        val oracleName = getConfigProperty(serviceHub, ORACLE_NAME)
        try {
            serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse(oracleName))?.let {
                return it
            }
        } catch(e: Exception) {
            when(e) {
                is IllegalArgumentException ->
                    throw RefappException("Malformed CordaX500Name: $oracleName in config property: $ORACLE_NAME. Please check your configuration!")
            }
        }
        throw RefappException("Oracle not found for config property: ${ORACLE_NAME} with value $oracleName. Please check your configuration!")
    }

    /**
     * Retrieves configured credit rating webservice address (host and port) [String] for refapp from [ServiceHub].
     * @param serviceHub the [ServiceHub]
     * @return returns configured address (host and port) [String]
     * @throws [RefappException] if configured address cannot be found in [ServiceHub],
     */
    fun getCreditRatingWebAddr(serviceHub: ServiceHub): String {
        return getConfigProperty(serviceHub, CREDIT_RATING_HOST_PORT)
    }

    /**
     * Retrieves configured credit rating threshold [String] for refapp from [ServiceHub].
     * @param serviceHub the [ServiceHub]
     * @return returns configured credit rating threshold [String]
     * @throws [RefappException] if configured property cannot be found in [ServiceHub], or is not a valid representation of a number
     */
    fun getCreditRatingThreshold(serviceHub: ServiceHub): Int {
        return getConfigPropertyInt(serviceHub, CREDIT_RATING_THRESHOLD)
    }
    /**
     * Retrieves configured credit rating validity time [Duration] for refapp from [ServiceHub].
     * This time specifies for how long should a queried credit rating be valid and is expressed in hours
     * @param serviceHub the [ServiceHub]
     * @return returns credit rating validity time [String]
     * @throws [RefappException] if configured property cannot be found in [ServiceHub], or is not a valid representation of a number
     */
    fun getCreditRatingValidityDuration(serviceHub: ServiceHub): Duration {
        return Duration.ofHours(getConfigPropertyInt(serviceHub, CREDIT_RATING_VALIDITY_DURATION_HOURS).toLong())
    }

    /**
     * Retrieves configured loan repayment period in [Duration] format for refapp from [ServiceHub].
     * @param serviceHub the [ServiceHub]
     * @return returns configured loan repayment period in minutes [Int]
     * @throws [RefappException] if configured property cannot be found in [ServiceHub], or is not a valid
     * representation of a [Duration]
     */
    fun getLoanRepaymentPeriod(serviceHub: ServiceHub): Duration {
        val durationString = getConfigProperty(serviceHub, LOAN_REPAYMENT_PERIOD)
        return Duration.parse(durationString)
    }

    /**
     * Retrieves configured execution provider thread pool size [String] for refapp from [ServiceHub].
     * @param serviceHub the [ServiceHub]
     * @return returns configured thread pool size [String]
     * @throws [RefappException] if configured thread pool size cannot be found in [ServiceHub], or is not a valid representation of a number
     */
    fun getExecProvThreadNum(serviceHub: ServiceHub): Int {
        return getConfigPropertyInt(serviceHub, EXECPROV_THREAD_NUM)
    }

    /**
     * Retrieves configured property [String] for refapp from [ServiceHub].
     * @param serviceHub the [ServiceHub]
     * @param property name to retrieve [ServiceHub]
     * @return returns configured property [String]
     * @throws [RefappException] if configured property cannot be found in [ServiceHub],
     */
    fun getConfigProperty(serviceHub: ServiceHub, property: String): String {
        try {
            return serviceHub.getAppContext().config.getString(property)
        } catch(e: CordappConfigException) {
            throw RefappException("Missing required configuration property: $property. Please check your configuration!")
        }
    }

    /**
     * Retrieves configured property [Int] for refapp from [ServiceHub].
     * @param serviceHub the [ServiceHub]
     * @param property name to retrieve [ServiceHub]
     * @return returns configured property [Int]
     * @throws [RefappException] if configured property cannot be found in [ServiceHub], or is not a valid representation of a number
     */
    fun getConfigPropertyInt(serviceHub: ServiceHub, property: String): Int {
        try {
            return getConfigProperty(serviceHub, property).toInt()
        } catch (e: NumberFormatException) {
            throw RefappException("Configuration property: $property cannot be converted to Int. Please check your configuration!")
        }
    }
}