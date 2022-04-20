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
    - (Send) Calling 'send' on an initiated and sends a session data event and schedules a wakeup event to resume the flow
    - (Send) Calling 'send' on a closed session schedules an error event (not fully implemented, assert CLOSING, CLOSED, WAIT_FOR_FINAL_ACK states)
    - (Send) Calling 'send' multiple times on an initiated session resumes the flow and sends a session data event each time

- Send + receiving
    - (SendAndReceive) Calling 'sendAndReceive` on an initiated session sends a session data event
    - (SendAndReceive) Calling 'sendAndReceive` on a closed session schedules an error event

- Receiving (can use parameterised tests to assert the same behaviour for `sendAndReceive`)
    - (Receive) Receiving a session data event resumes the flow and sends a session ack
    - (Receive) Receiving a session data event for a closing session resumes the flow and sends a session ack (not fully implemented)
    - (Receive) Receiving a session data event for a closed session resumes the flow with an error (not fully implemented, assert WAIT_FOR_FINAL_ACK session as well)
    - (Receive) Receiving an out-of-order message does not resume the flow and sends a session ack
    - (Receive) Receiving a wakeup event while waiting to receive does not resume the flow and resends any unacknowledged events (any non session event?)
    - (Receive) Receiving a session close does not resume the flow and sends a session ack
    - (Receive) Receiving a session data event for an unrelated session does not resume the flow and sends a session ack
    - (Receive) Given a session has already received a session data event when the flow calls 'receive' for that session it should resume the flow
    - (Receive) Receiving a session close event for an unrelated session does not resume the flow and sends a session ack
    - (Receive) Given a session has already received a session close event when the flow calls 'close' for that session it should resume the flow
    - (Receive-multiple) Receiving a single session data event does not resume the flow and sends a session ack
    - (Receive-multiple) Receiving all session data events resumes the flow and sends session acks
    - (Receive-multiple) Receiving a session data event for one session and a close for another resumes the flow with an error
    - (Any-non-receive-request-type) Receiving session data events does not resume the flow and send session acks
    - (Any-non-receive-request-type) Receiving session close events does not resume the flow and send session acks (can this be combined with the test above?)
    - Communicating with other sessions and receiving data from peer which the flow then suspends to receive from

- Closing
    - Closing an initiated session
    - Closing a closed session - closed by local session
    - Closing a closed session - closed by peer session
    - Closing an uninitiated session (not possible because of code in the fiber)?
    - Receiving an ack?
    - Receiving an out-of-order message
    - Receiving a close message in response
    - Receiving a data message in response
    - Receiving a wakeup event while waiting for close (could be any non session data event really)
    - Closing multiple sessions at once
    - Closing multiple sessions at once with one already closed
    - Closing multiple closed sessions at once
    - Sessions closed when subflow finishes
    - Receiving a data message for an unrelated session while waiting for some sessions to close
    - Receiving a close message for an unrelated session while waiting for some sessions to close

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