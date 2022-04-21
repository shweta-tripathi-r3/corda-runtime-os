package net.corda.flow.pipeline

import net.corda.data.flow.FlowStackItem
import net.corda.data.flow.event.Wakeup
import net.corda.flow.acceptance.dsl.filterOutputFlowTopicEventPayloads
import net.corda.flow.acceptance.dsl.flowEventDSL
import net.corda.flow.fiber.FlowIORequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
/*

Session tests
-------------

{Given} {when} {expect}

- Sending
    - (Send) Calling 'send' on an initiated session sends a session data event and schedules a wakeup event to resume the flow
    - (Send) Calling 'send' on a closed session schedules an error event (not fully implemented, assert CLOSING, CLOSED, WAIT_FOR_FINAL_ACK states)
    - (Send) Calling 'send' multiple times on an initiated session resumes the flow and sends a session data event each time

- Send + receiving
    - (SendAndReceive) Calling 'sendAndReceive` on an initiated session sends a session data event
    - (SendAndReceive) Calling 'sendAndReceive` on a closed session schedules an error event

- Receiving (can use parameterised tests to assert the same behaviour for `sendAndReceive`)
    - (Receive) Receiving an out-of-order message does not resume the flow and sends a session ack
    - (Receive) Receiving a wakeup event does not resume the flow and resends any unacknowledged events (any non session event?)
    - (Receive) Receiving all session data events for closing sessions resumes the flow and sends a session ack (not fully implemented)
    - (Receive) Given two sessions have already received their session data events when the flow calls 'receive' for both sessions at once the flow should schedule a wakeup event
    - (Receive) Given two sessions have already received their session data events when the flow calls 'receive' for each session individually the flow should schedule a wakeup event
    - (Receive) Receiving a session data event for an unrelated session does not resume the flow and sends a session ack
    - (Receive) Receiving a session close event for an unrelated session does not resume the flow and sends a session ack
    - (Receive) Receiving a single session data event does not resume the flow and sends a session ack
    - (Receive) Receiving all session data events resumes the flow and sends session acks
    - (Receive) Given two sessions receiving a session close event for one resumes the flow with an error (not fully implemented, assert WAIT_FOR_FINAL_ACK session as well)
    - (Receive) Given two sessions receiving a session data event for one and a close for another resumes the flow with an error
    - (Receive) Given two sessions have already received their session data events when the flow calls 'receive' for the sessions the flow should resume
    - (Any-non-receive-request-type) Receiving session data events does not resume the flow and send session acks
    - (Any-non-receive-request-type) Receiving session close events does not resume the flow and send session acks (can this be combined with the test above?)

- Closing
    - (CloseSessions) Given sessions have states of 'WAIT_FOR_FINAL_ACK' receiving all session acks resumes the flow (is this correct, not sure sequence of events to get here)
    - (CloseSessions) Receiving an out-of-order message does not resume the flow and sends a session ack
    - (CloseSessions) Calling 'close' on initiated sessions sends session close events
    - (CloseSessions) Calling 'close' on an initiated and closed session sends a session close event to the initiated session
    - (CloseSessions) Calling 'close' on locally closed sessions schedules a wakeup event to resume the flow
    - (CloseSessions) Calling 'close' on sessions closed by peers sends session close events
    - (CloseSessions) Receiving a single session close event does not resume the flow and sends a session ack
    - (CloseSessions) Receiving all session close events resumes the flow and sends session acks
    - (CloseSessions) Receiving a session close event for one session and a data for another resumes the flow with an error
    - (CloseSessions) Receiving a wakeup event does not resume the flow and resends any unacknowledged events
    - (CloseSessions) Given two sessions where one has already received a session close event calling `close` and then receiving a session close event for the other session resumes the flow and sends a session ack
    - (CloseSessions) Given two sessions have already received their session close events calling 'close' for the sessions should schedule a wakeup event

- SubFlow session closing
    - (SubFlowFinished) Given a subFlow contains only initiated sessions when the subFlow finishes session closes are sent
    - (SubFlowFinished) Given a subFlow contains an initiated and closed session when the subFlow finishes a single session close is sent
    - (SubFlowFinished) Given a subFlow contains only closed sessions when the subFlow finishes a wakeup event is scheduled to resume the flow
    == Tests below are the same as (CloseSessions) but with a different request type ==
    - (SubFlowFinished) Receiving a single session close event does not resume the flow and sends a session ack
    - (SubFlowFinished) Receiving all session close events resumes the flow and sends session acks
    - (SubFlowFinished) Receiving a session close event for one session and a data for another resumes the flow with an error
    - (SubFlowFinished) Given two sessions where one has already received a session close event receiving a session close event for the other session resumes the flow and sends a session ack
    - (SubFlowFinished) Given two sessions have already received their session close events when the flow calls 'close' for the sessions the flow should resume

Wakeup tests

- waiting for a wakeup and receiving a wakeup - triggered by force checkpoint
- waiting for a wakeup and receiving a session message - triggered by force checkpoint

Feels like I need to set up the fiber for some of my tests because its easier than putting the starting state into the correct state for the
test case. Is there a way to write the DSL to make this easier? Really need a way to assert this better.

Need to set up flow stack (should this be part of a handlers' logic?) - Requires the [MockFlowRunner] to be expanded to have logic similar
to the real implementation...

 */
//class FlowEventAcceptanceTest {
//
//    @Test
//    fun `forcing a checkpoint schedules a wakeup event`() {
//        flowEventDSL {
//
//            startedFlowFiber {
//                repeatSuspension(FlowIORequest.ForceCheckpoint, 1)
//            }
//
//            inputLastOutputEvent()
//
//            val response = processOne()
//            val outputPayloads = response.filterOutputFlowTopicEventPayloads()
//
//            assertEquals(net.corda.data.flow.state.waiting.Wakeup(), response.updatedState?.flowState?.waitingFor?.value)
//            assertEquals(1, outputPayloads.size)
//            assertTrue(outputPayloads.single() is Wakeup)
//        }
//    }
//
//    @Test
//    fun `finishing a subFlow suspends the flow and schedules a wakeup event`() {
//        flowEventDSL {
//
//            startedFlowFiber {
//                queueSuspension(FlowIORequest.SubFlowFinished(FlowStackItem()))
//            }
//
//            inputLastOutputEvent()
//
//            val response = processOne()
//            val outputPayloads = response.filterOutputFlowTopicEventPayloads()
//
//            assertEquals(net.corda.data.flow.state.waiting.Wakeup(), response.updatedState?.flowState?.waitingFor?.value)
//            assertEquals(1, outputPayloads.size)
//            assertTrue(outputPayloads.single() is Wakeup)
//        }
//    }
//}