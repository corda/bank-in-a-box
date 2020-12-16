package com.r3.refapp.webservers.creditrating.service.impl

import com.r3.refapp.states.CreditRatingInfo
import com.r3.refapp.webservers.creditrating.service.CreditRatingService
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

/**
 * Services related to credit rating
 */
@Component("dummyCreditRatingService")
class DummyCreditRatingService: CreditRatingService {

    /**
     * Return credit rating for a client
     * @param customerId the customer's id
     * @return [CreditRatingInfo] credit rating object containing the requested data
     */
    override fun getCustomerCreditRating(customerId: UUID) : CreditRatingInfo {
        val customerName = "Customer Name"
        val rating = 600
        return CreditRatingInfo(customerName, customerId, rating, Instant.now())
    }
}