package com.r3.refapp.schemas

import com.r3.refapp.states.AccountStatus
import net.corda.core.schemas.PersistentStateRef
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AccountStateTests : AbstractEntityTest() {

    private val txDate = Instant.now()

    private val accountState1 = AccountStateSchemaV1.PersistentBalance(
            account = UUID.randomUUID(),
            balance = 0L,
            txDate = txDate,
            status = AccountStatus.PENDING,
            withdrawalDailyLimit = 100,
            transferDailyLimit = 1000,
            customerId = UUID.randomUUID(),
            linearId = UUID.randomUUID()
    )

    private val accountState2 = AccountStateSchemaV1.PersistentBalance(
            account = UUID.randomUUID(),
            balance = 1L,
            txDate = txDate.plusSeconds(1),
            status = AccountStatus.ACTIVE,
            withdrawalDailyLimit = 101,
            transferDailyLimit = 1001,
            overdraftBalance = 1L,
            overdraftLimit = 1L,
            customerId = UUID.randomUUID(),
            linearId = UUID.randomUUID()
    )

    @Before
    fun setup() {
        accountState2.stateRef = PersistentStateRef("txId", 1)
    }

    @Test
    fun `verify all fields in AccountState equals method`() {
        val clonedAccountState = clone(accountState1)

        assertNotEquals(accountState1, accountState2)
        assertEquals(accountState1, clonedAccountState)

        verifyWithEachPropertyChanged(accountState1, accountState2) {
            state1: AccountStateSchemaV1.PersistentBalance,
            state2: AccountStateSchemaV1.PersistentBalance -> assertNotEquals(state1, state2)
        }
    }

    @Test
    fun `verify all fields in AccountState hashCode method`() {
        val clonedAccountState = clone(accountState1)

        assertNotEquals(accountState1, accountState2)
        assertEquals(accountState1.hashCode(), clonedAccountState.hashCode())

        verifyWithEachPropertyChanged(accountState1, accountState2) {
            state1: AccountStateSchemaV1.PersistentBalance,
            state2: AccountStateSchemaV1.PersistentBalance -> assertNotEquals(state1.hashCode(), state2.hashCode())
        }
    }

}