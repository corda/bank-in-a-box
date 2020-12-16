package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.refapp.contracts.FinancialAccountContract
import com.r3.refapp.contracts.FinancialAccountContract.Commands.VerifyCreditRating
import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.flows.internal.OracleSignCreditRatingRequestFlow
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.states.*
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.TimeWindow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.function.Predicate

/**
 * Issue loan to an account in the following steps:
 *  - deposit the loan amount to the current account
 *  - check the customer credit rating against a threshold
 *  - the customer credit rating is validated by the oracle
 *  - create a new loan account which has loan amount balance and has the same customer as the current account
 *  - create a recurring payment from the current account to the loan account
 * @param accountId the beneficiary account's ID. The loan amount will be transferred to this account
 * @param loan the amount of fiat currency to loan
 * @param periodInUnits loan term in configured time units
 * @return [SignedTransaction] created SignedTransaction on the ledger
 */
@StartableByRPC
class IssueLoanFlow(val accountId: UUID, val loan: Amount<Currency>, val periodInUnits: Int)
    : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val oracle: Party = ConfigurationUtils.getConfiguredOracle(serviceHub)
        val accountRepository: AccountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val accountStateAndRef = accountRepository.getCurrentAccountStateById(accountId)
        val repaymentAccountIn = accountStateAndRef.state.data

        val creditRatingValidityDuration = ConfigurationUtils.getCreditRatingValidityDuration(serviceHub)
        // loan repayments are executed every 30 days by default, value can be overriden in configuration
        val paymentPeriod = ConfigurationUtils.getLoanRepaymentPeriod(serviceHub)
        // prepare necessary commands
        val commandCreditRating = prepareVerifyCreditRatingCommand(repaymentAccountIn.accountData.customerId, oracle, creditRatingValidityDuration)
        val commandIssueLoan = Command(FinancialAccountContract.Commands.IssueLoan(repaymentAccountIn.accountData.accountInfo.identifier, loan), ourIdentity.owningKey)

        // add funds to the beneficiary's account
        val repaymentAccountOut = repaymentAccountIn.deposit(loan)
        // create a new loan account
        val loanAccountOut = createLoanAccount(repaymentAccountIn.accountData.customerId, loan)

        val notary = ConfigurationUtils.getConfiguredNotary(serviceHub)

        val txBuilder = TransactionBuilder(notary)
                .addInputState(accountStateAndRef)
                .addOutputState(repaymentAccountOut)
                .addOutputState(loanAccountOut)
                .addCommand(commandIssueLoan)
                .addCommand(commandCreditRating)
                .setTimeWindow(TimeWindow.between(commandCreditRating.value.dateStart, commandCreditRating.value.dateStart.plus(creditRatingValidityDuration)))
        txBuilder.verify(serviceHub)

        // add own signature to the transaction
        val partStx = serviceHub.signInitialTransaction(txBuilder)

        // hide unnecessary information form the oracle, leave only credit rating command information
        val mtx = partStx.buildFilteredTransaction(Predicate { creditRatingCmdFilterForOracle(it, oracle) })
        // get the oracle's signature
        val txtSignature = subFlow(OracleSignCreditRatingRequestFlow(txBuilder, oracle, mtx))
        val oracleSignedTxt = partStx.withAdditionalSignature(txtSignature)

        val tx =  subFlow(FinalityFlow(oracleSignedTxt, emptyList()))

        // reoccurring payment is created after the notary signed the transaction
        createLoanRecurringRepayment(accountId, loanAccountOut, periodInUnits, paymentPeriod)

        return tx
    }

    /**
     * Prepares a VerifyCreditRating command by querying the credit rating information from a webservice
     * @param customerId customer's id
     * @param oracle party that validates the credit rating command
     * @param creditRatingValidityDuration the command will be valid for this period of time
     * @throws [RefappException] if the queried credit rating does not reach the configured threshold
     * @return [VerifyCreditRating]
     */
    @Suspendable
    private fun prepareVerifyCreditRatingCommand(customerId: UUID, oracle: Party, creditRatingValidityDuration: Duration): Command<VerifyCreditRating> {
        // query credit rating information from web server
        val creditRatingInfo = subFlow(GetCustomerCreditRatingFlow(customerId))
        val creditRatingThreshold = ConfigurationUtils.getCreditRatingThreshold(serviceHub)

        // verifies if the credit rating fulfills the minimum conditions to receive a loan
        verifyCreditRatingInfo(creditRatingInfo, creditRatingThreshold )
        return Command(VerifyCreditRating(creditRatingInfo, creditRatingThreshold, oracle.owningKey, Instant.now(), creditRatingValidityDuration), listOf(ourIdentity.owningKey, oracle.owningKey))
    }

    /**
     * Only exposes CreditRating commands in which the oracle is on the list of requested signers, to avoid leaking privacy
     */
    @Suspendable
    private fun creditRatingCmdFilterForOracle(elem: Any, oracle: Party): Boolean {
        return when (elem) {
            is Command<*> -> oracle.owningKey in elem.signers && elem.value is VerifyCreditRating
            else -> false
        }
    }

    /**
     * Verifies if the credit rating fulfills the minimum condition to receive a loan
     * @param creditRatingInfo as [CreditRatingInfo]
     * @throws [RefappException]
     */
    @Suspendable
    private fun verifyCreditRatingInfo(creditRatingInfo: CreditRatingInfo, creditRatingThreshold: Int) {
        if (creditRatingInfo.rating < creditRatingThreshold) throw RefappException("Credit rating for ${creditRatingInfo.customerId} customer not enough for receiving a loan")
    }

    /**
     * Create a monthly recurring payment from a current account to a loan account
     * A single payment amount is calculated by dividing the loan balance with periodInMonths
     * @param accountId from which the payment is initialized
     * @param loanAccount beneficiary of the payment
     * @param periodInUnits loan repayment period in units
     * @param paymentPeriod configured payment period in hours
     */
    @Suspendable
    private fun createLoanRecurringRepayment(accountId: UUID, loanAccount: LoanAccountState,
                                             periodInUnits: Int, paymentPeriod: Duration) {

        val periodQuantity = (loanAccount.accountData.balance.quantity + periodInUnits - 1).div(periodInUnits)
        val periodPayment = Amount(periodQuantity, loanAccount.accountData.balance.displayTokenSize, loanAccount
                .accountData.balance.token)

        subFlow(CreateRecurringPaymentFlow(accountId, loanAccount.accountData.accountId, periodPayment, Instant.now().plus(paymentPeriod),
                paymentPeriod, periodInUnits))
    }
    /**
     * Create a loan account based on the parameters. The created object is not stored on ledger
     * @param customerId [UUID]
     * @loan [Amount]] this will be the balance of the loan account
     * @return [LoanAccountState]
     */
    @Suspendable
    private fun createLoanAccount(customerId: UUID, loan: Amount<Currency>): LoanAccountState {
        val cordaLoanAccount = subFlow(CreateCordaAccountFlow()).state.data
        subFlow(RequestKeyForAccount(cordaLoanAccount))
        return LoanAccountState(AccountData(cordaLoanAccount.identifier.id, cordaLoanAccount, customerId,
                loan, Instant.now(), AccountStatus.ACTIVE))
    }
}

