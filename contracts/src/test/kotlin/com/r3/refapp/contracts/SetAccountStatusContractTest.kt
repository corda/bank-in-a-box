package com.r3.refapp.contracts

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.refapp.states.AccountStatus
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.contracts.UniqueIdentifier
import org.junit.Test
import java.time.Instant
import java.util.*

class SetAccountStatusContractTest : AbstractContractTest() {

    private val txDate = Instant.now()
    private val uniqueIdentifier = UniqueIdentifier()
    private val customerId = UUID.randomUUID()
    private val currentAccountIn = getCurrentAccount(aliceId.id, bank, id = uniqueIdentifier,
            accountStatus = AccountStatus.PENDING, txDate = txDate, linearId = uniqueIdentifier, customerId = customerId)
    private val currentAccountOut = getCurrentAccount(aliceId.id, bank, id = uniqueIdentifier,
            txDate = txDate, linearId = uniqueIdentifier, customerId = customerId)

    @Test
    fun `verify SetAccountStatus happy path for all account types`() {

        transaction {

            // verifies for overdraft account
            tweak {
                input(FinancialAccountContract.ID, getOverdraftAccount(currentAccountIn))
                output(FinancialAccountContract.ID, getOverdraftAccount(currentAccountOut))
                command(listOf(bank.publicKey), getSetAccountStateCommand(status = AccountStatus.ACTIVE))
                verifies()
            }

            // verifies for loan account
            tweak {
                val loanAccount = getLoanAccountState(aliceId.id.toString(), uniqueIdentifier, bank, 0 of EUR)
                input(FinancialAccountContract.ID, loanAccount)
                output(FinancialAccountContract.ID, loanAccount.copy(accountData = loanAccount.accountData.copy(status = AccountStatus.SUSPENDED)))
                command(listOf(bank.publicKey), getSetAccountStateCommand(loanAccount.accountData.accountId, status = AccountStatus.SUSPENDED))
                verifies()
            }

            // verifies for savings account
            tweak {
                val savingsAccount = getSavingAccountState(aliceId.id.toString(), uniqueIdentifier, bank, 0 of EUR, status = AccountStatus.ACTIVE)
                input(FinancialAccountContract.ID, savingsAccount)
                output(FinancialAccountContract.ID, savingsAccount.copy(accountData = savingsAccount.accountData.copy(status = AccountStatus.SUSPENDED)))
                command(listOf(bank.publicKey), getSetAccountStateCommand(savingsAccount.accountData.accountId, status = AccountStatus.SUSPENDED))
                verifies()
            }

            // verifies for savings account from suspended to active
            tweak {
                val savingsAccount = getSavingAccountState(aliceId.id.toString(), uniqueIdentifier, bank, 0 of EUR, status = AccountStatus.SUSPENDED)
                input(FinancialAccountContract.ID, savingsAccount)
                output(FinancialAccountContract.ID, savingsAccount.copy(accountData = savingsAccount.accountData.copy(status = AccountStatus.ACTIVE)))
                command(listOf(bank.publicKey), getSetAccountStateCommand(savingsAccount.accountData.accountId, status = AccountStatus.ACTIVE))
                verifies()
            }

            // verifies for current account
            input(FinancialAccountContract.ID, currentAccountIn)
            output(FinancialAccountContract.ID, currentAccountOut)
            command(listOf(bank.publicKey), getSetAccountStateCommand())
            verifies()

        }
    }

    @Test
    fun `verify SetAccountStatus fails on command checks`() {

        transaction {
            // fails on command type check
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), DummyCommand())
                `fails with`("Contract verification failed: Command not recognized")
            }

            // fails on number of commands check
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                `fails with`("Contract verification failed: Failed requirement: Number of commands should be equal to 1 for SetAccountStatus")
            }

            // fails on required signers check
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank2.publicKey), getSetAccountStateCommand())
                `fails with`("Account key key must be only signer for SetAccountStatus")
            }
        }
    }

    @Test
    fun `verify SetAccountStatus fails on input checks`() {

        transaction {
            // fails on number of inputs
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                `fails with`("Number of inputs should be equal to 1 for SetAccountStatus")
            }

            // fails on number of inputs of type
            tweak {
                input(FinancialAccountContract.ID, DummyState())
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                `fails with`("Number of inputs of type Account should be equal to 1 for SetAccountStatus")
            }

            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), getSetAccountStateCommand(bobId.id))
                `fails with`("Command account should be the same as input account")
            }
        }
    }

    @Test
    fun `verify SetAccountStatus fails on out checks`() {

        transaction {
            // fails on number of outputs
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, currentAccountOut)
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                `fails with`("Number of outputs should be equal to 1 for SetAccountStatus")
            }

            // fails on number of outputs of type
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, DummyState())
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                `fails with`("Number of outputs of type Account should be equal to 1 for SetAccountStatus")
            }
        }
    }

    @Test
    fun `verify SetAccountStatus fails on in and out checks`() {

        val overdraftAccount = getOverdraftAccount(currentAccountIn)
        transaction {
            // fails on in and out accounts not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(accountData = overdraftAccount.accountData.copy(status = AccountStatus.ACTIVE,
                        accountInfo = AccountInfo("test", bank.party, UniqueIdentifier()))))
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                `fails with`("Account status should be set to commands status, other fields must be unchanged")
            }

            // fails on in and out accounts not in same bank
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(accountData = overdraftAccount.accountData.copy(status = AccountStatus.ACTIVE,
                        accountInfo = AccountInfo(currentAccountIn.accountData.accountInfo.name, bank2.party,
                                currentAccountIn.accountData.accountInfo.identifier))))
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                `fails with`("Account status should be set to commands status, other fields must be unchanged")
            }

            // fails on in and out balance not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(accountData = overdraftAccount.accountData.copy(
                        status = AccountStatus.ACTIVE, balance = 5 of EUR)))
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                `fails with`("Account status should be set to commands status, other fields must be unchanged")
            }

            // fails on in and out customer not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(accountData = overdraftAccount.accountData.copy(
                        status = AccountStatus.ACTIVE, customerId = UUID.randomUUID())))
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                `fails with`("Account status should be set to commands status, other fields must be unchanged")
            }

            // fails on in and out linear id not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(accountData = overdraftAccount.accountData.copy(
                        status = AccountStatus.ACTIVE), linearId = UniqueIdentifier()))
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                `fails with`("Account status should be set to commands status, other fields must be unchanged")
            }
            // fails on in and out linear id not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(accountData = overdraftAccount.accountData.copy(
                        status = AccountStatus.ACTIVE), overdraftBalance = 5000))
                command(listOf(bank.publicKey), getSetAccountStateCommand())
                `fails with`("Account status should be set to commands status, other fields must be unchanged")
            }
        }
    }

    @Test
    fun `verify SetAccountStatus fails on invalid status changes`() {

        val loanAccount = getLoanAccountState(aliceId.id.toString(), uniqueIdentifier, bank,
                0 of EUR, status = AccountStatus.SUSPENDED)

        transaction {
            // fails on transition from suspended to pending
            tweak {
                input(FinancialAccountContract.ID, loanAccount)
                output(FinancialAccountContract.ID, loanAccount.copy(accountData = loanAccount.accountData.copy(
                        status = AccountStatus.PENDING)))
                command(listOf(bank.publicKey), getSetAccountStateCommand(loanAccount.accountData.accountId, status = AccountStatus.PENDING))
                `fails with`("Account cannot progress from status: SUSPENDED to status PENDING")
            }
            // fails on transition from active to pending
            tweak {
                input(FinancialAccountContract.ID, loanAccount.copy(accountData = loanAccount.accountData.copy(
                        status = AccountStatus.ACTIVE)))
                output(FinancialAccountContract.ID, loanAccount.copy(accountData = loanAccount.accountData.copy(
                        status = AccountStatus.PENDING)))
                command(listOf(bank.publicKey), getSetAccountStateCommand(loanAccount.accountData.accountId, status = AccountStatus.PENDING))
                `fails with`("Account cannot progress from status: ACTIVE to status PENDING")
            }
        }
    }

    private fun getSetAccountStateCommand(accountId: UUID = aliceId.id, status: AccountStatus = AccountStatus.ACTIVE)
            = FinancialAccountContract.Commands.SetAccountStatus(accountId, status)
}