package com.r3.refapp.schemas

import org.junit.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TransactionLogTest : AbstractEntityTest() {

    private val txDate = Instant.now()

    private val transactionLog1 = TransactionLogSchemaV1.TransactionLog(
            txId = "tx1234",
            accountFrom = UUID.randomUUID(),
            accountTo = UUID.randomUUID(),
            amount = 1000,
            currency = "EUR",
            txDate = txDate,
            txType = TransactionType.TRANSFER
    )

    private val transactionLog2 = TransactionLogSchemaV1.TransactionLog(
            txId = "tx5678",
            accountFrom = UUID.randomUUID(),
            accountTo = null,
            amount = 2000,
            currency = "GBP",
            txDate = txDate.plusSeconds(1),
            txType = TransactionType.WITHDRAWAL
    )

    @Test
    fun `verify all fields in equals method`() {
        val cloneTransactionLog1 = clone(transactionLog1)

        assertNotEquals(transactionLog1, transactionLog2)
        assertEquals(transactionLog1, cloneTransactionLog1)

        verifyWithEachPropertyChanged(transactionLog1, transactionLog2) {
            log1: TransactionLogSchemaV1.TransactionLog,
            log2: TransactionLogSchemaV1.TransactionLog -> assertNotEquals(log1, log2)
        }
    }

    @Test
    fun `verify all fields in hashCode method`() {
        val cloneTransactionLog1 = clone(transactionLog1)

        assertNotEquals(transactionLog1.hashCode(), transactionLog2.hashCode())
        assertEquals(transactionLog1.hashCode(), cloneTransactionLog1.hashCode())

        verifyWithEachPropertyChanged(transactionLog1, transactionLog2) {
            log1: TransactionLogSchemaV1.TransactionLog,
            log2: TransactionLogSchemaV1.TransactionLog -> assertNotEquals(log1.hashCode(), log2.hashCode())
        }
    }
}