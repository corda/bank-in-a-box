package com.r3.refapp.client.controllers

import com.r3.refapp.client.NodeRPCConnection
import com.r3.refapp.client.response.IntrabankPaymentResponse
import com.r3.refapp.client.response.MessageResponse
import com.r3.refapp.client.utils.ControllerUtils
import com.r3.refapp.flows.*
import com.r3.refapp.flows.reports.GetCustomerNameByAccountFlow
import com.r3.refapp.states.*
import com.r3.refapp.util.of
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.Instant
import java.util.*


/**
 * Provides Payments API endpoints.
 */
@RestController
@RequestMapping("/payments") // The paths for HTTP requests are relative to this base path.
class PaymentsController(rpc: NodeRPCConnection){

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val proxy = rpc.proxy

    /**
     * Withdraw amount of fiat currency from the account's balance
     * @param accountId ID of the account
     * @param tokenType token type of the amount
     * @param amount the amount of fiat currency to withdraw
     * @return [Account] the updated account object
     */
    @PostMapping(value = ["/withdraw-fiat"], produces = ["application/json"])
    private fun withdrawFiat(@RequestParam(value = "accountId") accountId: UUID,
                             @RequestParam(value = "tokenType") tokenType: String,
                             @RequestParam(value = "amount") amount: Long
    ): Account {
        val amountInCurrency = ControllerUtils.getAmountFromQuantityAndCurrency(amount, tokenType)
        return proxy.startFlow (::WithdrawFiatFlow, accountId, amountInCurrency)
                .returnValue.getOrThrow().tx.outputsOfType<Account>().single()
    }

    /**
     * Deposit amount of fiat currency to the account's balance
     * @param accountId ID of the account
     * @param tokenType token type of the amount
     * @param amount the amount of fiat currency to deposit
     * @return [Account] the updated account object
     */
    @PostMapping(value = ["/deposit-fiat"], produces = ["application/json"])
    private fun depositFiat(@RequestParam(value = "accountId") accountId: UUID,
                            @RequestParam(value = "tokenType") tokenType: String,
                            @RequestParam(value = "amount") amount: Long
    ): Account {
        val amountInCurrency = ControllerUtils.getAmountFromQuantityAndCurrency(amount, tokenType)
        return proxy.startFlow(::DepositFiatFlow, accountId, amountInCurrency)
                .returnValue.getOrThrow().tx.outputsOfType<Account>().single()
    }

    /**
     * Sends amount of fiat currency from one account to another
     * @param fromAccountId ID of the account from which the funds will be transferred
     * @param toAccountId ID of the account to which the funds will be transferred
     * @param tokenType token type of the amount
     * @param amount the amount of fiat currency to transfer
     * @return [IntrabankPaymentResponse] a response object containing the updated from and to accounts
     */
    @PostMapping(value = ["/intrabank-payment"], produces = ["application/json"])
    private fun intrabankPayment(@RequestParam(value = "fromAccountId") fromAccountId: UUID,
                                 @RequestParam(value = "toAccountId") toAccountId: UUID,
                                 @RequestParam(value = "tokenType") tokenType: String,
                                 @RequestParam(value = "amount") amount: Long
    ): IntrabankPaymentResponse {
        val amountInCurrency = ControllerUtils.getAmountFromQuantityAndCurrency(amount, tokenType)
        val txt = proxy.startFlow(::IntrabankPaymentFlow, fromAccountId, toAccountId, amountInCurrency)
                .returnValue.getOrThrow().tx

        val fromAcc = txt.outputsOfType<CurrentAccountState>().first()
        val toAcc = txt.outputsOfType<Account>()[1]
        val toCustomerName = proxy.startFlow(::GetCustomerNameByAccountFlow, toAcc.accountData.accountId).returnValue.getOrThrow()

        return IntrabankPaymentResponse(fromAcc, toAcc.accountData.accountId, toAcc.accountData.customerId, toCustomerName)
    }

    /**
     * Create a recurring payment between two parties
     * @param fromAccountId ID of the account from which the funds will be transferred
     * @param toAccountId ID of the account to which the funds will be transferred
     * @param amount the amount of fiat currency to transfer
     * @param tokenType token type of the amount
     * @param dateStart start of the recurring payment
     * @param period activity time period
     * @param iterationNum number of payment iterations
     * @return [RecurringPaymentState]
     */
    @PostMapping(value = ["/create-recurring-payment"], produces = ["application/json"])
    private fun createRecurringPayment(@RequestParam(value = "fromAccountId") fromAccountId: UUID,
                                       @RequestParam(value = "toAccountId") toAccountId: UUID,
                                       @RequestParam(value = "amount") amount: Long,
                                       @RequestParam(value = "tokenType") tokenType: String,
                                       @RequestParam(value = "dateStart") dateStart: Instant,
                                       @RequestParam(value = "period") period: Duration,
                                       @RequestParam(value = "iterationNum") iterationNum: Int?
    ): RecurringPaymentState {
        val amountInCurrency = ControllerUtils.getAmountFromQuantityAndCurrency(amount, tokenType)
        return proxy.startFlow(::CreateRecurringPaymentFlow, fromAccountId, toAccountId, amountInCurrency,
                dateStart, period, iterationNum).returnValue.getOrThrow().tx.outputsOfType<RecurringPaymentState>().first()
    }

    /**
     * Cancel an existing recurring payment
     * @param recurringPaymentId id of the recurring payment to cancel
     * return [String] Success message if the operation was successful
     */
    @PostMapping(value = ["/cancel-recurring-payment"], produces = ["application/json"])
    private fun cancelRecurringPayment(@RequestParam(value = "recurringPaymentId") recurringPaymentId: UUID
    ): MessageResponse {
        proxy.startFlow(::CancelRecurringPaymentFlow, UniqueIdentifier(id=recurringPaymentId))
                .returnValue.getOrThrow()
        return MessageResponse("Recurring payment $recurringPaymentId cancelled")
    }
}