package com.r3.refapp.contracts

import com.r3.refapp.states.AccountStatus
import com.r3.refapp.states.CreditRatingInfo
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.contracts.Amount
import net.corda.core.contracts.TimeWindow
import net.corda.testing.node.transaction
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.*

class IssueLoanContractTest: AbstractContractTest() {

    private val txTimeWindow = TimeWindow.between(Instant.now(), Instant.now().plus(Duration.ofHours(2)))

    @Test
    fun `verify IssueLoan successful path`() {
        ledgerServices.transaction {
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                verifies()
            }
        }
    }

    @Test
    fun `verify IssueLoan fails on command`() {
        ledgerServices.transaction() {
            //fail unknown command
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), DummyCommand())
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Command not recognized")
            }

            //fail 0 loan
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 0 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 0 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Loan amount should be greater than 0")
            }
        }
    }

    @Test
    fun `verify IssueLoan fails on input check`() {
        ledgerServices.transaction {
            //fail with 0 input
            tweak {
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Number of inputs should be equal to 1 for IssueLoan")
            }

            // fail with DumyState type input
            tweak {
                input(FinancialAccountContract.ID, DummyState())
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Number of inputs of type CurrentAccountState should be equal to 1 for IssueLoan")
            }

            // fail with multiple input
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                input(FinancialAccountContract.ID, getCurrentAccount(bobId.id, balance = 10 of EUR, customerId = bobCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Number of inputs should be equal to 1 for IssueLoan")
            }
        }
    }

    @Test
    fun `verify IssueLoan fails on output check`() {
        ledgerServices.transaction {
            //fail on no Account output
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Number of outputs should be equal to 2 for IssueLoan")
            }

            //fail on no LoanAccount output
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Number of outputs should be equal to 2 for IssueLoan")
            }

            //fail on output type check         }
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, DummyState())
                output(FinancialAccountContract.ID, DummyState())
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Number of outputs of type CurrentAccountState should be equal to 1 for IssueLoan")
            }

            //fail to manny outputs
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                output(FinancialAccountContract.ID, getLoanAccountState(bob.name.organisation, bobId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Number of outputs should be equal to 2 for IssueLoan")
            }
        }
    }

    @Test
    fun `verify IssueLoan fails on account balance inconsistency`() {
        ledgerServices.transaction() {
            // fail account balance not incremented
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Current account balance should be credited with an amount that is equal to the output loan balance")
            }

            // fail account balance not incremented correctly
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 45 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Current account balance should be credited with an amount that is equal to the output loan balance")
            }

            // fail loan account balance not correct
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 35 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Command loan amount should be equal to the output loan account balance")
            }
        }
    }

    @Test
    fun `verify IssueLoan fails on the same bank check`() {
        ledgerServices.transaction() {
            // fail on current account not having the same bank
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, accBank = bank2, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Account bank should not be changed between the input and the output")
            }

            // fail on loan account not having the same bank
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR, accBank = bank2))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Loan Account bank should be the same as Current Account bank")
            }
        }
    }

    @Test
    fun `verify IssueLoan fails Account is not active`() {
        ledgerServices.transaction {
            // fail in account is suspended
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, accountStatus = AccountStatus.SUSPENDED, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: In Account should be active")
            }

            // fail out account is suspended
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, accountStatus = AccountStatus.SUSPENDED, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Out Account should be active")
            }

            // fail out loan is suspended
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR, status = AccountStatus.SUSPENDED))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Out Loan Account should be active")
            }
        }
    }

    @Test
    fun `verify IssueLoan fails on bank signer check`() {
        ledgerServices.transaction() {
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(alice.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(alice.publicKey, oracle.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Account bank key must be in signers list for IssueLoan")
            }
        }
    }

    @Test
    fun `verify IssueLoan fails on missing oracle signature`() {
        ledgerServices.transaction {
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey), getPassingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Oracle key must be in signers list for IssueLoan")
            }
        }
    }

    @Test
    fun `verify IssueLoan fails on credit rating checks`() {
        ledgerServices.transaction {
            // verify IssueLoan fails on not enough credit rating
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getFailingCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Customer credit rating should be greater than the credit rating threshold")
            }

            //verify IssueLoan fails on expired credit rating
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getExpiredStartCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Customer credit rating validity start time cannot be after transaction TimeWindow start time")
            }

            //verify IssueLoan fails on expired credit rating
            tweak {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 55 of EUR, customerId = aliceCustomerId))
                output(FinancialAccountContract.ID, getLoanAccountState(alice.name.organisation, aliceId, amount = 45 of EUR))
                command(listOf(bank.publicKey), getIssueLoanCommand(amount = 45 of EUR))
                command(listOf(bank.publicKey, oracle.publicKey), getExpiredEndCreditRatingCmd(aliceCustomerId))
                timeWindow(txTimeWindow)
                `fails with`("Contract verification failed: Failed requirement: Customer credit rating validity end time cannot be before transaction TimeWindow end time")
            }
        }
    }

    private fun getIssueLoanCommand(amount: Amount<Currency>) = FinancialAccountContract.Commands.IssueLoan(aliceId, amount)
    private fun getPassingCreditRatingCmd(customerId: UUID) = FinancialAccountContract.Commands.VerifyCreditRating(CreditRatingInfo("", customerId, 100, txTimeWindow.fromTime!! ), 50, oracle.publicKey, txTimeWindow.fromTime!!, Duration.ofHours(2) )
    private fun getFailingCreditRatingCmd(customerId: UUID) = FinancialAccountContract.Commands.VerifyCreditRating(CreditRatingInfo("", customerId, 49, txTimeWindow.fromTime!!), 50, oracle.publicKey, txTimeWindow.fromTime!!, Duration.ofHours(2) )
    private fun getExpiredStartCreditRatingCmd(customerId: UUID) = FinancialAccountContract.Commands.VerifyCreditRating(CreditRatingInfo("", customerId, 100, txTimeWindow.fromTime!!), 50, oracle.publicKey, txTimeWindow.fromTime!!.plus(Duration.ofHours(1)) , Duration.ofHours(2))
    private fun getExpiredEndCreditRatingCmd(customerId: UUID) = FinancialAccountContract.Commands.VerifyCreditRating(CreditRatingInfo("", customerId, 100, txTimeWindow.fromTime!!), 50, oracle.publicKey, txTimeWindow.fromTime!!, Duration.ofHours(1))
}