package com.r3.refapp.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import com.r3.refapp.flows.internal.IntrabankCashPaymentFlow
import com.r3.refapp.flows.internal.verifySameBank
import com.r3.refapp.repositories.AccountRepository
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import com.r3.refapp.states.CurrentAccountState
import java.util.*

/**
 * Public API Intrabank payment flow.
 * Verifies that for given @param [accountFrom] and @param [accountTo] actual [CurrentAccountState] states exists on the ledger.
 * For @param [accountFrom] [CurrentAccountState] must have positive balance which is greater or equal than specified amount.
 * Only participant in transaction is the bank that holds both accounts.
 * @param accountFrom credit account to transfer funds from
 * @param accountTo account to transfer funds to
 * @param amount the balance to transfer
 */
@InitiatingFlow
@StartableByRPC
class IntrabankPaymentFlow(val accountFrom: UUID,
                           val accountTo: UUID,
                           val amount: Amount<Currency>) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val accountRepository: AccountRepository = serviceHub.cordaService(AccountRepository::class.java)
        val accountFromState = accountRepository.getCreditAccountStateById(accountFrom)
        val accountToState = accountRepository.getAccountStateById(accountTo)

        accountFromState.state.data.verifyHasSufficientFunds(amount)
        accountFromState.state.data.accountData.accountInfo.verifySameBank(accountToState.state.data.accountData.accountInfo)

        return subFlow(IntrabankCashPaymentFlow(accountFromState, accountToState, amount))
    }
}