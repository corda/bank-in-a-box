package com.r3.refapp.flows.internal;

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.TransactionSignature
import net.corda.core.crypto.isFulfilledBy
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap


/**
 * Sends partialMerkleTx to the Oracle for validation and signing. It will also validate the received signature against tx
 * @param tx transaction to be used to validate the signature
 * @param oracle responsible for signing the transaction
 * @param partialMerkleTx filtered transaction to be sent to the Oracle
 * @return [TransactionSignature]
 */
@InitiatingFlow
class OracleSignCreditRatingRequestFlow(val tx: TransactionBuilder, val oracle: Party,
                                        val partialMerkleTx: FilteredTransaction) : FlowLogic<TransactionSignature>() {
    @Suspendable
    override fun call(): TransactionSignature {
        val oracleSession = initiateFlow(oracle)
        val resp = oracleSession.sendAndReceive<TransactionSignature>(partialMerkleTx)
        return resp.unwrap { sig ->
            check(oracleSession.counterparty.owningKey.isFulfilledBy(listOf(sig.by)))
            tx.toWireTransaction(serviceHub).checkSignature(sig)
            sig
        }
    }
}

