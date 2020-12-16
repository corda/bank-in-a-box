package com.r3.refapp.operations

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.states.CreditRatingInfo
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.client.jackson.JacksonSupport
import net.corda.core.flows.FlowExternalAsyncOperation
import net.corda.core.node.ServiceHub
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

/**
 * Async Credit Rating operation that gets the credit rating of a customer from the credit rating web REST service
 * @param customerId the customer's id
 */
class CreditRatingRequestOperation(val customerId: UUID, val serviceHub: ServiceHub): FlowExternalAsyncOperation<CreditRatingInfo> {
    @Suspendable
    override fun execute(deduplicationId: String): CompletableFuture<CreditRatingInfo> {

        return CompletableFuture.supplyAsync {
            val creditRatingServiceAddr = ConfigurationUtils.getCreditRatingWebAddr(serviceHub)
            CreditRatingSupplier(customerId, creditRatingServiceAddr, deduplicationId).get()
        }
    }
}


/**
 * Supplier that gets the actual data through a http request
 * @param customerId the customer's id
 * @param creditRatingServiceAddr host and port of the credit rating provider server
 * @param deduplicationId for each duplicate call, the deduplicationId is guaranteed to be the same allowing duplicate requests to be de-duplicated if necessary
 */
class CreditRatingSupplier(val customerId: UUID, private val creditRatingServiceAddr: String, val deduplicationId: String): Supplier<CreditRatingInfo> {

    companion object {
        val httpClient: OkHttpClient =  OkHttpClient.Builder().build()
    }

    @Suspendable
    override fun get(): CreditRatingInfo {
        val clientCreditURL = "http://$creditRatingServiceAddr/creditRating/customer/${customerId}"
        val objectMapper = JacksonSupport.createNonRpcMapper()
        val response = httpClient.newCall(
                Request.Builder().url(clientCreditURL).get().build()
        ).execute()
        return objectMapper.readValue(response.body()?.string(), CreditRatingInfo::class.java)
    }
}

