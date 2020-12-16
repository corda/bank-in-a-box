package com.r3.refapp.contracts

import com.r3.refapp.states.*
import com.r3.refapp.util.*
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.security.PublicKey
import java.time.*
import java.util.*

/**
 * Contract used to verify transactions related to financial accounts, including account creation, modification,
 * deposits, withdrawals and transfers.
 */
class FinancialAccountContract : Contract {

    companion object {
        const val ID = "com.r3.refapp.contracts.FinancialAccountContract"
    }

    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.first()

        when (command.value) {
            is Commands.CreateIntrabankPayment -> {
                verifyCreateIntrabankPayment(tx)
            }
            is Commands.CreateCurrentAccount -> {
                verifyCreateCurrentAccount(tx)
            }
            is Commands.CreateSavingsAccount -> {
                verifyCreateSavingsAccount(tx)
            }
            is Commands.DepositFunds -> {
                verifyDepositFunds(tx)
            }
            is Commands.WithdrawFunds -> {
                verifyWithdrawFunds(tx)
            }
            is Commands.IssueLoan -> {
                verifyIssueLoan(tx)
            }
            is Commands.SetAccountStatus -> {
                verifySetAccountStatus(tx)
            }
            is Commands.ApproveOverdraft -> {
                verifyApproveOverdraft(tx)
            }
            is Commands.SetLimits -> {
                verifySetLimits(tx)
            }
            else -> throw IllegalArgumentException("Command not recognized")
        }
    }

    /**
     * Verification method for [Commands.IssueLoan] command.
     * @param tx the [LedgerTransaction]
     * Require that:
     *   There is one IssueLoan command
     *   The number of inputs is one, and is of type CurrentAccountState
     *   The number of outputs is two, and one is of type CurrentAccountState, the other is LoanAccountState
     *   The out AccountState balance [amount] is increased with the loan amount
     *   The balance [amount] of the out LoanAccount is equal with the loan amount
     *   The account isActive
     *   Required signers must contain the bank [PublicKey]
     */
    private fun verifyIssueLoan(tx: LedgerTransaction) {
        requireThat {

            executeBasicContractVerification(
                    tx = tx,
                    commands = listOf(1 ofType Commands.IssueLoan::class.java, 1 ofType Commands.VerifyCreditRating::class.java),
                    inputs = listOf(1 ofType CurrentAccountState::class.java),
                    outputs = listOf(1 ofType CurrentAccountState::class.java,
                            1 ofType LoanAccountState::class.java),
                    signerKeys = listOf("Account bank" key { tx.inputsOfType<CurrentAccountState>().single().accountData.accountInfo.host.owningKey},
                                "Oracle" key { tx.commandsOfType<Commands.VerifyCreditRating>().single().value.oracleKey}
                            ))

            val commandIssueLoan = tx.commandsOfType<Commands.IssueLoan>().single().value
            val repaymentAccountIn = tx.inputsOfType<CurrentAccountState>().single()
            val repaymentAccountOut = tx.outputsOfType<CurrentAccountState>().single()
            val loanAccountOut = tx.outputsOfType<LoanAccountState>().single()

            val commandCreditRating = tx.commandsOfType<Commands.VerifyCreditRating>().single().value
            val customerCreditRatingInfo = commandCreditRating.creditRatingInfo
            val creditRatingValidityEnd = commandCreditRating.dateStart.plus(commandCreditRating.validityPeriod)

            "Transaction TimeWindow should not be null" using (tx.timeWindow != null)
            "Customer credit rating should be greater than the credit rating threshold" using (customerCreditRatingInfo.rating > commandCreditRating.creditRatingThreshold)

            // credit rating validity should cover entire transaction TimeWindow
            "Customer credit rating validity start time cannot be after transaction TimeWindow start time" using (!commandCreditRating.dateStart.isAfter(tx.timeWindow!!.fromTime))
            "Customer credit rating validity end time cannot be before transaction TimeWindow end time" using ( !creditRatingValidityEnd.isBefore(tx.timeWindow!!.untilTime))


            "Loan amount should be greater than 0" using (commandIssueLoan.loan.toDecimal() > BigDecimal.ZERO)

            "Command account should be the same as input account" using
                    (commandIssueLoan.account == repaymentAccountOut.accountData.accountInfo.identifier)

            "Command loan amount should be equal to the output loan account balance" using
                    (loanAccountOut.accountData.balance == commandIssueLoan.loan)

            "In and out accounts should have the same ID's" using
                    (repaymentAccountIn.accountData.accountInfo.identifier == repaymentAccountOut.accountData.accountInfo.identifier)

            "Current account balance should be credited with an amount that is equal to the output loan balance" using
                    (repaymentAccountOut.accountData.balance - repaymentAccountIn.accountData.balance
                            == loanAccountOut.accountData.balance )

            "Account bank should not be changed between the input and the output" using
                    (repaymentAccountIn.accountData.accountInfo.host
                            == repaymentAccountOut.accountData.accountInfo.host)

            "In Account should be active" using
                    (repaymentAccountIn.accountData.status == AccountStatus.ACTIVE)
            "Out Account should be active" using
                    (repaymentAccountOut.accountData.status == AccountStatus.ACTIVE)
            "Out Loan Account should be active" using
                    (loanAccountOut.accountData.status == AccountStatus.ACTIVE)

            "Loan Account bank should be the same as Current Account bank" using
                    (repaymentAccountIn.accountData.accountInfo.host
                            == loanAccountOut.accountData.accountInfo.host)
        }
    }

    /**
     * Verification method for [Commands.CreateCurrentAccount] command.
     * @param tx the [LedgerTransaction]
     * Require that:
     *   There is one CreateCurrentAccount command
     *   The number of inputs is zero
     *   The number of outputs is one, and is of type CurrentAccountState
     *   The balance [amount] is zero
     *   The account isActive
     *   Required signers must contain the account owner [PublicKey]
     */
    private fun verifyCreateCurrentAccount(tx: LedgerTransaction) = verifyCreateAccount<CurrentAccountState, Commands.CreateCurrentAccount>(tx) {
        // additional checks if needed
    }

    /**
     * Verification method for [Commands.CreateSavingsAccount] command.
     * @param tx the [LedgerTransaction]
     * Require that:
     *   There is one CreateSavingsAccount command
     *   The number of inputs is zero
     *   The number of outputs is one, and is of type SavingsAccountState
     *   The balance [amount] is zero
     *   The account isActive
     *   Required signers must contain the account owner [PublicKey]
     */
    private fun verifyCreateSavingsAccount(tx: LedgerTransaction) = verifyCreateAccount<SavingsAccountState, Commands.CreateSavingsAccount>(tx) {
        val savingsAccountOut = tx.outputsOfType<SavingsAccountState>().single()
        val command = tx.commandsOfType<Commands.CreateSavingsAccount>().single()
        val savingsEndDate = LocalDateTime.ofInstant(command.value.savingsStartDate, ZoneId.systemDefault())
                .plusMonths(command.value.period.toLong()).atZone(ZoneId.systemDefault()).toInstant()

        "Savings period should be set to commands value" using (savingsAccountOut.period == Period.ofMonths(command.value.period))

        "Savings end date should be set to commands startDate plus savings period" using (savingsAccountOut.savingsEndDate == savingsEndDate)
    }


    /**
     * Verification method for account creation commands.
     * @param tx the [LedgerTransaction]
     * Require that:
     *   There is one C command
     *   The number of inputs is zero
     *   The number of outputs is one, and is of type A
     *   The balance [amount] is zero
     *   The account isActive
     *   Required signers must contain the account owner [PublicKey]
     */
    private inline fun <reified A: Account, reified C : Commands.AbstractAccountCreationCmd> verifyCreateAccount(tx: LedgerTransaction, additionalVerifications: (LedgerTransaction) -> Unit) {
        requireThat {

            executeBasicContractVerification(
                    tx = tx,
                    commands = listOf(1 ofType C::class.java),
                    inputs = listOf(0 ofType A::class.java),
                    outputs = listOf(1 ofType A::class.java),
                    signerKeys = listOf("Account key" key { tx.commandsOfType<C>().single().value.accountKey },
                            "Account bank" key {tx.outputsOfType<A>().single().accountData.accountInfo.host.owningKey} ))

            val accountOut = tx.outputsOfType<A>().single()
            "${A::class.qualifiedName} output state must have a zero balance" using (accountOut.accountData.balance == 0 of accountOut.accountData.balance.token)

            "${A::class.qualifiedName} output state must be in pending state" using (accountOut.accountData.status == AccountStatus.PENDING)
            additionalVerifications(tx)
        }
    }

    /**
     * Verification method for [Commands.CreateIntrabankPayment] command.
     * @param tx the [LedgerTransaction]
     */
    private fun verifyCreateIntrabankPayment(tx: LedgerTransaction) {
        requireThat {
            executeBasicContractVerification(
                    tx = tx,
                    commands = listOf(1 ofType Commands.CreateIntrabankPayment::class.java),
                    inputs = listOf(2 ofType Account::class.java),
                    outputs = listOf(2 ofType Account::class.java),
                    signerKeys = listOf("AccountFrom" key { tx.commandsOfType<Commands.CreateIntrabankPayment>().single().value.fromAccountPublicKey },
                            "AccountTo" key { tx.commandsOfType<Commands.CreateIntrabankPayment>().single().value.toAccountPublicKey }))

            val command = tx.commandsOfType<Commands.CreateIntrabankPayment>().single()

            val accountFromIn = tx.inputsOfType<CreditAccount>()
                    .single { it.accountData.accountId == command.value.fromAccount }
            val accountFromOut = tx.outputsOfType<Account>()
                    .single { it.accountData.accountId == command.value.fromAccount }

            "FromIn Account must be a CurrentAccount" using (accountFromIn is CurrentAccountState)
            "FromOut Account must be a CurrentAccount" using (accountFromOut is CurrentAccountState)


            val accountToIn = tx.inputsOfType<Account>()
                    .single { it.accountData.accountId == command.value.toAccount }
            val accountToOut = tx.outputsOfType<Account>()
                    .single { it.accountData.accountId == command.value.toAccount }

            "Amount should be greater than 0" using (command.value.amount.toDecimal() > BigDecimal.ZERO)

            "From and to in accounts should be different" using
                    (accountFromIn.accountData.accountId != accountToIn.accountData.accountId)
            "From and to out accounts should be different" using
                    (accountFromOut.accountData.accountId != accountToOut.accountData.accountId)

            "From account input state should have sufficient amount on balance" using
                    (accountFromIn.verifyHasSufficientFunds(command.value.amount))

            "Account from balance should be credited by tx amount between input and output" using
                    (accountFromIn.withdraw(command.value.amount).accountData.balance == accountFromOut.accountData.balance)

            "Account to balance should be debited by tx amount between input and output" using
                    (accountToIn.deposit(command.value.amount).accountData.balance == accountToOut.accountData.balance)

            "Account from bank should not be changed between input and output" using
                    (accountFromIn.accountData.accountInfo.host == accountFromOut.accountData.accountInfo.host)
            "Account to bank should not be changed between input and output" using
                    (accountToIn.accountData.accountInfo.host == accountToOut.accountData.accountInfo.host)

            "Banks should be the same for both accounts" using
                    (accountToIn.accountData.accountInfo.host == accountFromIn.accountData.accountInfo.host)
        }

    }

    /**
     * Verification method for [Commands.DepositFunds] command.
     * @param tx the [LedgerTransaction]
     */
    private fun verifyDepositFunds(tx: LedgerTransaction) {
        requireThat {

            executeBasicContractVerification(
                    tx = tx,
                    commands = listOf(1 ofType Commands.DepositFunds::class.java),
                    inputs = listOf(1 ofType Account::class.java),
                    outputs = listOf(1 ofType Account::class.java),
                    signerKeys = listOf("Account bank" key { tx.inputsOfType<Account>().single().accountData.accountInfo.host.owningKey}))

            val command = tx.commandsOfType<Commands.DepositFunds>().single()
            val accountIn = tx.inputsOfType<Account>().single()
            val accountOut = tx.outputsOfType<Account>().single()

            "Amount should be greater than 0" using (command.value.amount.toDecimal() > BigDecimal.ZERO)

            "Command account should be the same as input account" using
                    (command.value.account == accountOut.accountData.accountInfo.identifier)

            "In and out accounts should be the same" using
                    (accountIn.accountData.accountInfo.identifier == accountOut.accountData.accountInfo.identifier)

            "Account balance should be credited by tx amount between the input and the output" using
                    (accountIn.deposit(command.value.amount).accountData.balance == accountOut.accountData.balance)

            "Account bank should not be changed between the input and the output" using
                    (accountIn.accountData.accountInfo.host == accountOut.accountData.accountInfo.host)
        }
    }

    /**
     * Verification method for [Commands.WithdrawFunds] command.
     * @param tx the [LedgerTransaction]
     */
    private fun verifyWithdrawFunds(tx: LedgerTransaction) {
        requireThat {

            executeBasicContractVerification(
                    tx = tx,
                    commands = listOf(1 ofType Commands.WithdrawFunds::class.java),
                    inputs = listOf(1 ofType CreditAccount::class.java),
                    outputs = listOf(1 ofType CreditAccount::class.java),
                    signerKeys = listOf("Account bank" key { tx.inputsOfType<CreditAccount>().single().accountData.accountInfo.host.owningKey}))

            val command = tx.commandsOfType<Commands.WithdrawFunds>().single()
            val accountIn = tx.inputsOfType<CreditAccount>().single()
            val accountOut = tx.outputsOfType<CreditAccount>().single()

            "Amount should be greater than 0" using (command.value.amount.toDecimal() > BigDecimal.ZERO)

            "Command account should be the same as input account" using
                    (command.value.account == accountOut.accountData.accountInfo.identifier)

            "In and out accounts should be the same" using
                    (accountIn.accountData.accountInfo.identifier == accountOut.accountData.accountInfo.identifier)

            "Account should have sufficient balance" using
                    (accountIn.verifyHasSufficientFunds(command.value.amount))

            "Account balance should be debited by tx amount between the input and the output" using
                    (accountIn.withdraw(command.value.amount).accountData.balance == accountOut.accountData.balance)

            "Account bank should not be changed between the input and the output" using
                    (accountIn.accountData.accountInfo.host == accountOut.accountData.accountInfo.host)
        }
    }

    /**
     * Verification method for [Commands.ApproveOverdraft] command.
     * @param tx the [LedgerTransaction]
     * Require that:
     *   There is one [Commands.ApproveOverdraft] command
     *   The number of inputs of type [CurrentAccountState] is one
     *   The number of outputs of type [CurrentAccountState] is one
     *   The overdraftBalance is set to zero
     *   The overdraftLimit is set ot value from command
     *   All fields from the input [CurrentAccountState] are copied to the output [CurrentAccountState]
     *   Required signers must contain the account owner [PublicKey]
     */
    private fun verifyApproveOverdraft(tx: LedgerTransaction) {
        requireThat {

            executeBasicContractVerification(
                    tx = tx,
                    commands = listOf(1 ofType  Commands.ApproveOverdraft::class.java),
                    inputs = listOf(1 ofType CurrentAccountState::class.java),
                    outputs = listOf(1 ofType CurrentAccountState::class.java),
                    signerKeys = listOf("Account key" key { tx.inputsOfType<CurrentAccountState>().single().accountData.accountInfo.host.owningKey } ))

            val command = tx.commandsOfType<Commands.ApproveOverdraft>().single()
            val accountIn = tx.inputsOfType<CurrentAccountState>().single()
            val accountOut = tx.outputsOfType<CurrentAccountState>().single()

            "Command account should be the same as input account" using
                    (command.value.accountId == accountIn.accountData.accountId)

            "In and out accounts should have same fields" using
                    (accountIn == accountOut.copy(approvedOverdraftLimit = null, overdraftBalance = null))

            "Overdraft balance should be equal to 0" using (accountOut.overdraftBalance ?: 0L == 0L)
            "Overdraft limit should be equal to command value" using
                    (accountOut.approvedOverdraftLimit == command.value.approvedLimit)

            "Account bank should not be changed between the input and the output" using
                    (accountIn.accountData.accountInfo.host == accountOut.accountData.accountInfo.host)
        }
    }

    /**
     * Verification method for [Commands.SetAccountStatus] command.
     * @param tx the [LedgerTransaction]
     * Require that:
     *   There is one [Commands.SetAccountStatus] command
     *   The number of inputs of type [Account] is one
     *   The number of outputs of type [Account] is one
     *   The [AccountStatus] is set to commands value, all other fields are unchanged
     *   Required signers must contain the account owner [PublicKey]
     */
    private fun verifySetAccountStatus(tx: LedgerTransaction) {
        requireThat {
            executeBasicContractVerification(
                    tx = tx,
                    commands = listOf(1 ofType Commands.SetAccountStatus::class.java),
                    inputs = listOf(1 ofType Account::class.java),
                    outputs = listOf(1 ofType Account::class.java),
                    signerKeys = listOf("Account key" key { tx.inputsOfType<Account>().single().accountData.accountInfo.host.owningKey } ))

            val command = tx.commandsOfType<Commands.SetAccountStatus>().single()
            val accountIn = tx.inputsOfType<Account>().single()
            val accountOut = tx.outputsOfType<Account>().single()

            "Command account should be the same as input account" using
                    (command.value.accountId == accountIn.accountData.accountId)

            "Account status should be set to commands status, other fields must be unchanged" using
                    (accountOut == accountIn.setStatus(command.value.accountStatus))

            "Account bank should not be changed between the input and the output" using
                    (accountIn.accountData.accountInfo.host == accountOut.accountData.accountInfo.host)

            "Account cannot progress from status: ${accountIn.accountData.status} to status ${command.value.accountStatus}" using
                    (accountIn.accountData.status.canProgressToStatus(command.value.accountStatus))
        }
    }

    /**
     * Verification method for [Commands.SetLimits] command.
     * @param tx the [LedgerTransaction]
     * Require that:
     *   There is one [Commands.SetLimits] command
     *   The number of inputs of type [CurrentAccountState] is one
     *   The number of outputs of type [CurrentAccountState] is one
     *   The [CurrentAccountState.withdrawalDailyLimit] and [CurrentAccountState.transferDailyLimit] are set to
     *   commands value, all other fields are unchanged
     *   Required signers must contain the accounts bank [PublicKey]
     */
    private fun verifySetLimits(tx: LedgerTransaction) {
        requireThat {
            executeBasicContractVerification(
                    tx = tx,
                    commands = listOf(1 ofType Commands.SetLimits::class.java),
                    inputs = listOf(1 ofType CurrentAccountState::class.java),
                    outputs = listOf(1 ofType CurrentAccountState::class.java),
                    signerKeys = listOf("Bank's key" key { tx.inputsOfType<CurrentAccountState>().single().accountData.accountInfo.host.owningKey } ))

            val command = tx.commandsOfType<Commands.SetLimits>().single()
            val accountIn = tx.inputsOfType<CurrentAccountState>().single()
            val accountOut = tx.outputsOfType<CurrentAccountState>().single()

            "Command account should be the same as input account" using
                    (command.value.accountId == accountIn.accountData.accountId)

            "Account's withdrawalDailyLimit and transferDailyLimit should be set to commands values, other fields must be unchanged" using
                    (accountOut == accountIn.withLimits(withdrawalDailyLimit = command.value.withdrawalDailyLimit,
                            transferDailyLimit = command.value.transferDailyLimit))
        }
    }

    interface Commands : CommandData {
        class CreateIntrabankPayment(val amount: Amount<Currency>,
                                     val fromAccount: UUID,
                                     val toAccount: UUID,
                                     val fromAccountPublicKey: PublicKey,
                                     val toAccountPublicKey: PublicKey) : Commands
        class DepositFunds(val account: UniqueIdentifier,
                           val amount: Amount<Currency>) : Commands
        class WithdrawFunds(val account: UniqueIdentifier,
                            val amount: Amount<Currency>) : Commands

        /**
         * Base of account creation COMMANDS
         * @param accountKey
         */
        abstract class AbstractAccountCreationCmd(open val accountKey: PublicKey): Commands
        /**
         * Command for creating a current account
         * @param accountKey
         */
        class CreateCurrentAccount(override val accountKey: PublicKey) : AbstractAccountCreationCmd(accountKey)
        /**
         * Command for creating a savings account
         * @param accountKey
         */
        class CreateSavingsAccount(override val accountKey: PublicKey, val period: Int, val savingsStartDate: Instant) : AbstractAccountCreationCmd(accountKey)

        /**
         * Command used to issue a loan
         * @param account the loan's beneficiary account
         * @param loan fiat amount representing the loan principal
         */
        class IssueLoan(val account: UniqueIdentifier, val loan: Amount<Currency>) :Commands
        /**
         * Command used to validate credit rating
         * @param creditRatingInfo information about the customer rating
         * @param creditRatingThreshold rating threshold above which a loan should be approved
         * @param oracleKey the key of the credit rating oracle
         * @param dateStart validity starting point of a credit rating
         * @param validityPeriod validity period of the credit rating
         */
        class VerifyCreditRating(val creditRatingInfo: CreditRatingInfo,
                                 val creditRatingThreshold: Int,
                                 val oracleKey: PublicKey,
                                 val dateStart: Instant,
                                 val validityPeriod: Duration): Commands
        /**
         * Command used to set [AccountStatus]. Initially all accounts are set to [AccountStatus.PENDING], command
         * can be used to activate or freeze account.
         * @param accountId the account id
         * @param accountStatus the [AccountStatus] to set for given account
         */
        class SetAccountStatus(val accountId: UUID, val accountStatus: AccountStatus) : Commands

        /**
         * Command used to approve overdraft
         * @param accountId the account id
         * @param approvedLimit overdraft amount limit
         */
        class ApproveOverdraft(val accountId: UUID, val approvedLimit: Long) : Commands

        /**
         * Command used to set withdrawalDailyLimit and transferDailyLimit
         * @param accountId the account id
         * @param withdrawalDailyLimit limit on withdrawals per day
         * @param transferDailyLimit limit on transfers per day
         */
        class SetLimits(val accountId: UUID, val withdrawalDailyLimit: Long?, val transferDailyLimit: Long?) : Commands
    }
}