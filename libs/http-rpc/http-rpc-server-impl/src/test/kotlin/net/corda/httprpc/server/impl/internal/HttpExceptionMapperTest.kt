package net.corda.httprpc.server.impl.internal

import net.corda.httprpc.response.ResponseCode
import net.corda.httprpc.exception.BadRequestException
import net.corda.httprpc.exception.InternalServerException
import net.corda.httprpc.exception.ResourceNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HttpExceptionMapperTest {

    @Test
    fun `map to response BadRequestException with message`() {
        val e = BadRequestException("Invalid id.")

        val response = HttpExceptionMapper.mapToResponse(e)

        assertEquals(400, response.status)
        assertEquals(1, response.details.size)
        assertEquals(ResponseCode.BAD_REQUEST.name, response.details["code"])
        assertEquals("Invalid id.", response.message)
    }

    @Test
    fun `map to response BadRequestException with message and details`() {
        val e = BadRequestException("Invalid id.", mapOf("abc" to "def"))

        val response = HttpExceptionMapper.mapToResponse(e)

        assertEquals(400, response.status)
        assertEquals("Invalid id.", response.message)
        assertEquals(2, response.details.size)
        assertEquals("def", response.details["abc"])
        assertEquals(ResponseCode.BAD_REQUEST.name, response.details["code"])
    }

    @Test
    fun `test ResourceNotFoundException response`() {
        val e = ResourceNotFoundException("User", "userlogin123")

        val response = HttpExceptionMapper.mapToResponse(e)

        assertEquals(404, response.status)
        assertEquals("User 'userlogin123' not found.", response.message)
        assertEquals(1, response.details.size)
        assertEquals(ResponseCode.RESOURCE_NOT_FOUND.name, response.details["code"])
    }

    @Test
    fun `test InternalServerException response`() {
        val e = InternalServerException("message", mapOf("detail" to "someinfo"))

        val response = HttpExceptionMapper.mapToResponse(e)

        assertEquals(500, response.status)
        assertEquals("message", response.message)
        assertEquals(2, response.details.size)
        assertEquals("someinfo", response.details["detail"])
        assertEquals(ResponseCode.INTERNAL_SERVER_ERROR.name, response.details["code"])
    }
}