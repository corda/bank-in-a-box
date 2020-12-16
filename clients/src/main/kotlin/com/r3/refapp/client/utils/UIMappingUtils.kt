package com.r3.refapp.client.utils

import com.r3.refapp.client.response.RecurringPaymentResponse
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.schemas.RecurringPaymentLogSchemaV1
import com.r3.refapp.states.RecurringPaymentState
import net.corda.core.contracts.Amount
import net.corda.core.crypto.sha256
import net.corda.core.internal.readFully
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.util.*

/**
 * Extension function on [Duration] object which returns [String] representation of duration in human readable format.
 * @return [String] representation of duration
 */
fun Duration.toUIString() =
        this.toDays().toString() + " days"

/**
 * Extension function on [RecurringPaymentState] object which maps state object to [RecurringPaymentResponse].
 * @return Mapped [RecurringPaymentResponse] object
 */
fun RecurringPaymentState.toResponse() =
        RecurringPaymentResponse(
                this.accountFrom,
                this.accountTo,
                this.amount.quantity,
                this.amount.token.toString(),
                this.dateStart,
                this.period.toUIString(),
                this.iterationNum,
                this.linearId.id,
                null, null, null)

/**
 * Extension function on [RecurringPaymentLogSchemaV1.RecurringPaymentLog] object which maps entity object to
 * [RecurringPaymentResponse].
 * @return Mapped [RecurringPaymentResponse] object
 */
fun RecurringPaymentLogSchemaV1.RecurringPaymentLog.toResponse() =
        RecurringPaymentResponse(
                this.recurringPayment.accountFrom,
                this.recurringPayment.accountTo,
                this.recurringPayment.amount,
                this.recurringPayment.currency,
                this.recurringPayment.dateStart,
                this.recurringPayment.period.toUIString(),
                this.recurringPayment.iterationNum,
                this.recurringPayment.linearId,
                this.error,
                this.logId,
                this.txDate)

/**
 * Extension function on [PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>] object which maps list of
 * entity objects to list of [RecurringPaymentResponse] objects.
 * @return Mapped [PaginatedResponse] object
 */
fun PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>.mapToRecurringPaymentResponse() =
        PaginatedResponse(this.result.map { it.toResponse() }, this.totalResults, this.pageSize,
                this.pageNumber, this.totalPages)

/**
 * Extension function on [MultipartFile] object which maps an attachment file to a hash String.
 * @return secure hash string
 */
fun MultipartFile.generateAttachmentHash(): String {
    val filename = this.originalFilename
    require(filename != null) { "File name must be set" }

    val hasZipJarExtension = filename.endsWith("zip", true) || filename.endsWith("jar", true)
    require(hasZipJarExtension) { "Only ZIP or Jar attachments are supported" }

    val bytes = this.inputStream.readFully()
    return bytes.sha256().toString()
}