package com.r3.refapp.contracts

import com.r3.refapp.states.AccountStatus
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.states.RecurringPaymentState
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.contracts.*
import net.corda.testing.core.TestIdentity
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.*

class RecurringPaymentContractTest : AbstractContractTest() {

    private val accountFromId = UUID.randomUUID()
    private val accountToId = UUID.randomUUID()
    private val accountsMap = mutableMapOf<UUID, CurrentAccountState>()
    private val recurringPaymentId = UniqueIdentifier()

    @Test
    fun `verify CreateRecurringPayment happy path`() {
        transaction {
            output(RecurringPaymentContract.ID, getRecurringPayment())
            command(listOf(alice.publicKey, bob.publicKey), getCreateRecurringPaymentCommand())
            timeWindow(TimeWindow.fromOnly(Instant.now()))
            verifies()
        }
    }

    @Test
    fun `verify CreateRecurringPayment fails on command checks`() {
        transaction {

            timeWindow(TimeWindow.fromOnly(Instant.now()))

            // fails on command type check
            tweak {
                output(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), DummyCommand())
                `fails with`("Required com.r3.refapp.contracts.RecurringPaymentContract.Commands command")
            }

            // fails on number of commands check
            tweak {
                output(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), DummyCommand())
                command(listOf(alice.publicKey, bob.publicKey), getCreateRecurringPaymentCommand())
                `fails with`("Number of commands should be equal to 1 for CreateRecurringPayment")
            }

            // fails on accountTo signer check
            tweak {
                output(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey), getCreateRecurringPaymentCommand())
                `fails with`("Account to key must be in signers list for CreateRecurringPayment")
            }

            // fails on accountFrom signer check
            tweak {
                output(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(bob.publicKey), getCreateRecurringPaymentCommand())
                `fails with`("Account from key must be in signers list for CreateRecurringPayment")
            }
        }
    }

    @Test
    fun `verify CreateRecurringPayment fails on input checks`() {
        transaction {
            // fails on number of inputs
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment())
                output(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), getCreateRecurringPaymentCommand())
                `fails with`("Number of inputs should be equal to 0 for CreateRecurringPayment")
            }
        }
    }

    @Test
    fun `verify CreateRecurringPayment fails on time window checks`() {
        transaction {
            // fails on time window not set
            tweak {
                output(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), getCreateRecurringPaymentCommand())
                `fails with`("Transaction must have time window and from time set")
            }
            // fails on from only in time window not set
            tweak {
                output(RecurringPaymentContract.ID, getRecurringPayment())
                timeWindow(TimeWindow.untilOnly(Instant.now()))
                command(listOf(alice.publicKey, bob.publicKey), getCreateRecurringPaymentCommand())
                `fails with`("Transaction must have time window and from time set")
            }
        }
    }

    @Test
    fun `verify CreateRecurringPayment fails on output checks`() {
        transaction {

            timeWindow(TimeWindow.fromOnly(Instant.now()))
            // fails on number of outputs
            tweak {
                output(RecurringPaymentContract.ID, getRecurringPayment())
                output(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), getCreateRecurringPaymentCommand())
                `fails with`("Number of outputs should be equal to 1 for CreateRecurringPayment")
            }

            // fails on number of outputs of type
            tweak {
                output(RecurringPaymentContract.ID, DummyState())
                command(listOf(alice.publicKey, bob.publicKey), getCreateRecurringPaymentCommand())
                `fails with`("Number of outputs of type RecurringPaymentState should be equal to 1 for CreateRecurringPayment")
            }

            // fails on amount not greater than 0
            tweak {
                output(RecurringPaymentContract.ID, getRecurringPayment(amount = 0 of EUR))
                command(listOf(alice.publicKey, bob.publicKey), getCreateRecurringPaymentCommand())
                `fails with`("Amount should be greater than 0")
            }

            // fails on start date in past
            tweak {
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = Instant.now().minus(Duration.ofDays(1))))
                command(listOf(alice.publicKey, bob.publicKey), getCreateRecurringPaymentCommand())
                `fails with`("Start date cannot be in past")
            }

            // fails on from and to accounts should be different
            tweak {
                output(RecurringPaymentContract.ID, getRecurringPayment(accountToUuid = accountFromId))
                command(listOf(alice.publicKey, bob.publicKey), getCreateRecurringPaymentCommand())
                `fails with`("From and to accounts should be different")
            }
        }
    }

    @Test
    fun `verify ExecuteRecurringPayment happyPath`() {
        val dateStart = Instant.now()
        transaction {
            // verifies for iteration num not set
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart, iterationNum = null))
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(1)),
                        iterationNum = null))
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                verifies()
            }

            // verifies for iteration num decremented
            input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
            output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(1)),
                    iterationNum = 19))
            command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
            verifies()
        }
    }

    @Test
    fun `verify ExecuteRecurringPayment fails on command checks`() {
        val dateStart = Instant.now()
        transaction {
            // fails on command type check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(1))))
                command(listOf(alice.publicKey, bob.publicKey), DummyCommand())
                `fails with`("Required com.r3.refapp.contracts.RecurringPaymentContract.Commands command")
            }

            // fails on number of commands check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(1))))
                command(listOf(alice.publicKey, bob.publicKey), DummyCommand())
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Number of commands should be equal to 1 for ExecuteRecurringPayment")
            }

            // fails on account to signer check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(1))))
                command(listOf(alice.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Account to key must be in signers list for ExecuteRecurringPayment")
            }

            // fails on account from signer check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(1))))
                command(listOf(bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Account from key must be in signers list for ExecuteRecurringPayment")
            }
        }
    }

    @Test
    fun `verify ExecuteRecurringPayment fails on input checks`() {
        transaction {
            // fails on number of inputs
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment())
                input(RecurringPaymentContract.ID, getRecurringPayment())
                output(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Number of inputs should be equal to 1 for ExecuteRecurringPayment")
            }

            // fails on number of inputs of type
            tweak {
                input(RecurringPaymentContract.ID, DummyState())
                output(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Number of inputs of type RecurringPaymentState should be equal to 1 for ExecuteRecurringPayment")
            }
        }
    }

    @Test
    fun `verify ExecuteRecurringPayment fails on output checks`() {
        val dateStart = Instant.now()
        transaction {
            // fails on number of outputs
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(1))))
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(1))))
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Number of outputs should be equal to 1 for ExecuteRecurringPayment")
            }

            // fails on number of outputs of type
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
                output(RecurringPaymentContract.ID, DummyState())
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Number of outputs of type RecurringPaymentState should be equal to 1 for ExecuteRecurringPayment")
            }

            // fails on start date plus period check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(2)), iterationNum = 19))
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Start date should be incremented by the period, iteration num if set should be decremented, all other fields should be unchanged between Input and Output")
            }

            // fails on iteration num not decremented
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(1))))
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Start date should be incremented by the period, iteration num if set should be decremented, all other fields should be unchanged between Input and Output")
            }

            // fails on iteration num not set in input and set in output
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart, iterationNum = null))
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(1))))
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Start date should be incremented by the period, iteration num if set should be decremented, all other fields should be unchanged between Input and Output")
            }

            // fails on from and to accounts should be different
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
                output(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart.plus(Duration.ofDays(1)), accountToUuid = accountFromId))
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Start date should be incremented by the period, iteration num if set should be decremented, all other fields should be unchanged between Input and Output")
            }
        }
    }

    @Test
    fun `verify ExecuteRecurringPayment fails on in and out checks`() {
        val dateStart = Instant.now()
        transaction {

            // fails on amount changed between in and out
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
                output(RecurringPaymentContract.ID, getRecurringPayment(amount = 5 of EUR))
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Start date should be incremented by the period, iteration num if set should be decremented, all other fields should be unchanged between Input and Output")
            }

            // fails on account from changed between in and out
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment(dateStart = dateStart))
                output(RecurringPaymentContract.ID, getRecurringPayment(accountFromUuid = accountToId))
                command(listOf(alice.publicKey, bob.publicKey), getExecuteRecurringPaymentCommand())
                `fails with`("Start date should be incremented by the period, iteration num if set should be decremented, all other fields should be unchanged between Input and Output")
            }
        }
    }

    @Test
    fun `verify CancelRecurringPayment happy path`() {
        transaction {
            val recurringPayment = getRecurringPayment()
            input(RecurringPaymentContract.ID, recurringPayment)
            reference(RecurringPaymentContract.ID, getAccount(recurringPayment.accountTo))
            command(listOf(alice.publicKey, bob.publicKey), getCancelRecurringPaymentCommand())
            verifies()
        }
    }

    @Test
    fun `verify CancelRecurringPayment fails on command checks`() {
        transaction {
            reference(RecurringPaymentContract.ID, getAccount(UUID.randomUUID()))
            // verify fails on command type check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), DummyCommand())
                `fails with`("Contract verification failed: Required com.r3.refapp.contracts.RecurringPaymentContract.Commands command")
            }

            // verify fails on commands count check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), getCancelRecurringPaymentCommand())
                command(listOf(alice.publicKey, bob.publicKey), DummyCommand())
                `fails with`("Number of commands should be equal to 1 for CancelRecurringPayment")
            }

            // verify fails on account to required signer check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey), getCancelRecurringPaymentCommand())
                `fails with`("Account to key must be in signers list for CancelRecurringPayment")
            }

            // verify fails on account from required signer check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(bob.publicKey), getCancelRecurringPaymentCommand())
                `fails with`("Account from key must be in signers list for CancelRecurringPayment")
            }
        }
    }

    @Test
    fun `verify CancelRecurringPayment fails on input checks`() {
        transaction {
            // verify fails on inputs count check
            reference(RecurringPaymentContract.ID, getAccount(UUID.randomUUID()))
            tweak {
                output(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), getCancelRecurringPaymentCommand())
                `fails with`("Number of inputs should be equal to 1 for CancelRecurringPayment")
            }

            // verify fails on inputs type count check
            tweak {
                input(RecurringPaymentContract.ID, DummyState())
                command(listOf(alice.publicKey, bob.publicKey), getCancelRecurringPaymentCommand())
                `fails with`("Number of inputs of type RecurringPaymentState should be equal to 1 for CancelRecurringPayment")
            }

            // verify fails on referenced state not equal to account to in recurring payment
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), getCancelRecurringPaymentCommand())
                `fails with`("Recurring payment account to must be equal to referenced account to")
            }
        }
    }

    @Test
    fun `verify CancelRecurringPayment fails on output checks`() {
        transaction {
            // verify fails on outputs count check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment())
                reference(RecurringPaymentContract.ID, getAccount(UUID.randomUUID()))
                output(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), getCancelRecurringPaymentCommand())
                `fails with`("Number of outputs should be equal to 0 for CancelRecurringPayment")
            }
        }
    }

    @Test
    fun `verify CancelRecurringPayment fails on reference checks`() {
        transaction {
            // verify fails on reference count check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment())
                command(listOf(alice.publicKey, bob.publicKey), getCancelRecurringPaymentCommand())
                `fails with`("Number of referenced states should be equal to 1 for CancelRecurringPayment")
            }
            // verify fails on reference type count check
            tweak {
                input(RecurringPaymentContract.ID, getRecurringPayment())
                reference(RecurringPaymentContract.ID, DummyState())
                command(listOf(alice.publicKey, bob.publicKey), getCancelRecurringPaymentCommand())
                `fails with`("Number of referenced states of type Account should be equal to 1 for CancelRecurringPayment")
            }
        }
    }

    @Test
    fun `verify CancelRecurringPayment fails on account to loan account check`() {
        transaction {
            val recurringPayment = getRecurringPayment()
            input(RecurringPaymentContract.ID, recurringPayment)
            reference(RecurringPaymentContract.ID, getLoanAccountState("test acc",
                    accountIdentifier = UniqueIdentifier(null, recurringPayment.accountTo), amount = 10 of EUR))
            command(listOf(alice.publicKey, bob.publicKey), getCancelRecurringPaymentCommand())
            `fails with`("Recurring payment cannot be cancelled for loan repayments")
        }
    }

    @Test
    fun `verify CancelRecurringPayment fails on account to savings account check`() {
        transaction {
            val recurringPayment = getRecurringPayment()
            input(RecurringPaymentContract.ID, recurringPayment)
            reference(RecurringPaymentContract.ID, getSavingAccountState("test acc",
                    accountIdentifier = UniqueIdentifier(null, recurringPayment.accountTo)))
            command(listOf(alice.publicKey, bob.publicKey), getCancelRecurringPaymentCommand())
            timeWindow(Instant.now().minusSeconds(1))
            `fails with`("Recurring payment cannot be cancelled for saving repayments during savings period")
        }
    }

    private fun getCreateRecurringPaymentCommand() = RecurringPaymentContract.Commands.CreateRecurringPayment(alice.publicKey,
            bob.publicKey)

    private fun getExecuteRecurringPaymentCommand() = RecurringPaymentContract.Commands.ExecuteRecurringPayment(alice.publicKey,
            bob.publicKey)

    private fun getCancelRecurringPaymentCommand() = RecurringPaymentContract.Commands.CancelRecurringPayment(alice.publicKey,
            bob.publicKey)

    private fun getAccount(accountId: UUID, accBank: TestIdentity = bank, id: UniqueIdentifier = UniqueIdentifier()) =
            getCurrentAccount(accountId, accBank, id, balance = 15 of EUR, txDate = Instant.now(), accountStatus = AccountStatus.ACTIVE)

    private fun getRecurringPayment(accountFromUuid: UUID = accountFromId, accountToUuid: UUID = accountToId, accBank: TestIdentity = bank,
                                    amount: Amount<Currency> = 10 of EUR, dateStart: Instant = Instant.now().plus(Duration.ofHours(1)),
                                    period: Duration = Duration.ofDays(1),
                                    iterationNum: Int? = 20): RecurringPaymentState {

        val uniqueIdentifier = UniqueIdentifier()
        val accountFrom = accountsMap.computeIfAbsent(accountFromUuid) { getAccount(accountFromUuid, bank, uniqueIdentifier) }
        val accountTo = accountsMap.computeIfAbsent(accountToUuid) { getAccount(accountToUuid, accBank, if (accountFromUuid == accountToUuid) uniqueIdentifier else UniqueIdentifier()) }
        return RecurringPaymentState(accountFrom.accountData.accountId, accountTo.accountData.accountId, amount, dateStart, period, iterationNum, bank.party, recurringPaymentId)
    }

}