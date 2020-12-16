package com.r3.refapp.contracts

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.refapp.states.*
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.ledger
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import java.time.Period
import java.util.*

enum class AccountTypeTest {
    CURRENT,
    OVERDRAFT,
    SAVINGS,
    LOAN
}

@RunWith(Parameterized::class)
class FinancialAccountContractTest(private val accountType: AccountTypeTest): AbstractContractTest() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
                arrayOf(AccountTypeTest.CURRENT),
                arrayOf(AccountTypeTest.OVERDRAFT),
                arrayOf(AccountTypeTest.SAVINGS),
                arrayOf(AccountTypeTest.LOAN)
          )
    }

    private val toAccount = getState(bob.name.organisation, bobId, amount = 20 of EUR)
    private val toAccountDifferentBank = getState(bob.name.organisation, bobId, bank2, amount = 20 of EUR)


    @Test
    fun `verify CreateIntrabankPayment happy path`() {
        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, toAccount)
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                output(FinancialAccountContract.ID, toAccount.deposit(5 of EUR))
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.verifies()
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on comand type check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, getState(bob.name.organisation, bobId, amount = 0 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                output(FinancialAccountContract.ID, getState(bob.name.organisation, bobId))
                command(listOf(alice.publicKey, bob.publicKey), DummyCommand())
                this.failsWith("Contract verification failed: Command not recognized")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on input check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                output(FinancialAccountContract.ID, getState(alice.name.organisation, aliceId))
                output(FinancialAccountContract.ID, getState(bob.name.organisation, bobId))
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Failed requirement: Number of inputs should be equal to 2 for CreateIntrabankPayment")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on output count check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, getState(bob.name.organisation, bobId, amount = 0 of EUR))
                output(FinancialAccountContract.ID, getState(alice.name.organisation, aliceId))
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Failed requirement: Number of outputs should be equal to 2 for CreateIntrabankPayment")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on inputs of type count check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, DummyState())
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(bobId.id, balance = 5 of EUR))
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Failed requirement: Number of inputs of type Account should be equal to 2 for CreateIntrabankPayment")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on outputs of type count check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, getState(bob.name.organisation, bobId, amount = 0 of EUR))
                output(FinancialAccountContract.ID, getState(bob.name.organisation, bobId))
                output(FinancialAccountContract.ID, DummyState())
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Failed requirement: Number of outputs of type Account should be equal to 2 for CreateIntrabankPayment")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on account from credit`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, getState(bob.name.organisation, bobId, amount = 0 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 7 of EUR))
                output(FinancialAccountContract.ID, getState(bob.name.organisation, bobId))
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Failed requirement: Account from balance should be credited by tx amount between input and output")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on account to debit`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, toAccount)
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                output(FinancialAccountContract.ID, getState(bob.name.organisation, bobId, amount = 7 of EUR))
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Failed requirement: Account to balance should be debited by tx amount between input and output")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on the same bank check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, toAccountDifferentBank)
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                output(FinancialAccountContract.ID, toAccountDifferentBank.deposit(5 of EUR))
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Failed requirement: Banks should be the same for both accounts")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on accountFrom input output bank check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, toAccount)
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, bank2, balance = 5 of EUR))
                output(FinancialAccountContract.ID, toAccount.deposit(5 of EUR))
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Failed requirement: Account from bank should not be changed between input and output")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on accountTo input output bank check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, toAccount)
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                output(FinancialAccountContract.ID, toAccountDifferentBank.deposit(5 of EUR))
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Failed requirement: Account to bank should not be changed between input and output")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on accountFrom signer check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, toAccount)
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                output(FinancialAccountContract.ID, toAccount.deposit(5 of EUR))
                command(listOf(bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Failed requirement: AccountFrom key must be in signers list for CreateIntrabankPayment")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on accountTo signer check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, toAccount)
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                output(FinancialAccountContract.ID, toAccount.deposit(5 of EUR))
                command(listOf(alice.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Failed requirement: AccountTo key must be in signers list for CreateIntrabankPayment")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on the same account check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 10 of EUR))
                input(FinancialAccountContract.ID, getState(alice.name.organisation, aliceId, amount = 0 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 5 of EUR))
                output(FinancialAccountContract.ID, getState(alice.name.organisation, aliceId))
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Collection contains more than one matching element")
            }
        }
    }

    @Test
    fun `verify CreateIntrabankPayment fails on balance check`() {

        ledgerServices.ledger {
            transaction {
                input(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 4 of EUR))
                input(FinancialAccountContract.ID, getState(bob.name.organisation, bobId, amount = 0 of EUR))
                output(FinancialAccountContract.ID, getCurrentAccount(aliceId.id, balance = 4 of EUR))
                output(FinancialAccountContract.ID, getState(bob.name.organisation, bobId))
                command(listOf(alice.publicKey, bob.publicKey), getIntrabankPaymentCommand())
                this.failsWith("Contract verification failed: Insufficient balance, missing 1.00 EUR")
            }
        }
    }

    private fun getIntrabankPaymentCommand(amount: Amount<Currency> = 5 of EUR) = FinancialAccountContract.Commands.CreateIntrabankPayment(amount, aliceId.id, bobId.id, alice.publicKey, bob.publicKey)
    private fun getAccount(accountName: String, accountIdentifier: UniqueIdentifier, accBank: TestIdentity) =
            AccountInfo(accountName, accBank.party, accountIdentifier)

    private fun getState(accountName: String, accountIdentifier: UniqueIdentifier, accBank: TestIdentity = bank, amount: Amount<Currency> = 5 of EUR): Account {
        val accountInfo = getAccount(accountName, accountIdentifier, accBank)
        return when(accountType) {
            AccountTypeTest.CURRENT -> CurrentAccountState(accountData = AccountData(accountInfo.identifier.id, accountInfo, UUID.randomUUID(), amount,
                    Instant.now(), AccountStatus.ACTIVE))
            AccountTypeTest.OVERDRAFT -> CurrentAccountState(accountData = AccountData(accountInfo.identifier.id, accountInfo, UUID.randomUUID(), amount,
                    Instant.now(), AccountStatus.ACTIVE), overdraftBalance = 0, approvedOverdraftLimit = 2000)
            AccountTypeTest.SAVINGS -> SavingsAccountState(accountData = AccountData(accountInfo.identifier.id, accountInfo, UUID.randomUUID(), amount,
                    Instant.now(), AccountStatus.ACTIVE), savingsEndDate = Instant.now(), period = Period.ofMonths(12))
            AccountTypeTest.LOAN -> LoanAccountState(accountData = AccountData(accountInfo.identifier.id, accountInfo, UUID.randomUUID(), amount,
                    Instant.now(), AccountStatus.ACTIVE))
        }
    }
}