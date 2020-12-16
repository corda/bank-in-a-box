package com.r3.refapp.contracts

import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.contracts.Amount
import net.corda.testing.node.transaction
import org.junit.Test
import java.util.*

class DepositFundsContractTest: AbstractContractTest() {

    @Test
    fun `verify DepositFunds successful path`() {
        ledgerServices.transaction {
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 15 of EUR))
                command(listOf(bank.publicKey), getDepositFundsCommand(amount = 5 of EUR))
                verifies()
            }
        }
    }

    @Test
    fun `verify DepositFunds fails on comand`() {
        ledgerServices.transaction() {
            //fail unknown command
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 15 of EUR))
                command(listOf(bank.publicKey), DummyCommand())
                `fails with`("Contract verification failed: Command not recognized")
            }

            //fail 0 deposit
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                command(listOf(bank.publicKey), getDepositFundsCommand(amount = 0 of EUR))
                `fails with`("Amount should be greater than 0")
            }
        }
    }

    @Test
    fun `verify DepositFunds fails on input check`() {

        ledgerServices.transaction {
            //fail with 0 input
            tweak {
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 15 of EUR))
                command(listOf(bank.publicKey), getDepositFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of inputs should be equal to 1 for DepositFunds")
            }

            // fail with DummyState type input
            tweak {
                input(FinancialAccountContract.ID, DummyState())
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 15 of EUR))
                command(listOf(bank.publicKey), getDepositFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of inputs of type Account should be equal to 1 for DepositFunds")
            }

            // fail with multiple input
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, getCurrentAccount(bobId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 15 of EUR))
                command(listOf(bank.publicKey), getDepositFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of inputs should be equal to 1 for DepositFunds")
            }
        }
    }

    @Test
    fun `verify DepositFunds fails on output check`() {
        ledgerServices.transaction {
            //fail on output count check
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                command(listOf(bank.publicKey), getDepositFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of outputs should be equal to 1 for DepositFunds")
            }

            //fail on output type count check
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, DummyState())
                command(listOf(bank.publicKey), getDepositFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of outputs of type Account should be equal to 1 for DepositFunds")
            }

            //fail to manny outputs
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                command(listOf(bank.publicKey), getDepositFundsCommand(amount = 5 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 15 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(bobId.id, balance = 15 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Number of outputs should be equal to 1 for DepositFunds")
            }
        }
    }

    @Test
    fun `verify DepositFunds fails on account balance inconsistency`() {
        ledgerServices.transaction() {
            // fail balance not incremented
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                command(listOf(bank.publicKey), getDepositFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Account balance should be credited by tx amount between the input and the output")
            }

            // fail balance not incremented correctly
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 20 of EUR))
                command(listOf(bank.publicKey), getDepositFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Account balance should be credited by tx amount between the input and the output")
            }

        }
    }

    @Test
    fun `verify DepositFunds fails on the same bank check`() {

        ledgerServices.transaction() {
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, accBank = bank2, balance = 15 of EUR))
                command(listOf(bank.publicKey), getDepositFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Account bank should not be changed between the input and the output")
            }
        }
    }

    @Test
    fun `verify DepositFunds fails on bank signer check`() {

        ledgerServices.transaction() {
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 15 of EUR))
                command(listOf(alice.publicKey), getDepositFundsCommand(amount = 5 of EUR))
                `fails with`("Contract verification failed: Failed requirement: Account bank key must be only signer for DepositFunds")
            }
        }
    }

    private fun getDepositFundsCommand(amount: Amount<Currency>) = FinancialAccountContract.Commands.DepositFunds(aliceId, amount)
}