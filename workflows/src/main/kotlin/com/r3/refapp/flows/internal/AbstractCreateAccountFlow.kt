package com.r3.refapp.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.refapp.contracts.FinancialAccountContract.Commands.AbstractAccountCreationCmd
import com.r3.refapp.flows.CreateCordaAccountFlow
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.states.Account
import com.r3.refapp.utils.ConfigurationUtils
import net.corda.core.contracts.*
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey
import java.util.*

/**
 * Create an account on ledger [SignedTransaction]
 * @param customerId the id of the customer
 * @return [SignedTransaction]
 */
abstract class AbstractCreateAccountFlow (private val customerId: UUID) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call() : SignedTransaction {
        val accountRepository = serviceHub.cordaService(AccountRepository::class.java)
        accountRepository.getCustomerWithId(customerId)
        val cordaAccount = subFlow(CreateCordaAccountFlow())
        subFlow(RequestKeyForAccount(cordaAccount.state.data))
        val newAccountState = createAccountStateObject(cordaAccount.state.data)
        val signedNewAccountTx = createNewAccountTransaction(newAccountState)
        return subFlow(FinalityFlow(signedNewAccountTx, emptyList()))
    }

    /**
     * Return a signed new account state transaction
     * @param newAccountState account state
     * @returns [SignedTransaction]
     */
    @Suspendable
    private fun createNewAccountTransaction(newAccountState: Account): SignedTransaction {
        val accountKey = subFlow(RequestKeyForAccount(newAccountState.accountData.accountInfo)).owningKey
        val keysToSignWith = mutableListOf(ourIdentity.owningKey, accountKey)

        val command = Command(
                createAccountCreationCommand(accountKey),
                keysToSignWith)

        val notary = ConfigurationUtils.getConfiguredNotary(serviceHub)
        val txBuilder = TransactionBuilder(notary = notary).apply {
            addOutputState(newAccountState)
            addCommand(command)
            verify(serviceHub)
        }

        return serviceHub.signInitialTransaction(txBuilder, keysToSignWith)
    }

    /**
     * Create account creation command based on cmdClazz
     * @param accountKey
     * @returns [SignedTransaction]
     */
    abstract fun createAccountCreationCommand(accountKey: PublicKey): AbstractAccountCreationCmd

    /**
     * Create account state object
     * @param cordaAccountInfo is assigned to the account
     * @returns [SignedTransaction]
     */
    abstract fun createAccountStateObject(cordaAccountInfo: AccountInfo): Account

}