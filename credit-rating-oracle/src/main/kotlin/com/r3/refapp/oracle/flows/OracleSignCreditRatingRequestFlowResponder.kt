package com.r3.refapp.oracle.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.contracts.FinancialAccountContract.Commands.VerifyCreditRating
import com.r3.refapp.flows.GetCustomerCreditRatingFlow
import com.r3.refapp.flows.internal.OracleSignCreditRatingRequestFlow
import com.r3.refapp.oracle.exception.CreditRatingOracleException
import net.corda.core.contracts.Command
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.utilities.unwrap
import java.time.Instant


/**
 * Receives, validates and signs a VerifyCreditRating command
 * @param otherSession the flow session that needs the credit rating validation
 * @return [Unit]
 */
@InitiatedBy(OracleSignCreditRatingRequestFlow::class)
class OracleSignCreditRatingRequestFlowResponder(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val filteredLoanIssuanceTxt= otherSession.receive<FilteredTransaction>().unwrap { it }

        // verifies if only credit rating command information is visible and if it has the appropriate signer
        require(filteredLoanIssuanceTxt.checkWithFun(::verifyTransaction))

        val creditRatingCmd = filteredLoanIssuanceTxt.commands.single().value as VerifyCreditRating
        validateCreditRatingCommand(creditRatingCmd)

        // creating transaction signature
        val signatureMetadata = SignatureMetadata(serviceHub.myInfo.platformVersion, Crypto.findSignatureScheme(ourIdentity.owningKey).schemeNumberID)
        val signableData = SignableData(filteredLoanIssuanceTxt.id, signatureMetadata)
        val signature = serviceHub.keyManagementService.sign(signableData, ourIdentity.owningKey)
        otherSession.send(signature)
    }

    /**
     * Validates a VerifyCreditRating command by doing the following:
     * - checks if it's not expired
     * - queries the current credit rating from the webservice and does the necessary comparision checks
     * @param creditRatingCmd the commands that is validated
     * @throws [CreditRatingOracleException] if the validation fails
     * @return [Unit]
     */
    @Suspendable
    private fun validateCreditRatingCommand(creditRatingCmd: VerifyCreditRating) {
        // check if we are still in the valid period
        val currentTime = Instant.now()
        if (currentTime < creditRatingCmd.dateStart || currentTime > creditRatingCmd.dateStart.plus(creditRatingCmd.validityPeriod)) {
            throw CreditRatingOracleException("CreditRating command expired")
        }

        val customerId = creditRatingCmd.creditRatingInfo.customerId
        // query latest credit rating information from web service
        val latestCreditRatingInfo = subFlow(GetCustomerCreditRatingFlow(customerId))

        // compare the command credit rating with the queried credit rating
        if (creditRatingCmd.creditRatingInfo.customerId != latestCreditRatingInfo.customerId)
            throw CreditRatingOracleException("Customer id is not the same for the two credit ratings")
        if (creditRatingCmd.creditRatingInfo.rating != latestCreditRatingInfo.rating)
            throw CreditRatingOracleException("Command credit rating and current credit rating are not equal: ${creditRatingCmd.creditRatingInfo.rating} != ${latestCreditRatingInfo.rating} ")

    }

    /**
     * Verifies if only command type elements are visible
     * @param elem transaction element
     * @throws [CreditRatingOracleException] if verification fails
     * @return [Boolean] true if verification is successful
     */
    @Suspendable
    private fun verifyTransaction(elem: Any): Boolean {
        return when (elem) {
            is Command<*> -> commandFormatValidator(elem)
            else -> throw CreditRatingOracleException("Oracle received data of different type than expected.")
        }
    }

    /**
     * Verifies the received command format
     * - command is a CreditRatingCmd
     * - self is in the required signers list
     */
    @Suspendable
    private fun commandFormatValidator(elem: Command<*>): Boolean {
        if (ourIdentity.owningKey !in elem.signers) throw CreditRatingOracleException("Oracle is not in signers list")
        if (elem.value !is VerifyCreditRating) throw CreditRatingOracleException("Unknown command received. Command ${VerifyCreditRating::class.qualifiedName} was expected")
        return true
    }
}
