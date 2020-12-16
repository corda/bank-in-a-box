package com.r3.refapp.client

import com.r3.refapp.client.utils.ControllerUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


@RunWith(MockitoJUnitRunner::class)
class CustomerControllerTest {

    companion object {
        private const val attachmentHash = "CC2716186BE065F2995044E8305DF7B6484EC21E4FBB721F59F20DBC71DECDEC"
    }

    @Test
    fun `test process string attachment success`() {
        val attachments = ControllerUtils.processStringAttachments(listOf("name:$attachmentHash"))

        assertEquals(attachments.size, 1)
        assertEquals(attachments.single().first.toString(), attachmentHash)
    }

    @Test
    fun `test empty attachment name fail`() {
        val message = assertFailsWith<IllegalArgumentException> {
            ControllerUtils.processStringAttachments(listOf(":$attachmentHash"))
        }.message!!
        assertEquals("attachment name must not be empty", message)
    }

    @Test
    fun `test invalid hex digit in attachment hash fail`() {
        val invalidAttachmentHash = attachmentHash.substring(0, attachmentHash.length - 6) + "ZZZZZZ"

        val message = assertFailsWith<IllegalArgumentException> {
            ControllerUtils.processStringAttachments(listOf("name:$invalidAttachmentHash"))
        }.message!!
        assertEquals("attachment hash is not a valid HEX String", message)
    }

    @Test
    fun `test invalid attachment hash length fail`() {
        val invalidAttachmentHashLength = attachmentHash.substring(0, attachmentHash.length - 1)

        val message = assertFailsWith<IllegalArgumentException> {
            ControllerUtils.processStringAttachments(listOf("name:$invalidAttachmentHashLength"))
        }.message!!
    }
}