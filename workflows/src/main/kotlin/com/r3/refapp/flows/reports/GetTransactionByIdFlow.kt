package com.r3.refapp.flows.reports

import co.paralleluniverse.fibers.Suspendable
import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.repositories.TransactionLogRepository
import com.r3.refapp.schemas.TransactionLogSchemaV1
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

/**
 * Public API flow used for reporting and views. Flow retrieves [TransactionLogSchemaV1.TransactionLog] for given
 * [txId].
 * @param txId Id of the transaction log
 * @return Returns [FlowLogic<TransactionLogSchemaV1.TransactionLog>] object for given [txId]
 * @throws [IllegalArgumentException] if [TransactionLogSchemaV1.TransactionLog] with given [txId] cannot be found
 */
@StartableByRPC
@InitiatingFlow
class GetTransactionByIdFlow(val txId: String) : FlowLogic<TransactionLogSchemaV1.TransactionLog>() {

    @Suspendable
    override fun call(): TransactionLogSchemaV1.TransactionLog {
        val transactionLogRepository = serviceHub.cordaService(TransactionLogRepository::class.java)
        return transactionLogRepository.getTransactionLogById(txId)
    }
}