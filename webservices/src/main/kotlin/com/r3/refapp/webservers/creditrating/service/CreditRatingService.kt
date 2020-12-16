package com.r3.refapp.webservers.creditrating.service

import com.r3.refapp.states.CreditRatingInfo
import java.util.*

/**
 * Service interface related to credit ratings
 */
interface CreditRatingService {
    /**
     * Return credit rating for a client
     * @param customerId the customer's id
     * @return [CreditRatingInfo] credit rating object containing the requested data
     */
    fun getCustomerCreditRating(customerId: UUID) : CreditRatingInfo
}