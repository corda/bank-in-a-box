package com.r3.refapp.contracts

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.refapp.util.of
import com.r3.refapp.util.EUR
import com.r3.refapp.states.AccountStatus
import com.r3.refapp.states.CurrentAccountState
import org.junit.Test
import java.time.Instant

class CreateCurrentAccountContractTest : AbstractContractTest() {

    private val accountInfo = AccountInfo(alice.name.organisation, bank.party, aliceId)
    private val accountTypeName = CurrentAccountState::class.simpleName
    private val commandTypeName = FinancialAccountContract.Commands.CreateCurrentAccount::class.simpleName

    @Test
    fun `test create account`() {
        transaction {
            output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, accountStatus = AccountStatus.PENDING))
            command(listOf(bank.publicKey, alice.publicKey), FinancialAccountContract.Commands.CreateCurrentAccount(alice.publicKey))
            verifies()
        }
    }

    @Test
    fun `test non zero balance fail`() {
        transaction {
            output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, id = aliceId, balance = 10 of EUR))
            command(listOf(bank.publicKey, alice.publicKey), FinancialAccountContract.Commands.CreateCurrentAccount(alice.publicKey))
            this `fails with` "$accountTypeName output state must have a zero balance"
        }
    }

    @Test
    fun `test account key non signer fail`() {
        transaction {
            output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, accountStatus = AccountStatus.PENDING))
            command(listOf(bank.publicKey), FinancialAccountContract.Commands.CreateCurrentAccount(alice.publicKey))
            this `fails with` "Account key key must be in signers list for CreateCurrentAccount"
        }
    }

    @Test
    fun `test incorrect command fail`() {
        // Required signers must contain the account owner [PublicKey]
        transaction {
            // transaction with 1 AccountState output and no command
            output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, id = aliceId))

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
            command(listOf(bank.publicKey, alice.publicKey), FinancialAccountContract.Commands.CreateCurrentAccount(alice.publicKey))

            tweak {
                // no outputs
                this `fails with` "A transaction must contain at least one input or output state"
            }

            tweak {
                // more than 1 output
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, id = aliceId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, id = aliceId))
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
            command(listOf(bank.publicKey, alice.publicKey), FinancialAccountContract.Commands.CreateCurrentAccount(alice.publicKey))
            input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, id = aliceId))
            this `fails with` "Number of inputs should be equal to 0 for $commandTypeName"
        }
    }

    @Test
    fun `test account active fail`() {
        val accountState = getCurrentAccount(
                accountId = accountInfo.identifier.id,
                balance = 0 of EUR,
                txDate = Instant.now()
        )

        transaction {
            output(FinancialAccountContract.ID, accountState)
            command(listOf(bank.publicKey, alice.publicKey), FinancialAccountContract.Commands.CreateCurrentAccount(alice.publicKey))
            this `fails with` "$accountTypeName output state must be in pending state"
        }
    }

    @Test
    fun `test all signers not present fail`() {
        val accountState = getCurrentAccount(
                accountId = accountInfo.identifier.id,
                balance = 0 of EUR,
                txDate = Instant.now()
        )

        transaction {
            output(FinancialAccountContract.ID, accountState)
            command(listOf(bank.publicKey, alice.publicKey, bob.publicKey), FinancialAccountContract.Commands.CreateCurrentAccount(alice.publicKey))
            this `fails with` "All signers from commands must be present in signerKeys"
        }
    }
}