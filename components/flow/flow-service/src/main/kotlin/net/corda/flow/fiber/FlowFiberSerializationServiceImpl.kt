package net.corda.flow.fiber

import java.io.NotSerializableException
import net.corda.v5.application.serialization.SerializationService
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.util.castIfPossible
import net.corda.v5.base.util.contextLogger
import net.corda.v5.serialization.SerializedBytes
import net.corda.v5.serialization.SingletonSerializeAsToken
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference

@Component(service = [FlowFiberSerializationService::class, SingletonSerializeAsToken::class])
class FlowFiberSerializationServiceImpl @Activate constructor(
    @Reference(service = FlowFiberService::class)
    private val flowFiberService: FlowFiberService
): FlowFiberSerializationService, SingletonSerializeAsToken {

    private companion object {
        private val log = contextLogger()
    }

    private fun getSerializationService(): SerializationService {
        return flowFiberService.getExecutingFiber().getExecutionContext().run {
            sandboxGroupContext.amqpSerializer
        }
    }

    override fun <R : Any> deserializePayload(bytes: ByteArray, expectedType: Class<R>): R {
        return try {
            val payload = getSerializationService().deserialize(bytes, expectedType)
            checkPayloadIs(payload, expectedType)
            payload
        } catch (e: NotSerializableException) {
            log.info("Received a payload but failed to deserialize it into a ${expectedType.name}", e)
            throw e
        }
    }

    /**
     * AMQP deserialization outputs an object whose type is solely based on the serialized content, therefore although the generic type is
     * specified, it can still be the wrong type. We check this type here, so that we can throw an accurate error instead of failing later
     * on when the object is used.
     */
    private fun <R : Any> checkPayloadIs(payload: Any, receiveType: Class<R>) {
        receiveType.castIfPossible(payload) ?: throw CordaRuntimeException(
            "Expecting to receive a ${receiveType.name} but received a ${payload.javaClass.name} instead, payload: ($payload)"
        )
    }

    override fun <T : Any> serialize(obj: T): SerializedBytes<T> {
        return getSerializationService().serialize(obj)
    }
}