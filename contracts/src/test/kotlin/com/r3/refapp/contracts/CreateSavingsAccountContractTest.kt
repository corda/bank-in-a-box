package com.r3.refapp.contracts

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.refapp.states.AccountStatus
import com.r3.refapp.states.SavingsAccountState
import com.r3.refapp.util.of
import com.r3.refapp.util.EUR
import org.junit.Test
import java.security.PublicKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class CreateSavingsAccountContractTest : AbstractContractTest() {

    private val accountInfo = AccountInfo(alice.name.organisation, bank.party, aliceId)
    private val accountTypeName = SavingsAccountState::class.simpleName
    private val commandTypeName = FinancialAccountContract.Commands.CreateSavingsAccount::class.simpleName
    private val savingsStartDate = Instant.now()
    private val savingsEndDate = LocalDateTime.ofInstant(savingsStartDate, ZoneId.systemDefault())
            .plusMonths(12L).atZone(ZoneId.systemDefault()).toInstant()

    @Test
    fun `test create account success`() {
        transaction {
            output(FinancialAccountContract.ID, getSavingAccountState(aliceId.id.toString(), savingsEndDate = savingsEndDate))
            command(listOf(bank.publicKey, alice.publicKey), getCreateSavingsAccountCommand())
            verifies()
        }
    }

    @Test
    fun `test non zero balance fail`() {
        transaction {
            val savingsAccountState = getSavingAccountState(
                    accountName = aliceId.id.toString(),
                    amount = 10 of EUR,
                    savingsEndDate = savingsEndDate)

            output(FinancialAccountContract.ID, savingsAccountState)
            command(listOf(bank.publicKey, alice.publicKey), getCreateSavingsAccountCommand())
            this `fails with` "$accountTypeName output state must have a zero balance"
        }
    }

    @Test
    fun `test period not set to command value fail`() {
        transaction {
            output(FinancialAccountContract.ID, getSavingAccountState(aliceId.id.toString(), savingsEndDate = savingsEndDate))
            command(listOf(bank.publicKey, alice.publicKey), getCreateSavingsAccountCommand(period = 11))
            this `fails with` "Savings period should be set to commands value"
        }
    }

    @Test
    fun `test end date not equal to start date plus period fail`() {
        transaction {
            output(FinancialAccountContract.ID, getSavingAccountState(aliceId.id.toString(), savingsEndDate = Instant.now()))
            command(listOf(bank.publicKey, alice.publicKey), getCreateSavingsAccountCommand())
            this `fails with` "Savings end date should be set to commands startDate plus savings period"
        }
    }

    @Test
    fun `test account key non signer fail`() {
        transaction {
            output(FinancialAccountContract.ID, getSavingAccountState(aliceId.id.toString(), savingsEndDate = savingsEndDate))
            command(listOf(bank.publicKey), getCreateSavingsAccountCommand())
            this `fails with` "Account key key must be in signers list for CreateSavingsAccount"
        }
    }

    @Test
    fun `test incorrect command fail`() {
        // Required signers must contain the account owner [PublicKey]
        transaction {
            // transaction with 1 AccountState output and no command
            output(FinancialAccountContract.ID, getSavingAccountState(aliceId.id.toString(), savingsEndDate = savingsEndDate))

            tweak {
                // Test no command fail
                this `fails with` "A transaction must contain at least one command"
            }

            tweak {
                command(listOf(bank.publicKey, alice.publicKey), DummyCommand())
                this `fails with` "Contract verification failed: Command not recognized"
            }
        }
    }

    @Test
    fun `test incorrect output fail`() {
        transaction {
            command(listOf(bank.publicKey, alice.publicKey), getCreateSavingsAccountCommand())

            tweak {
                // no outputs
                this `fails with` "A transaction must contain at least one input or output state"
            }

            tweak {
                // more than 1 output
                output(FinancialAccountContract.ID, getSavingAccountState(aliceId.id.toString(), savingsEndDate = savingsEndDate))
                output(FinancialAccountContract.ID, getSavingAccountState(aliceId.id.toString(), savingsEndDate = savingsEndDate))
                this `fails with` "Number of outputs should be equal to 1 for $commandTypeName"
            }

            tweak {
                // output of incorrect type
                output(FinancialAccountContract.ID, DummyState())
                this `fails with` "Number of outputs of type $accountTypeName should be equal to 1 for $commandTypeName"
            }
        }
    }

    @Test
    fun `test incorrect number of inputs fail`() {
        transaction {
            command(listOf(bank.publicKey, alice.publicKey), getCreateSavingsAccountCommand())
            input(FinancialAccountContract.ID, getSavingAccountState(aliceId.id.toString(), savingsEndDate = savingsEndDate))
            this `fails with` "Number of inputs should be equal to 0 for $commandTypeName"
        }
    }

    @Test
    fun `test account active fail`() {
        transaction {
            output(FinancialAccountContract.ID, getSavingAccountState(aliceId.id.toString(), savingsEndDate = savingsEndDate,
                    status = AccountStatus.ACTIVE))
            command(listOf(bank.publicKey, alice.publicKey), getCreateSavingsAccountCommand())
            this `fails with` "$accountTypeName output state must be in pending state"
        }
    }

    @Test
    fun `test all signers not present fail`() {
        transaction {
            output(FinancialAccountContract.ID, getSavingAccountState(aliceId.id.toString(), savingsEndDate = savingsEndDate))
            command(listOf(bank.publicKey, alice.publicKey, bob.publicKey), getCreateSavingsAccountCommand())
            this `fails with` "All signers from commands must be present in signerKeys"
        }
    }

    private fun getCreateSavingsAccountCommand(accountKey: PublicKey = alice.publicKey, period: Int = 12)
            = FinancialAccountContract.Commands.CreateSavingsAccount(accountKey, period, savingsStartDate)
}