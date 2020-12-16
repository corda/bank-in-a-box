package com.r3.refapp.contracts

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.refapp.states.AccountStatus
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.contracts.UniqueIdentifier
import org.junit.Test
import java.time.Instant
import java.util.*

class SetLimitsContractTest : AbstractContractTest() {

    private val txDate = Instant.now()
    private val uniqueIdentifier = UniqueIdentifier()
    private val customerId = UUID.randomUUID()
    private val currentAccountIn = getCurrentAccount(aliceId.id, bank, id = uniqueIdentifier,
            txDate = txDate, linearId = uniqueIdentifier, customerId = customerId)
    private val currentAccountOut = getCurrentAccount(aliceId.id, bank, id = uniqueIdentifier,
            txDate = txDate, linearId = uniqueIdentifier, customerId = customerId, withdrawalDailyLimit = 100L,
            transferDailyLimit = 100L)

    @Test
    fun `verify SetLimits happy path for current and overdraft account types`() {

        transaction {

            // verifies for overdraft account
            tweak {
                input(FinancialAccountContract.ID, getOverdraftAccount(currentAccountIn))
                output(FinancialAccountContract.ID, getOverdraftAccount(currentAccountOut))
                command(listOf(bank.publicKey), getSetLimitsCommand())
                verifies()
            }

            // verifies for current account
            input(FinancialAccountContract.ID, currentAccountIn)
            output(FinancialAccountContract.ID, currentAccountOut)
            command(listOf(bank.publicKey), getSetLimitsCommand())
            verifies()
        }
    }

    @Test
    fun `verify SetLimits fails on command checks`() {

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
                command(listOf(bank.publicKey), getSetLimitsCommand())
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Contract verification failed: Failed requirement: Number of commands should be equal to 1 for SetLimits")
            }

            // fails on required signers check
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank2.publicKey), getSetLimitsCommand())
                `fails with`("Bank's key key must be only signer for SetLimits")
            }
        }
    }

    @Test
    fun `verify SetLimits fails on input checks`() {

        transaction {
            // fails on number of inputs
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Number of inputs should be equal to 1 for SetLimits")
            }

            // fails on number of inputs of type
            tweak {
                input(FinancialAccountContract.ID, DummyState())
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Number of inputs of type CurrentAccountState should be equal to 1 for SetLimits")
            }

            // fails on input loan account
            tweak {
                input(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Number of inputs of type CurrentAccountState should be equal to 1 for SetLimits")
            }

            // fails on input savings account
            tweak {
                input(FinancialAccountContract.ID, getSavingAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Number of inputs of type CurrentAccountState should be equal to 1 for SetLimits")
            }

            // fails on account id check
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), getSetLimitsCommand(bobId.id))
                `fails with`("Command account should be the same as input account")
            }
        }
    }

    @Test
    fun `verify SetLimits fails on out checks`() {

        transaction {
            // fails on number of outputs
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, currentAccountOut)
                output(FinancialAccountContract.ID, currentAccountOut)
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Number of outputs should be equal to 1 for SetLimits")
            }

            // fails on number of outputs of type
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, DummyState())
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Number of outputs of type CurrentAccountState should be equal to 1 for SetLimits")
            }

            // fails on output loan account
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Number of outputs of type CurrentAccountState should be equal to 1 for SetLimits")
            }

            // fails on output savings account
            tweak {
                input(FinancialAccountContract.ID, currentAccountIn)
                output(FinancialAccountContract.ID, getSavingAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Number of outputs of type CurrentAccountState should be equal to 1 for SetLimits")
            }
        }
    }

    @Test
    fun `verify SetLimits fails on in and out checks`() {

        val overdraftAccount = getOverdraftAccount(currentAccountIn)
        transaction {
            // fails on in and out accounts not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(accountData = overdraftAccount.accountData.copy(
                        accountInfo = AccountInfo("test", bank.party, UniqueIdentifier()))))
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Account's withdrawalDailyLimit and transferDailyLimit should be set to commands values, other fields must be unchanged")
            }

            // fails on in and out accounts not in same bank
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(accountData = overdraftAccount.accountData.copy(
                        accountInfo = AccountInfo(currentAccountIn.accountData.accountInfo.name, bank2.party,
                                currentAccountIn.accountData.accountInfo.identifier))))
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Account's withdrawalDailyLimit and transferDailyLimit should be set to commands values, other fields must be unchanged")
            }

            // fails on in and out balance not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(accountData = overdraftAccount.accountData.copy(
                        balance = 5 of EUR)))
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Account's withdrawalDailyLimit and transferDailyLimit should be set to commands values, other fields must be unchanged")
            }

            // fails on in and out customer not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(accountData = overdraftAccount.accountData.copy(
                        customerId = UUID.randomUUID())))
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Account's withdrawalDailyLimit and transferDailyLimit should be set to commands values, other fields must be unchanged")
            }

            // fails on in and out linear id not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(linearId = UniqueIdentifier()))
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Account's withdrawalDailyLimit and transferDailyLimit should be set to commands values, other fields must be unchanged")
            }

            // fails on in and out status not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(accountData = overdraftAccount.accountData.copy(
                        status = AccountStatus.ACTIVE)))
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Account's withdrawalDailyLimit and transferDailyLimit should be set to commands values, other fields must be unchanged")
            }

            // fails on in and out overdraftBalance not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(overdraftBalance = 2000))
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Account's withdrawalDailyLimit and transferDailyLimit should be set to commands values, other fields must be unchanged")
            }

            // fails on in and out overdraftLimit not same
            tweak {
                input(FinancialAccountContract.ID, overdraftAccount)
                output(FinancialAccountContract.ID, overdraftAccount.copy(approvedOverdraftLimit = 2000))
                command(listOf(bank.publicKey), getSetLimitsCommand())
                `fails with`("Account's withdrawalDailyLimit and transferDailyLimit should be set to commands values, other fields must be unchanged")
            }
        }
    }

    private fun getSetLimitsCommand(accountId: UUID = aliceId.id, withdrawalDailyLimit: Long = 100L,
                                    transferDailyLimit: Long = 100L) = FinancialAccountContract.Commands.SetLimits(
            accountId, withdrawalDailyLimit, transferDailyLimit)
}