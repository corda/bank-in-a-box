package com.r3.refapp.flows

import com.r3.corda.lib.accounts.contracts.commands.Create
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

/**
 * Create and return a Corda Account with random UUID name using notary given by config.
 * @return [AccountInfo]
 */
@StartableByRPC
class CreateCordaAccountFlow() : FlowLogic<StateAndRef<AccountInfo>>() {

    override fun call(): StateAndRef<AccountInfo> {
        val signedTx = createCordaAccountTransaction()

        val finalisedTx = subFlow(FinalityFlow(signedTx, emptyList()))
        return finalisedTx.coreTransaction.outRefsOfType<AccountInfo>().single()
    }

    /**
     * Return a signed Corda account transaction.
     * Implementation has been adapted from Corda accounts CreateAccount flow to allow for custom notaries
     * (CreateAccount takes the first notary from the notary list)
     * @returns [SignedTransaction]
     */
    private fun createCordaAccountTransaction(): SignedTransaction {
        val newAccountInfo = AccountInfo(
                name = UUID.randomUUID().toString(),
                host = ourIdentity,
                identifier = UniqueIdentifier(id = UUID.randomUUID())
        )

        val notary = ConfigurationUtils.getConfiguredNotary(serviceHub)
        val txBuilder = TransactionBuilder(notary = notary).apply {
            addOutputState(newAccountInfo)
            addCommand(Create(), ourIdentity.owningKey)
            verify(serviceHub)
        }

        return serviceHub.signInitialTransaction(txBuilder)
    }
}