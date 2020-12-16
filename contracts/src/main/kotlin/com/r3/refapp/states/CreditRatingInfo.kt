package com.r3.refapp.states

import net.corda.core.serialization.CordaSerializable
import java.time.Instant
import java.util.*

/**
 * Object which holds all the information related to credit rating
 * @property customerName name of the customer
 * @property customerId id of the customer
 * @property rating credit rating of the customer
 * @property time of the credit rating calculation
 */
@CordaSerializable
data class CreditRatingInfo(val customerName: String,
                            val customerId: UUID,
                            val rating: Int,
                            val time: Instant)