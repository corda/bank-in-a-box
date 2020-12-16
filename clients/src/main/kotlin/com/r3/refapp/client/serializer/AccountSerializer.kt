package com.r3.refapp.client.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.r3.refapp.states.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Provides custom json serialization for states implementing the Account interface.
 * @param type handled type
 */
class AccountSerializer(type: Class<Account>? = null) : StdSerializer<Account>(type) {

    /**
     * Serialization implementation
     * @param account to serialize
     * @param jgen Generator used to output resulting Json content
     * @param provider Provider that can be used to get serializers
     */
    override fun serialize(account: Account, jgen: JsonGenerator, provider: SerializerProvider?) {
        val accountProperties = account::class.memberProperties.map { it as  KProperty1<Account, *>}
        jgen.writeStartObject()
        for (property in accountProperties) {
            val value = property.get(account)
            jgen.writeObjectField(property.name, value)
        }
        val type = getAccountTypeStr(account)
        jgen.writeObjectField("type", type)
        jgen.writeEndObject()
    }
    /**
     * Returns the account's type as string
     */
    private fun getAccountTypeStr(account: Account): String {
        return when(account) {
            is CurrentAccountState -> "current"
            is LoanAccountState -> "loan"
            is SavingsAccountState -> "savings"
            else -> "undefined"
        }
    }
}