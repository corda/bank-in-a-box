package com.r3.refapp.webservers.creditrating

import com.r3.refapp.states.CreditRatingInfo
import com.r3.refapp.webservers.creditrating.service.CreditRatingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

/**
 * Credit rating API endpoints.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(@Autowired @Qualifier(value="dummyCreditRatingService")val creditRatingService: CreditRatingService){

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    /**
     * Return credit rating for a client
     * @param customerId the customer's id
     * @return [CreditRatingInfo] credit rating object containing the requested data
     */
    @GetMapping(value = ["/creditRating/customer/{customerId}"], produces = ["application/json"])
    private fun getCustomerCredit(@PathVariable(value="customerId") customerId: UUID): CreditRatingInfo {
        return creditRatingService.getCustomerCreditRating(customerId)
    }
}