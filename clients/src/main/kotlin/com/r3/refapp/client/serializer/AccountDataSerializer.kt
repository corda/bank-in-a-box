package com.r3.refapp.client.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.r3.refapp.states.*

/**
 * Provides custom json serialization for AcountData.
 */
class AccountDataSerializer(t: Class<AccountData>? = null) : StdSerializer<AccountData>(t) {
    /**
     * Serialization implementation
     * @param accountData to serialize
     * @param jgen Generator used to output resulting Json content
     * @param provider Provider that can be used to get serializers (not used for AccountData)
     */
    override fun serialize(accountData: AccountData, jgen: JsonGenerator, provider: SerializerProvider?) {
        val balance = accountData.balance
        jgen.writeStartObject()
        jgen.writeObjectField("accountId", accountData.accountId)
        jgen.writeObjectField("accountInfo", accountData.accountInfo)
        jgen.writeObjectField("customerId", accountData.customerId)
        jgen.writeStringField("balance",  balance.toDecimal().toString() + " of " + balance.token.currencyCode)
        jgen.writeObjectField("txDate", accountData.txDate)
        jgen.writeObjectField("status", accountData.status)

        jgen.writeEndObject()
    }
}