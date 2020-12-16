package com.r3.refapp.util

import net.corda.core.contracts.Amount
import java.util.*
import kotlin.math.pow

val GBP: Currency = Currency.getInstance("GBP")
val EUR: Currency = Currency.getInstance("EUR")

infix fun Number.of(currency: Currency) =
        Amount((this.toDouble() * 10.toDouble().pow(currency.defaultFractionDigits.toDouble())).toLong(), currency)