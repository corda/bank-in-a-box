package com.r3.refapp.client.response

import com.r3.refapp.schemas.RecurringPaymentLogSchemaV1
import com.r3.refapp.schemas.RecurringPaymentSchemaV1
import java.time.Instant
import java.util.*

/**
 * DTO (Data Transfer Object) for recurring payments. Maps [RecurringPaymentSchemaV1.RecurringPayment] and
 * [RecurringPaymentLogSchemaV1.RecurringPaymentLog] objects to UI friendly format.
 * @param accountFrom ID of the accountFrom
 * @param accountFrom ID of the accountTo
 * @param amount quantity of currency units e.g. 1000 cents
 * @param currencyCode the currency code of amount
 * @param dateStart Start date of recurring payment
 * @param period String representation of recurring payment period e.g. '10 days'
 * @param iterationNum Number of recurring payment iterations left
 * @param recurringPaymentId ID of the recurring payment
 * @param error Recurring payment execution error
 * @param logId ID of the recurring payment log entry
 * @param txDate Date of the recurring payment execution
 */
data class RecurringPaymentResponse(val accountFrom: UUID,
                                    val accountTo: UUID,
                                    val amount: Long,
                                    val currencyCode: String,
                                    val dateStart: Instant,
                                    val period: String,
                                    val iterationNum: Int?,
                                    val recurringPaymentId: UUID,
                                    val error: String?,
                                    val logId: String?,
                                    val txDate: Instant?)