package com.r3.refapp.client.response

import com.r3.refapp.states.CurrentAccountState
import java.util.*

/** Class to store the intrabank payment endpoint response */
data class IntrabankPaymentResponse(val fromAccount: CurrentAccountState, val toAccountId: UUID, val toAccountCustomerId: UUID,
                                    val toAccountCustomerName: String)