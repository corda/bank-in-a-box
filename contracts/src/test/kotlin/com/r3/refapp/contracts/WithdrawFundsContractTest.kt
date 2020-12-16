package com.r3.refapp.contracts

import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.contracts.Amount
import net.corda.testing.node.transaction
import org.junit.Test
import java.util.*

class WithdrawFundsContractTest: AbstractContractTest() {

    @Test
    fun `verify WithdrawFunds successful path`() {
        ledgerServices.transaction {
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 5 of EUR))
                verifies()
            }
        }
    }

    @Test
    fun `verify WithdrawFunds fails on command`() {
        ledgerServices.transaction {
            // fail unknown command type
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                command(listOf(bank.publicKey), DummyCommand())
                `fails with`("Contract verification failed: Command not recognized")
            }

            // fail 0 amount command
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 0 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Amount should be greater than 0")
            }
        }
    }

    @Test
    fun `verify WithdrawFunds fails on input check`() {
        ledgerServices.transaction {
            // fail no input
            tweak {
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 15 of EUR))
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of inputs should be equal to 1 for WithdrawFunds")
            }

            // fail unknown input type
            tweak {
                input(FinancialAccountContract.ID, DummyState())
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of inputs of type CreditAccount should be equal to 1 for WithdrawFunds")
            }
            // fail multiple input types
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, getCurrentAccount(bobId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of inputs should be equal to 1 for WithdrawFunds")
            }
        }
    }

    @Test
    fun `verify WithdrawFunds fails on output check`() {
        ledgerServices.transaction {
            // fail on no output
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of outputs should be equal to 1 for WithdrawFunds")
            }

            // fail on type count check
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, DummyState())
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of outputs of type CreditAccount should be equal to 1 for WithdrawFunds")
            }

            // fail on to manny outputs
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(bobId.id, balance = 5 of EUR))
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of outputs should be equal to 1 for WithdrawFunds")
            }
        }
    }

    @Test
    fun `verify WithdrawFunds fails on account balance`() {
        ledgerServices.transaction {
            // fail on account balance not decremented
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Account balance should be debited by tx amount between the input and the output")
            }

            // fail on not enough balance
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 0 of EUR))
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 50 of EUR))
                `fails with`("Contract verification failed: Insufficient balance, missing 40.00 EUR")
            }

            // fail balance not decremented correctly
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 8 of EUR))
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Account balance should be debited by tx amount between the input and the output")
            }
        }
    }

    @Test
    fun `verify WithdrawFunds fails on same bank check`() {
        ledgerServices.transaction {
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, bank2, balance = 5 of EUR))
                command(listOf(bank.publicKey), getWithdrawFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Account bank should not be changed between the input and the output")
            }
        }
    }

    @Test
    fun `verify WithdrawFunds fails on bank signer check`() {
        ledgerServices.transaction {
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                command(listOf(alice.publicKey), getWithdrawFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Account bank key must be only signer for WithdrawFunds")
            }
        }
    }

    private fun getWithdrawFundsCommand(amount: Amount<Currency>) = FinancialAccountContract.Commands.WithdrawFunds(aliceId, amount)
}