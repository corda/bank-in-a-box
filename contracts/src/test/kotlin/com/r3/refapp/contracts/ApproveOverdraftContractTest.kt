package com.r3.refapp.contracts

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.refapp.util.of
import com.r3.refapp.util.EUR
import net.corda.core.contracts.UniqueIdentifier
import org.junit.Test
import java.util.*

class ApproveOverdraftContractTest : AbstractContractTest() {

    @Test
    fun `verify ApproveOverdraft happy path`() {
        transaction {
            val currentAccount = getCurrentAccount(aliceId.id, bank)
            input(FinancialAccountContract.ID, currentAccount)
            output(FinancialAccountContract.ID, getOverdraftAccount(currentAccount))
            command(listOf(bank.publicKey), getApproveOverdraftCommand())
            verifies()
        }
    }

    @Test
    fun `verify ApproveOverdraft fails on command checks`() {
        transaction {

            val currentAccount = getCurrentAccount(aliceId.id, bank)
            val overdraftAccount = getOverdraftAccount(currentAccount)

            // fails on command type check
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, overdraftAccount)
                command(listOf(bank.publicKey), DummyCommand())
                `fails with`("Contract verification failed: Command not recognized")
            }

            // fails on number of commands check
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, overdraftAccount)
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("Contract verification failed: Failed requirement: Number of commands should be equal to 1 for ApproveOverdraft")
            }

            // fails on required signers check
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, overdraftAccount)
                command(listOf(bank2.publicKey), getApproveOverdraftCommand())
                `fails with`("Account key key must be only signer for ApproveOverdraft")
            }
        }
    }

    @Test
    fun `verify ApproveOverdraft fails on input checks`() {

        val currentAccount = getCurrentAccount(aliceId.id, bank)
        val overdraftAccount = getOverdraftAccount(currentAccount)

        transaction {
            // fails on number of inputs
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, overdraftAccount)
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("Number of inputs should be equal to 1 for ApproveOverdraft")
            }

            // fails on number of inputs of type
            tweak {
                input(FinancialAccountContract.ID, DummyState())
                output(FinancialAccountContract.ID, overdraftAccount)
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("Number of inputs of type CurrentAccountState should be equal to 1 for ApproveOverdraft")
            }

            // fails on number of inputs of type
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, overdraftAccount)
                command(listOf(bank.publicKey), getApproveOverdraftCommand(bobId.id))
                `fails with`("Command account should be the same as input account")
            }
        }
    }

    @Test
    fun `verify ApproveOverdraft fails on out checks`() {

        val currentAccount = getCurrentAccount(aliceId.id, bank)
        val overdraftAccount = getOverdraftAccount(currentAccount)

        transaction {
            // fails on number of outputs
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount)
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("Number of outputs should be equal to 1 for ApproveOverdraft")
            }

            // fails on number of outputs of type
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, DummyState())
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("Number of outputs of type CurrentAccountState should be equal to 1 for ApproveOverdraft")
            }

            // fails on overdraft balance not equal to 0
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, getOverdraftAccount(currentAccount, overdraftBalance = 500))
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("Overdraft balance should be equal to 0")
            }

            // fails on overdraft limit not equal to command's value
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, getOverdraftAccount(currentAccount, approvedLimit = 1000))
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("Overdraft limit should be equal to command value")
            }
        }
    }

    @Test
    fun `verify ApproveOverdraft fails on in and out checks`() {

        val currentAccount = getCurrentAccount(aliceId.id, bank)

        transaction {
            // fails on in and out accounts not same
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, getOverdraftAccount(currentAccount, linearId = UniqueIdentifier()))
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("In and out accounts should have same fields")
            }

            // fails on in and out accounts not in same bank
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, getOverdraftAccount(currentAccount,
                        accountInfo = AccountInfo(currentAccount.accountData.accountInfo.name, bank2.party, currentAccount.accountData.accountInfo.identifier)))
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("In and out accounts should have same fields")
            }

            // fails on in and out balance not same
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, getOverdraftAccount(currentAccount, balance = 5 of EUR))
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("In and out accounts should have same fields")
            }

            // fails on in and out customer not same
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, getOverdraftAccount(currentAccount, customerId = UUID.randomUUID()))
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("In and out accounts should have same fields")
            }

            // fails on in and out linear id not same
            tweak {
                input(FinancialAccountContract.ID, currentAccount)
                output(FinancialAccountContract.ID, getOverdraftAccount(currentAccount, linearId = UniqueIdentifier()))
                command(listOf(bank.publicKey), getApproveOverdraftCommand())
                `fails with`("In and out accounts should have same fields")
            }
        }
    }

    private fun getApproveOverdraftCommand(accountId: UUID = aliceId.id, amount: Long = 500)
            = FinancialAccountContract.Commands.ApproveOverdraft(accountId, amount)
}