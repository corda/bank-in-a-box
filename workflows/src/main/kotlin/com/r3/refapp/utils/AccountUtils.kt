package com.r3.refapp.utils

import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.repositories.TransactionLogRepository
import com.r3.refapp.schemas.CustomerSchemaV1
import com.r3.refapp.schemas.TransactionType
import com.r3.refapp.states.CreditAccount
import com.r3.refapp.states.CurrentAccountState
import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import java.time.temporal.ChronoUnit
import java.util.*


/**
 * Calculate the sum of the daily transaction amounts debited from this account.
 * @param transactionType type of transaction to query
 * @param transactionLogRepository transaction log repository services
 * @return daily transaction value total [Long]
 */
fun CreditAccount.calculateDailyTotalDebited(
        transactionType: TransactionType,
        transactionLogRepository: TransactionLogRepository): Long {

    val dailyTxs = transactionLogRepository.getTransactionLogByTransactionTypeAndBetweenTime(
            accountId = this.accountData.accountId,
            txType = transactionType,
            startTime = this.accountData.txDate.truncatedTo(ChronoUnit.DAYS),
            endTime = this.accountData.txDate
    )
    // filter txs for transactions from this account
    val dailyFromTxs = dailyTxs.filter { it.accountFrom == this.accountData.accountId }
    return dailyFromTxs.map { it.amount }.sum()
}


/**
 * Calculate the daily transfer from amount and check that it has not exceeded [CurrentAccountState.transferDailyLimit]
 * @param amount the value of the transaction to be checked
 * @param transactionLogRepository transaction log repository services
 * @throws [RefappException] if transfer daily limit has been exceeded
 */
fun CreditAccount.checkTransferDailyLimit(amount: Amount<Currency>, transactionLogRepository: TransactionLogRepository) {
    if (this is CurrentAccountState && this.transferDailyLimit != null) {
        val dailyTransferredValue = this.calculateDailyTotalDebited(TransactionType.TRANSFER, transactionLogRepository)
        val totalTransferredAmount = dailyTransferredValue + amount.quantity

        if (totalTransferredAmount > this.transferDailyLimit!!) {
            throw RefappException("Daily transfer limit exceeded " +
                    "(amount transferred from: $dailyTransferredValue, amount to transfer: $amount, " +
                    "limit: ${this.transferDailyLimit})")
        }
    }
}


/**
 * Calculate the daily withdrawn amount and check that it has not exceeded [currentAccountState.withdrawalDailyLimit]
 * @param amount the value of the transaction to be checked
 * @param transactionLogRepository transaction log repository services
 * @throws [RefappException] if withdrawal daily limit has been exceeded
 */
fun CreditAccount.checkWithdrawalDailyLimit(amount: Amount<Currency>, transactionLogRepository: TransactionLogRepository) {
    if (this is CurrentAccountState && this.withdrawalDailyLimit != null) {
        val dailyWithdrawnValue = this.calculateDailyTotalDebited(TransactionType.WITHDRAWAL, transactionLogRepository)
        val totalWithdrawnAmount = dailyWithdrawnValue + amount.quantity

        if (totalWithdrawnAmount > this.withdrawalDailyLimit!!) {
            throw RefappException("Daily withdrawal limit exceeded " +
                    "(amount withdrawn: $dailyWithdrawnValue, amount to withdraw: $amount, " +
                    "limit: ${this.withdrawalDailyLimit})")
        }
    }
}


/**
 * Check customer details and throw a RefappException if details are invalid
 * @param serviceHub Corda ServiceHub
 */
fun CustomerSchemaV1.Customer.verifyCustomerDetails(serviceHub: ServiceHub) {
    // check that all customer attachments exist
    val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
    this.attachments.forEach {
        val attachmentId = SecureHash.parse(it.attachmentHash)
        if (!serviceHub.attachments.hasAttachment(attachmentId)) {
            throw RefappException("Attachment $attachmentId does not exist in AttachmentStorage")
        }
    }
    if(accountRepository.attachmentExists(this.attachments.map { it.attachmentHash }, customerId)) {
        throw RefappException("Some attachments from the customer's attachment list are associated with different " +
                "customer, Please provide customer specific attachments.")
    }
}