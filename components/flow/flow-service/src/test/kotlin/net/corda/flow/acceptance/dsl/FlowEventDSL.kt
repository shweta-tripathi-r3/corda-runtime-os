package net.corda.flow.acceptance.dsl

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import net.corda.data.flow.event.FlowEvent
import net.corda.data.flow.event.StartFlow
import net.corda.data.flow.state.Checkpoint
import net.corda.flow.acceptance.getBasicFlowStartContext
import net.corda.flow.fiber.FlowContinuation
import net.corda.flow.fiber.FlowIORequest
import net.corda.flow.pipeline.factory.impl.FlowEventPipelineFactoryImpl
import net.corda.flow.pipeline.factory.impl.FlowMessageFactoryImpl
import net.corda.flow.pipeline.factory.impl.FlowRecordFactoryImpl
import net.corda.flow.pipeline.handlers.events.SessionEventHandler
import net.corda.flow.pipeline.handlers.events.StartFlowEventHandler
import net.corda.flow.pipeline.handlers.events.WakeupEventHandler
import net.corda.flow.pipeline.handlers.requests.FlowFailedRequestHandler
import net.corda.flow.pipeline.handlers.requests.FlowFinishedRequestHandler
import net.corda.flow.pipeline.handlers.requests.ForceCheckpointRequestHandler
import net.corda.flow.pipeline.handlers.requests.InitialCheckpointRequestHandler
import net.corda.flow.pipeline.handlers.requests.SleepRequestHandler
import net.corda.flow.pipeline.handlers.requests.SubFlowFailedRequestHandler
import net.corda.flow.pipeline.handlers.requests.SubFlowFinishedRequestHandler
import net.corda.flow.pipeline.handlers.requests.sessions.CloseSessionsRequestHandler
import net.corda.flow.pipeline.handlers.requests.sessions.GetFlowInfoRequestHandler
import net.corda.flow.pipeline.handlers.requests.sessions.InitiateFlowRequestHandler
import net.corda.flow.pipeline.handlers.requests.sessions.ReceiveRequestHandler
import net.corda.flow.pipeline.handlers.requests.sessions.SendAndReceiveRequestHandler
import net.corda.flow.pipeline.handlers.requests.sessions.SendRequestHandler
import net.corda.flow.pipeline.handlers.requests.sessions.WaitForSessionConfirmationsRequestHandler
import net.corda.flow.pipeline.handlers.waiting.StartFlowWaitingForHandler
import net.corda.flow.pipeline.handlers.waiting.WakeupWaitingForHandler
import net.corda.flow.pipeline.handlers.waiting.sessions.SessionConfirmationWaitingForHandler
import net.corda.flow.pipeline.handlers.waiting.sessions.SessionDataWaitingForHandler
import net.corda.flow.pipeline.handlers.waiting.sessions.SessionInitWaitingForHandler
import net.corda.flow.pipeline.impl.FlowEventProcessorImpl
import net.corda.flow.pipeline.impl.FlowGlobalPostProcessorImpl
import net.corda.flow.pipeline.sandbox.FlowSandboxService
import net.corda.flow.pipeline.sessions.FlowSessionManagerImpl
import net.corda.flow.state.impl.FlowCheckpointFactoryImpl
import net.corda.libs.configuration.SmartConfigFactory
import net.corda.messaging.api.processor.StateAndEventProcessor
import net.corda.messaging.api.records.Record
import net.corda.sandboxgroupcontext.SandboxGroupContext
import net.corda.schema.Schemas
import net.corda.schema.configuration.FlowConfig
import net.corda.session.manager.impl.SessionManagerImpl
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.nio.ByteBuffer
import java.time.Instant
import java.util.UUID

fun flowEventDSL(dsl: FlowEventDSL.() -> Unit) {
    FlowEventDSL().run(dsl)
}

class FlowEventDSL {

    private val flowsToLastContinuations = mutableMapOf<String, FlowContinuation>()

    private val flowRunner = MockFlowRunner()
    private val flowEventPipelineFactory = FlowEventPipelineFactoryImpl(
        flowRunner,
        flowGlobalPostProcessor,
        FlowCheckpointFactoryImpl(),
        flowEventHandlers,
        flowWaitingForHandlers,
        flowRequestHandlers
    ).apply {
        registerRunOrContinueCallback { flowId, flowContinuation -> flowsToLastContinuations[flowId] = flowContinuation }
    }
    private val processor = FlowEventProcessorImpl(flowEventPipelineFactory, testSmartConfig)

    class PipelineOutput(val response: StateAndEventProcessor.Response<Checkpoint>, val lastContinuation: FlowContinuation)

    private var checkpoints = mutableMapOf<String, Checkpoint>()

    fun input(event: FlowEvent): PipelineOutput {
        val flowId = event.flowId
        val response = processor.onNext(
            state = checkpoints[flowId],
            event = Record(Schemas.Flow.FLOW_EVENT_TOPIC, event.flowId, event)
        )
        updateDSLStateWithEventResponse(flowId, response)
        return PipelineOutput(response, flowsToLastContinuations[flowId]!!)
    }

    fun input(event: FlowEvent, nextSuspension: FlowIORequest<*>): PipelineOutput {
        val flowId = event.flowId
        setNextSuspension(flowId, nextSuspension)
        val response = processor.onNext(
            state = checkpoints[flowId],
            event = Record(Schemas.Flow.FLOW_EVENT_TOPIC, event.flowId, event)
        )
        updateDSLStateWithEventResponse(flowId, response)
        return PipelineOutput(response, flowsToLastContinuations[flowId]!!)
    }

    fun startFlow(flowId: String = UUID.randomUUID().toString()): String {
        val startRPCFlowPayload = StartFlow.newBuilder()
            .setStartContext(getBasicFlowStartContext())
            .setFlowStartArgs(" { \"json\": \"args\" }")
            .build()
        input(FlowEvent(flowId, startRPCFlowPayload), FlowIORequest.ForceCheckpoint)
        return flowId
    }

    private fun setNextSuspension(flowId: String, nextSuspension: FlowIORequest<*>) {
        flowRunner.setNextSuspension(
            flowId,
            nextSuspension = when (nextSuspension) {
                is FlowIORequest.FlowFinished -> nextSuspension
                is FlowIORequest.FlowFailed -> nextSuspension
                else -> FlowIORequest.FlowSuspended(ByteBuffer.wrap(byteArrayOf(0)), nextSuspension)
            }
        )
    }

    private fun updateDSLStateWithEventResponse(flowId: String, response: StateAndEventProcessor.Response<Checkpoint>) {
        response.updatedState?.let { checkpoint -> checkpoints[flowId] = checkpoint } ?: checkpoints.remove(flowId)
    }
}

private val sessionManager = SessionManagerImpl()
private val flowRecordFactory = FlowRecordFactoryImpl()
private val flowMessageFactory = FlowMessageFactoryImpl { Instant.now() }

private val flowSessionManager = FlowSessionManagerImpl(sessionManager)

private val flowGlobalPostProcessor = FlowGlobalPostProcessorImpl(sessionManager, flowRecordFactory)

private val sandboxGroupContext = mock<SandboxGroupContext>()

private val flowSandboxService = mock<FlowSandboxService>().apply {
    whenever(get(any())).thenReturn(sandboxGroupContext)
}

// Must be updated when new flow event handlers are added
private val flowEventHandlers = listOf(
    SessionEventHandler(flowSandboxService, sessionManager),
    StartFlowEventHandler(),
    WakeupEventHandler(),
)

private val flowWaitingForHandlers = listOf(
    StartFlowWaitingForHandler(),
    WakeupWaitingForHandler(),
    SessionConfirmationWaitingForHandler(flowSessionManager),
    SessionDataWaitingForHandler(flowSessionManager),
    SessionInitWaitingForHandler(sessionManager)
)

// Must be updated when new flow request handlers are added
private val flowRequestHandlers = listOf(
    CloseSessionsRequestHandler(flowSessionManager, flowRecordFactory),
    FlowFailedRequestHandler(flowMessageFactory, flowRecordFactory),
    FlowFinishedRequestHandler(flowMessageFactory, flowRecordFactory),
    ForceCheckpointRequestHandler(flowRecordFactory),
    GetFlowInfoRequestHandler(),
    InitialCheckpointRequestHandler(flowMessageFactory, flowRecordFactory),
    InitiateFlowRequestHandler(flowSessionManager),
    ReceiveRequestHandler(flowSessionManager, flowRecordFactory),
    SendAndReceiveRequestHandler(flowSessionManager, flowRecordFactory),
    SendRequestHandler(flowSessionManager, flowRecordFactory),
    SleepRequestHandler(),
    SubFlowFailedRequestHandler(),
    SubFlowFinishedRequestHandler(flowSessionManager, flowRecordFactory),
    WaitForSessionConfirmationsRequestHandler()
)

private val testConfig = ConfigFactory.empty()
    .withValue(FlowConfig.SESSION_MESSAGE_RESEND_WINDOW, ConfigValueFactory.fromAnyRef(500000L))
    .withValue(FlowConfig.SESSION_HEARTBEAT_TIMEOUT_WINDOW, ConfigValueFactory.fromAnyRef(500000L))
private val configFactory = SmartConfigFactory.create(testConfig)
private val testSmartConfig = configFactory.create(testConfig)