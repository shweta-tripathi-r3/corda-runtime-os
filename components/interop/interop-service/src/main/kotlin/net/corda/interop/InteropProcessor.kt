package net.corda.interop

import net.corda.data.CordaAvroDeserializer
import net.corda.data.CordaAvroSerializationFactory
import net.corda.data.CordaAvroSerializer
import net.corda.data.interop.InteropMessage
import net.corda.data.p2p.app.AppMessage
import net.corda.data.p2p.app.AuthenticatedMessage
import net.corda.data.p2p.app.AuthenticatedMessageHeader
import net.corda.interop.service.InteropAliasMappingService
import net.corda.interop.service.InteropMemberRegistrationService
import net.corda.interop.service.InteropMessageTransformer
import net.corda.messaging.api.processor.DurableProcessor
import net.corda.messaging.api.records.Record
import net.corda.schema.Schemas
import net.corda.virtualnode.read.VirtualNodeInfoReadService
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.UUID

//Based on FlowP2PFilter
@Suppress("Unused")
class InteropProcessor(cordaAvroSerializationFactory: CordaAvroSerializationFactory) :
    DurableProcessor<String, AppMessage> {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val SUBSYSTEM = "interop"
    }

    private val cordaAvroDeserializer: CordaAvroDeserializer<InteropMessage> =
        cordaAvroSerializationFactory.createAvroDeserializer({}, InteropMessage::class.java)
    private val cordaAvroSerializer: CordaAvroSerializer<InteropMessage> = cordaAvroSerializationFactory.createAvroSerializer {}
// when a new message comes in, after we decrypt then convert alias to real
    //add method (class) to extract real identity, output should match info needed to start a flow
    override fun onNext(
        events: List<Record<String, AppMessage>>
    ): List<Record<*, *>> {
        val outputEvents = mutableListOf<Record<*, *>>()
        events.forEach { appMessage ->
            //1. identity mapping params: alias identity (return real holding identity & groupId)
//            val realIdentity = getMapping()
            logger.info("This alias ${InteropMemberRegistrationService} is mapped to this real holding identity")
            //2. authorisation placeholder: sender's identity, real holding id of recipient, facade request
            //3. convert facade name to underlying flow name
            //4. interop processor sends start flow request
            val authMessage = appMessage.value?.message
            if (authMessage != null && authMessage is AuthenticatedMessage && authMessage.header.subsystem == SUBSYSTEM) {
                getOutputRecord(authMessage.header, authMessage.payload, appMessage.key)?.let { outputRecord ->
                    outputEvents.add(outputRecord)
                }
            }
        }
        return outputEvents
    }

    // Returns an OUTBOUND message to P2P layer, in the future it will pass a message to FlowProcessor
    private fun getOutputRecord(
        header: AuthenticatedMessageHeader,
        payload: ByteBuffer,
        key: String
    ): Record<String, AppMessage>? {
        val interopMessage  = cordaAvroDeserializer.deserialize(payload.array())
        //following logging is added just check serialisation/de-serialisation result and can be removed later
        logger.info ( "Processing message from p2p.in with subsystem $SUBSYSTEM. Key: $key, facade request: $interopMessage" )
        return if (interopMessage != null) {
            logger.info("The new destination: ${header.destination}")
            val facadeRequest = InteropMessageTransformer.getFacadeRequest(interopMessage)
            logger.info("Converted interop message to facade request : $facadeRequest")
            val message : InteropMessage = InteropMessageTransformer.getInteropMessage(interopMessage.messageId, facadeRequest)
            logger.info("Converted facade request to interop message : $message")
            Record(Schemas.P2P.P2P_OUT_TOPIC, key, generateAppMessage(header, message, cordaAvroSerializer))
        } else {
            null
        }
    }

    override val keyClass = String::class.java
    override val valueClass = AppMessage::class.java

    fun generateAppMessage(
        header: AuthenticatedMessageHeader,
        interopMessage: InteropMessage,
        interopMessageSerializer: CordaAvroSerializer<InteropMessage>
    ): AppMessage {
        val responseHeader = AuthenticatedMessageHeader(
            header.source,
            header.destination,
            header.ttl,
            header.messageId + "-" + UUID.randomUUID(),
            "",
            SUBSYSTEM
        )
        return AppMessage(
            AuthenticatedMessage(
                responseHeader,
                ByteBuffer.wrap(interopMessageSerializer.serialize(interopMessage))
            )
        )
    }

    fun getRealIdentity(recipientName: String) {
        //get real identity from alias member info
    }

    fun facadeToFlowMapper() {
        //placeholder method
    }

}
