package net.cordapp.demo.consensual

import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.marshalling.MarshallingService

class RPCRequestDataTestImpl( val body: String) : RPCRequestData {
    override fun getRequestBody(): String {
        return body
    }

    override fun <T> getRequestBodyAs(marshallingService: MarshallingService, clazz: Class<T>): T {
        return marshallingService.parse(body, clazz)
    }

    override fun <T> getRequestBodyAsList(marshallingService: MarshallingService, clazz: Class<T>): List<T> {
        return marshallingService.parseList(body, clazz)
    }
}