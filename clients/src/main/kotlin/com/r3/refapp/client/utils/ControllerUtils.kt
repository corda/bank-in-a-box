package com.r3.refapp.client.utils

import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import java.util.*


object ControllerUtils {

    private const val HEX_DIGITS = "0123456789abcdefABCDEF"
    private const val ATTACHMENT_HASH_LENGTH = 64
    private val availableCurrencies = Currency.getAvailableCurrencies().map { it.toString() }

    /**
     * Return [Amount] for quantity of currency units and currency code
     * @param quantity in currency units
     * @param tokenType currency code
     * @return [Currency]
     * @throws [IllegalArgumentException] if currency code is invalid
     */
    fun getAmountFromQuantityAndCurrency(quantity: Long, tokenType: String): Amount<Currency> {
        val currency = getCurrencyInstanceFromString(tokenType)
        return Amount(quantity, currency)
    }

    /**
     * Return [Currency] for currency code String.
     * @param tokenType currency code
     * @return [Currency]
     * @throws [IllegalArgumentException] if currency code is invalid
     */
    fun getCurrencyInstanceFromString(tokenType: String): Currency {
        if (!availableCurrencies.contains(tokenType)) {
            throw IllegalArgumentException("Invalid currency code '$tokenType', available currencies are: " +
                    "$availableCurrencies")
        }
        return Currency.getInstance(tokenType)
    }

    /**
     * Converts a list of string to a List of Pair<SecureHash, String>
     * @param attachments the strings are expected to be in nameStr:secureHash format otherwise the conversion will fail
     * @throws IllegalArgumentException The string are not in correct format or second string cannot be converted into a SecureHash
     */
    fun processStringAttachments(attachments: List<String>): List<Pair<SecureHash, String>> {
        return attachments.map {
            val ss = it.split(':')

            require(ss.size == 2) { "attachment $it not in a correct format, `name:secureHash` expected" }
            require(ss[0].isNotBlank()) { "attachment name must not be empty" }
            require(ss[1].length == ATTACHMENT_HASH_LENGTH) {
                "Incorrect attachment hash length (got ${ss[1].length}, expected ${ATTACHMENT_HASH_LENGTH})"
            }
            require(ss[1].all { char -> HEX_DIGITS.contains(char) }) { "attachment hash is not a valid HEX String" }

            Pair(SecureHash.parse(ss[1]), ss[0])
        }
    }
}