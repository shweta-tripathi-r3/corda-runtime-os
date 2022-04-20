package net.corda.flow.pipeline

import net.corda.data.flow.FlowStackItem
import net.corda.data.flow.event.FlowEvent
import net.corda.data.flow.event.MessageDirection
import net.corda.data.flow.event.SessionEvent
import net.corda.data.flow.event.Wakeup
import net.corda.data.flow.event.mapper.FlowMapperEvent
import net.corda.data.flow.event.session.SessionAck
import net.corda.data.flow.event.session.SessionClose
import net.corda.data.flow.event.session.SessionData
import net.corda.data.flow.event.session.SessionError
import net.corda.flow.ALICE_X500_HOLDING_IDENTITY
import net.corda.flow.ALICE_X500_NAME
import net.corda.flow.BOB_X500_HOLDING_IDENTITY
import net.corda.flow.BOB_X500_NAME
import net.corda.flow.acceptance.dsl.FlowEventDSL
import net.corda.flow.acceptance.dsl.flowEventDSL
import net.corda.flow.fiber.FlowContinuation
import net.corda.flow.fiber.FlowIORequest
import net.corda.test.flow.util.buildSessionEvent
import net.corda.v5.application.flows.exceptions.FlowException
import net.corda.v5.base.exceptions.CordaRuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer
import java.util.stream.Stream

class SessionCloseAcceptanceTest {

    private companion object {
        
        const val SESSION_ID = "session id"
        const val ANOTHER_SESSION_ID = "another session id"
        val FLOW_STACK_ITEM = FlowStackItem("flow name", false, listOf())

        @JvmStatic
        fun flowIORequests(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(FlowIORequest.ForceCheckpoint),
                Arguments.of(FlowIORequest.InitialCheckpoint),
                Arguments.of(FlowIORequest.CloseSessions(setOf(SESSION_ID))),
                Arguments.of(FlowIORequest.InitiateFlow(ALICE_X500_NAME, SESSION_ID)),
                Arguments.of(FlowIORequest.Send(mapOf(SESSION_ID to byteArrayOf(1)))),
                Arguments.of(FlowIORequest.SendAndReceive(mapOf(SESSION_ID to byteArrayOf(1)))),
                Arguments.of(FlowIORequest.Receive(setOf(SESSION_ID))),
                Arguments.of(FlowIORequest.SubFlowFinished(FLOW_STACK_ITEM)),
                Arguments.of(FlowIORequest.SubFlowFailed(RuntimeException(), FLOW_STACK_ITEM)),
            )
        }

        @JvmStatic
        fun flowIORequestsExcludingSessionRelatedRequests(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(FlowIORequest.ForceCheckpoint),
                Arguments.of(FlowIORequest.InitialCheckpoint),
                Arguments.of(FlowIORequest.SubFlowFinished(FLOW_STACK_ITEM)),
                Arguments.of(FlowIORequest.SubFlowFailed(RuntimeException(), FLOW_STACK_ITEM)),
            )
        }
    }

    @ParameterizedTest(name = "(CloseSessions) Receiving a session close resumes the flow and outputs a session ack after suspending with a {0}")
    @MethodSource("flowIORequestsExcludingSessionRelatedRequests")
    fun `(CloseSessions) Receiving a session close resumes the flow and outputs a session ack`(flowIORequest: FlowIORequest<*>) {
        flowEventDSL {

            val flowId = startFlow()

            input(FlowEvent(flowId, Wakeup()), nextSuspension = FlowIORequest.InitiateFlow(BOB_X500_NAME, SESSION_ID))

            input(
                flowId,
                buildSessionEvent(
                    MessageDirection.INBOUND,
                    SESSION_ID,
                    1,
                    SessionAck(),
                    1,
                    initiatingIdentity = ALICE_X500_HOLDING_IDENTITY,
                    initiatedIdentity = BOB_X500_HOLDING_IDENTITY
                ),
                nextSuspension = FlowIORequest.CloseSessions(setOf(SESSION_ID))
            )

            val output = input(
                flowId,
                buildSessionEvent(
                    MessageDirection.INBOUND,
                    SESSION_ID,
                    2,
                    SessionClose(),
                    2,
                    initiatingIdentity = ALICE_X500_HOLDING_IDENTITY,
                    initiatedIdentity = BOB_X500_HOLDING_IDENTITY
                ),
                nextSuspension = flowIORequest
            )

            output
                .flowResumedWithValue(Unit)
                .containsRecords(sessionEvent<SessionAck>())

        }
    }

    // this scenario is not implemented yet
    @Disabled
    @Test
    fun `(CloseSessions) Receiving a session data resumes the flow with an error and outputs a session error`() {
        flowEventDSL {

            val flowId = startFlow()

            input(FlowEvent(flowId, Wakeup()), nextSuspension = FlowIORequest.InitiateFlow(BOB_X500_NAME, SESSION_ID))

            input(
                flowId,
                buildSessionEvent(
                    MessageDirection.INBOUND,
                    SESSION_ID,
                    1,
                    SessionAck(),
                    1,
                    initiatingIdentity = ALICE_X500_HOLDING_IDENTITY,
                    initiatedIdentity = BOB_X500_HOLDING_IDENTITY
                ),
                nextSuspension = FlowIORequest.CloseSessions(setOf(SESSION_ID))
            )

            val output = input(
                flowId,
                buildSessionEvent(
                    MessageDirection.INBOUND,
                    SESSION_ID,
                    2,
                    SessionData(ByteBuffer.wrap(byteArrayOf(0))),
                    2,
                    initiatingIdentity = ALICE_X500_HOLDING_IDENTITY,
                    initiatedIdentity = BOB_X500_HOLDING_IDENTITY
                ),
                nextSuspension = FlowIORequest.ForceCheckpoint
            )

            output
                .flowResumedWithError<FlowException>()
                .containsRecords(flowEvent<Wakeup>(), sessionEvent<SessionError>(), sessionEvent<SessionAck>())
        }
    }

    @Test
    fun `(CloseSessions) Receiving session events for unrelated sessions does not resume the flow and sends session acks`() {
        flowEventDSL {

            val flowId = startFlow()

            input(FlowEvent(flowId, Wakeup()), nextSuspension = FlowIORequest.InitiateFlow(BOB_X500_NAME, SESSION_ID))

            input(
                flowId,
                buildSessionEvent(
                    MessageDirection.INBOUND,
                    SESSION_ID,
                    1,
                    SessionAck(),
                    1,
                    initiatingIdentity = ALICE_X500_HOLDING_IDENTITY,
                    initiatedIdentity = BOB_X500_HOLDING_IDENTITY
                ),
                nextSuspension = FlowIORequest.InitiateFlow(BOB_X500_NAME, ANOTHER_SESSION_ID)
            )

            input(
                flowId,
                buildSessionEvent(
                    MessageDirection.INBOUND,
                    ANOTHER_SESSION_ID,
                    1,
                    SessionAck(),
                    1,
                    initiatingIdentity = ALICE_X500_HOLDING_IDENTITY,
                    initiatedIdentity = BOB_X500_HOLDING_IDENTITY
                ),
                nextSuspension = FlowIORequest.CloseSessions(setOf(SESSION_ID))
            )

            val output = input(
                flowId,
                buildSessionEvent(
                    MessageDirection.INBOUND,
                    ANOTHER_SESSION_ID,
                    2,
                    SessionData(ByteBuffer.wrap(byteArrayOf(0))),
                    2,
                    initiatingIdentity = ALICE_X500_HOLDING_IDENTITY,
                    initiatedIdentity = BOB_X500_HOLDING_IDENTITY
                )
            )

            output
                .flowDidNotResume()
                .containsRecords(sessionEvent<SessionAck>())
        }
    }

    private fun FlowEventDSL.input(
        flowId: String,
        sessionEvent: SessionEvent,
        nextSuspension: FlowIORequest<*>
    ): FlowEventDSL.PipelineOutput {
        return input(FlowEvent(flowId, sessionEvent), nextSuspension)
    }

    private fun FlowEventDSL.input(flowId: String, sessionEvent: SessionEvent): FlowEventDSL.PipelineOutput {
        return input(FlowEvent(flowId, sessionEvent))
    }

    private fun FlowEventDSL.PipelineOutput.flowDidNotResume(): FlowEventDSL.PipelineOutput {
        assertThat(lastContinuation).isExactlyInstanceOf(FlowContinuation.Continue::class.java)
        return this
    }

    private fun FlowEventDSL.PipelineOutput.flowResumedWithValue(value: Any?): FlowEventDSL.PipelineOutput {
        assertThat(lastContinuation).isExactlyInstanceOf(FlowContinuation.Run::class.java)
        assertThat((lastContinuation as FlowContinuation.Run).value).isEqualTo(value)
        return this
    }

    private inline fun <reified T: Throwable> FlowEventDSL.PipelineOutput.flowResumedWithError(): FlowEventDSL.PipelineOutput {
        assertThat(lastContinuation).isExactlyInstanceOf(FlowContinuation.Error::class.java)
        assertThat((lastContinuation as FlowContinuation.Error).exception).isEqualTo(T::class.java)
        return this
    }

    private fun FlowEventDSL.PipelineOutput.containsRecords(vararg recordTypes: RecordAssertion): FlowEventDSL.PipelineOutput {
        val assertThat = assertThat(response.responseEvents.map { it.value })
        for (type in recordTypes) {
            assertThat.anySatisfy { type.assertThat(it) }
        }
        return this
    }

    class RecordAssertion(val assertThat: (Any?) -> ObjectAssert<*>)

    private inline fun <reified T> flowEvent(): RecordAssertion {
        return RecordAssertion {
            assertThat(it).isInstanceOf(FlowEvent::class.java)
            assertThat((it as FlowEvent).payload).isInstanceOf(T::class.java)
        }
    }

    private inline fun <reified T> sessionEvent(): RecordAssertion {
        return RecordAssertion {
            assertThat(it).isInstanceOf(FlowMapperEvent::class.java)
            assertThat((it as FlowMapperEvent).payload).isInstanceOf(SessionEvent::class.java)
            assertThat((it.payload as SessionEvent).payload).isInstanceOf(T::class.java)
        }
    }
}