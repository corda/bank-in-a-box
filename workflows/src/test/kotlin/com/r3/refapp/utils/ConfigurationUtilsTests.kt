package com.r3.refapp.utils

import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.utils.ConfigurationUtils.EXECPROV_THREAD_NUM
import com.r3.refapp.utils.ConfigurationUtils.NOTARY_NAME_PROPERTY
import net.corda.core.identity.CordaX500Name
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ConfigurationUtilsTests {

    lateinit var network: MockNetwork
    lateinit var bankA: StartedMockNode
    lateinit var bankLegalName: CordaX500Name

    @Rule
    @JvmField
    var name = TestName()

    @Before
    fun setup() {

        val (notaryName, propertyName) = when(name.methodName!!) {
            "test getConfiguredNotary happy path" -> Pair("O=Notary Service, L=Zurich, C=CH", NOTARY_NAME_PROPERTY)
            "test getConfiguredNotary fails with missing notary" -> Pair("O=Notary2 Service, L=Zurich, C=CH", NOTARY_NAME_PROPERTY)
            "test getConfiguredNotary fails with malformed notary name" -> Pair("AB=Malformed Name, DD=Malformed", NOTARY_NAME_PROPERTY)
            "test getConfiguredNotary fails with missing notary name config" -> Pair("O=Notary Service, L=Zurich, C=CH", "prop")
            else -> Pair("O=Notary Service, L=Zurich, C=CH", NOTARY_NAME_PROPERTY)
        }
        val mockNetworkParameters = MockNetworkParameters(
                cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                        TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"),
                        TestCordapp.findCordapp("com.r3.refapp.flows")
                                .withConfig(mapOf(propertyName to notaryName,
                                                EXECPROV_THREAD_NUM to 2
                                        ))
                )
        )
        network = MockNetwork(mockNetworkParameters)

        bankLegalName = CordaX500Name(organisation = "BankA", locality = "London", country = "GB")
        bankA = network.createNode(MockNodeParameters(legalName = bankLegalName))

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `test getConfiguredNotary happy path`() {
        val notary = ConfigurationUtils.getConfiguredNotary(bankA.services)
        assertNotNull(notary)
        assertEquals(CordaX500Name("Notary Service", "Zurich", "CH"), notary.name)
    }

    @Test
    fun `test getConfiguredNotary fails with missing notary`() {
        val message = assertFailsWith<RefappException> {
            ConfigurationUtils.getConfiguredNotary(bankA.services)
        }.message!!
        assertEquals("Refapp exception: Notary not found for config property: refapp_notary with value O=Notary2 Service, L=Zurich, C=CH. Please check your configuration!", message)
    }

    @Test
    fun `test getConfiguredNotary fails with malformed notary name`() {
        val message = assertFailsWith<RefappException> {
            ConfigurationUtils.getConfiguredNotary(bankA.services)
        }.message!!
        assertEquals("Refapp exception: Malformed CordaX500Name: AB=Malformed Name, DD=Malformed in config property: refapp_notary. Please check your configuration!", message)
    }

    @Test
    fun `test getConfiguredNotary fails with missing notary name config`() {
        val message = assertFailsWith<RefappException> {
            ConfigurationUtils.getConfiguredNotary(bankA.services)
        }.message!!
        assertEquals("Refapp exception: Missing required configuration property: refapp_notary. Please check your configuration!", message)
    }
}