package net.corda.flow.fiber

import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.marshalling.MarshallingService

/**
 * Read the start args from the start context. This prevents the full start args from being serialized on every
 * checkpoint for an RPC started flow.
 */
class RPCRequestDataImpl(private val fiberService: FlowFiberService) : RPCRequestData {

    companion object {
        private const val MAX_STRING_LENGTH = 200
    }

    override fun getRequestBody(): String {
        return fiberService.getExecutingFiber().getExecutionContext().flowCheckpoint.flowStartContext.startArgs
            ?: throw IllegalStateException("Failed to find the start args for RPC started flow")
    }

    override fun <T> getRequestBodyAs(marshallingService: MarshallingService, clazz: Class<T>): T {
        return marshallingService.parse(requestBody, clazz)
    }

    override fun <T> getRequestBodyAsList(marshallingService: MarshallingService, clazz: Class<T>): List<T> {
        return marshallingService.parseList(requestBody, clazz)
    }

    override fun toString(): String {
        // Truncate the JSON object to ensure that we don't try and write too much data into logs.
        return "RPCRequestData(input=${requestBody.take(MAX_STRING_LENGTH)})"
    }
}